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


import fko.javaUCIEngineFramework.UCI.IUCIEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank
 */
class TestMateSearch {

  private static final Logger LOG = LoggerFactory.getLogger(TestMateSearch.class);

  private IUCIEngine engine;
  private Search     search;

  @BeforeEach
  void setUp() {

    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();

  }

  @Test
  @Disabled
  public void testMateSearch() {

    // FIXME: This mate search seems to be wrong - Fritz says 8 (15ply) this says 12

    testMate0Search();
    testMate1Search();
    testMate2Search();
    testMate3Search();
    testMate4Search();
    testMate5Search();
    testMate6Search();
    testMate7Search();
    testMate8Search();
  }

  @Test
  @Disabled
  public void testMate0Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/8/8/8/6K1/8/R5k1 b - - 0 8"; // 10000
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 1, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertEquals(0, search.getSearchCounter().leafPositionsEvaluated);
    assertEquals(0, search.getSearchCounter().currentIterationDepth);
    assertEquals(search.getLastSearchResult().bestMove, Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate1Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/8/8/8/6K1/R7/6k1 w - - 0 8"; // 9999
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 1, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 1, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate2Search() {

    String fen;
    Position position;
    SearchMode searchMode;


    fen = "8/8/8/8/8/5K2/R7/7k w - - 0 7"; // 9997
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 2, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 3, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate3Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/8/8/8/4K3/R7/6k1 w - - 0 6"; // 9993
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 3, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 5, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate4Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/8/8/8/3K4/R7/5k2 w - - 0 5"; // 9993
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 7, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate5Search() {

    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_PVS = true;
    search.config.USE_TRANSPOSITION_TABLE = true;

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/8/8/4K3/8/R7/4k3 w - - 0 4"; // 9991
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 5, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 9, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate6Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/8/5K2/8/8/R7/5k2 w - - 0 3"; // 9989
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 11, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate7Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/8/6K1/8/8/8/R7/6k1 w - - 0 2"; // 9987
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 7, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 13, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testMate8Search() {

    String fen;
    Position position;
    SearchMode searchMode;

    fen = "8/7K/8/8/8/8/R7/7k w - - 0 1"; // 9985
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 15, search.getLastSearchResult().resultValue);

  }

  private void waitWhileSearching() {
    while (search.isSearching()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignored) {
      }
    }
  }


}
