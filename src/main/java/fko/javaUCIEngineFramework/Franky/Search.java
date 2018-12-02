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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Search implements the actual search for best move of a given position.
 * <p>
 * Search runs in a separate thread when the actual search is started. When
 * the search is finished it calls <code>engine.sendResult</code> ith the best move and a ponder
 * move if it has one.
 *
 * TODO: - QUIESCENE (https://www.chessprogramming.org/Quiescence_Search)
 * TODO: - SEE (https://www.chessprogramming.org/Static_Exchange_Evaluation)
 * TODO: - Mate Distance Pruning (https://www.chessprogramming.org/Mate_Distance_Pruning)
 * TODO: - MTDf (http://people.csail.mit.edu/plaat/mtdf.html)
 *
 *
 *
 */
public class Search implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Search.class);

  private static final int MAX_SEARCH_DEPTH = 100;

  // search counters
  private final SearchCounter searchCounter = new SearchCounter();

  // back reference to the engine
  private         IUCIEngine    engine;
  protected final Configuration config;

  // the thread in which we will do the actual search
  private Thread searchThread = null;

  // flag to indicate to stop the search - can be called externally or via the timer clock.
  private boolean stopSearch = true;

  // hash tables
  private TranspositionTable transpositionTable;
  private EvaluationCache    evaluationCache;

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
  private BoardPosition currentBoardPosition;
  private Color         myColor;
  private SearchMode    searchMode;
  private SearchResult  lastSearchResult;

  // running search global variables
  private RootMoveList rootMoves          = new RootMoveList();
  // current variation of the search
  private MoveList     currentVariation   = new MoveList(MAX_SEARCH_DEPTH);
  private MoveList[]   principalVariation = new MoveList[MAX_SEARCH_DEPTH];

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
   * @param boardPosition
   * @param searchMode
   */
  public void startSearch(BoardPosition boardPosition, SearchMode searchMode) {
    if (searchThread != null && searchThread.isAlive()) {
      final String s = "Search already running - can only be started once";
      IllegalStateException e = new IllegalStateException(s);
      LOG.error(s, e);
      throw e;
    }

    // create a deep copy of the position
    this.currentBoardPosition = new BoardPosition(boardPosition);
    this.myColor = currentBoardPosition.getNextPlayer();
    this.searchMode = searchMode;

    // setup latch
    waitForInitializationLatch = new CountDownLatch(1);

    // reset the stop search flag
    stopSearch = false;

    // create new search thread and start it
    String threadName = "Engine: " + myColor.toString();
    if (this.searchMode.isPonder()) threadName += " (Pondering)";
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
    if (searchThread == null) return;

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

    if (isPerftSearch()) LOG.info("****** PERFT SEARCH *******");
    if (searchMode.isPonder()) LOG.info("****** PONDER SEARCH *******");
    if (searchMode.isInfinite()) LOG.info("****** INFINITE SEARCH *******");
    if (searchMode.getMate() > 0) LOG.info("****** MATE SEARCH *******");

    // reset lastSearchResult
    lastSearchResult = new SearchResult();

    // reset counter
    searchCounter.resetCounter();

    // reset time limits
    softTimeLimit = hardTimeLimit = 0;

    // release latch so the caller can continue
    waitForInitializationLatch.countDown();

    // run the search itself
    lastSearchResult = iterativeSearch(currentBoardPosition);

    // if the mode still is ponder at this point we have a ponder miss
    if (searchMode.isPonder()) {
      LOG.info("Ponder Miss!");
    }

    LOG.info("Search result was: {} Value {} PV {}", lastSearchResult.toString(),
             lastSearchResult.resultValue, principalVariation[0].toNotationString());

    // send result to engine
    engine.sendResult(lastSearchResult.bestMove, lastSearchResult.ponderMove);
  }

  /**
   * This starts the actual iterative search.
   *
   * @param position
   * @return the best move
   */
  private SearchResult iterativeSearch(BoardPosition position) {

    // remember the start of the search
    startTime = System.currentTimeMillis();

    // generate all root moves
    MoveList legalMoves = moveGenerators[0].getLegalMoves(position, false);

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
        rootMoves.add(legalMoves.get(i), Evaluation.Value.NOVALUE);
      } else {
        if (searchMode.getMoves().contains(Move.toUCINotation(position, legalMoves.get(i)))) {
          rootMoves.add(legalMoves.get(i), Evaluation.Value.NOVALUE);
        }
      }
    }

    // temporary best move - take the first move available
    searchCounter.currentBestRootMove = rootMoves.getMove(0);
    searchCounter.currentBestRootValue = Evaluation.Value.NOVALUE;

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
      if (searchCounter.currentBestRootValue >= Evaluation.Value.CHECKMATE - depth ||
          searchCounter.currentBestRootValue <= -Evaluation.Value.CHECKMATE + depth) {
        stopSearch = true;
      }

      // send info to UCI
      // @formatter:off
      engine.sendInfoToUCI("depth " + searchCounter.currentSearchDepth
                           + " seldepth " + searchCounter.currentExtraSearchDepth
                           + " multipv 1"
                           + " score cp " + (searchCounter.currentBestRootValue / 100f)
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
    searchResult.depth = searchCounter.currentIterationDepth;

    // retrieved ponder move from pv
    int p_move;
    if (principalVariation[0].size() > 1 &&
        (p_move = principalVariation[0].get(1)) != Move.NOMOVE) {
      searchResult.ponderMove = p_move;
    } else {
      searchResult.ponderMove = Move.NOMOVE;
    }

    stopTime = System.currentTimeMillis();
    searchCounter.lastSearchTime = elapsedTime(stopTime);

    if (LOG.isInfoEnabled()) {
      LOG.info("{}", String.format(
        "Search complete. " + "Nodes visited: %,d " + "Boards Evaluated: %,d " + "Captures: %,d " +
        "EP: %,d " + "Checks: %,d " + "Mates: %,d ", searchCounter.nodesVisited,
        searchCounter.leafPositionsEvaluated, searchCounter.captureCounter,
        searchCounter.enPassantCounter, searchCounter.checkCounter,
        searchCounter.checkMateCounter));
      LOG.info("Search Depth was {} ({})", searchCounter.currentIterationDepth,
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
  private void rootMovesSearch(BoardPosition position, int depth) {

    final int rootPly = 0;

    int bestValue = Evaluation.Value.NOVALUE;

    final int alpha = -Evaluation.Value.INFINITE;
    final int beta = Evaluation.Value.INFINITE;

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

      if (isPerftSearch()) {

        value = -negamax(position, depth - 1, rootPly + 1, -beta, -alpha, false);

      } else {

        // ########################################
        // ### START PVS ###
        if (!config.USE_PVS || bestValue == Evaluation.Value.NOVALUE) { // no PV yet
          value = -negamax(position, depth - 1, rootPly + 1, -beta, -alpha, false);
        } else {
          // try null window search
          value = -negamax(position, depth - 1, rootPly + 1, -alpha - 1, -alpha, true);
          if (value > alpha && value < beta) { // not failed - research
            searchCounter.pv_researches++;
            value = -negamax(position, depth - 1, rootPly + 1, -beta, -alpha, true);
          }
        }
        // ### END PVS ###
        // ########################################

      }

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
      if (stopSearch || hardTimeLimitReached()) break;

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
   * @return value of the search
   */
  private int negamax(BoardPosition position, int depthLeft, int ply, int alpha, int beta,
                      boolean pvSearch) {

    // nodes counter
    searchCounter.nodesVisited++;
    if (searchMode.getNodes() > 0 && searchCounter.nodesVisited >= searchMode.getNodes()) {
      stopSearch = true;
    }

    // check draw through 50-moves-rule, 3-fold-repetition, insufficient material
    if (!isPerftSearch()) {
      if (position.check50Moves()
          || position.check3Repetitions()
          || position.checkInsufficientMaterial()) {
        return Evaluation.Value.DRAW;
      }
    }

    // on leaf node call quiescence
    if (depthLeft <= 0) {
      return quiescence(position, ply, alpha, beta);
    }

    // ###############################################
    // ## BEGIN Mate Distance Pruning
    if (config.MATE_DISTANCE_PRUNING && !isPerftSearch()) {
      int mateValue = -Evaluation.Value.CHECKMATE + ply;
      // lower bound
      if (mateValue > alpha) {
        alpha = mateValue;
        if (mateValue >= beta) {
          searchCounter.prunings++;
          return mateValue;
        }
      }
      // upper bound
      mateValue = Evaluation.Value.CHECKMATE - ply;
      if (mateValue < beta) {
        beta = mateValue;
        if (mateValue <= alpha) {
          searchCounter.prunings++;
          return mateValue;
        }
      }
    }
    // ## ENDOF Mate Distance Pruning
    // ###############################################

    // ###############################################
    // TT Lookup
    // prepare hash type
    TT_EntryType ttType = TT_EntryType.ALPHA;
    if (config.TRANSPOSITION_TABLE && !isPerftSearch()) {
      final TT_Entry ttEntry = transpositionTable.get(position);
      if (ttEntry != null) { // possible TT Hit
        if (ttEntry.depth >= depthLeft) { // only if tt depth was equal or deeper
          if (ttEntry.type == TT_EntryType.EXACT) {
            searchCounter.nodeCache_Hits++;
            return ttEntry.value;
          } else if (ttEntry.type == TT_EntryType.ALPHA) {
            if (ttEntry.value <= alpha) {
              searchCounter.nodeCache_Hits++;
              return alpha;
            }
          } else if (ttEntry.type == TT_EntryType.BETA) {
            if (ttEntry.value >= beta) {
              searchCounter.nodeCache_Hits++;
              return beta;
            }
          }
        }
      } else {
        searchCounter.nodeCache_Misses++;
      }
    }
    // End TT Lookup
    // ###############################################

    // Initialize best values
    int bestValue = Evaluation.Value.NOVALUE;

    // current search depth
    if (searchCounter.currentSearchDepth < ply) searchCounter.currentSearchDepth = ply;
    if (searchCounter.currentExtraSearchDepth < ply) searchCounter.currentExtraSearchDepth = ply;

    // clear principal Variation for this depth
    principalVariation[ply].clear();

    // needed to remember if we even had a legal move
    boolean hadLegaMove = false;

    // generate moves
    MoveList moves = moveGenerators[ply].getPseudoLegalMoves(position, false);
    searchCounter.movesGenerated++;

    // TODO sort moves???

    // moves to search recursively
    long ticker = System.currentTimeMillis();
    for (int i = 0; i < moves.size(); i++) {
      int move = moves.get(i);
      int value;

      // Minor Promotion Pruning
      if (config.USE_MINOR_PROMOTION_PRUNING && !isPerftSearch()) {
        if (Move.getMoveType(move) == MoveType.PROMOTION &&
            Move.getPromotion(move).getType() != PieceType.QUEEN &&
            Move.getPromotion(move).getType() != PieceType.KNIGHT) {
          // prune non queen or knight promotion as they are redundant
          // exception would be stale mate situations.
          continue;
        }
      }

      position.makeMove(move);
      // Check if legal move before going into recursion
      if (!position.isAttacked(position._nextPlayer,
                               position._kingSquares[position._nextPlayer.getInverseColor()
                                                                         .ordinal()])) {

        // needed to remember if we even had a legal move
        hadLegaMove = true;

        // keep track of current variation
        currentVariation.add(move);

        // send current root move info to UCI every x milli seconds
        if (System.currentTimeMillis() - ticker >= 500) {
          // @formatter:off
          engine.sendInfoToUCI("depth " + searchCounter.currentSearchDepth
                               + " seldepth " + searchCounter.currentExtraSearchDepth
                               + " nodes " + searchCounter.nodesVisited
                               + " nps " + 1000 * searchCounter.nodesVisited / elapsedTime()
                               + " time " + elapsedTime()
                               + " currmove " + Move.toUCINotation(position, searchCounter.currentRootMove)
                               + " currmovenumber " + searchCounter.currentRootMoveNumber
                               + " currline " + currentVariation.toNotationString()
                              );
          // @formatter:on
          ticker = System.currentTimeMillis();
        }

        // go one ply deeper into the search tree
        if (isPerftSearch()) {

          value = -negamax(position, depthLeft - 1, ply + 1, -beta, -alpha, true);

        } else {

          // ########################################
          // ### START PVS ###
          if (!config.USE_PVS || bestValue == Evaluation.Value.NOVALUE) { // no PV yet
            value = -negamax(position, depthLeft - 1, ply + 1, -beta, -alpha, pvSearch);
          } else {
            // try null window search
            value = -negamax(position, depthLeft - 1, ply + 1, -alpha - 1, -alpha, false);
            if (value > alpha && value < beta) { // no fail - research
              searchCounter.pv_researches++;
              value = -negamax(position, depthLeft - 1, ply + 1, -beta, -alpha, true);
            }
          }
          // ### END PVS ###
          // ########################################
        }

        // PRUNING START
        if (value > bestValue) {
          bestValue = value;

          if (value > alpha) {
            alpha = value;
            ttType = TT_EntryType.EXACT;
            MoveList.savePV(move, principalVariation[ply + 1], principalVariation[ply]);

            // AlphaBeta Pruning
            if (value >= beta) {
              if (config.USE_ALPHABETA_PRUNING && !isPerftSearch()) {
                bestValue = beta; // same as return beta
                ttType = TT_EntryType.BETA;
                currentVariation.removeLast();
                position.undoMove();
                searchCounter.prunings++;
                break;
              }
            }
          }
        }
        // PRUNING END

        currentVariation.removeLast();
      }
      position.undoMove();

      // check if we need to stop search - could be external or time.
      // we should have any best move here
      if (stopSearch || hardTimeLimitReached()) break;
    }

    // if we did not have a legal move then we have a mate
    if (!hadLegaMove && !stopSearch) {
      // as we will not enter evaluation we count it here
      searchCounter.leafPositionsEvaluated++;
      searchCounter.nonLeafPositionsEvaluated++;
      if (position.hasCheck()) {
        // We have a check mate. Return a -CHECKMATE.
        bestValue = -Evaluation.Value.CHECKMATE + ply;
      } else {
        // We have a stale mate. Return the draw value.
        bestValue = Evaluation.Value.DRAW;
      }
    }

    // *****************************************************
    // TT Store
    if (config.TRANSPOSITION_TABLE && !isPerftSearch()) {
      // now store to tt
      transpositionTable.put(position, bestValue, ttType, depthLeft);
    }
    // TT Store
    // *****************************************************

    return bestValue;
  }

  private int quiescence(BoardPosition position, int ply, int alpha, int beta) {

    if (isPerftSearch() || !config.USE_QUIESCENCE) {
      return evaluate(position);
    }

    // do we even have legal moves?
    if (moveGenerators[ply].hasLegalMove(position)) {

      // ##############################################################
      // START QUIESCENCE

      // TODO QUIESCENCE
      return evaluate(position);

      // END QUIESCENCE
      // ##############################################################

    } // no moves - mate position?
    else {
      // as we will not enter evaluation we count it here
      searchCounter.leafPositionsEvaluated++;

      if (position.hasCheck()) {
        // We have a check mate. Return a -CHECKMATE.
        alpha = -Evaluation.Value.CHECKMATE + ply;
      } else {
        // We have a stale mate. Return the draw value.
        alpha = Evaluation.Value.DRAW;
      }
    }
    return alpha;
  }

  private int evaluate(BoardPosition position) {

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

    // Evaluation Cache
    // This only is worth the effort if the evaluation function takes longer than
    // the lookup and the storage of evaluations. Also this cache reduces memory space
    // for TT cache which also is a penalty of using it.
    // retrieve evaluation value from evaluation cache
    if (config.USE_EVALUATION_CACHE && !isPerftSearch()) {
      final int value = evaluationCache.get(position.getZobristKey());
      if (value > Integer.MIN_VALUE) {
        searchCounter.evalCache_Hits++;
        return value;
      }
      searchCounter.evalCache_Misses++;
    }

    // call the evaluation
    // count all leaf nodes evaluated
    final int value = evaluator.evaluate(position);
    searchCounter.leafPositionsEvaluated++;

    // Evaluation Cache
    // store evaluation value in evaluation cache
    if (config.USE_EVALUATION_CACHE && !isPerftSearch()) {
      evaluationCache.put(position.getZobristKey(), value);
    }

    return value;
  }

  private boolean isPerftSearch() {
    return config.PERFT || searchMode.isPerft();
  }

  private boolean softTimeLimitReached() {
    if (!searchMode.isTimeControl()) return false;
    return elapsedTime() >= softTimeLimit;
  }

  private boolean hardTimeLimitReached() {
    if (!searchMode.isTimeControl()) return false;
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
      // Give some overhead time so that in games with very low available time we do not run out of time
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
   * Called by engine whenever hash size changes.
   * Initially set in constructor
   *
   * @param hashSize
   */
  public void setHashSize(int hashSize) {
    transpositionTable = new TranspositionTable(hashSize / 2);
    evaluationCache = new EvaluationCache(hashSize / 2);
  }

  /**
   * Called when the state of this search is no longer valid as the last call to startSearch is not from
   * the same game as the next.
   */
  public void newGame() {
    transpositionTable.clear();
    evaluationCache.clear();
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
   * Clears the hashtables
   */
  public void clearHashtables() {
    transpositionTable.clear();
    evaluationCache.clear();
  }

  /**
   * Parameter class for the search result
   */
  static final class SearchResult {
    int  bestMove    = Move.NOMOVE;
    int  ponderMove  = Move.NOMOVE;
    int  bound       = 0;
    int  resultValue = 0;
    long time        = -1;
    int  moveNumber  = 0;
    int  depth       = 0;

    @Override
    public String toString() {
      return "Best Move: " + Move.toString(bestMove) + " Ponder Move: " + Move.toString(ponderMove);
    }
  }

  /**
   * Convenience Wrapper class for all counters
   */
  public class SearchCounter {

    // Info values
    int  currentBestRootMove     = Move.NOMOVE;
    int  currentBestRootValue    = Evaluation.Value.NOVALUE;
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
    long positionsNonQuiet = 0;
    long prunings          = 0;
    long pv_researches     = 0;
    long evalCache_Hits    = 0;
    long evalCache_Misses  = 0;
    long nodeCache_Hits    = 0;
    long nodeCache_Misses  = 0;
    long movesGenerated    = 0;
    long nodesVisited      = 0;

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
      pv_researches = 0;
      evalCache_Hits = 0;
      evalCache_Misses = 0;
      nodeCache_Hits = 0;
      nodeCache_Misses = 0;
      movesGenerated = 0;
      checkCounter = 0;
      checkMateCounter = 0;
      captureCounter = 0;
      enPassantCounter = 0;
      lastSearchTime = 0;
    }

    @Override
    public String toString() {
      return "SearchCounter{" + "nodesVisited=" + nodesVisited+ ", lastSearchTime=" +
             DurationFormatUtils.formatDurationHMS(lastSearchTime) + ", currentBestRootMove=" +
             currentBestRootMove + ", currentBestRootValue=" + currentBestRootValue +
             ", currentIterationDepth=" + currentIterationDepth + ", currentSearchDepth=" +
             currentSearchDepth + ", currentExtraSearchDepth=" + currentExtraSearchDepth +
             ", currentRootMove=" + currentRootMove + ", currentRootMoveNumber=" +
             currentRootMoveNumber + ", leafPositionsEvaluated=" + leafPositionsEvaluated +
             ", nonLeafPositionsEvaluated=" + nonLeafPositionsEvaluated + ", checkCounter=" +
             checkCounter + ", checkMateCounter=" + checkMateCounter + ", captureCounter=" +
             captureCounter + ", enPassantCounter=" + enPassantCounter + ", positionsNonQuiet=" +
             positionsNonQuiet + ", prunings=" + prunings + ", pv_researches=" + pv_researches +
             ", evalCache_Hits=" + evalCache_Hits + ", evalCache_Misses=" + evalCache_Misses +
             ", nodeCache_Hits=" + nodeCache_Hits + ", nodeCache_Misses=" + nodeCache_Misses +
             ", movesGenerated=" + movesGenerated + '}';
    }
  }

}
