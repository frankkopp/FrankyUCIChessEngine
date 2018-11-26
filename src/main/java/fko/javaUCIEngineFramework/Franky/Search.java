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

import fko.javaUCIEngineFramework.UCI.IUCIEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

/**
 * Search
 */
public class Search implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Search.class);

  private static final int MAX_SEARCH_DEPTH = 100;

  // back reference to the engine
  private IUCIEngine engine;

  // the thread in which we will do the actual search
  private Thread searchThread = null;

  // flag to indicate to stop the search - can be called externally or via the timer clock.
  private boolean stopSearch = true;

  // hash tables
  private TranspositionTable transpositionTable;
  private EvaluationCache    evalCache;

  // Move Generators - each depth in search gets it own to avoid object creation during search
  private final MoveGenerator[] moveGenerators = new MoveGenerator[MAX_SEARCH_DEPTH];

  // Position Evaluator
  private final Evaluation evaluator;

  // used to wait for move from search
  private CountDownLatch waitForInitializaitonLatch = new CountDownLatch(1);

  // time variables
  private Instant startTime;
  private Instant ponderStartTime;
  private long    hardTimeLimit;
  private long    softTimeLimit;

  // search state - valid for one call to startSearch
  private BoardPosition currentBoardPosition;
  private Color         myColor;
  private SearchMode    searchMode;

  // running search global variables
  private RootMoveList rootMoves = new RootMoveList();
  // current variation of the search
  MoveList   currentVariation   = new MoveList(MAX_SEARCH_DEPTH);
  MoveList[] principalVariation = new MoveList[MAX_SEARCH_DEPTH];
  // @formatter:off
  private int  currentBestRootMove     = Move.NOMOVE; // current best move found by search
  private int  currentBestRootValue    = Evaluation.Value.NOVALUE; // value of the current best move
  private int  currentIterationDepth   = 0; // how deep will the search go in the current iteration
  private int  currentSearchDepth      = 0; // how deep did the search go this iteration
  private int  currentExtraSearchDepth = 0; // how deep did we search including quiescence depth this iteration
  private int  currentRootMove         = 0; // current root move that is searched
  private int  currentRootMoveNumber   = 0; // number of the current root move in the list of root moves
  private long nodesVisited            = 0; // how many times a node has been visited (negamax calls)
  private long boardsEvaluated         = 0; // how many times a node has been visited (= boards evaluated)
  private long boardsNonQuiet          = 0; // board/nodes evaluated in quiescence search
  private long prunings                = 0;
  private long pv_researches           = 0;
  private long evalCache_Hits          = 0;
  private long evalCache_Misses        = 0;
  private long nodeCache_Hits          = 0;
  private long nodeCache_Misses        = 0;
  private long movesFromCache          = 0;
  private long movesGenerated          = 0;
  // @formatter:on

  private void resetCounter() {
    currentIterationDepth = 0;
    currentSearchDepth = 0;
    currentExtraSearchDepth = 0;
    currentRootMove = 0;
    currentRootMoveNumber = 0;
    nodesVisited = 0;
    boardsEvaluated = 0;
    boardsNonQuiet = 0;
    prunings = 0;
    pv_researches = 0;
    evalCache_Hits = 0;
    evalCache_Misses = 0;
    nodeCache_Hits = 0;
    nodeCache_Misses = 0;
    movesFromCache = 0;
    movesGenerated = 0;
  }

  /**
   * /**
   * Creates a search object and stores a back reference to the engine object.<br>
   * Hash is setup up to the given hash size.
   *
   * @param engine
   * @param hashSize
   */
  public Search(IUCIEngine engine, int hashSize) {
    this.engine = engine;

    // set hash sizes
    setHashSize(hashSize);

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
    if (searchThread != null) {
      final String s = "OmegaSearch already running - can only be started once";
      IllegalStateException e = new IllegalStateException(s);
      LOG.error(s, e);
      throw e;
    }

    // create a deep copy of the position
    this.currentBoardPosition = new BoardPosition(boardPosition);
    this.myColor = currentBoardPosition.getNextPlayer();
    this.searchMode = searchMode;

    // configure search
    //TODO

    // setup latch
    waitForInitializaitonLatch = new CountDownLatch(1);

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
      waitForInitializaitonLatch.await();
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

    // reset counter
    resetCounter();

    // release latch so the caller can continue
    waitForInitializaitonLatch.countDown();

    // run the search itself
    SearchResult searchResult = iterativeSearch(currentBoardPosition);

    // TODO: send info to UCI

    // send the result
    engine.sendResult(searchResult.bestMove, searchResult.ponderMove);
  }

  /**
   * This starts the actual iterative search.
   *
   * @param position
   * @return the best move
   */
  private SearchResult iterativeSearch(BoardPosition position) {

    // remember the start of the search
    startTime = Instant.now();
    ponderStartTime = Instant.now();

    // generate all root moves
    MoveList legalMoves = moveGenerators[0].getLegalMoves(position, false);

    // no legal root moves - game already ended!
    if (legalMoves.size() == 0) return new SearchResult();

    // prepare principal variation lists
    for (int i = 0; i < MAX_SEARCH_DEPTH; i++) {
      principalVariation[i].clear();
    }

    // create rootMoves list
    rootMoves.clear();
    for (int i = 0; i < legalMoves.size(); i++) {
      // TODO filter UCI search moves
      rootMoves.add(legalMoves.get(i), Evaluation.Value.NOVALUE);
    }

    // temporary best move - take the first move available
    currentBestRootMove = rootMoves.getMove(0);
    currentBestRootValue = Evaluation.Value.NOVALUE;

    // prepare search result
    SearchResult searchResult = new SearchResult();

    // define start search depth - for timed games this 1 otherwise it is
    // either mate depth for mate searches or max depth otherwise
    int startIterativeDepth = 1;
    if (searchMode.isTimeControl()) {
      configureTimeLimits();
    } else {
      startIterativeDepth =
        searchMode.getMate() > 0 ? searchMode.getMate() // TODO: do we iterate for mate searches?
                                 : searchMode.getDepth();
    }
    // define max search depth according to search mode
    int maxIterativeDepth = searchMode.getMate() > 0 ? searchMode.getMate() : searchMode.getDepth();

    // #############################
    // ### BEGIN Iterative Deepening
    int depth = startIterativeDepth;
    do {
      currentIterationDepth = depth;

      // do search
      rootMovesSearch(position, depth);

      // sure mate value found?
      if (currentBestRootValue >= Evaluation.Value.CHECKMATE - depth ||
          currentBestRootValue <= -Evaluation.Value.CHECKMATE + depth) {
        //System.err.println("Already found Mate: "+position.toFENString());
        stopSearch = true;
      }

      // check if we need to stop search - could be external or time.
      final long elapsedTime = Duration.between(Instant.now(), startTime).toMillis();
      if (stopSearch || elapsedTime >= softTimeLimit || elapsedTime >= hardTimeLimit) {
        break;
      }

    } while (++depth <= maxIterativeDepth);
    // ### ENDOF Iterative Deepening
    // #############################

    // we should have a sorted _rootMoves list here
    // create searchRestult here
    searchResult.bestMove = currentBestRootMove;
    searchResult.resultValue = currentBestRootValue;
    searchResult.depth = currentIterationDepth;
    int p_move;
    if (principalVariation[0].size() > 1 &&
        (p_move = principalVariation[0].get(1)) != Move.NOMOVE) {
      //System.out.println("Best Move: "+OmegaMove.toString(searchResult.bestMove)+" Ponder Move: "+OmegaMove.toString(p_move)+" ("+_principalVariation[0].toNotationString()+")");
      searchResult.ponderMove = p_move;
    } else {
      searchResult.ponderMove = Move.NOMOVE;
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

    // TODO rootMovesSearch
    
  }

  /**
   * Configure time limits<br>
   * Chooses if search mode is time per move or remaining time
   * and set time limits accordingly
   */
  private void configureTimeLimits() {

    if (searchMode.getMovetime().toMillis() > 0) { // mode time per move
      hardTimeLimit = searchMode.getMovetime().toMillis();
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
    evalCache = new EvaluationCache(hashSize / 2);
  }

  /**
   * Called when the state of this search is no longer valid as the last call to startSearch is not from
   * the same game as the next.
   */
  public void newGame() {
    transpositionTable.clear();
    evalCache.clear();
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

}
