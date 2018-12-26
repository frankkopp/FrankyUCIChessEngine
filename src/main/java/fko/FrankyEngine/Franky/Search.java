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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Search implements the actual search for best move of a given position.
 * <p>
 * Search runs in a separate thread when the actual search is started. When
 * the search is finished it calls <code>engine.sendResult</code> ith the best move and a ponder
 * move if it has one.
 * <p>
 * TODO: SEE (https://www.chessprogramming.org/Static_Exchange_Evaluation)
 * TODO: KILLER Moves - search quiet moves previoulsy causing cut-offs first
 */
public class Search implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Search.class);

  public static final int MAX_SEARCH_DEPTH = 100;

  // Readabilty constants
  private static final boolean DO_NULL = true;
  private static final boolean NO_NULL = false;
  private static final boolean IS_PV   = true;
  private static final boolean NO_PV   = false;

  // search counters
  private final SearchCounter searchCounter = new SearchCounter();

  // back reference to the engine
  private final IUCIEngine    engine;
  final         Configuration config;

  // opening book
  private final OpeningBook book;

  // the thread in which we will do the actual search
  private Thread searchThread = null;

  // flag to indicate to stop the search - can be called externally or via the timer clock.
  private boolean stopSearch = true;

  // hash tables
  private TranspositionTable transpositionTable;

  // Move Generators - each depth in search gets it own to avoid object creation during search
  private final MoveGenerator[] moveGenerators = new MoveGenerator[MAX_SEARCH_DEPTH];

  // Position Evaluator
  private final Evaluation evaluator;

  // used to wait for move from search
  private CountDownLatch waitForInitializationLatch = new CountDownLatch(1);

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

  // running search global variables
  private final RootMoveList rootMoves          = new RootMoveList();
  // current variation of the search
  private final MoveList     currentVariation   = new MoveList(MAX_SEARCH_DEPTH);
  private final MoveList[]   principalVariation = new MoveList[MAX_SEARCH_DEPTH];
  private       long         uciUpdateTicker;

  /**
   * Creates a search object and stores a back reference to the engine object.<br>
   *
   * @param engine
   * @param config
   */
  public Search(IUCIEngine engine, Configuration config) {
    this.engine = engine;
    this.config = config;

    // set opening book - will be initialized in each search
    this.book = new OpeningBookImpl(config.OB_FolderPath + config.OB_fileNamePlain, config.OB_Mode);

    // set hash sizes
    setHashSize(this.config.HASH_SIZE);

    // Move Generators - each depth in search gets it own to avoid object creation
    // during search. This is in preparation for move generators which keep a state
    // per depth
    for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
      moveGenerators[i] = new MoveGenerator();
    }

    // prepare principal variation lists
    for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
      principalVariation[i] = new MoveList(MAX_SEARCH_DEPTH);
    }

    // create position evaluator
    evaluator = new Evaluation();
  }

  /**
   * Start the search in a separate thread.<br>
   * Calls <code>Engine.sendResult(searchResult);</code> to
   * store the result is it has found one. After storing the result
   * the search is ended and the thread terminated.<br>
   * The search will stop when it has reach the configured conditions. Either
   * reached a certain depth oder used up the time or found a move.<br>
   * The search also can be stopped by calling stop at any time. The
   * search will stop gracefully by storing the best move so far.
   *
   * @param position
   * @param searchMode
   */
  public void startSearch(Position position, SearchMode searchMode) {
    if (searchThread != null && searchThread.isAlive()) {
      final String s = "Search already running - can only be started once";
      IllegalStateException e = new IllegalStateException(s);
      LOG.error(s, e);
      throw e;
    }

    // create a deep copy of the position to not change the original position given
    this.currentPosition = new Position(position);

    // convenience fields
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

    // stop pondering if we are
    if (searchMode.isPonder()) {
      if (searchThread == null || !searchThread.isAlive()) {
        // ponder search has finished before we stopped
        // need to send the result anyway although a miss
        LOG.info(
          "Pondering has been stopped after ponder search has finished. " + "Send obsolete result");
        LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
                 principalVariation[0].toNotationString());
        engine.sendResult(lastSearchResult.bestMove, lastSearchResult.ponderMove);
      } else {
        LOG.info("Pondering has been stopped. Ponder Miss!");
      }
      searchMode.ponderStop();
    } else {
      LOG.info("Search has been stopped");
    }

    // set stop flag - search needs to check regularly and stop accordingly
    stopSearch = true;

    // return if no search is running
    if (searchThread == null) {
      return;
    }
    // Wait for the thread to die
    try {
      this.searchThread.join();
    } catch (InterruptedException ignored) {
    }

    // clear thread
    searchThread = null;
  }

  @Override
  public void run() {

    if (Thread.currentThread() != searchThread) {
      final String s = "run() cannot be called directly!";
      UnsupportedOperationException e = new UnsupportedOperationException(s);
      LOG.error(s, e);
      throw e;
    }

    if (isPerftSearch()) {
      LOG.info("****** PERFT SEARCH (" + searchMode.getMaxDepth() + ") *******");
    }
    if (searchMode.isTimeControl()) {
      LOG.info("****** TIMED SEARCH *******");
    }
    if (searchMode.isPonder()) {
      LOG.info("****** PONDER SEARCH *******");
    }
    if (searchMode.isInfinite()) {
      LOG.info("****** INFINITE SEARCH *******");
    }
    if (searchMode.getMate() > 0) {
      LOG.info("****** MATE SEARCH (" + searchMode.getMaxDepth() + ") *******");
    }

    // reset lastSearchResult
    lastSearchResult = new SearchResult();

    // reset counter
    searchCounter.resetCounter();

    // reset time limits
    softTimeLimit = hardTimeLimit = 0;

    // release latch so the caller can continue
    waitForInitializationLatch.countDown();

    // Opening book move
    if (config.USE_BOOK) {
      if (searchMode.isTimeControl()) {
        LOG.info("Time controlled search => Using book");
        // initialize book - only happens the first time
        book.initialize();
        // retrieve a move from the book
        int bookMove = book.getBookMove(currentPosition.toFENString());
        if (bookMove != Move.NOMOVE && Move.isValid(bookMove)) {
          LOG.info("Book move found: {}", Move.toString(bookMove));
          lastSearchResult.bestMove = bookMove;
          engine.sendResult(lastSearchResult.bestMove, Move.NOMOVE);
          return;
        } else {
          LOG.info("No Book move found");
        }
      } else {
        LOG.info("Non time controlled search => not using book");
      }
    }

    // run the search itself
    lastSearchResult = iterativeDeepening(currentPosition);

    // if the mode still is ponder at this point we finished the ponder
    // search early before a miss or hit has been signaled. We need to
    // wait with sending the result until we get a miss (stop) or a hit.
    if (searchMode.isPonder()) {
      LOG.info("Ponder Search finished! Waiting for Ponderhit to send result");
      return;
    }

    LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
             principalVariation[0].toNotationString());

    // send result to engine
    engine.sendResult(lastSearchResult.bestMove, lastSearchResult.ponderMove);
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
        LOG.debug("Ponderhit: reset searchMode");
        String threadName = "Engine: " + myColor.toString();
        threadName += " (PHit)";
        searchThread.setName(threadName);
        LOG.debug("Ponderhit: renamed thread");
        // if time based game setup the time soft and hard time limits
        if (searchMode.isTimeControl()) {
          LOG.debug("Ponderhit: is time controlled");
          configureTimeLimits();
          LOG.debug("Ponderhit: time configured");
        }
      } else {
        LOG.info("Ponderhit when ponder search already ended. Sending result.");
        LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
                 principalVariation[0].toNotationString());
        engine.sendResult(lastSearchResult.bestMove, lastSearchResult.ponderMove);
      }
    } else {
      LOG.warn("Ponderhit when not pondering!");
    }
  }

  /**
   * This starts the actual iterative search.
   *
   * @param position
   * @return search result
   */
  private SearchResult iterativeDeepening(Position position) {

    // remember the start of the search
    startTime = System.currentTimeMillis();
    uciUpdateTicker = System.currentTimeMillis();

    // generate all root moves
    MoveList legalMoves = moveGenerators[0].getLegalMoves(position);

    // no legal root moves - game already ended!
    if (legalMoves.size() == 0) {
      final SearchResult searchResult = new SearchResult();
      searchResult.bestMove = Move.NOMOVE;
      if (position.hasCheck()) {
        searchResult.resultValue = Evaluation.CHECKMATE;
      } else {
        searchResult.resultValue = Evaluation.DRAW;
      }
      return searchResult;
    }

    // prepare principal variation lists
    for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
      principalVariation[i].clear();
    }

    // create rootMoves list
    rootMoves.clear();
    for (int i = 0; i < legalMoves.size(); i++) {
      // filter UCI search moves
      if (searchMode.getMoves().isEmpty()) {
        rootMoves.add(legalMoves.get(i), Evaluation.NOVALUE);
      } else {
        if (searchMode.getMoves().contains(Move.toUCINotation(position, legalMoves.get(i)))) {
          rootMoves.add(legalMoves.get(i), Evaluation.NOVALUE);
        }
      }
    }

    // temporary best move - take the first move available
    searchCounter.currentBestRootMove = rootMoves.getMove(0);
    searchCounter.currentBestRootValue = Evaluation.NOVALUE;

    // prepare search result
    SearchResult searchResult = new SearchResult();

    // if time based game setup the time soft and hard time limits
    if (searchMode.isTimeControl()) {
      configureTimeLimits();
    }

    // for fixed depth searches we start at the final depth directly
    // no iterative deepening
    int depth = searchMode.getStartDepth();

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
      LOG.debug("");
    }

    // max window search - preparation for aspiration window search
    final int alpha = Evaluation.MIN;
    final int beta = Evaluation.MAX;

    // #############################
    // ### BEGIN Iterative Deepening
    do {
      searchCounter.currentIterationDepth = depth;

      // *******************************************
      // do search
      rootMovesSearch(position, depth, alpha, beta);
      // *******************************************

      assert (searchCounter.currentBestRootValue != Evaluation.NOVALUE);

      // sure mate value found?
      if (searchCounter.currentBestRootValue >= Evaluation.CHECKMATE - depth ||
          searchCounter.currentBestRootValue <= -Evaluation.CHECKMATE + depth) {
        stopSearch = true;
      }

      // send info to UCI
      // @formatter:off
      engine.sendInfoToUCI("depth " + searchCounter.currentSearchDepth
                           + " seldepth " + searchCounter.currentExtraSearchDepth
                           + " multipv 1"
                           + " score cp " + searchCounter.currentBestRootValue
                           + " nodes " + searchCounter.nodesVisited
                           + " nps " + 1000 * (searchCounter.nodesVisited / (elapsedTime()+2L))
                           + " time " + elapsedTime()
                           + " pv " + principalVariation[0].toNotationString());
      // @formatter:on

      // check if we need to stop search - could be external or time.
      if (stopSearch || softTimeLimitReached() || hardTimeLimitReached()) {
        break;
      }

    } while (++depth <= searchMode.getMaxDepth());
    // ### ENDOF Iterative Deepening
    // #############################

    // create searchResult here
    assert (principalVariation[0].get(0) == searchCounter.currentBestRootMove);
    searchResult.bestMove = searchCounter.currentBestRootMove;
    searchResult.resultValue = searchCounter.currentBestRootValue;
    searchResult.depth = searchCounter.currentSearchDepth;
    searchResult.extraDepth = searchCounter.currentExtraSearchDepth;
    // retrieved ponder move from pv
    searchResult.ponderMove = Move.NOMOVE;
    if (principalVariation[0].size() > 1 && (principalVariation[0].get(1)) != Move.NOMOVE) {
      searchResult.ponderMove = principalVariation[0].get(1);
    }

    // search is finished - stop timer
    stopTime = System.currentTimeMillis();
    searchCounter.lastSearchTime = elapsedTime(stopTime);

    // print result of the search
    if (LOG.isInfoEnabled()) {
      LOG.info("{}", String.format(
        "Search complete. " + "Nodes visited: %,d " + "Boards Evaluated: %,d (+%,d) " +
        "Captures: %,d " + "EP: %,d " + "Checks: %,d " + "Mates: %,d ", searchCounter.nodesVisited,
        searchCounter.leafPositionsEvaluated, searchCounter.nonLeafPositionsEvaluated,
        searchCounter.captureCounter, searchCounter.enPassantCounter, searchCounter.checkCounter,
        searchCounter.checkMateCounter));
      LOG.info("Search Depth was {} ({})", searchCounter.currentSearchDepth,
               searchCounter.currentExtraSearchDepth);
      LOG.info("Search took {}", DurationFormatUtils.formatDurationHMS(elapsedTime(stopTime)));
      LOG.info("Speed: {}", String.format("%,d", (int) (searchCounter.leafPositionsEvaluated /
                                                        (elapsedTime() / 1e3))) + " nps");
    }

    return searchResult;
  }

  /**
   * Performs the search on the root moves and calls the recursive search for each move
   *
   * @param position
   * @param depth
   * @param alpha
   * @param beta
   */
  private void rootMovesSearch(Position position, int depth, int alpha, int beta) {

    // root node is always first searched node
    searchCounter.nodesVisited++;

    // root = ply 0
    final int rootPly = 0;

    // search for bestValue among root moves
    int bestValue = -Evaluation.INFINITE;

    // current search depth
    searchCounter.currentSearchDepth = 0;
    searchCounter.currentExtraSearchDepth = 0;

    // #########################################################
    // ##### ROOT MOVES
    // ##### Iterate through all available root moves
    for (int i = 0; i < rootMoves.size(); i++) {
      final int move = rootMoves.getMove(i);

      // store the current move
      searchCounter.currentRootMove = move;
      searchCounter.currentRootMoveNumber = i + 1;

      // #### START - Commit move and go deeper into recursion
      position.makeMove(move);
      currentVariation.add(move);

      //      ASPIRATION
      //      int previousValue = rootMoves.get(i).value;
      //      if (depth < 4 || !config.USE_ASPIRATION_WINDOW || isPerftSearch()) {
      //        value = -search(position, depth - 1, rootPly + 1, alpha, beta, IS_PV, NO_NULL);
      //      } else {
      //
      //        // ########################################
      //        // ### START ASPIRATION WINDOW SEARCH   ###
      //        int delta = 50; // ASPIRATION WINDOW
      //        alpha = Math.max(previousValue - delta, -Evaluation.INFINITE);
      //        beta = Math.min(previousValue + delta, Evaluation.INFINITE);
      //        value = -search(position, depth - 1, rootPly + 1, alpha, beta, NO_PV, DO_NULL);
      //        if (value <= alpha || value >= beta) {
      //          // failed
      //          alpha = -Evaluation.INFINITE;
      //          beta = Evaluation.INFINITE;
      //          value = -search(position, depth - 1, rootPly + 1, alpha, beta, IS_PV, DO_NULL);
      //        }
      //        // ### END ASPIRATION WINDOW SEARCH     ###
      //        // ########################################
      //
      //      }

      //        ASPIRATION Search with loop
      //        do {
      //          value = -search(position, depth - 1, rootPly + 1, alpha, beta, false);
      //          if (value <= alpha) {
      //            // failed low
      //            beta = (alpha + beta) / 2;
      //            alpha = Math.max(value - delta, -Evaluation.Value.INFINITE);
      //          } else if (value >= beta) {
      //            // failed high
      //            beta = Math.max(value + delta, Evaluation.Value.INFINITE);
      //          } else {
      //            // not failed
      //            break;
      //          }
      //          delta += delta / 4 + 5;
      //        } while (true);

      int value;

      // ########################################
      // ### START PVS ROOT SEARCH            ###
      if (!config.USE_PVS || isPerftSearch() || bestValue == -Evaluation.INFINITE // no pv yet
      ) {
        value = -search(position, depth - 1, rootPly + 1, -beta, -alpha, true, true);
      } else {
        // try null window search
        value = -search(position, depth - 1, rootPly + 1, -alpha - 1, -alpha, false, true);
        if (alpha < value && value < beta) { // not failed - research
          searchCounter.pv_root_researches++;
          value = -search(position, depth - 1, rootPly + 1, -beta, -alpha, true, true);
        } else {
          searchCounter.pv_root_cutoffs++;
        }
      }
      // ### END PVS ROOT SEARCH              ###
      // ########################################

      position.undoMove();
      currentVariation.removeLast();
      // #### END - Commit move and go deeper into recursion

      // write the value back to the root moves list
      rootMoves.set(i, move, value);

      // Evaluate the calculated value and compare to current best move
      // TODO: && stopSearch is a workaround for now - need to find cause of issue
      //  when stopping via time to still send viable move back down.
      if (value > bestValue && !stopSearch) {
        bestValue = value;
        MoveList.savePV(move, principalVariation[rootPly + 1], principalVariation[rootPly]);
        storeTT(position, depth, TT_EntryType.EXACT, bestValue);
        if (value > alpha) {
          alpha = value;
          searchCounter.currentBestRootMove = move;
          searchCounter.currentBestRootValue = value;
        }
        if (value >= beta) {
          LOG.warn("value >= beta at Root Search");
        }
      }

      // check if we need to stop search - could be external or time.
      // we should have any best move here
      if (stopSearch || hardTimeLimitReached()) {
        break;
      }

    } // end for root moves loop
    // ##### Iterate through all available moves
    // #########################################################

    if (config.USE_ROOT_MOVES_SORT) {
      // sort root moves - higher values first
      // best move is not necessarily at index 0
      // best move is in _currentBestMove or _principalVariation[0].get(0)
      rootMoves.sort();
      // push PV move to head of list
      if (principalVariation[0].size() != 0) {
        rootMoves.pushToHead(principalVariation[0].getFirst());
      }
    }

  }

  /**
   * Search - recursive search
   *
   * @param position
   * @param depthLeft
   * @param ply
   * @param pvSearch
   * @param doNullMove
   * @return value of the search
   */
  private int search(Position position, int depthLeft, int ply, int alpha, int beta,
                     boolean pvSearch, final boolean doNullMove) {

    // update current search depth stats
    if (searchCounter.currentSearchDepth < ply) {
      searchCounter.currentSearchDepth = ply;
    }
    if (searchCounter.currentExtraSearchDepth < ply) {
      searchCounter.currentExtraSearchDepth = ply;
    }

    // on leaf node call qsearch
    if (depthLeft < 1) {
      return qsearch(position, ply, alpha, beta);
    }

    // check draw through 50-moves-rule, 3-fold-repetition, insufficient material
    if (!isPerftSearch()) {
      if (position.check50Moves() || position.check3Repetitions() ||
          position.checkInsufficientMaterial()) {
        return Evaluation.DRAW;
      }
    }

    // check if we search max allowed nodes and update nodes counter
    if (searchMode.getNodes() > 0 && searchCounter.nodesVisited >= searchMode.getNodes()) {
      stopSearch = true;
      return alpha;
    }
    searchCounter.nodesVisited++;

    // ###############################################
    // ## BEGIN Mate Distance Pruning
    // ## Did we already find a shorter mate then ignore this one
    if (config.USE_MATE_DISTANCE_PRUNING && !isPerftSearch()) {
      alpha = Math.max(-Evaluation.CHECKMATE + ply, alpha);
      beta = Math.min(Evaluation.CHECKMATE - ply, beta);
      if (alpha >= beta) {
        searchCounter.mateDistancePrunings++;
        return alpha;
      }
    }
    // ## ENDOF Mate Distance Pruning
    // ###############################################

    // ###############################################
    // TT Lookup
    int ttValue = probeTT(position, ply, depthLeft, alpha, beta);
    if (ttValue != Evaluation.NOVALUE) {
      assert (ttValue != Evaluation.MIN);
      // in PV node only return ttValue if it was an exact hit
      if (!pvSearch || (alpha < ttValue && ttValue < beta)) {
        return ttValue;
      }
    }
    // End TT Lookup
    // ###############################################

    // Static Eval
    int staticEval = 0;
    if (!isPerftSearch()) {
      staticEval = evaluate(position);
    }

    // ###############################################
    // STATIC NULL MOVE PRUNING
    // https://www.chessprogramming.org/Reverse_Futility_Pruning
    // Cut very bad position when static eval is low anyway
    // @formatter:off
    if (config.USE_STATIC_NULL_PRUNING
        && !isPerftSearch()
        && !pvSearch
        && depthLeft < config.STATIC_NULL_PRUNING_DEPTH
        && doNullMove
        && !position.hasCheck()
        && Math.abs(beta) < Evaluation.CHECKMATE_THRESHOLD
    ) {
      final int evalMargin = config.STATIC_NULL_PRUNING_MARGIN * depthLeft;
      if (staticEval - evalMargin > beta ){
        return staticEval - evalMargin;
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
        && !pvSearch
        && depthLeft > config.NULL_MOVE_DEPTH
        && doNullMove
        && !position.hasCheck()
        && bigPiecePresent(position)
    ) {
      // @formatter:on

      position.makeNullMove();
      // null move search
      int reduction = depthLeft > 6 ? 3 : 2;
      int nullValue =
        -search(position, depthLeft - reduction, ply + 1, -beta, -beta + 1, NO_PV, NO_NULL);
      position.undoNullMove();

      // TODO: NULL Verification

      // Do not return unproven mate values
      if (nullValue > Evaluation.CHECKMATE_THRESHOLD) {
        nullValue = Evaluation.CHECKMATE_THRESHOLD;
      }

      // pruning
      if (nullValue >= beta) {
        searchCounter.nullMovePrunings++;
        return nullValue;
      }
    }
    // NULL MOVE PRUNING
    // ###############################################

    // ###############################################
    // RAZORING
    // @formatter:off
    if (config.USE_RAZOR_PRUNING
        && !isPerftSearch()
        && !pvSearch
        && depthLeft <= config.RAZOR_PRUNING_DEPTH
        && !position.hasCheck()
    ) {
      final int threshold = alpha - config.RAZOR_PRUNING_MARGIN;
      if (staticEval < threshold ){
        final int qsearchVal = qsearch(position, ply, alpha, beta);
        if (qsearchVal < threshold) return alpha;
      }
    }
    // @formatter:on
    // RAZORING
    // ###############################################

    // needed to remember if we even had a legal move
    int numberOfSearchedMoves = 0;

    // Generate moves
    MoveList moves = moveGenerators[ply].getPseudoLegalMoves(position);
    searchCounter.movesGenerated += moves.size();

    // if we have already a PV move from the last iteration push it to the head
    // of the move list to be evaluated first for more cutoffs
    if (config.USE_PVS_MOVE_ORDERING) {
      if (principalVariation[ply].size() > 0) {
        moves.pushToHead(principalVariation[ply].getFirst());
      }
    }

    // clear principal Variation for this depth
    principalVariation[ply].clear();

    // Initialize best values
    int bestValue = -Evaluation.INFINITE;

    // Prepare hash type
    TT_EntryType ttType = TT_EntryType.ALPHA;

    // Search all generated moves
    for (int i = 0; i < moves.size(); i++) {
      int move = moves.get(i);
      int value;

      // Minor Promotion Pruning
      if (config.USE_MINOR_PROMOTION_PRUNING && !isPerftSearch()) {
        // @formatter:off
        if (Move.getMoveType(move) == MoveType.PROMOTION
            && Move.getPromotion(move).getType() != PieceType.QUEEN
            && Move.getPromotion(move).getType() != PieceType.KNIGHT) {
          // prune non queen or knight promotion as they are redundant
          // exception would be stale mate situations.
          searchCounter.minorPromotionPrunings++;
          continue;
        }
        // @formatter:on
      }

      position.makeMove(move);

      // Skip illegal moves
      if (wasIllegalMove(position)) {
        position.undoMove();
        continue;
      }

      // needed to remember if we even had a legal move
      numberOfSearchedMoves++;

      // keep track of current variation
      currentVariation.add(move);

      // update UCI
      sendUCIUpdate(position);

      // go one ply deeper into the search tree
      if (isPerftSearch()) {
        value = -search(position, depthLeft - 1, ply + 1, -beta, -alpha, IS_PV, NO_NULL);
      } else {

        // TODO: depth reduction for later moves

        // ########################################
        // ### START PVS ###
        if (!config.USE_PVS || !pvSearch || bestValue == -Evaluation.INFINITE) {
          // no PV yet - do a full search
          value = -search(position, depthLeft - 1, ply + 1, -beta, -alpha, pvSearch, DO_NULL);
        } else {
          // try null window search
          value = -search(position, depthLeft - 1, ply + 1, -alpha - 1, -alpha, NO_PV, DO_NULL);
          if (value > alpha && value < beta) {
            // no fail - do a full research
            searchCounter.pv_researches++;
            value = -search(position, depthLeft - 1, ply + 1, -beta, -alpha, IS_PV, DO_NULL);
          } else {
            searchCounter.pv_cutoffs++;
          }
        }
        // ### END PVS ###
        // ########################################
      }

      currentVariation.removeLast();
      position.undoMove();

      // PRUNING START
      if (value > bestValue) { // to find first PV
        bestValue = value;
        MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);
        if (value > alpha) { // improved?
          if (value >= beta) { // fail-high
            if (config.USE_ALPHABETA_PRUNING && !isPerftSearch()) {
              // TODO KILLER MOVES
              searchCounter.prunings++;
              storeTT(position, depthLeft, TT_EntryType.BETA, beta);
              return beta;
            }
          }
          // alpha improved
          alpha = value;
          ttType = TT_EntryType.EXACT;
        }
      }
      // PRUNING END

      // check if we need to stop search - could be external or time.
      // we should have any best move here
      if (stopSearch || hardTimeLimitReached()) {
        break;
      }
    } // iteration over all moves

    // if we did not have a legal move then we have a mate
    if (numberOfSearchedMoves == 0 && !stopSearch) {
      // as we will not enter evaluation we count it here
      searchCounter.nonLeafPositionsEvaluated++;
      if (position.hasCheck()) {
        // We have a check mate. Return a -CHECKMATE.
        int value = -Evaluation.CHECKMATE + ply;
        storeTT(position, depthLeft, TT_EntryType.EXACT, value);
        return value;
      } else {
        // We have a stale mate. Return the draw value.
        storeTT(position, depthLeft, TT_EntryType.EXACT, Evaluation.DRAW);
        return Evaluation.DRAW;
      }
    }

    storeTT(position, depthLeft, ttType, alpha);
    return alpha;
  }

  private int qsearch(Position position, int ply, int alpha, int beta) {

    // update current search depth stats
    if (searchCounter.currentExtraSearchDepth < ply) {
      searchCounter.currentExtraSearchDepth = ply;
    }

    // check draw through 50-moves-rule, 3-fold-repetition, insufficient material
    if (!isPerftSearch()) {
      if (position.check50Moves() || position.check3Repetitions() ||
          position.checkInsufficientMaterial()) {
        return Evaluation.DRAW;
      }
    }

    // nodes counter
    if (searchMode.getNodes() > 0 && searchCounter.nodesVisited >= searchMode.getNodes()) {
      stopSearch = true;
      return alpha;
    }
    searchCounter.nodesVisited++;

    // evaluate position
    int value = evaluate(position);

    // Return with evaluation if no QUIESCENCE or PERFT
    if (isPerftSearch() || !config.USE_QUIESCENCE) {
      storeTT(position, 0, TT_EntryType.EXACT, value);
      return value;
    }

    // use evaluation as a standing pat (lower bound)
    if (!position.hasCheck()) {
      // Standing Pat
      if (value > alpha) {
        if (value >= beta) {
          return value; // TODO: value or beta???
        }
        alpha = value;
      }
    }

    // needed to remember if we even had a legal move
    int numberOfSearchedMoves = 0;

    // TODO QUIESCENCE
    // TODO Check that there is no endless qsearch due to
    // TODO endless captures or checks

    // Generate all PseudoLegalMoves for QSearch
    // Usually only capture moves and check evasions
    // will be determined in move generator
    MoveList moves;
    moves = moveGenerators[ply].getPseudoLegalQSearchMoves(position);
    searchCounter.movesGenerated += moves.size();

    // if we have already a PV move from the last iteration push it to the head
    // of the move list to be evaluated first for more cutoffs
    if (config.USE_PVS_MOVE_ORDERING) {
      if (principalVariation[ply].size() > 0) {
        moves.pushToHead(principalVariation[ply].getFirst());
      }
    }

    // clear principal Variation for this depth
    principalVariation[ply].clear();

    int bestValue = -Evaluation.INFINITE;

    // moves to search recursively
    for (int i = 0; i < moves.size(); i++) {
      int move = moves.get(i);

      // TODO: Delta Cutoff
      //  https://www.chessprogramming.org/Delta_Pruning
      // TODO: Bad Capture or SEE

      position.makeMove(move);

      // Skip illegal moves
      if (wasIllegalMove(position)) {
        position.undoMove();
        continue;
      }

      // needed to remember if we even had a legal move
      numberOfSearchedMoves++;

      // count as non quiet board
      searchCounter.positionsNonQuiet++;

      // needed to remember if we even had a legal move
      currentVariation.add(move);

      // update UCI
      sendUCIUpdate(position);

      // go one ply deeper into the search tree
      value = -qsearch(position, ply + 1, -beta, -alpha);

      currentVariation.removeLast();
      position.undoMove();

      // PRUNING START
      if (value > bestValue) { // to find first PV
        bestValue = value;
        MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);
        if (value > alpha) { // improved?
          if (value >= beta) { // fail-high
            if (config.USE_ALPHABETA_PRUNING && !isPerftSearch()) {
              // TODO KILLER MOVES
              searchCounter.prunings++;
              return beta;
            }
          }
          // alpha improved
          alpha = value;
        }
      }
      // PRUNING END

      // check if we need to stop search - could be external or time.
      // we should have any best move here
      if (stopSearch || hardTimeLimitReached()) {
        break;
      }
    } // iteration over all qmoves

    // if we did not have a legal move then we have a mate
    if (numberOfSearchedMoves == 0 && position.hasCheck()) {
      // as we will not enter evaluation we count it here
      searchCounter.leafPositionsEvaluated++;
      if (position.hasCheck()) {
        // We have a check mate. Return a -CHECKMATE.
        int mateValue = -Evaluation.CHECKMATE + ply;
        storeTT(position, MAX_SEARCH_DEPTH, TT_EntryType.EXACT, mateValue);
        return mateValue;
      } else {
        // We have a stale mate. Return the draw value.
        storeTT(position, MAX_SEARCH_DEPTH, TT_EntryType.EXACT, Evaluation.DRAW);
        return Evaluation.DRAW;
      }
    }

    return alpha;
  }

  private int evaluate(Position position) {

    // update some perft stats - no real performance hit here
    int lastMove = position.getLastMove();
    if (Move.getTarget(lastMove) != Piece.NOPIECE) {
      searchCounter.captureCounter++;
    }
    if (Move.getMoveType(lastMove) == MoveType.ENPASSANT) {
      searchCounter.enPassantCounter++;
    }

    // special cases for PERFT testing as hasCheck is expensive
    // that is on normal searches we do not count checks and mates
    if (isPerftSearch()) {
      if (position.hasCheck()) {
        searchCounter.checkCounter++;
        if (position.hasCheckMate()) {
          searchCounter.checkMateCounter++;
        }
      }
      searchCounter.leafPositionsEvaluated++;
      return 1;
    }

    // call the evaluation
    // count all leaf nodes evaluated
    final int value = evaluator.evaluate(position);
    searchCounter.leafPositionsEvaluated++;

    storeTT(position, 0, TT_EntryType.EXACT, value);
    return value;
  }

  private boolean wasIllegalMove(final Position position) {
    return position.isAttacked(position.getNextPlayer(),
                               position.getKingSquares()[position.getNextPlayer()
                                                                 .getInverseColor()
                                                                 .ordinal()]);
  }

  private void storeTT(final Position position, final int depthLeft, final TT_EntryType ttType,
                       final int value) {
    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch() && !stopSearch) {
      transpositionTable.put(position, value, ttType, depthLeft);
      // FIXME
      // if (Math.abs(value) > Evaluation.CHECKMATE - MAX_SEARCH_DEPTH &&
      //   ttType == TT_EntryType.EXACT) {
      //   // LOG.debug("STORE: ttType: {} ttValue: {} ttDepth: {} Depthleft: {}", ttType,
      //   value,
      //   // depthLeft, depthLeft);
      // }
    }
  }

  private int probeTT(final Position position, final int ply, final int depthLeft, final int alpha,
                      final int beta) {
    TT_Entry ttEntry;

    // TODO: MATE value need correction

    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch()) {
      ttEntry = transpositionTable.get(position);

      if (ttEntry != null) {
        // hit
        searchCounter.nodeCache_Hits++;
        if (ttEntry.depth >= depthLeft) { // only if tt depth was equal or deeper
          int value = ttEntry.value;

          // check the retrieved hash table entry
          if (ttEntry.type == TT_EntryType.EXACT) {

            // FIXME: MATE Values could be wrong - need correction due depth???
            // compensate for mate in # moves - the value in hash table is absolute
            // and must be corrected by current ply
            if (Math.abs(value) > Evaluation.CHECKMATE - MAX_SEARCH_DEPTH) {
              // LOG.debug("READ: ttType: {} ttValue: {} ttDepth: {} Ply: {}
              // Depthleft: {}",
              // ttEntry.type, value, ttEntry.depth, ply, depthLeft);
              if (value > 0) {
                value -= 0;
              } else {
                value += 0;
              }
            }

            return value;
          } else if (ttEntry.type == TT_EntryType.ALPHA) {
            if (value <= alpha) {
              return alpha;
            }
          } else if (ttEntry.type == TT_EntryType.BETA) {
            if (value >= beta) {
              return beta;
            }
          }
        }
      }
      // miss
      searchCounter.nodeCache_Misses++;
    }
    return Evaluation.NOVALUE;
  }

  private boolean isPerftSearch() {
    return config.PERFT || searchMode.isPerft();
  }

  private boolean softTimeLimitReached() {
    if (!searchMode.isTimeControl()) {
      return false;
    }
    stopSearch = elapsedTime() >= softTimeLimit;
    return stopSearch;
  }

  private boolean hardTimeLimitReached() {
    if (!searchMode.isTimeControl()) {
      return false;
    }
    stopSearch = elapsedTime() >= hardTimeLimit;
    return stopSearch;
  }

  private long elapsedTime() {
    return System.currentTimeMillis() - startTime;
  }

  private long elapsedTime(final long stopTime) {
    return stopTime - startTime;
  }

  /**
   * Configure time limits<br>
   * Chooses if search mode is time per move or remaining time
   * and set time limits accordingly
   */
  private void configureTimeLimits() {

    // TODO calculate time inc into the estimation

    if (searchMode.getMoveTime().toMillis() > 0) { // mode time per move
      hardTimeLimit = searchMode.getMoveTime().toMillis();
    } else { // remaining time - estimated time per move
      // reset flags
      long timeLeft = searchMode.getRemainingTime(myColor).toMillis();
      // Give some overhead time so that in games with very low available time we do not run out
      // of time
      timeLeft -= 1000; // this should do
      // when we know the move to go (until next time control) use them otherwise assume 40
      final int movesLeft = searchMode.getMovesToGo() > 0 ? searchMode.getMovesToGo() : 40;
      // for timed games with remaining time
      hardTimeLimit = Duration.ofMillis((long) ((timeLeft / movesLeft) * 1.0f)).toMillis();
    }

    softTimeLimit = (long) (hardTimeLimit * 0.8f);
    // limits for very short available time
    if (hardTimeLimit < 100) {
      hardTimeLimit = (long) (hardTimeLimit * 0.9f);
      softTimeLimit = (long) (hardTimeLimit * 0.8f);
    }
    // limits for higher available time
    else if (hardTimeLimit > 10000) {
      softTimeLimit = hardTimeLimit;
    }
  }

  /**
   * Returns true if at least on non pawn/king piece is on the
   * board for the moving side.
   *
   * @param position
   * @return
   */
  private static boolean bigPiecePresent(Position position) {
    final int activePlayer = position.getNextPlayer().ordinal();
    return !(position.getKnightSquares()[activePlayer].isEmpty() &&
             position.getBishopSquares()[activePlayer].isEmpty() &&
             position.getRookSquares()[activePlayer].isEmpty() &&
             position.getQueenSquares()[activePlayer].isEmpty());
  }

  private void sendUCIUpdate(final Position position) {
    // send current root move info to UCI every x milli seconds
    if (System.currentTimeMillis() - uciUpdateTicker >= 250) {
      // @formatter:off
      engine.sendInfoToUCI("depth " + searchCounter.currentSearchDepth
                             + " seldepth " + searchCounter.currentExtraSearchDepth
                             + " nodes " + searchCounter.nodesVisited
                             + " nps " + 1000 * searchCounter.nodesVisited / elapsedTime()
                             + " time " + elapsedTime()
                             + " hashfull " + (int) (1000 * ((float)transpositionTable.getNumberOfEntries()
                                               / transpositionTable.getMaxEntries()))
                            );
      engine.sendInfoToUCI("currmove " + Move.toUCINotation(position,
                                                              searchCounter.currentRootMove)
                             + " currmovenumber " + searchCounter.currentRootMoveNumber
                            );
      if (config.UCI_ShowCurrLine) {
        engine.sendInfoToUCI("currline " + currentVariation.toNotationString());
      }
      // @formatter:on
      uciUpdateTicker = System.currentTimeMillis();
    }
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
   * Called when the state of this search is no longer valid as the last call to startSearch is
   * not from
   * the same game as the next.
   */
  public void newGame() {
    transpositionTable.clear();
  }

  /**
   * @return true if previous search is still running
   */
  public boolean isSearching() {
    return searchThread != null && searchThread.isAlive();
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
   * Clears the hashtables
   */
  public void clearHashTables() {
    transpositionTable.clear();
  }

  /**
   * Parameter class for the search result
   */
  static final class SearchResult {

    int  bestMove    = Move.NOMOVE;
    int  ponderMove  = Move.NOMOVE;
    int  resultValue = Evaluation.NOVALUE;
    long time        = -1;
    int  depth       = 0;
    int  extraDepth  = 0;

    @Override
    public String toString() {
      return "Best Move: " + Move.toString(bestMove) + " (" + resultValue + ") " +
             " Ponder Move: " + Move.toString(ponderMove) + " Depth: " + depth + "/" + extraDepth;
    }
  }

  /**
   * Convenience Wrapper class for all counters
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
    long pv_root_researches     = 0;
    long pv_root_cutoffs        = 0;
    long pv_researches          = 0;
    long pv_cutoffs             = 0;
    long nodeCache_Hits         = 0;
    long nodeCache_Misses       = 0;
    long movesGenerated         = 0;
    long nodesVisited           = 0;
    int  minorPromotionPrunings = 0;
    int  mateDistancePrunings   = 0;
    int  nullMovePrunings       = 0;


    private void resetCounter() {
      currentIterationDepth = 0;
      currentSearchDepth = 0;
      currentExtraSearchDepth = 0;
      currentRootMove = 0;
      currentRootMoveNumber = 0;
      nodesVisited = 0;
      leafPositionsEvaluated = 0;
      positionsNonQuiet = 0;
      prunings = 0;
      pv_root_researches = 0;
      pv_root_cutoffs = 0;
      pv_researches = 0;
      pv_cutoffs = 0;
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
             ", pv_root_researches=" + pv_root_researches +
             ", pv_root_cutoffs=" + pv_root_cutoffs +
             ", pv_researches=" + pv_researches +
             ", pv_cutoffs=" + pv_cutoffs +
             ", mateDistancePrunings=" + mateDistancePrunings +
             ", minorPromotionPrunings=" + minorPromotionPrunings +
             ", nullMovePrunings=" + nullMovePrunings + '}';
      // @formatter:on
    }
  }
}
