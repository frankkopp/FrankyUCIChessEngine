/*
 * MIT License
 *
 * Copyright (c) 2018 Frank Kopp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fko.FrankyEngine.Franky;

import fko.FrankyEngine.Franky.TranspositionTable.TT_Entry;
import fko.FrankyEngine.Franky.TranspositionTable.TT_EntryType;
import fko.FrankyEngine.Franky.openingbook.OpeningBook;
import fko.FrankyEngine.Franky.openingbook.OpeningBookImpl;
import fko.UCI.IUCIEngine;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.SearchResult;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Search implements the actual search for best move of a given position.
 * <p>
 * Search runs in a separate thread when the actual search is started. When
 * the search is finished it calls <code>engine.sendResult</code> ith the best move and a ponder
 * move if it has one.
 * <p>
 * TODO: Improve ASPIRATION WINDOWS
 * TODO: SEE (https://www.chessprogramming.org/Static_Exchange_Evaluation)
 * TODO: More extensions and reductions
 * TODO: Better time management - dynamic adjustments
 *       When iteration has stable best move
 *       Obvious replies
 *       Big fail low on evaluation between iterations
 * TODO: Use fail-soft and test
 */
public class Search implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Search.class);

  private static final boolean TRACE = false;

  private static final int UCI_UPDATE_INTERVAL = 500;

  /** Maximum depth this search can go. */
  public static final int MAX_SEARCH_DEPTH = Byte.MAX_VALUE;

  // Readability constants
  private static final boolean DO_NULL    = true;
  private static final boolean NO_NULL    = false;
  private static final boolean PV_NODE    = true;
  private static final boolean NPV_NODE   = false;
  private static final int     DEPTH_NONE = 0;
  private static final int     ROOT_PLY   = 0;

  /** Configuration object for direct manipulation */
  public final Configuration config;

  // search counters
  private final SearchCounter searchCounter;

  // back reference to the engine
  private final IUCIEngine engine;

  // the thread in which we will do the actual search
  private Thread searchThread = null;

  // used to wait for move from search
  private CountDownLatch waitForInitializationLatch;

  // flag to indicate to stop the search - can be called externally or via the timer clock.
  private boolean stopSearch = true;

  // opening book
  private final OpeningBook book;

  // Position Evaluator
  private final Evaluation evaluator;

  // running search global variables
  private final RootMoveList rootMoves;

  // current variation of the search
  private final MoveList currentVariation;

  // Move Generators - each depth in search gets it own to avoid object creation during search
  private final MoveGenerator[] moveGenerators;
  // to store the best move or principal variation we need to generate the move sequence backwards
  private final MoveList[]      principalVariation;
  // in the recursion. This field stores the pv for each ply so far
  // killer move lists per ply
  private       MoveList[]      killerMoves;
  // remember if there have been mate threads in a ply
  private       boolean[]       mateThreat;
  // remember if this ply only has one move to play
  private       boolean[]       singleReply;

  // hash tables
  private TranspositionTable transpositionTable;

  // time variables
  private long startTime;
  private long stopTime;
  private long hardTimeLimit;
  private long softTimeLimit;

  // search state - valid for one call to startSearch
  private Position     currentPosition;
  private Color        myColor;
  private SearchMode   searchMode;
  private SearchResult lastSearchResult;
  private long         uciUpdateTicker;
  private boolean      hadBookMove = false;

  /**
   * Creates a search object and stores a back reference to the engine object and also the
   * used configuration instance.<br>
   *
   * @param engine
   * @param config
   */
  public Search(IUCIEngine engine, Configuration config) {
    this.engine = engine;
    this.config = config;

    waitForInitializationLatch = new CountDownLatch(1);

    // initialize ply variables
    currentVariation = new MoveList(MAX_SEARCH_DEPTH);
    moveGenerators = new MoveGenerator[MAX_SEARCH_DEPTH];
    principalVariation = new MoveList[MAX_SEARCH_DEPTH];
    killerMoves = new MoveList[MAX_SEARCH_DEPTH];
    mateThreat = new boolean[MAX_SEARCH_DEPTH];
    singleReply = new boolean[MAX_SEARCH_DEPTH];

    // initialize search variables
    rootMoves = new RootMoveList();
    searchCounter = new SearchCounter();
    evaluator = new Evaluation();

    // set opening book - will be initialized in each search
    this.book = new OpeningBookImpl(config.OB_FolderPath + config.OB_fileNamePlain, config.OB_Mode);

    // set hash sizes
    setHashSize(this.config.HASH_SIZE);
  }

  /**
   * Start the search in a separate thread.<br>
   * The search will stop when it has reach the configured conditions. Either
   * reached a certain depth oder used up the time or found a move.<br>
   * The search also can be stopped by calling stop at any time. The
   * search will stop gracefully by storing the best move so far.
   * <p>
   * The finished search calls <code>Engine.sendResult(searchResult);</code> to
   * store the result is it has found one. After storing the result
   * the search is ended and the thread terminated.<br>
   *
   * @param position
   * @param searchMode
   */
  public void startSearch(Position position, SearchMode searchMode) {
    if (searchThread != null && searchThread.isAlive()) {
      final String s = "Search already running - can only be started once";
      IllegalStateException e = new IllegalStateException(s);
      LOG.error(s, e);
    }

    // create a deep copy of the position to do not change
    // the original position given
    this.currentPosition = new Position(position);

    this.myColor = currentPosition.getNextPlayer();
    this.searchMode = searchMode;

    // setup latch - used to wait until run() has finished initialization
    waitForInitializationLatch = new CountDownLatch(1);

    // reset the stop search flag
    stopSearch = false;

    // create new search thread and start it
    String threadName = "Engine: " + myColor.toString();
    if (this.searchMode.isPonder()) {
      threadName += " (Pondering)";
    }
    searchThread = new Thread(this, threadName);
    searchThread.setDaemon(true);
    searchThread.setUncaughtExceptionHandler((t, e) -> {
      LOG.error("Caught uncaught exception", e);
      System.exit(1);
    });
    searchThread.start();

    // Wait for initialization in run() before returning from call
    try {
      waitForInitializationLatch.await();
    } catch (InterruptedException ignored) {
    }
  }

  /**
   * Stops a current search. If no search is running it does nothing.<br>
   * The search will stop gracefully by sending the best move so far
   */
  public void stopSearch() {
    // return if no search is running
    if (searchThread == null) return;

    // stop pondering if we are
    if (searchMode.isPonder()) {
      if (searchThread == null || !searchThread.isAlive()) {
        // Ponder search has finished before we stopped it
        // Per UCI protocoll we need to send the result anyway although a miss
        LOG.info(
          "Pondering has been stopped after ponder search has finished. " + "Send obsolete result");
        LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
                 principalVariation[ROOT_PLY].toNotationString());
        sendUCIBestMove();
      } else {
        LOG.info("Pondering has been stopped. Ponder Miss!");
      }
      searchMode.ponderStop();
    }

    // set stop flag - search needs to check regularly and stop accordingly
    stopSearch = true;

    // Wait for the thread to die
    try {
      this.searchThread.join();
    } catch (InterruptedException ignored) {
    }

    // clear thread
    searchThread = null;

    LOG.info("Search thread has been stopped");
  }

  /**
   * Called when the new search thread is started.
   * Initializes the search and checks the opening book if required.
   * Calls <code>iterativeDeepening()</code> when search is initialized.
   * <p>
   * The finished search calls <code>Engine.sendResult(searchResult)</code> to
   * store the result is it has found one. After storing the result
   * the search is ended and the thread terminated.<br>
   */
  @Override
  public void run() {

    if (Thread.currentThread() != searchThread) {
      final String s = "run() cannot be called directly!";
      UnsupportedOperationException e = new UnsupportedOperationException(s);
      LOG.error(s, e);
    }

    // Initialize for new search
    lastSearchResult = null;
    searchCounter.resetCounter();
    softTimeLimit = hardTimeLimit = 0;

    // Initialize ply based data
    // Each depth in search gets it own global field to avoid object creation
    // during search.
    for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
      moveGenerators[i] = new MoveGenerator();
      moveGenerators[i].SORT_MOVES = config.USE_SORT_ALL_MOVES;
      // prepare principal variation lists
      principalVariation[i] = new MoveList(MAX_SEARCH_DEPTH);
      // init killer moves
      killerMoves[i] = new MoveList(config.NO_KILLER_MOVES + 1);
      // mateThreads
      mateThreat[i] = false;
      // singleReply
      singleReply[i] = false;
    }

    // age TT entries
    transpositionTable.ageEntries();

    // print info about search mode
    if (isPerftSearch()) {
      LOG.info("****** PERFT SEARCH (" + searchMode.getMaxDepth() + ") *******");
    }
    if (searchMode.isTimeControl() && searchMode.getMate() > 0) {
      LOG.info("****** TIMED MATE SEARCH *******");
    }
    if (searchMode.isTimeControl() && searchMode.getMate() <= 0) {
      LOG.info("****** TIMED SEARCH *******");
    }
    if (searchMode.isPonder()) {
      LOG.info("****** PONDER SEARCH *******");
    }
    if (searchMode.isInfinite()) {
      LOG.info("****** INFINITE SEARCH *******");
    }
    if (searchMode.getMate() > 0) {
      LOG.info("****** MATE SEARCH (time: {} max depth {}) ) *******", searchMode.getMoveTime(),
               searchMode.getMaxDepth());
    }

    // release latch so the caller can continue
    waitForInitializationLatch.countDown();

    // try to get book move
    lastSearchResult = getBookMove(currentPosition);

    // if we did get a book move start the search
    if (lastSearchResult == null) {
      if (TRACE) trace("START search for %s", currentPosition.toFENString());
      lastSearchResult = iterativeDeepening(currentPosition);
    }
    assert lastSearchResult != null;

    // if the mode still is ponder at this point we finished the ponder
    // search early before a miss or hit has been signaled. We need to
    // wait with sending the result until we get a miss (stop) or a hit.
    if (searchMode.isPonder()) {
      LOG.info("Ponder Search finished! Waiting for Ponderhit to send result");
      return;
    }

    LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
             principalVariation[ROOT_PLY].toNotationString());

    // send result to engine
    sendUCIBestMove();
  }

  /**
   * Generates root moves and starts the actual iterative search by calling the
   * root moves search <code>rootMovesSearch()</code>.
   * <p>
   * Detects mate if started on a mate position.
   *
   * @param position
   * @return search result
   */
  private SearchResult iterativeDeepening(Position position) {
    if (TRACE) trace("Iterative deepening start");

    // prepare search result
    SearchResult searchResult = new SearchResult();

    // no legal root moves - game already ended!
    if (!moveGenerators[ROOT_PLY].hasLegalMove(position)) {
      if (position.hasCheck()) {
        searchResult.resultValue = -Evaluation.CHECKMATE;
      } else {
        searchResult.resultValue = Evaluation.DRAW;
      }
      return searchResult;
    }

    // remember the start of the search
    startTime = System.currentTimeMillis();
    uciUpdateTicker = System.currentTimeMillis();

    // max window search - preparation for aspiration window search
    final int alpha = Evaluation.MIN;
    final int beta = Evaluation.MAX;

    // for fixed depth searches we start at the final depth directly
    // no iterative deepening
    int depth = searchMode.getStartDepth();

    // if time based game setup the time soft and hard time limits
    if (searchMode.isTimeControl()) {
      configureTimeLimits();
    }

    // add some extra time for the move after the last book move
    if (hadBookMove) {
      double extraTimeFactor = 2;
      LOG.debug("Last book move detected. Adding some extra time. Before: {} After: {}",
                hardTimeLimit, (long) (extraTimeFactor * hardTimeLimit));
      hadBookMove = false;
      timeLimitChange(extraTimeFactor);
    }

    // print search setup for debugging
    if (LOG.isDebugEnabled()) {
      LOG.debug("Searching in Position: {}", position.toFENString());
      LOG.debug("Searching these moves: {}", rootMoves.toString());
      LOG.debug("Search Mode: {}", searchMode.toString());
      LOG.debug("Time Management: {} soft: {} ms hard: {} ms",
                (searchMode.isTimeControl() ? "ON" : "OFF"), String.format("%,d", softTimeLimit),
                String.format("%,d", hardTimeLimit));
      LOG.debug("Start Depth: {}", depth);
      LOG.debug("Max Depth: {}", searchMode.getMaxDepth());
      LOG.debug("Start iterative deepening now");
    }

    // current search depth
    searchCounter.currentSearchDepth = ROOT_PLY;
    searchCounter.currentExtraSearchDepth = ROOT_PLY;

    // current best values
    assert searchCounter.currentBestRootValue == Evaluation.NOVALUE;
    assert searchCounter.currentBestRootMove == Move.NOMOVE;

    // clear principal Variation for root depth
    principalVariation[ROOT_PLY].clear();

    // Do a TT lookup to try to find a first best move for this position and
    // maybe even be able to skip some iteration when a valid cache hit has been
    // found.
    if (config.USE_TT_ROOT && config.USE_TRANSPOSITION_TABLE) {
      TTHit ttHit = probeTT(position, depth, alpha, beta, ROOT_PLY);
      if (ttHit != null) {
        mateThreat[ROOT_PLY] = ttHit.mateThreat;
        // determine pv moves from TT
        if (ttHit.bestMove != Move.NOMOVE) {
          searchCounter.currentBestRootMove = ttHit.bestMove;
          // get PV line from TT
          getPVLine(position, ttHit.depth, principalVariation[ROOT_PLY]);
          // update depth if we already searched these depths and have a value
          if (ttHit.value != Evaluation.NOVALUE && ttHit.type == TT_EntryType.EXACT) {
            searchCounter.currentBestRootValue = ttHit.value;
            if (ttHit.depth >= depth) {
              depth = ttHit.depth + 1;
              LOG.debug("TT cached result of depth {}. Start depth is now {}", ttHit.depth, depth);
              // send info to UCI to let the user know that we have a result for the cached depth
              engine.sendInfoToUCI(String.format("depth %d %s time %d pv %s", ttHit.depth,
                                                 getScoreString(searchCounter.currentBestRootValue),
                                                 elapsedTime(),
                                                 principalVariation[ROOT_PLY].toNotationString()));
            }
          }
        }
      }
    }

    // generate all legal root moves, and set pv move
    moveGenerators[ROOT_PLY].setPosition(position);
    if (config.USE_PVS_MOVE_ORDERING) {
      moveGenerators[ROOT_PLY].setPVMove(searchCounter.currentBestRootMove);
    }
    MoveList legalMoves = moveGenerators[ROOT_PLY].getLegalMoves(true);

    // filter the root move list according to the given UCI moves
    rootMoves.clear();
    for (int i = 0; i < legalMoves.size(); i++) {
      if (searchMode.getMoves().isEmpty()) {
        rootMoves.add(legalMoves.get(i), Evaluation.NOVALUE);
      } else {
        if (searchMode.getMoves().contains(Move.toUCINotation(position, legalMoves.get(i)))) {
          rootMoves.add(legalMoves.get(i), Evaluation.NOVALUE);
        }
      }
    }

    // single reply in root
    if (rootMoves.size() == 1) {
      singleReply[ROOT_PLY] = true;
      // reduce time for this move
      if (searchMode.isTimeControl()) {
        timeLimitChange(0.5);
      }
    } else {
      singleReply[ROOT_PLY] = false;
    }

    // ###########################################
    // ### BEGIN Iterative Deepening
    do {
      if (TRACE) trace("Depth %d start", depth);

      searchCounter.currentIterationDepth = depth;

      // @formatter:off
      int value;
      assert config.ASPIRATION_START_DEPTH > 1 : "ASPIRATION_START_DEPTH must be > 1";
      if (config.USE_ASPIRATION_WINDOW
          && depth >= config.ASPIRATION_START_DEPTH
          && !isPerftSearch()
          && searchCounter.currentBestRootValue != Evaluation.NOVALUE) {

        value = aspiration_search(position, depth);

      } else {

        value = rootMovesSearch(position, depth, Evaluation.MIN, Evaluation.MAX);

      }
      // @formatter:on

      assert value != Evaluation.NOVALUE;
      assert !principalVariation[ROOT_PLY].empty();

      // update the current best move and value
      searchCounter.currentBestRootMove = principalVariation[ROOT_PLY].getFirst();
      searchCounter.currentBestRootValue = value;

      sendUCIIterationEndInfo();

      // check if we need to stop search - could be external or time.
      if (stopSearch || softTimeLimitReached() || hardTimeLimitReached()) break;

      if (TRACE) trace("Depth %d end", depth);
    } while (++depth <= searchMode.getMaxDepth());
    // ### ENDOF Iterative Deepening
    // ###########################################

    // create searchResult here
    searchResult.bestMove = searchCounter.currentBestRootMove;
    searchResult.resultValue = searchCounter.currentBestRootValue;
    searchResult.depth = searchCounter.currentSearchDepth;
    searchResult.extraDepth = searchCounter.currentExtraSearchDepth;

    // retrieve ponder move from pv
    searchResult.ponderMove = Move.NOMOVE;
    if (principalVariation[ROOT_PLY].size() > 1
        && (principalVariation[ROOT_PLY].get(1)) != Move.NOMOVE) {
      searchResult.ponderMove = principalVariation[ROOT_PLY].get(1);
    }

    // search is finished - stop timer
    stopTime = System.currentTimeMillis();
    searchCounter.lastSearchTime = elapsedTime(stopTime);

    // print result of the search
    printSearchResultInfo();

    if (TRACE) trace("Iterative deepening end");
    return searchResult;
  }

  /**
   * Aspiration search works with the assumption that the value from previous searches will not
   * change too much and therefore the search can be tried with a narrow window for alpha and beta
   * around the previous value to cause more cut offs. If the result is at the edge or outside
   * (not possible in fail-hard) of our window, we try another search with a wider window. If this
   * also fails we fall back to a full window search.
   *
   * @param position
   * @param depth
   */
  private int aspiration_search(Position position, int depth) {
    if (TRACE) trace("Aspiration for depth %d: START", depth);

    // need to have a good guess for the score of the best move
    assert searchCounter.currentBestRootValue != Evaluation.NOVALUE;
    assert searchCounter.currentBestRootMove != Move.NOMOVE;
    assert !principalVariation[ROOT_PLY].empty();
    assert principalVariation[ROOT_PLY].getFirst() == searchCounter.currentBestRootMove;

    final int bestValue = searchCounter.currentBestRootValue;

    // 1st aspiration
    int alpha = Math.max(Evaluation.MIN, bestValue - 30);
    int beta = Math.min(Evaluation.MAX, bestValue + 30);

    if (TRACE) {
      trace("Aspiration for depth %d: START 1st window %d/%d (bestValue=%d)", depth, alpha, beta,
            bestValue);
    }

    int value = rootMovesSearch(position, depth, alpha, beta);

    if (stopSearch && (value <= alpha || value >= beta)) return bestValue;

    // 2nd aspiration
    // FAIL LOW - decrease lower bound
    if (value <= alpha) {
      if (TRACE) {
        trace("Aspiration for depth %d: FAIL_LOW 1st window %d/%d value=%d", depth, alpha, beta,
              value);
      }

      searchCounter.aspirationResearches++;

      // add some extra time because of fail low - we might have found strong opponents move
      timeLimitChange(1.5);

      alpha = Math.max(Evaluation.MIN, bestValue - 200);

      if (TRACE) trace("Aspiration for depth %d: START 2nd window %d/%d", depth, alpha, beta);
      value = rootMovesSearch(position, depth, alpha, beta);
    }

    // FAIL HIGH - increase upper bound
    else if (value >= beta) {
      if (TRACE) {
        trace("Aspiration for depth %d: FAIL-HIGH: 2nd window %d/%d value=%d ", depth, alpha, beta,
              value);
      }

      searchCounter.aspirationResearches++;
      beta = Math.min(Evaluation.MAX, bestValue + 200);

      if (TRACE) trace("Aspiration for depth %d: START 2nd window %d/%d", depth, alpha, beta);
      value = rootMovesSearch(position, depth, alpha, beta);
    }

    if (stopSearch && (value <= alpha || value >= beta)) return bestValue;

    // FAIL - full window search
    if (value <= alpha || value >= beta) {
      if (TRACE) {
        trace("Aspiration for depth %d: %s 2nd window %d/%d value=%d ",
              value <= alpha ? "FAIL-LOW" : "FAIL-HIGH", depth, alpha, beta, value);
      }
      searchCounter.aspirationResearches++;

      // add some extra time because of fail low - we might have found strong opponents move
      if (value <= alpha) timeLimitChange(1.5);

      alpha = Evaluation.MIN;
      beta = Evaluation.MAX;

      if (TRACE) trace("Aspiration for depth %d: START 3rd window %d/%d", depth, alpha, beta);
      value = rootMovesSearch(position, depth, alpha, beta);

    }

    if (TRACE) {
      trace("Aspiration for depth %d: END (Result %d in window %d/%d)", depth, value, alpha, beta);
    }

    return stopSearch ? bestValue : value;
  }

  /**
   * Performs the search on the root moves and calls the recursive search for each move.
   * This is basically a special version of the normal search.
   *
   * @param position
   * @param depth
   * @param alpha
   * @param beta
   */
  private int rootMovesSearch(Position position, int depth, int alpha, int beta) {
    if (TRACE) trace("Root Search for depth %d: START (alpha=%d beta==%d)", depth, alpha, beta);

    assert depth > 0 && depth <= MAX_SEARCH_DEPTH;
    assert alpha >= Evaluation.MIN && beta <= Evaluation.MAX;

    // root ply = 0
    final int ply = 0;

    // root node is always first searched node
    // update current search depth stats
    searchCounter.currentSearchDepth = Math.max(searchCounter.currentSearchDepth, ply);
    searchCounter.currentExtraSearchDepth = Math.max(searchCounter.currentExtraSearchDepth, ply);
    searchCounter.nodesVisited++;

    // check if root position is already draw
    if (position.check50Moves() || position.check3Repetitions()) {
      principalVariation[ROOT_PLY].clear();
      principalVariation[ROOT_PLY].add(rootMoves.getMove(0));
      return Evaluation.DRAW;
    }

    // needed to remember if we even had a legal move
    int numberOfSearchedMoves = 0;

    // Initialize best values
    int bestNodeValue = -Evaluation.INFINITE;
    int bestNodeMove = Move.NOMOVE;

    // Prepare hash type
    byte ttType = TT_EntryType.ALPHA;
    String nodeType;

    // ##########################################################
    // ##### ROOT_PLY MOVES -Iterate through all available root moves
    for (int i = 0; i < rootMoves.size(); i++) {
      final int move = rootMoves.getMove(i);

      if (TRACE) {
        trace("Root Search for depth %d: MOVE %s (%d/%d) ", depth, Move.toSimpleString(move), i + 1,
              rootMoves.size());
      }

      // store the current move
      searchCounter.currentRootMoveNumber = i + 1;
      searchCounter.currentRootMove = move;

      // #### START - Commit move and go deeper into recursion
      position.makeMove(move);
      currentVariation.add(move);

      // update UCI
      sendUCIUpdate(position);

      // ###############################################
      // Late Move Reduction
      int lmrReduce = 0;
      // @formatter:off
      if (config.USE_LMR
          && numberOfSearchedMoves >= 1
          && depth >= config.LMR_MIN_DEPTH
          && !isPerftSearch()
          && !position.hasCheck()
          && Move.getTarget(move).equals(Piece.NOPIECE)
          && !Move.getMoveType(move).equals(MoveType.PROMOTION)
          && !killerMoves[ply].contains(move)
      ) { // @formatter:on
        lmrReduce = config.LMR_REDUCTION;
        searchCounter.lmrReductions++;
      }
      // ###############################################

      int value;
      // ########################################
      // ### START PVS ROOT_PLY SEARCH            ###
      if (!config.USE_PVS || isPerftSearch() || numberOfSearchedMoves == 0) {
        if (TRACE) {
          trace("Root Search for depth %d: PV-SEARCH: %d/%d %s window %d/%d", depth, i,
                rootMoves.size(), Move.toString(move), -beta, -alpha);
        }
        value = -search(position, depth - 1, ply + 1, -beta, -alpha, PV_NODE, DO_NULL);
      } else {
        if (TRACE) {
          trace("Root Search for depth %d: NULL-WINDOW: %d/%d %s window %d/%d", depth, i,
                rootMoves.size(), Move.toString(move), -alpha - 1, -alpha);
        }

        value =
          -search(position, depth - 1 - lmrReduce, ply + 1, -alpha - 1, -alpha, NPV_NODE, DO_NULL);
        if (value > alpha && !stopSearch) {
          searchCounter.pvs_root_researches++;
          if (TRACE) {
            trace("Root Search for depth %d: RE-SEARCH: %d/%d %s value=%d window %d/%d", depth, i,
                  rootMoves.size(), Move.toString(move), value, -beta, -alpha);
          }
          value = -search(position, depth - 1, ply + 1, -beta, -alpha, PV_NODE, DO_NULL);
        } else {
          searchCounter.pvs_root_cutoffs++;
        }
      }
      // ### END PVS ROOT_PLY SEARCH              ###
      // ########################################

      numberOfSearchedMoves++;
      position.undoMove();
      currentVariation.removeLast();
      // #### END - Commit move and go deeper into recursion

      if (TRACE) {
        trace("Root Search for depth %d: VALUE %d", depth, value);
      }

      // End a stopped search here as the value from this is not reliable.
      // If we already have searched moves and found a better alpha then we
      // still use this better move.
      if (stopSearch) {
        if (TRACE) trace("Root Search for depth %d: STOP FLAG", depth);
        break;
      }

      // write the value back to the root moves list
      rootMoves.set(i, move, value);

      // we found a better value and move for this node
      if (value > bestNodeValue && !isPerftSearch()) {
        bestNodeValue = value;
        bestNodeMove = move;

        // If we found a move that is better or equal than beta this means that the
        // opponent can/will avoid this position altogether so we can stop search
        // this node
        if (value >= beta) { // fail-high
          // Aspiration Cutoff
          if (TRACE) trace("Root Search for depth %d: CUTOFF (ASPIRATION FAIL-HIGH)", depth);
          ttType = TT_EntryType.BETA;
          searchCounter.prunings++;
          // store the bestNodeMove any way as this is the a refutation and
          // should be checked in other nodes very early
          storeTT(position, beta, ttType, depth, bestNodeMove, mateThreat[ply]);
          return beta; // return beta in a fail-hard / value in fail-soft
        }

        // if we indeed found a better move (value > alpha) then we need to update
        // the PV (best move) and also store the the new exact value in the TT:
        if (value > alpha) {
          // PV_NODE
          if (TRACE) trace("Root Search for depth %d: NEW PV (ASPIRATION WINDOW)", depth);
          ttType = TT_EntryType.EXACT;
          alpha = value;
          MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);
        }
      }

      // check if we need to stop search - could be external or time.
      if (stopSearch || softTimeLimitReached() || hardTimeLimitReached()) {
        if (TRACE) {
          trace("Root Search for depth %d: STOPPED (soft=%,d hard=%,d)", depth, softTimeLimit,
                hardTimeLimit);
        }
        break;
      }

      if (TRACE) trace("Root Search for depth %d: MOVE %s DONE", depth, Move.toSimpleString(move));
    } // end for root moves loop
    // ##### Iterate through all available moves
    // ##########################################################

    if (config.USE_ASPIRATION_WINDOW && !isPerftSearch()) {
      // if we never had a beta cut off (cut-node) or never found a better
      // alpha (pv-node) we have an aspiration fail-low
      if (config.USE_ASPIRATION_WINDOW && principalVariation[ply].empty()) {
        if (TRACE) trace("Root Search for depth %d: ASPIRATION FAIL-LOW", depth);
      }
    } else {
      // search has been stopped before we ever got a best move - use first root move
      if (principalVariation[ply].empty()) {
        if (TRACE) trace("Root Search for depth %d: FAIL-LOW (no best move found)", depth);
        principalVariation[ply].add(rootMoves.getMove(0));
      }
    }

    // push PV move to head of list
    rootMoves.pushToHead(principalVariation[ROOT_PLY].getFirst());

    // store the best alpha
    storeTT(position, alpha, ttType, depth, bestNodeMove, mateThreat[ply]);
    if (TRACE) trace("Root Search for depth %d: END. (alpha=%d beta==%d)", depth, alpha, beta);
    return alpha;
  }

  /**
   * Search - recursive AlphaBeta search with several optimizations.
   *
   * @param position
   * @param depth
   * @param ply
   * @param pvNode
   * @param doNullMove
   * @return value of the search
   */
  private int search(Position position, int depth, int ply, int alpha, int beta, boolean pvNode,
                     final boolean doNullMove) {

    if (TRACE) {
      trace("%sSearch in ply %d for depth %d: START alpha=%d beta=%d pvnode=%s (%s)",
            getSpaces(ply), ply, depth, alpha, beta, pvNode, currentVariation.toNotationString());
    }

    // update current search depth stats
    searchCounter.currentSearchDepth = Math.max(searchCounter.currentSearchDepth, ply);
    searchCounter.currentExtraSearchDepth = Math.max(searchCounter.currentExtraSearchDepth, ply);
    searchCounter.nodesVisited++;

    // on leaf node call qsearch
    if (depth < 1 || ply >= MAX_SEARCH_DEPTH - 1) { // will be checked again in qsearch
      if (TRACE) trace("%sSearch in ply %d for depth %d: LEAF NODE", getSpaces(ply), ply, depth);
      return qsearch(position, depth, ply, alpha, beta, pvNode);
    }

    assert
      (isPerftSearch() || !config.USE_ALPHABETA_PRUNING || (alpha >= Evaluation.MIN && alpha < beta
                                                            && beta <= Evaluation.MAX));
    assert (pvNode || (alpha == beta - 1));
    assert ply >= 1;
    assert depth <= MAX_SEARCH_DEPTH;

    // check draw through 50-moves-rule, 3-fold-repetition
    if (!isPerftSearch()) {
      if (position.check50Moves() || position.check3Repetitions()) {
        if (TRACE) {
          trace("%sSearch in ply %d for depth %d: REPETITON DRAW", getSpaces(ply), ply, depth);
        }
        return contempt(position);
      }
    }

    // Check if we need to stop search - could be external or time or
    // max allowed nodes.
    // @formatter:off
    if (stopSearch
        || hardTimeLimitReached()
        || (searchMode.getNodes() > 0
            && searchCounter.nodesVisited >= searchMode.getNodes())) {
      if (TRACE) if (!stopSearch) trace("%sSearch in ply %d for depth %d: STOPPED (time=%,d)",
                                        getSpaces(ply), ply, depth, hardTimeLimit);
      stopSearch = true;
      return 0; // value does ont matter because of top flag
    }
    // @formatter:on

    // ###############################################
    // ## BEGIN Mate Distance Pruning
    // ## Did we already find a shorter mate then ignore this one
    if (config.USE_MATE_DISTANCE_PRUNING && !isPerftSearch()) {
      alpha = Math.max(-Evaluation.CHECKMATE + ply, alpha);
      beta = Math.min(Evaluation.CHECKMATE - ply, beta);
      if (alpha >= beta) {
        assert isCheckMateValue(alpha);
        searchCounter.mateDistancePrunings++;
        if (TRACE) trace("%sSearch in ply %d for depth %d: MDP CUT", getSpaces(ply), ply, depth);
        return alpha;
      }
    }
    // ## ENDOF Mate Distance Pruning
    // ###############################################

    // ###############################################
    // TT Lookup
    TTHit ttHit = probeTT(position, depth, alpha, beta, ply);
    if (ttHit != null && ttHit.type != TT_EntryType.NONE) {
      assert (ttHit.value >= Evaluation.MIN && ttHit.value <= Evaluation.MAX);
      mateThreat[ply] = ttHit.mateThreat;
      // in PV node only return ttHit if it was an exact hit
      if (!pvNode || ttHit.type == TT_EntryType.EXACT) {
        if (TRACE) {
          trace("%sSearch in ply %d for depth %d: TT CUT value=%d", getSpaces(ply), ply, depth,
                ttHit.value);
        }
        return ttHit.value;
      }
    }
    // End TT Lookup
    // ###############################################

    // Static Eval
    int staticEval = isPerftSearch() ? 0 : evaluate(position, ply, alpha, beta);

    // ###############################################
    // STATIC NULL MOVE PRUNING
    // https://www.chessprogramming.org/Reverse_Futility_Pruning
    // Cut very bad position when static eval is low anyway
    // @formatter:off
    if (config.USE_STATIC_NULL_PRUNING
        && !isPerftSearch()
        && !pvNode
        && depth < config.STATIC_NULL_PRUNING_DEPTH
        && doNullMove
        && !position.hasCheck()
        && !isCheckMateValue(beta)
    ) {
      final int evalMargin = config.STATIC_NULL_PRUNING_MARGIN * depth;
      if (staticEval - evalMargin >= beta ){
        if (TRACE) trace("%sSearch in ply %d for depth %d: STATIC CUT", getSpaces(ply), ply, depth);
        return beta; // fail-hard / fail-soft: staticEval - evalMargin;
      }
    }
    // @formatter:on
    // EVAL PRUNING
    // ###############################################

    // ###############################################
    // NULL MOVE PRUNING
    // @formatter:off
    if (config.USE_NULL_MOVE_PRUNING
        && !isPerftSearch()
        && !pvNode
        && depth > config.NULL_MOVE_DEPTH
        && doNullMove
        && !position.hasCheck()
        && bigPiecePresent(position)
    ) {
      // @formatter:on

      // reduce more on higher depths
      int r = depth > 6 ? 3 : 2;
      if (config.USE_VERIFY_NMP) r++;

      position.makeNullMove();
      int nullValue = -search(position, depth - r, ply + 1, -beta, -beta + 1, NPV_NODE, NO_NULL);
      position.undoNullMove();

      // Verify on beta exceeding
      if (config.USE_VERIFY_NMP) {
        if (depth > config.NULL_MOVE_REDUCTION_VERIFICATION) {
          if (nullValue >= beta) {
            searchCounter.nullMoveVerifications++;
            nullValue =
              search(position, depth - config.NULL_MOVE_REDUCTION_VERIFICATION, ply, alpha, beta,
                     PV_NODE, NO_NULL);
          }
        }
      }

      // Check for mate threat
      if (isCheckMateValue(nullValue)) mateThreat[ply] = true;

      // pruning
      if (nullValue >= beta && !mateThreat[ply]) {
        if (TRACE) trace("%sSearch in ply %d for depth %d: NULL CUT", getSpaces(ply), ply, depth);
        searchCounter.nullMovePrunings++;
        return beta; // fail-hard, fail-soft: nullValue;
      }
    }
    // NULL MOVE PRUNING
    // ###############################################

    // ###############################################
    // RAZORING
    // @formatter:off
    if (config.USE_RAZOR_PRUNING
        && !isPerftSearch()
        && !pvNode
        && depth <= config.RAZOR_PRUNING_DEPTH
        && !position.hasCheck()
        && !mateThreat[ply]
        && !isCheckMateValue(beta)
    ) {
      final int threshold = alpha - config.RAZOR_PRUNING_MARGIN;
      if (staticEval <= threshold ){
        if (TRACE) trace("%sSearch in ply %d for depth %d: RAZOR CUT", getSpaces(ply), ply, depth);
        return qsearch(position, DEPTH_NONE, ply, alpha, beta, NPV_NODE);
      }
    }
    // @formatter:on
    // RAZORING
    // ###############################################

    // needed to remember if we even had a legal move
    int numberOfSearchedMoves = 0;

    // clear principal Variation for this depth
    principalVariation[ply].clear();

    // Initialize best values
    int bestNodeValue = -Evaluation.INFINITE;
    int bestNodeMove = Move.NOMOVE;

    // Prepare hash type
    byte ttType = TT_EntryType.ALPHA;

    // prepare move generator
    // set position, killers and TT move
    moveGenerators[ply].setPosition(position);

    if (config.USE_KILLER_MOVES && !killerMoves[ply].empty()) {
      moveGenerators[ply].setKillerMoves(killerMoves[ply]);
    }
    if (config.USE_PVS_MOVE_ORDERING && ttHit != null && ttHit.bestMove != Move.NOMOVE) {
      moveGenerators[ply].setPVMove(ttHit.bestMove);
    }

    int legalMovesSize;
    if (TRACE) {
      legalMovesSize = moveGenerators[ply].getLegalMoves().size();
      trace("%sSearch %d moves in ply %d for depth %d: MOVE GEN", getSpaces(ply), legalMovesSize,
            ply, depth);
    }

    // Search all generated moves using the onDemand move generator.
    int move;
    while ((move = moveGenerators[ply].getNextPseudoLegalMove(false)) != Move.NOMOVE) {
      searchCounter.movesGenerated++;

      // ###############################################
      // Minor Promotion Pruning
      if (config.USE_MINOR_PROMOTION_PRUNING && !isPerftSearch()) {
        // @formatter:off
        if (Move.getMoveType(move) == MoveType.PROMOTION
            && Move.getPromotion(move).getType() != PieceType.QUEEN
            && Move.getPromotion(move).getType() != PieceType.KNIGHT) {
          // prune non queen or knight promotion as they are redundant
          // exception would be stale mate situations.
          searchCounter.minorPromotionPrunings++;
          if (TRACE) trace("%sSearch in ply %d for depth %d: MPP CUT", getSpaces(ply), ply, depth);
          continue;
        }
        // @formatter:on
      }
      // ###############################################

      position.makeMove(move);

      // Skip illegal moves
      if (wasIllegalMove(position)) {
        position.undoMove();
        continue;
      }

      if (TRACE) {
        trace("%sSearch %s (%d/%d) ply %d for depth %d: MOVE", getSpaces(ply),
              Move.toSimpleString(move), numberOfSearchedMoves + 1, legalMovesSize, ply, depth);
      }

      // keep track of current variation
      currentVariation.add(move);

      // update UCI
      sendUCIUpdate(position);

      // TODO: EXTENSIONS check, castlings, queen promotions,

      // ###############################################
      // Late Move Reduction
      int lmrReduce = 0;
      // @formatter:off
      if (config.USE_LMR
          && numberOfSearchedMoves >= 1
          && depth >= config.LMR_MIN_DEPTH
          && !isPerftSearch()
          && !pvNode
          && !position.hasCheck()
          && !mateThreat[ply]
          && !isCheckMateValue(beta)
          && Move.getTarget(move).equals(Piece.NOPIECE)
          && !Move.getMoveType(move).equals(MoveType.PROMOTION)
          && !killerMoves[ply].contains(move)
      ) { // @formatter:on
        lmrReduce = config.LMR_REDUCTION;
        searchCounter.lmrReductions++;
        if (TRACE) {
          trace("%sSearch in ply %d for depth %d: LMR (%d)", getSpaces(ply), ply, depth, lmrReduce);
        }
      }
      // ###############################################

      // go one ply deeper into the search tree
      // ########################################
      // ### START PVS ###
      int value;
      if (!config.USE_PVS || isPerftSearch() || (numberOfSearchedMoves == 0)) {
        // In a non root node we need to establish a best move for this ply
        // by using the first move (b/o good move sorting).
        // We will then try to prove that all other moves are worse or at best equal
        // to alpha with a null window search. Alpha might have been set in a previous
        // sibling node. If we do not find a better move we can go to the next move.
        // If we found a better move we need to re-search the move to get the exact
        // value.
        if (TRACE) {
          trace("%sSearch PV-SEARCH in ply %d for depth %d move %s window %d/%d", getSpaces(ply),
                ply, depth, Move.toString(move), -beta, -alpha);
        }
        value = -search(position, depth - 1, ply + 1, -beta, -alpha, PV_NODE, DO_NULL);
      } else {
        // Try null window search to prove the move is worse or at best equal the
        // known alpha (best value so far in his ply)
        if (TRACE) {
          trace("%sSearch NULL-WINDOW in ply %d for depth %d move %s window %d/%d", getSpaces(ply),
                ply, depth, Move.toString(move), -beta, -alpha);
        }
        value =
          -search(position, depth - 1 - lmrReduce, ply + 1, -alpha - 1, -alpha, NPV_NODE, DO_NULL);
        if (value > alpha && !stopSearch) {
          searchCounter.pvs_researches++;
          if (TRACE) {
            trace("%sSearch RE-SEARCH in ply %d for depth %d move %s value %d window %d/%d",
                  getSpaces(ply), ply, depth, Move.toString(move), value, -beta, -alpha);
          }
          // We found a better move a need to get the exact value be doing a full
          // window search.
          value = -search(position, depth - 1, ply + 1, -beta, -alpha, PV_NODE, DO_NULL);
        } else {
          searchCounter.pvs_cutoffs++;
        }
        // ### END PVS ###
        // ########################################
      }

      // needed to remember if we even had a legal move
      numberOfSearchedMoves++;

      currentVariation.removeLast();
      position.undoMove();

      if (TRACE) {
        trace("%sSearch in ply %d for depth %d: VALUE %d", getSpaces(ply), ply, depth, value);
      }

      // if stopped we ignore the last result and end the loop
      // which will the return the last alpha value we have
      if (stopSearch) {
        if (TRACE) trace("%sSearch in ply %d for depth %d: STOP FLAG", getSpaces(ply), ply, depth);
        break;
      }

      // Did we find a better move for this node?
      // For the PV move this is always the case.
      if (value > bestNodeValue && !isPerftSearch()) {
        if (TRACE) {
          trace("%sSearch in ply %d for depth %d: NEW BEST NODE %d > %d (bestNodeValue)",
                getSpaces(ply), ply, depth, value, bestNodeValue);
        }
        bestNodeValue = value;
        bestNodeMove = move;

        // If we found a move that is better or equal than beta this means that the
        // opponent can/will avoid this position altogether so we can stop search
        // this node
        if (value >= beta) { // fail-high
          if (config.USE_ALPHABETA_PRUNING) {
            // save killer moves so they will be search earlier on following nodes
            if (config.USE_KILLER_MOVES && Move.getTarget(move).equals(Piece.NOPIECE)) {
              if (!killerMoves[ply].pushToHeadStable(move)) {
                killerMoves[ply].addFront(move);
                while (killerMoves[ply].size() > config.NO_KILLER_MOVES) {
                  killerMoves[ply].removeLast(); // keep size stable
                }
              }
            }
            searchCounter.prunings++;
            ttType = TT_EntryType.BETA;
            if (TRACE) {
              trace("%sSearch in ply %d for depth %d: CUT NODE %d > %d (beta)", getSpaces(ply), ply,
                    depth, value, beta);
            }
            // store the bestNodeMove any way as this is the a refutation and
            // should be checked in other nodes very early
            storeTT(position, beta, ttType, depth, bestNodeMove, mateThreat[ply]);
            return beta; // return beta in a fail-hard / value in fail-soft
          }
        }

        // Did we find a better move than in previous nodes then this is our new
        // PV and best move for this ply.
        // If we never find a better alpha we do have a best move for this node
        // but not for the ply. We will return alpha and store a alpha node in
        // TT.
        if (value > alpha) {
          if (TRACE) {
            trace("%sSearch in ply %d for depth %d: NEW PV %d > %d (alpha)", getSpaces(ply), ply,
                  depth, value, alpha);
          }
          alpha = value;
          MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);
          ttType = TT_EntryType.EXACT;
        }
      }

    } // iteration over all moves

    // if we never had a beta cut off (cut-node) or never found a better
    // alpha (pv-node) we have a all-node
    if (principalVariation[ply].empty()) {
      if (TRACE) {
        trace("%sSearch in ply %d for depth %d: ALL NODE", getSpaces(ply), ply, depth);
      }
    }

    // if we did not have a legal move then we have a mate
    if (numberOfSearchedMoves == 0 && !stopSearch) {
      searchCounter.nonLeafPositionsEvaluated++;
      if (position.hasCheck()) {
        // We have a check mate. Return a -CHECKMATE.
        if (TRACE) trace("%sSearch in ply %d for depth %d: CHECKMATE", getSpaces(ply), ply, depth);
        alpha = -Evaluation.CHECKMATE + ply;
      } else {
        // We have a stale mate. Return the draw value.
        if (TRACE) trace("%sSearch in ply %d for depth %d: STALEMATE", getSpaces(ply), ply, depth);
        alpha = Evaluation.DRAW;
      }
      assert ttType == TT_EntryType.ALPHA;
    }

    if (TRACE) {
      trace("%sSearch in ply %d for depth %d: END value=%d (%d moves searched) (%s)",
            getSpaces(ply), ply, depth, alpha, numberOfSearchedMoves,
            currentVariation.toNotationString());
    }

    storeTT(position, alpha, ttType, depth, bestNodeMove, mateThreat[ply]);
    return alpha;
  }

  /**
   * After the normal search has reached its intended depth the search is extended for certain
   * moves. Typically this are moves which are capturing or checking. All other moves are called
   * "quiet" moves and therefore the term quiescence search (qsearch).
   * <p>
   * A quiescence search is a special und usually simpler version of the normal search where only
   * certain moves are generated and searched deeper.
   *
   * @param position
   * @param depth
   * @param ply
   * @param alpha
   * @param beta
   * @param pvNode
   * @return evaluation
   */
  private int qsearch(Position position, int depth, int ply, int alpha, int beta, boolean pvNode) {

    if (TRACE) {
      trace("%sQuiescence in ply %d: START alpha=%d beta=%d pvnode=%s (%s)", getSpaces(ply), ply,
            alpha, beta, pvNode, currentVariation.toNotationString());
    }

    assert
      (isPerftSearch() || !config.USE_ALPHABETA_PRUNING || (alpha >= Evaluation.MIN && alpha < beta
                                                            && beta <= Evaluation.MAX));
    assert (pvNode || (alpha == beta - 1));
    assert (depth <= 0);
    assert ply >= 1;

    // update current search depth stats
    searchCounter.currentExtraSearchDepth = Math.max(searchCounter.currentExtraSearchDepth, ply);

    // if PERFT return with eval to count all captures etc.
    if (isPerftSearch()) return evaluate(position, ply, alpha, beta);

    // check draw through 50-moves-rule, 3-fold-repetition, insufficient material
    if (position.check50Moves() || position.check3Repetitions()) {
      if (TRACE) trace("%sQuiescence in ply %d: REPETITON DRAW", getSpaces(ply), ply);
      return contempt(position);
    }

    // If quiescence is turned off return evaluation
    if (!config.USE_QUIESCENCE || ply >= MAX_SEARCH_DEPTH - 1) {
      if (TRACE) {
        trace("%sQuiescence in ply %d: EVAL value=", getSpaces(ply), ply,
              evaluate(position, ply, alpha, beta));
      }
      return evaluate(position, ply, alpha, beta);
    }

    // Check if we need to stop search - could be external or time or
    // max allowed nodes.
    // @formatter:off
    if (stopSearch
        || hardTimeLimitReached()
        || (searchMode.getNodes() > 0
            && searchCounter.nodesVisited >= searchMode.getNodes())) {
      if (TRACE) if (!stopSearch) trace("%sQuiescence in ply %d: STOPPED (time=%,d)",
                                        getSpaces(ply), ply, hardTimeLimit);
      stopSearch = true;
      return 0; // value does ont matter because of top flag
    }
    // @formatter:on

    // Prepare hash type
    byte ttType = TT_EntryType.ALPHA;

    // Initialize best values
    int bestNodeValue = -Evaluation.INFINITE;
    int bestNodeMove = Move.NOMOVE;

    // ###############################################
    // ## BEGIN Mate Distance Pruning
    // ## Did we already find a shorter mate then ignore this one
    if (config.USE_MATE_DISTANCE_PRUNING && !isPerftSearch()) {
      alpha = Math.max(-Evaluation.CHECKMATE + ply, alpha);
      beta = Math.min(Evaluation.CHECKMATE - ply, beta);
      if (alpha >= beta) {
        assert isCheckMateValue(alpha);
        searchCounter.mateDistancePrunings++;
        if (TRACE) trace("%sQuiescence in ply %d: MDP CUT", getSpaces(ply), ply);
        return alpha;
      }
    }
    // ## ENDOF Mate Distance Pruning
    // ###############################################

    // ###############################################
    // TT Lookup
    TTHit ttHit = probeTT(position, 0, alpha, beta, ply);
    if (ttHit != null && ttHit.type != TT_EntryType.NONE) {
      assert (ttHit.value >= Evaluation.MIN && ttHit.value <= Evaluation.MAX);
      mateThreat[ply] = ttHit.mateThreat;
      // in PV node only return ttHit if it was an exact hit
      if (!pvNode || ttHit.type == TT_EntryType.EXACT) {
        if (TRACE) trace("%sQuiescence in ply %d: TT CUT", getSpaces(ply), ply);
        return ttHit.value;
      }
    }
    // End TT Lookup
    // ###############################################

    // Use evaluation as a standing pat (lower bound)
    // https://www.chessprogramming.org/Quiescence_Search#Standing_Pat
    // Assuption is that there is at least on move which would improve the
    // current position. So if we are already >beta we don't need to look at it.
    if (!position.hasCheck()) {
      int statEval = evaluate(position, ply, alpha, beta);
      if (TRACE) trace("%sQuiescence in ply %d: STANDPAT %d", getSpaces(ply), ply, statEval);
      if (statEval >= beta) {
        storeTT(position, beta, TT_EntryType.BETA, DEPTH_NONE, Move.NOMOVE, mateThreat[ply]);
        assert beta != Evaluation.NOVALUE;
        if (TRACE) {
          trace("%sQuiescence in ply %d: STANDPAT CUT (%d > %d beta)", getSpaces(ply), ply,
                statEval, beta);
        }
        return beta; // fail-hard, fail-soft: value
      }
      if (statEval > alpha) {
        alpha = statEval;
      }
    }

    // needed to remember if we even had a legal move
    int numberOfSearchedMoves = 0;

    // clear principal Variation for this depth
    principalVariation[ply].clear();

    // Prepare move generator - set position, killers and TT move and generate
    // all PseudoLegalMoves for QSearch. Usually only capture moves and check
    // evasions will be determined in move generator
    moveGenerators[ply].setPosition(position);
    if (config.USE_PVS_MOVE_ORDERING && ttHit != null && ttHit.bestMove != Move.NOMOVE) {
      moveGenerators[ply].setPVMove(ttHit.bestMove);
    }
    MoveList moves = moveGenerators[ply].getPseudoLegalQSearchMoves();
    searchCounter.movesGenerated += moves.size();

    if (TRACE) {
      trace("%sQuiescence %d moves in ply %d: MOVE GEN", getSpaces(ply), moves.size(), ply, depth);
    }

    // moves to search recursively
    int value;
    for (int i = 0; i < moves.size(); i++) {
      int move = moves.get(i);

      // ###############################################
      // Minor Promotion Pruning
      if (config.USE_MINOR_PROMOTION_PRUNING && !isPerftSearch()) {
        // @formatter:off
        if (Move.getMoveType(move) == MoveType.PROMOTION
            && Move.getPromotion(move).getType() != PieceType.QUEEN
            && Move.getPromotion(move).getType() != PieceType.KNIGHT) {
          // prune non queen or knight promotion as they are redundant
          // exception would be stale mate situations.
          searchCounter.minorPromotionPrunings++;
          if (TRACE) trace("%sQuiescence in ply %d: MPP CUT", getSpaces(ply), ply);
          continue;
        }
        // @formatter:on
      }
      // ###############################################

      position.makeMove(move);

      // Skip illegal moves
      if (wasIllegalMove(position)) {
        position.undoMove();
        continue;
      }

      if (TRACE) {
        trace("%sQuiescence %s (%d/%d) ply %d: MOVE", getSpaces(ply), Move.toSimpleString(move),
              numberOfSearchedMoves + 1, moves.size(), ply);
      }

      // update nodes visited and count as non quiet board
      searchCounter.nodesVisited++;
      searchCounter.positionsNonQuiet++;

      // needed to remember if we even had a legal move
      currentVariation.add(move);

      // update UCI
      sendUCIUpdate(position);

      // go one ply deeper into the search tree
      value = -qsearch(position, depth, ply + 1, -beta, -alpha, pvNode);
      assert value > Evaluation.MIN && value < Evaluation.MAX;

      // needed to remember if we even had a legal move
      numberOfSearchedMoves++;

      currentVariation.removeLast();
      position.undoMove();

      if (TRACE) {
        trace("%sQuiescence in ply %d: VALUE %d", getSpaces(ply), ply, value);
      }

      // Did we find a better move for this node?
      // For the PV move this is always the case.
      if (value > bestNodeValue) { // to find first PV
        if (TRACE) {
          trace("%sQuiescence in ply %d: NEW BEST NODE %d > %d (bestNodeValue)", getSpaces(ply),
                ply, value, bestNodeValue);
        }
        bestNodeValue = value;
        bestNodeMove = move;

        // If we found a move that is better or equal than beta this mean that the
        // opponent can/will avoid this move altogether so we can stop search this node
        if (value >= beta) { // fail-high
          if (config.USE_ALPHABETA_PRUNING && !isPerftSearch()) {
            searchCounter.prunings++;
            if (TRACE) {
              trace("%sQuiescence in ply %d: CUT NODE %d > %d (beta)", getSpaces(ply), ply, value,
                    beta);
            }
            assert beta != Evaluation.NOVALUE;
            storeTT(position, value, TT_EntryType.BETA, DEPTH_NONE, bestNodeMove, mateThreat[ply]);
            return beta; // return beta in a fail-hard / value in fail-soft
          }
        }

        // Did we find a better move than in previous nodes then this is our new
        // PV and best move for this ply.
        if (value > alpha) {
          if (TRACE) {
            trace("%sQuiescence in ply %d: NEW PV %d > %d (alpha)", getSpaces(ply), ply, value,
                  alpha);
          }
          alpha = value;
          MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);
          ttType = TT_EntryType.EXACT;
        }
      }
      // PRUNING END

    } // iteration over all qmoves

    // if we did not have a legal move then we have a mate
    if (numberOfSearchedMoves == 0 && position.hasCheck() && !stopSearch) {
      // as we will not enter evaluation we count it here
      searchCounter.nonLeafPositionsEvaluated++;
      // We have a check mate. Return a -CHECKMATE.
      if (TRACE) trace("%sQuiescence in ply %d: CHECKMATE", getSpaces(ply), ply);
      alpha = -Evaluation.CHECKMATE + ply;
      assert ttType == TT_EntryType.ALPHA;
    }

    assert alpha != Evaluation.NOVALUE;
    assert alpha > Evaluation.MIN;

    if (TRACE) {
      trace("%sQuiescence in ply %d: END value=%d (%d moves searched) (%s)", getSpaces(ply), ply,
            alpha, numberOfSearchedMoves, currentVariation.toNotationString());
    }

    storeTT(position, alpha, ttType, 0, bestNodeMove, mateThreat[ply]);
    return alpha;
  }

  /**
   * Call the Evaluator evaluation and updates some statistics
   *
   * @param position
   * @param ply
   * @param alpha
   * @param beta
   * @return the evaluation value of the position
   */
  private int evaluate(Position position, int ply, int alpha, int beta) {

    // count all leaf nodes evaluated
    searchCounter.leafPositionsEvaluated++;

    // update some perft stats - no real performance hit here
    final int lastMove = position.getLastMove();
    if (Move.getTarget(lastMove) != Piece.NOPIECE) searchCounter.captureCounter++;
    if (Move.getMoveType(lastMove) == MoveType.ENPASSANT) searchCounter.enPassantCounter++;
    if (position.hasCheck()) searchCounter.checkCounter++;

    // special cases for PERFT testing as hasCheck is expensive
    // that is on normal searches we do not count checks and mates
    if (isPerftSearch()) {
      if (position.hasCheckMate()) searchCounter.checkMateCounter++;
      return 1;
    }

    // do evaluation
    final int value = evaluator.evaluate(position);

    if (TRACE) {
      trace("%SEvaluation: %s = %d  ply: %d  var: <%s>  position: %s", getSpaces(ply),
            Move.toString(position.getLastMove()), value, ply,
            currentVariation.toNotationString().trim(), position.toFENString());
    }

    return value;
  }

  /**
   * Stores position values and node best moves into the transposition table.
   *
   * @param position
   * @param value
   * @param ttType
   * @param depthLeft
   * @param bestMove
   * @param mateThreat
   */
  private void storeTT(final Position position, final int value, final byte ttType,
                       final int depthLeft, int bestMove, boolean mateThreat) {

    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch() && !stopSearch) {
      assert depthLeft >= 0 && depthLeft <= MAX_SEARCH_DEPTH;
      assert (value >= Evaluation.MIN && value <= Evaluation.MAX);
      transpositionTable.put(position, (short) value, ttType, (byte) depthLeft, bestMove,
                             mateThreat);
    }
  }

  /**
   * Probes the transposition table. Translates and checks the parameters and also
   * encapsulates the check for the type of entry/value (exact/alpha/beta). It also translates
   * mate values to the correct ply.
   * <p>
   * Uses a small data structure TTHit as a return type.
   *
   * @param position
   * @param depthLeft
   * @param alpha
   * @param beta
   * @param ply
   * @return
   */
  private TTHit probeTT(final Position position, final int depthLeft, final int alpha,
                        final int beta, final int ply) {

    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch()) {
      assert alpha != Evaluation.NOVALUE;
      assert beta != Evaluation.NOVALUE;
      assert depthLeft >= 0 && depthLeft <= MAX_SEARCH_DEPTH;
      assert ply >= 0 && ply <= Byte.MAX_VALUE;

      TT_Entry ttEntry = transpositionTable.get(position);

      if (ttEntry != null) {
        TTHit hit = new TTHit();

        // hit
        searchCounter.nodeCache_Hits++;

        // return the depth as well
        hit.depth = ttEntry.depth;
        // get best move form last search of this node
        hit.bestMove = ttEntry.bestMove;
        // get mate threat
        hit.mateThreat = ttEntry.mateThreat;

        assert hit.type == TT_EntryType.NONE;

        // only if tt depth was equal or deeper
        if (ttEntry.depth >= depthLeft) {

          int value = ttEntry.value;
          assert value != Evaluation.NOVALUE;

          // correct the mate value as this has been recorded
          // relative to a different ply
          if (isCheckMateValue(value)) {
            value = value > 0 ? value - ply : value + ply;
          }

          // check the retrieved hash table entry
          if (ttEntry.type == TT_EntryType.EXACT) {
            hit.value = value;
            hit.type = TT_EntryType.EXACT;

          } else if (ttEntry.type == TT_EntryType.ALPHA) {
            if (value <= alpha) {
              hit.value = value;
              hit.type = TT_EntryType.ALPHA;
            }

          } else if (ttEntry.type == TT_EntryType.BETA) {
            if (value >= beta) {
              hit.value = value;
              hit.type = TT_EntryType.BETA;
            }
          }
        }
        return hit;
      }
      // miss
      searchCounter.nodeCache_Misses++;
    }
    return null;
  }

  /**
   * Retrieves the PV line from the transposition table in root search.
   *
   * @param position
   * @param depth
   * @param pv
   */
  private void getPVLine(final Position position, final byte depth, final MoveList pv) {
    if (depth < 0) return;
    TT_Entry ttEntry = transpositionTable.get(position);
    if (ttEntry != null && ttEntry.bestMove != Move.NOMOVE) {
      pv.add(ttEntry.bestMove);
      position.makeMove(ttEntry.bestMove);
      getPVLine(position, (byte) (depth - 1), pv);
      position.undoMove();
    }
  }

  /**
   * Probes the openbook for the given position and returns a move from
   * the opening book or null if no move was found.
   *
   * @param position
   * @return move from opening book or null if no move was found
   */
  private SearchResult getBookMove(Position position) {
    // prepare search result
    SearchResult searchResult = new SearchResult();

    // Look for a possible opening book move and send it as result
    if (config.USE_BOOK && !isPerftSearch()) {
      if (searchMode.isTimeControl()) {
        LOG.info("Time controlled search => Using book");
        // initialize book - only happens the first time
        book.initialize();
        // retrieve a move from the book
        int bookMove = book.getBookMove(position.toFENString());
        if (bookMove != Move.NOMOVE && Move.isValid(bookMove)) {
          LOG.info("Book move found: {}", Move.toString(bookMove));
          hadBookMove = true;
          searchResult.bestMove = bookMove;
          searchResult.ponderMove = Move.NOMOVE;
          return searchResult;
        } else {
          LOG.info("No Book move found");
        }
      } else {
        LOG.info("Non time controlled search => not using book");
      }
    }
    return null;
  }

  /**
   * Returns true if at least on non pawn/king piece is on the
   * board for the moving side.
   *
   * @param position
   * @return true if at least one officer is on the board, false otherwise.
   */
  private static boolean bigPiecePresent(Position position) {
    final int activePlayer = position.getNextPlayer().ordinal();
    return !(position.getKnightSquares()[activePlayer].isEmpty()
             && position.getBishopSquares()[activePlayer].isEmpty()
             && position.getRookSquares()[activePlayer].isEmpty()
             && position.getQueenSquares()[activePlayer].isEmpty());
  }

  /**
   * @param value
   * @return true if absolute value is a mate value, false otherwise
   */
  private static boolean isCheckMateValue(int value) {
    final int abs = Math.abs(value);
    return abs >= Evaluation.CHECKMATE_THRESHOLD && abs <= Evaluation.CHECKMATE;
  }

  /**
   * @param value
   * @return a UCI compatible string for th score in cp or in mate in ply
   */
  private static String getScoreString(int value) {
    String scoreString;
    if (isCheckMateValue(value)) {
      scoreString = "score mate ";
      scoreString += value < 0 ? "-" : "";
      scoreString += (Evaluation.CHECKMATE - Math.abs(value) + 1) / 2;
    } else {
      scoreString = "score cp " + value;
    }
    return scoreString;
  }

  /**
   * @param position
   * @return value depending on game phase to avoid easy draws
   */
  private int contempt(Position position) {
    return -Evaluation.getGamePhaseFactor(position) * EvaluationConfig.CONTEMPT_FACTOR;
  }

  /**
   * @param position
   * @return true it last move made on poistion was illegal (left the king in check)
   */
  private boolean wasIllegalMove(final Position position) {
    return position.isAttacked(position.getNextPlayer(),
                               position.getKingSquares()[position.getNextPlayer()
                                                                 .getInverseColor()
                                                                 .ordinal()]);
  }

  /**
   * @return if configuration of search mode are in PERFT mode
   */
  private boolean isPerftSearch() {
    return config.PERFT || (searchMode != null && searchMode.isPerft());
  }

  /**
   * Configure time limits-
   * <p>
   * Chooses if search mode is time per move or remaining time
   * and set time limits accordingly
   */
  private void configureTimeLimits() {

    if (searchMode.getMoveTime().toMillis() > 0) { // mode time per move

      hardTimeLimit = searchMode.getMoveTime().toMillis();
      softTimeLimit = hardTimeLimit;

    } else { // remaining time - estimated time per move

      // retrieve time left from search mode
      long timeLeft = searchMode.getRemainingTime(myColor).toMillis();

      // Give some overhead time so that in games with very low available time we do not run out
      // of time
      timeLeft -= 1000; // this should do

      // when we know the move to go (until next time control) use them otherwise assume 40
      int movesLeft = searchMode.getMovesToGo() > 0 ? searchMode.getMovesToGo() : 40;

      // when we have a time increase per move we estimate the additional time we should have
      if (myColor.isWhite()) {
        timeLeft += 40 * searchMode.getWhiteInc().toMillis();
      } else if (myColor.isBlack()) {
        timeLeft += 40 * searchMode.getBlackInc().toMillis();
      }

      // for timed games with remaining time
      hardTimeLimit = Duration.ofMillis((long) ((timeLeft / movesLeft) * 1.0f)).toMillis();
      softTimeLimit = (long) (hardTimeLimit * 0.8);
    }

    // limits for very short available time
    if (hardTimeLimit < 100) {
      timeLimitChange(0.9);
    }

  }

  /**
   * Changes the time limit by the given factor and also sets the soft time limit
   * to 0.8 of the hard time limit.
   *
   * @param factor factor for changing the time for the current search
   */
  private void timeLimitChange(double factor) {
    if (searchMode.getMoveTime().toMillis() == 0) {
      hardTimeLimit *= factor;
      softTimeLimit = (long) (hardTimeLimit * 0.8);
    }
  }

  /**
   * Soft time limit is used in iterative deepening to decide if an new depth should even be started.
   *
   * @return true if soft time limit is reached, false otherwise
   */
  private boolean softTimeLimitReached() {
    if (!searchMode.isTimeControl()) {
      return false;
    }
    stopSearch = elapsedTime() >= softTimeLimit;
    return stopSearch;
  }

  /**
   * Hard time limit is used to check time regularily in the search to stop the search when
   * time is out
   *
   * @return true if hard time limit is reached, false otherwise
   */
  private boolean hardTimeLimitReached() {
    if (!searchMode.isTimeControl()) {
      return false;
    }
    stopSearch = elapsedTime() >= hardTimeLimit;
    return stopSearch;
  }

  /**
   * @return the elapsed time in ms since the start of the search
   */
  private long elapsedTime() {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * @param t
   * @returnthe elapsed time from the start of the search to the given t
   */
  private long elapsedTime(final long t) {
    return t - startTime;
  }

  /**
   * Print log info after search is finish.
   */
  private void printSearchResultInfo() {
    if (LOG.isInfoEnabled()) {
      LOG.info("{}", String.format(
        "Search complete. " + "Nodes visited: %,d " + "Boards Evaluated: %,d (+%,d) "
        + "Captures: %,d " + "EP: %,d " + "Checks: %,d " + "Mates: %,d ",
        searchCounter.nodesVisited, searchCounter.leafPositionsEvaluated,
        searchCounter.nonLeafPositionsEvaluated, searchCounter.captureCounter,
        searchCounter.enPassantCounter, searchCounter.checkCounter,
        searchCounter.checkMateCounter));
      LOG.info("Search Depth was {} ({})", searchCounter.currentSearchDepth,
               searchCounter.currentExtraSearchDepth);
      LOG.info("Search took {}", DurationFormatUtils.formatDurationHMS(elapsedTime(stopTime)));
      LOG.info("Speed: {}", String.format("%,d nps", 1000 * (searchCounter.nodesVisited / (
        elapsedTime(stopTime) + 2L))));
      LOG.info("Aspiration Researches: {}", searchCounter.aspirationResearches);
    }
  }

  /**
   * Send UCI info after each iteration of search depth
   */
  private void sendUCIIterationEndInfo() {
    engine.sendInfoToUCI(
      String.format("depth %d seldepth %d multipv 1 %s nodes %d nps %d time %d pv %s",
                    searchCounter.currentIterationDepth, searchCounter.currentExtraSearchDepth,
                    getScoreString(searchCounter.currentBestRootValue), searchCounter.nodesVisited,
                    1000 * (searchCounter.nodesVisited / (elapsedTime() + 2L)), elapsedTime(),
                    principalVariation[ROOT_PLY].toNotationString()));
  }

  /**
   * Send the UCI info command line to the UI. Uses a ticker interval to avoid
   * flooding the protocol. <code>UCI_UPDATE_INTERVAL</code> is used as a time
   * interval in ms
   *
   * @param position
   */
  private void sendUCIUpdate(final Position position) {
    // send current root move info to UCI every x milli seconds
    if (System.currentTimeMillis() - uciUpdateTicker >= UCI_UPDATE_INTERVAL) {
      engine.sendInfoToUCI(String.format("depth %d seldepth %d nodes %d nps %d time %d hashfull %d",
                                         searchCounter.currentSearchDepth,
                                         searchCounter.currentExtraSearchDepth,
                                         searchCounter.nodesVisited,
                                         1000 * searchCounter.nodesVisited / (1 + elapsedTime()),
                                         elapsedTime(), (int) (1000 * (
          (float) transpositionTable.getNumberOfEntries() / transpositionTable.getMaxEntries()))));
      engine.sendInfoToUCI(String.format("currmove %s currmovenumber %d",
                                         Move.toUCINotation(position,
                                                            searchCounter.currentRootMove),
                                         searchCounter.currentRootMoveNumber));
      if (config.UCI_ShowCurrLine) {
        engine.sendInfoToUCI(String.format("currline %s", currentVariation.toNotationString()));
      }

      //      LOG.debug(searchCounter.toString());
      //      LOG.debug(String.format("TT Entries %,d/%,d TT Updates %,d TT Collisions %,d "
      //                              + "TT Hits %,d TT Misses %,d"
      //                              , transpositionTable.getNumberOfEntries()
      //                              , transpositionTable.getMaxEntries()
      //                              , transpositionTable.getNumberOfUpdates()
      //                              , transpositionTable.getNumberOfCollisions()
      //                              , searchCounter.nodeCache_Hits
      //                              , searchCounter.nodeCache_Misses
      //                              ));

      uciUpdateTicker = System.currentTimeMillis();
    }
  }

  /**
   * Sends the lastSearchResult to the UCI UI via the engine
   */
  private void sendUCIBestMove() {
    if (!Move.isValid(lastSearchResult.bestMove)) {
      LOG.error("Engine Best Move is invalid move!" + Move.toString(lastSearchResult.bestMove));
      LOG.error("Position: " + currentPosition.toFENString());
      LOG.error("Last Move: " + currentPosition.getLastMove());
    }
    engine.sendResult(lastSearchResult.bestMove, lastSearchResult.ponderMove);
  }

  /**
   * Logs formatted trace messages to LOG
   * @param format
   * @param args
   */
  private void trace(String format, Object... args) {
    LOG.trace("{}", String.format(format, args));
  }

  private StringBuilder getSpaces(int ply) {
    StringBuilder spaces = new StringBuilder();
    for (int i = 1; i <= ply; i++) {
      spaces.append("  ");
    }
    return spaces;
  }

  /**
   * Is called when our last ponder suggestion has been executed by opponent.
   * If we are already pondering just continue the search but switch to time control.
   */
  public void ponderHit() {
    if (searchMode.isPonder()) {
      LOG.info("****** PONDERHIT *******");
      if (isSearching()) {
        LOG.info("Ponderhit when ponder search still running. Continue searching.");
        startTime = System.currentTimeMillis();
        searchMode.ponderHit();
        String threadName = "Engine: " + myColor.toString();
        threadName += " (PHit)";
        searchThread.setName(threadName);
        // if time based game setup the time soft and hard time limits
        if (searchMode.isTimeControl()) {
          configureTimeLimits();
        }
      } else {
        LOG.info("Ponderhit when ponder search already ended. Sending result.");
        LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
                 principalVariation[ROOT_PLY].toNotationString());

        sendUCIBestMove();
      }

    } else {
      LOG.warn("Ponderhit when not pondering!");
    }
  }

  /**
   * @return true if previous search is still running
   */
  public boolean isSearching() {
    return searchThread != null && searchThread.isAlive();
  }

  /**
   * Called when the state of this search is no longer valid as the last call to startSearch is
   * not from
   * the same game as the next.
   */
  public void newGame() {
    clearHashTables();
  }

  /**
   * @return Search result of the last search. Has NOMOVE if no result available.
   */
  public SearchResult getLastSearchResult() {
    return lastSearchResult;
  }

  /**
   * @return a wrapper for all search counters
   */
  public SearchCounter getSearchCounter() {
    return searchCounter;
  }

  /**
   * @return
   */
  public TranspositionTable getTranspositionTable() {
    return transpositionTable;
  }

  /**
   * Called by engine whenever hash size changes.
   * Initially set in constructor
   *
   * @param hashSize
   */
  public void setHashSize(int hashSize) {
    transpositionTable = new TranspositionTable(hashSize);
  }

  /**
   * Clears the hashtables
   */
  public void clearHashTables() {
    transpositionTable.clear();
  }

  /**
   * Pauses the calling thread while in search.
   * <p>
   * Uses join() on the search thread.
   */
  public void waitWhileSearching() {
    while (isSearching()) {
      try {
        searchThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Parameter class for the search result
   */
  public static final class SearchResult {

    public int  bestMove    = Move.NOMOVE;
    public int  ponderMove  = Move.NOMOVE;
    public int  resultValue = Evaluation.NOVALUE;
    public long time        = -1;
    public int  depth       = 0;
    public int  extraDepth  = 0;

    @Override
    public String toString() {
      return "Best Move: " + Move.toString(bestMove) + " (" + getScoreString(resultValue) + ") "
             + " Ponder Move: " + Move.toString(ponderMove) + " Depth: " + depth + "/" + extraDepth;
    }
  }

  /**
   * Simple data structure for return value when probing the transposition table.
   */
  class TTHit {
    int  value    = Evaluation.NOVALUE;
    byte type     = TT_EntryType.NONE;
    byte depth    = 0;
    int  bestMove = Move.NOMOVE;
    public boolean mateThreat;
  }

  /**
   * Convenience Wrapper class for all search data and counters
   */
  public class SearchCounter {

    // Info values
    int  currentBestRootMove     = Move.NOMOVE;
    int  currentBestRootValue    = Evaluation.NOVALUE;
    int  currentIterationDepth   = 0;
    int  currentSearchDepth      = 0;
    int  currentExtraSearchDepth = 0;
    int  currentRootMove         = 0;
    int  currentRootMoveNumber   = 0;
    long lastSearchTime          = 0;

    // PERFT Values
    // For PERFT nonLeafPositionsEvaluated have to be subtracted
    // from leafPositionsEvaluated as PERFT only counts visited lead nodes.
    long leafPositionsEvaluated    = 0;
    long nonLeafPositionsEvaluated = 0;
    long checkCounter              = 0;
    long checkMateCounter          = 0;
    long captureCounter            = 0;
    long enPassantCounter          = 0;

    // Optimization Values
    long positionsNonQuiet      = 0;
    long prunings               = 0;
    long pvs_root_researches    = 0;
    long pvs_root_cutoffs       = 0;
    long pvs_researches         = 0;
    long pvs_cutoffs            = 0;
    long nodeCache_Hits         = 0;
    long nodeCache_Misses       = 0;
    long movesGenerated         = 0;
    long nodesVisited           = 0;
    int  minorPromotionPrunings = 0;
    int  mateDistancePrunings   = 0;
    int  nullMovePrunings       = 0;
    int  nullMoveVerifications  = 0;
    int  lmrReductions          = 0;
    int  aspirationResearches   = 0;

    private void resetCounter() {
      currentBestRootMove = Move.NOMOVE;
      currentBestRootValue = Evaluation.NOVALUE;
      currentIterationDepth = 0;
      currentSearchDepth = 0;
      currentExtraSearchDepth = 0;
      currentRootMove = 0;
      currentRootMoveNumber = 0;
      nodesVisited = 0;
      leafPositionsEvaluated = 0;
      positionsNonQuiet = 0;
      prunings = 0;
      pvs_root_researches = 0;
      pvs_root_cutoffs = 0;
      pvs_researches = 0;
      pvs_cutoffs = 0;
      nodeCache_Hits = 0;
      nodeCache_Misses = 0;
      movesGenerated = 0;
      checkCounter = 0;
      checkMateCounter = 0;
      captureCounter = 0;
      enPassantCounter = 0;
      lastSearchTime = 0;
      mateDistancePrunings = 0;
      minorPromotionPrunings = 0;
      nullMovePrunings = 0;
      nullMoveVerifications = 0;
      lmrReductions = 0;
      aspirationResearches = 0;
    }

    @Override
    public String toString() {
      // @formatter:off
      return "SearchCounter{" +
             "nodesVisited=" + nodesVisited +
             ", lastSearchTime=" + DurationFormatUtils.formatDurationHMS(lastSearchTime) +
             ", currentBestRootMove=" + currentBestRootMove +
             ", currentBestRootValue=" + currentBestRootValue +
             ", currentIterationDepth=" + currentIterationDepth +
             ", currentSearchDepth=" + currentSearchDepth +
             ", currentExtraSearchDepth=" + currentExtraSearchDepth +
             ", currentRootMove=" + currentRootMove +
             ", currentRootMoveNumber=" + currentRootMoveNumber +
             ", leafPositionsEvaluated=" + leafPositionsEvaluated +
             ", nonLeafPositionsEvaluated=" + nonLeafPositionsEvaluated +
             ", checkCounter=" + checkCounter +
             ", checkMateCounter=" + checkMateCounter +
             ", captureCounter=" + captureCounter +
             ", enPassantCounter=" + enPassantCounter +
             ", positionsNonQuiet=" + positionsNonQuiet +
             ", nodeCache_Hits=" + nodeCache_Hits +
             ", nodeCache_Misses=" + nodeCache_Misses +
             ", movesGenerated=" + movesGenerated +
             ", pvs_root_researches=" + pvs_root_researches +
             ", pvs_root_cutoffs=" + pvs_root_cutoffs +
             ", pvs_researches=" + pvs_researches +
             ", pvs_cutoffs=" + pvs_cutoffs +
             ", mateDistancePrunings=" + mateDistancePrunings +
             ", minorPromotionPrunings=" + minorPromotionPrunings +
             ", nullMovePrunings=" + nullMovePrunings +
             ", nullMoveVerifications=" + nullMoveVerifications +
             ", lmrReductions=" + lmrReductions +
             ", aspirationResearches=" + aspirationResearches +
             '}';
      // @formatter:on
    }
  }
}
