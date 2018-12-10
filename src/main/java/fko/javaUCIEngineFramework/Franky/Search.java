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

package fko.javaUCIEngineFramework.Franky;

import fko.javaUCIEngineFramework.Franky.TranspositionTable.TT_Entry;
import fko.javaUCIEngineFramework.Franky.TranspositionTable.TT_EntryType;
import fko.javaUCIEngineFramework.UCI.IUCIEngine;
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
 * DONE: - QUIESCENCE (https://www.chessprogramming.org/Quiescence_Search)
 * TODO: - SEE (https://www.chessprogramming.org/Static_Exchange_Evaluation)
 * DONE: - Mate Distance Pruning (https://www.chessprogramming.org/Mate_Distance_Pruning)
 * DONE: - NULL MOVE PRUNING
 */
public class Search implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Search.class);

  // Readabilty constants
  private static final boolean DO_NULL = true;
  private static final boolean NO_NULL = false;
  private static final boolean IS_PV   = true;
  private static final boolean NO_PV   = false;

  private static final int MAX_SEARCH_DEPTH = 100;

  // search counters
  private final SearchCounter searchCounter = new SearchCounter();

  // back reference to the engine
  private final IUCIEngine    engine;
  final         Configuration config;

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
   * /**
   * Creates a search object and stores a back reference to the engine object.<br>
   * Hash is setup up to the given hash size.
   *
   * @param engine
   * @param config
   */
  public Search(IUCIEngine engine, Configuration config) {
    this.engine = engine;
    this.config = config;

    // set hash sizes
    setHashSize(this.config.HASH_SIZE);

    // Move Generators - each depth in search gets it own
    // to avoid object creation during search
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

    // create a deep copy of the position
    this.currentPosition = new Position(position);
    this.myColor = currentPosition.getNextPlayer();
    this.searchMode = searchMode;

    // setup latch
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
    // set stop flag - search needs to check regularly and stop accordingly
    stopSearch = true;

    // return if no search is running
    if (searchThread == null) {
      return;
    }

    LOG.info("Search has been stopped");

    // Wait for the thread to die
    try {
      this.searchThread.join();
    } catch (InterruptedException ignored) {
    }

    // clear thread
    searchThread = null;
  }

  /**
   * Is called when our last ponder suggestion has been executed by opponent.
   * If we are already pondering just continue the search but switch to time control.
   * If we were not pondering start searching.
   */
  public void ponderHit() {
    if (isSearching() && searchMode.isPonder()) {
      LOG.info("****** PONDERHIT *******");
      startTime = System.currentTimeMillis();
      searchMode.ponderHit();
      // if time based game setup the time soft and hard time limits
      if (searchMode.isTimeControl()) {
        configureTimeLimits();
      }
    } else {
      LOG.warn("Ponderhit when not pondering!");
    }
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
      LOG.info("****** PERFT SEARCH *******");
    }
    if (searchMode.isPonder()) {
      LOG.info("****** PONDER SEARCH *******");
    }
    if (searchMode.isInfinite()) {
      LOG.info("****** INFINITE SEARCH *******");
    }
    if (searchMode.getMate() > 0) {
      LOG.info("****** MATE SEARCH *******");
    }

    // reset lastSearchResult
    lastSearchResult = new SearchResult();

    // reset counter
    searchCounter.resetCounter();

    // reset time limits
    softTimeLimit = hardTimeLimit = 0;

    // release latch so the caller can continue
    waitForInitializationLatch.countDown();

    // run the search itself
    lastSearchResult = iterativeSearch(currentPosition);

    // if the mode still is ponder at this point we have a ponder miss
    if (searchMode.isPonder()) {
      LOG.info("Ponder Miss!");
    }

    LOG.info("Search result was: {} PV {}", lastSearchResult.toString(),
             principalVariation[0].toNotationString());

    // send result to engine
    engine.sendResult(lastSearchResult.bestMove, lastSearchResult.ponderMove);
  }

  /**
   * This starts the actual iterative search.
   *
   * @param position
   * @return the best move
   */
  private SearchResult iterativeSearch(Position position) {

    // remember the start of the search
    startTime = System.currentTimeMillis();
    uciUpdateTicker = System.currentTimeMillis();

    // generate all root moves
    MoveList legalMoves = moveGenerators[0].getLegalMoves(position);

    // no legal root moves - game already ended!
    if (legalMoves.size() == 0) {
      return new SearchResult();
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

    int depth = searchMode.getStartDepth();

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

    // #############################
    // ### BEGIN Iterative Deepening
    do {
      searchCounter.currentIterationDepth = depth;

      // do search
      rootMovesSearch(position, depth);

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

    // we should have a sorted rootMoves list here
    // create searchResult here
    searchResult.bestMove = searchCounter.currentBestRootMove;
    searchResult.resultValue = searchCounter.currentBestRootValue;
    searchResult.depth = searchCounter.currentSearchDepth;
    searchResult.extraDepth = searchCounter.currentExtraSearchDepth;

    // retrieved ponder move from pv
    int p_move;
    if (principalVariation[0].size() > 1 && (p_move = principalVariation[0].get(1)) !=
                                            Move.NOMOVE) {
      searchResult.ponderMove = p_move;
    } else {
      searchResult.ponderMove = Move.NOMOVE;
    }

    stopTime = System.currentTimeMillis();
    searchCounter.lastSearchTime = elapsedTime(stopTime);

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
   */
  private void rootMovesSearch(Position position, int depth) {

    final int rootPly = 0;

    // current search depth
    searchCounter.currentSearchDepth = 0;
    searchCounter.currentExtraSearchDepth = 0;

    int bestValue = Evaluation.NOVALUE;

    int alpha = -Evaluation.INFINITE;
    int beta = Evaluation.INFINITE;

    // ##### Iterate through all available root moves
    for (int i = 0; i < rootMoves.size(); i++) {
      int move = rootMoves.getMove(i);

      // store the current move for Engine Watcher
      searchCounter.currentRootMove = move;
      searchCounter.currentRootMoveNumber = i + 1;

      // #### START - Commit move and go deeper into recursion
      position.makeMove(move);
      currentVariation.add(move);

      int value;
      int previousValue = rootMoves.get(i).value;

      //      ASPIRATION
      //      if (depth < 4 || !config.USE_ASPIRATION_WINDOW || isPerftSearch()) {
      //        value = -negamax(position, depth - 1, rootPly + 1, alpha, beta, IS_PV, NO_NULL);
      //      } else {
      //
      //        // ########################################
      //        // ### START ASPIRATION WINDOW SEARCH   ###
      //        int delta = 50; // ASPIRATION WINDOW
      //        alpha = Math.max(previousValue - delta, -Evaluation.INFINITE);
      //        beta = Math.min(previousValue + delta, Evaluation.INFINITE);
      //        value = -negamax(position, depth - 1, rootPly + 1, alpha, beta, NO_PV, DO_NULL);
      //        if (value <= alpha || value >= beta) {
      //          // failed
      //          alpha = -Evaluation.INFINITE;
      //          beta = Evaluation.INFINITE;
      //          value = -negamax(position, depth - 1, rootPly + 1, alpha, beta, IS_PV, DO_NULL);
      //        }
      //        // ### END ASPIRATION WINDOW SEARCH     ###
      //        // ########################################
      //
      //      }

      //        ASPIRATION Search with loop
      //        do {
      //          value = -negamax(position, depth - 1, rootPly + 1, alpha, beta, false);
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

      // PVS in ROOT
      // ########################################
      // ### START PVS ROOT SEARCH            ###
      if (!config.USE_PVS || bestValue == Evaluation.NOVALUE || isPerftSearch()) { // no PV yet
        value = -negamax(position, depth - 1, rootPly + 1, -beta, -alpha, true, true);
      } else {
        // try null window search
        value = -negamax(position, depth - 1, rootPly + 1, -alpha - 1, -alpha, false, true);
        if (value > alpha && value < beta) { // not failed - research
          searchCounter.pv_root_researches++;
          value = -negamax(position, depth - 1, rootPly + 1, -beta, -alpha, true, true);
        } else {
          searchCounter.pv_root_cutoffs++;
        }
      }
      // ### END PVS ROOT SEARCH              ###
      // ########################################

      // write the value back to the root moves list
      rootMoves.set(i, move, value);

      // Evaluate the calculated value and compare to current best move
      if (value > bestValue) {
        bestValue = value;
        searchCounter.currentBestRootValue = value;
        searchCounter.currentBestRootMove = move;
        MoveList.savePV(move, principalVariation[rootPly + 1], principalVariation[rootPly]);
      }

      position.undoMove();
      currentVariation.removeLast();
      // #### END - Commit move and go deeper into recursion

      // check if we need to stop search - could be external or time.
      // we should have any best move here
      if (stopSearch || hardTimeLimitReached()) {
        break;
      }

    } // ##### Iterate through all available moves

    if (config.USE_ROOT_MOVES_SORT) {
      // sort root moves - higher values first
      // best move is not necessarily at index 0
      // best move is in _currentBestMove or _principalVariation[0].get(0)
      rootMoves.sort();
      // push PV move to head of list
      if (principalVariation[0].size() != 0) {
        rootMoves.pushToHead(principalVariation[0].get(0));
      }
    }
  }

  /**
   * NegaMax Search
   *
   * @param position
   * @param depthLeft
   * @param ply
   * @param pvSearch
   * @param doNullMove
   * @return value of the search
   */
  private int negamax(Position position, int depthLeft, int ply, int alpha, int beta,
                      boolean pvSearch, final boolean doNullMove) {

    // nodes counter
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

    // if in check extend search depth
    // this means no quiescence search while in check
    if (position.hasCheck()) {
      //      depthLeft++;
    }

    // on leaf node call quiescence
    if (depthLeft < 1) {
      return quiescence(position, ply, alpha, beta);
    }

    // current search depth
    if (searchCounter.currentSearchDepth < ply) {
      searchCounter.currentSearchDepth = ply;
    }
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

    // ###############################################
    // TT Lookup
    // TODO: MATE value need correction
    final TT_Entry ttEntry = probeTT(position);
    if (ttEntry != null) { // possible TT Hit
      if (ttEntry.depth >= depthLeft) { // only if tt depth was equal or deeper
        if (ttEntry.type == TT_EntryType.EXACT) {
          return ttEntry.value;
        } else if (ttEntry.type == TT_EntryType.ALPHA) {
          if (ttEntry.value <= alpha) {
            return alpha;
          }
        } else if (ttEntry.type == TT_EntryType.BETA) {
          if (ttEntry.value >= beta) {
            return beta;
          }
        }
      }
    }
    // End TT Lookup
    // ###############################################

    // ###############################################
    // NULL MOVE PRUNING
    if (config.USE_NULL_MOVE_PRUNING && !isPerftSearch() && !pvSearch
        // do we search outside of PV
        && depthLeft > config.NULL_MOVE_DEPTH
        // null move check only makes sense not too close to the leaf nodes
        && doNullMove                       // avoid multiple null moves in a row
        && !position.hasCheck()             // no null move pruning when in check
        && bigPiecePresent(position)        // avoid zugzwang by checking if officer is present
    ) {

      position.makeNullMove();
      // null move search
      int reduction = depthLeft > 6 ? 3 : 2;
      int nullValue = -negamax(position, depthLeft - reduction, ply + 1, -beta, -beta + 1, NO_PV,
                               NO_NULL);
      position.undoNullMove();

      // pruning
      if (nullValue >= beta) {
        searchCounter.nullMovePrunings++;
        return beta;
      }
    }
    // NULL MOVE PRUNING
    // ###############################################

    // clear principal Variation for this depth
    principalVariation[ply].clear();

    // needed to remember if we even had a legal move
    boolean hadLegaMove = false;

    // Generate moves
    MoveList moves = moveGenerators[ply].getPseudoLegalMoves(position);
    searchCounter.movesGenerated++;

    // TODO: Move list sorting needed????

    // Initialize best values
    int pvValue = Evaluation.NOVALUE;

    // Prepare hash type
    TT_EntryType ttType = TT_EntryType.ALPHA;

    // Search all generated moves
    for (int i = 0; i < moves.size(); i++) {
      int move = moves.get(i);
      int value;


      position.makeMove(move);

      // Skip illegal moves
      if (wasIllegalMove(position)) {
        position.undoMove();
        continue;
      }

      // Minor Promotion Pruning
      if (config.USE_MINOR_PROMOTION_PRUNING && !isPerftSearch()) {
        // @formatter:off
        if (Move.getMoveType(move) == MoveType.PROMOTION
            && Move.getPromotion(move).getType() != PieceType.QUEEN
            && Move.getPromotion(move).getType() != PieceType.KNIGHT) {
          // prune non queen or knight promotion as they are redundant
          // exception would be stale mate situations.
          searchCounter.minorPromotionPrunings++;
          position.undoMove();
          continue;
        }
        // @formatter:on
      }

      // needed to remember if we even had a legal move
      hadLegaMove = true;

      // keep track of current variation
      currentVariation.add(move);

      // update UCI
      sendUCIUpdate(position);

      // go one ply deeper into the search tree
      if (isPerftSearch()) {
        value = -negamax(position, depthLeft - 1, ply + 1, -beta, -alpha, IS_PV, NO_NULL);
      } else {

        // ########################################
        // ### START PVS ###
        if (!config.USE_PVS || !pvSearch || pvValue == Evaluation.NOVALUE) {
          // no PV yet - do a full search
          value = -negamax(position, depthLeft - 1, ply + 1, -beta, -alpha, pvSearch, DO_NULL);
        } else {
          // try null window search
          value = -negamax(position, depthLeft - 1, ply + 1, -alpha - 1, -alpha, NO_PV, DO_NULL);
          if (value > alpha && value < beta) {
            // no fail - do a full research
            searchCounter.pv_researches++;
            value = -negamax(position, depthLeft - 1, ply + 1, -beta, -alpha, IS_PV, DO_NULL);
          } else {
            searchCounter.pv_cutoffs++;
          }
        }
        // ### END PVS ###
        // ########################################
      }

      // PRUNING START
      if (value > pvValue) { // to find first PV
        pvValue = value;
        if (value > alpha) { // improved?
          if (value >= beta) { // fail-high
            if (config.USE_ALPHABETA_PRUNING && !isPerftSearch()) {
              storeTT(position, depthLeft, TT_EntryType.BETA, beta);
              currentVariation.removeLast();
              position.undoMove();
              searchCounter.prunings++;
              return value; // TODO: return value or beta???
            }
          }
          // alpha improved
          MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);
          alpha = value;
          ttType = TT_EntryType.EXACT;
        }
      }
      // PRUNING END

      currentVariation.removeLast();
      position.undoMove();

      // check if we need to stop search - could be external or time.
      // we should have any best move here
      if (stopSearch || hardTimeLimitReached()) {
        break;
      }
    }

    // if we did not have a legal move then we have a mate
    if (!hadLegaMove && !stopSearch) {
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

  private int quiescence(Position position, int ply, int alpha, int beta) {

    // ###############################################
    // TT Lookup
    final TT_Entry ttEntry = probeTT(position);
    if (ttEntry != null) { // possible TT Hit
      if (ttEntry.type == TT_EntryType.EXACT) {
        return ttEntry.value;
      } else if (ttEntry.type == TT_EntryType.ALPHA) {
        if (ttEntry.value <= alpha) {
          return alpha;
        }
      } else if (ttEntry.type == TT_EntryType.BETA) {
        if (ttEntry.value >= beta) {
          return beta;
        }
      }
    }
    // End TT Lookup
    // ###############################################

    // prepare hash type
    TT_EntryType ttType = null;

    // evaluate the position and use it a standing pat (lower bound)
    int value = evaluate(position);

    if (isPerftSearch() || !config.USE_QUIESCENCE) {
      storeTT(position, 0, TT_EntryType.EXACT, value);
      return value;
    }

    // Standing Pat
    if (value >= beta) {
      storeTT(position, 0, TT_EntryType.BETA, beta);
      return beta;
    }
    if (value > alpha) {
      ttType = TT_EntryType.EXACT;
      alpha = value;
    }

    // do we even have legal moves?
    if (moveGenerators[ply].hasLegalMove(position)) {

      // ##############################################################
      // START QUIESCENCE

      // ###############################################
      // ## BEGIN Mate Distance Pruning
      // ## Did we already find a shorter mate then ignore this one
      if (config.USE_MATE_DISTANCE_PRUNING && !isPerftSearch()) {
        alpha = Math.max(-Evaluation.CHECKMATE + ply, alpha);
        beta = Math.min(Evaluation.CHECKMATE - ply, beta);
        if (alpha >= beta) {
          return alpha;
        }
      }
      // ## ENDOF Mate Distance Pruning
      // ###############################################

      // TODO QUIESCENCE
      // TODO Check that there is no endless qsearch due to
      // TODO endless captures or checks

      // Generate all PseudoLegalMoves for QSearch
      // Usually only capture moves and check evasions
      // will be determined in move generator
      MoveList moves = moveGenerators[ply].getPseudoLegalQSearchMoves(position);

      // moves to search recursively
      for (int i = 0; i < moves.size(); i++) {
        int move = moves.get(i);

        position.makeMove(move);

        // Check if legal move before going into recursion
        if (wasIllegalMove(position)) {
          position.undoMove();
          continue;
        }

        // nodes counter
        if (searchMode.getNodes() > 0 && searchCounter.nodesVisited >= searchMode.getNodes()) {
          stopSearch = true;
          return value;
        }
        searchCounter.nodesVisited++;

        // count as non quiet board
        searchCounter.positionsNonQuiet++;

        // needed to remember if we even had a legal move
        currentVariation.add(move);

        // in quiescence search we count extra depth here
        if (searchCounter.currentExtraSearchDepth < ply) {
          searchCounter.currentExtraSearchDepth = ply;
        }

        // check draw through 50-moves-rule, 3-fold-repetition, insufficient material
        if (position.check50Moves() || position.check3Repetitions() ||
            position.checkInsufficientMaterial()) {
          value = Evaluation.DRAW;
        } else {
          // go one ply deeper into the search tree
          value = -quiescence(position, ply + 1, -beta, -alpha);
        }

        // PRUNING START
        if (value > alpha) {
          if (value >= beta) {
            storeTT(position, 0, TT_EntryType.BETA, beta);
            searchCounter.prunings++;
            currentVariation.removeLast();
            position.undoMove();
            return beta;
          }
          alpha = value;
          ttType = TT_EntryType.EXACT;
        }
        // PRUNING END

        currentVariation.removeLast();
        position.undoMove();

        // check if we need to stop search - could be external or time.
        // we should have any best move here
        if (stopSearch || hardTimeLimitReached()) {
          break;
        }
      }
      // END QUIESCENCE
      // ##############################################################

    } // no moves - mate position?
    else {
      // as we will not enter evaluation we count it here
      searchCounter.leafPositionsEvaluated++;
      if (position.hasCheck()) {
        // We have a check mate. Return a -CHECKMATE.
        value = -Evaluation.CHECKMATE + ply;
        storeTT(position, 0, TT_EntryType.EXACT, value);
        return value;
      } else {
        // We have a stale mate. Return the draw value.
        storeTT(position, 0, TT_EntryType.EXACT, Evaluation.DRAW);
        return Evaluation.DRAW;
      }
    }

    storeTT(position, 0, ttType, alpha);
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

    // ###############################################
    // TT Lookup
    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch()) {
      final TT_Entry ttEntry = transpositionTable.get(position);
      if (ttEntry != null) { // possible TT Hit
        if (ttEntry.type == TT_EntryType.EXACT) {
          searchCounter.nodeCache_Hits++;
          return ttEntry.value;
        }
      } else {
        searchCounter.nodeCache_Misses++;
      }
    }
    // End TT Lookup
    // ###############################################

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

  private TT_Entry probeTT(final Position position) {
    TT_Entry ttEntry = null;
    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch()) {
      ttEntry = transpositionTable.get(position);
      if (ttEntry == null) {
        searchCounter.nodeCache_Misses++;
        return null;
      }
      searchCounter.nodeCache_Hits++;
    }
    return ttEntry;
  }

  private void storeTT(final Position position, final int depthLeft, final TT_EntryType ttType,
                       final int bestValue) {
    if (config.USE_TRANSPOSITION_TABLE && !isPerftSearch()) {
      transpositionTable.put(position, bestValue, ttType, depthLeft);
    }
  }

  private boolean isPerftSearch() {
    return config.PERFT || searchMode.isPerft();
  }

  private boolean softTimeLimitReached() {
    if (!searchMode.isTimeControl()) {
      return false;
    }
    return elapsedTime() >= softTimeLimit;
  }

  private boolean hardTimeLimitReached() {
    if (!searchMode.isTimeControl()) {
      return false;
    }
    return elapsedTime() >= hardTimeLimit;
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
  public void clearHashtables() {
    transpositionTable.clear();
  }

  /**
   * Parameter class for the search result
   */
  static final class SearchResult {

    int  bestMove    = Move.NOMOVE;
    int  ponderMove  = Move.NOMOVE;
    int  resultValue = 0;
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
             ", pv_root_researches" + pv_root_researches +
             ", pv_root_cutoffs" + pv_root_cutoffs +
             ", pv_researches" + pv_researches +
             ", pv_cutoffs" + pv_cutoffs +
             ", mateDistancePrunings=" + mateDistancePrunings +
             ", minorPromotionPrunings=" + minorPromotionPrunings +
             ", nullMovePrunings=" + nullMovePrunings + '}';
      // @formatter:on
    }
  }
}