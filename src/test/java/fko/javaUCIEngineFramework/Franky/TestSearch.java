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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank
 */
public class TestSearch {

  private static final Logger LOG = LoggerFactory.getLogger(TestSearch.class);

  private IUCIEngine engine;
  private Search     search;

  @BeforeEach
  void setUp() {

    engine = new FrankyEngine();
    search = new Search(engine, new Configuration());

  }

  @Test
  public void testDepthSearch() {

    //fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    //fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    //boardPosition.makeMove(Move.fromUCINotation(boardPosition,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 4, 0, 0, 0, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testNodesSearch() {

    //fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    //fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    //boardPosition.makeMove(Move.fromUCINotation(boardPosition,"e2e4"));

    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 0, 5000000, 0, 0, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(5000000, search.getSearchCounter().nodesVisited);
  }

  @Test
  public void testMultipleStartAndStopSearch() {

    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    //boardPosition.makeMove(Move.fromUCINotation(boardPosition,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, true, false, false);

    // test search
    waitWhileSearching();

    // Test start and stop search
    for (int i = 0; i < 20; i++) {
      search.startSearch(boardPosition, searchMode);
      try {
        Thread.sleep(new Random().nextInt(1000));
      } catch (InterruptedException ignored) {
      }

      search.stopSearch();

      assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    }
  }


  @Test
  public void testBasicTimeControl_RemainingTime() {
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);

    SearchMode searchMode =
      new SearchMode(300000, 300000, 0, 0, 0, 0, 0, 0, 0, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testBasicTimeControl_RemainingTimeInc() {
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);

    SearchMode searchMode =
      new SearchMode(300000, 300000, 2000, 2000, 0, 0, 0, 0, 0, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    // TODO: Inc not implemented in search time estimations yet - so this is similar to non inc time control

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  /**
   *
   */
  @Test
  public void testBasicTimeControl_TimePerMove() {
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 5, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testMateSearch() {

    // Test - Mate in 2
    //setupFromFEN("1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - - 0 1");

    // Test - Mate in 3
    //setupFromFEN("4rk2/p5p1/1p2P2N/7R/nP5P/5PQ1/b6K/q7 w - - 0 1");

    // Test - Mate in 3
    //setupFromFEN("4k2r/1q1p1pp1/p3p3/1pb1P3/2r3P1/P1N1P2p/1PP1Q2P/2R1R1K1 b k - 0 1");

    // Test - Mate in 4
    //setupFromFEN("r2r1n2/pp2bk2/2p1p2p/3q4/3PN1QP/2P3R1/P4PP1/5RK1 w - - 0 1");

    // Test - Mate in 5 (1.Sc6+! bxc6 2.Dxa7+!! Kxa7 3.Ta1+ Kb6 4.Thb1+ Kc5 5.Ta5# 1-0)
    //setupFromFEN("1kr4r/ppp2bq1/4n3/4P1pp/1NP2p2/2PP2PP/5Q1K/4R2R w - - 0 1");

    // Test - Mate in 3
    //setupFromFEN("1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - 0 1");

    // Test - Mate in 11
    //setupFromFEN("8/5k2/8/8/2N2N2/2B5/2K5/8 w - - 0 1");

    // Test - Mate in 13
    //setupFromFEN("8/8/6k1/8/8/8/P1K5/8 w - - 0 1");

    // Test - Mate in 15
    //setupFromFEN("8/5k2/8/8/8/8/1BK5/1B6 w - - 0 1");

    // Test - HORIZONT EFFECT
    //setupFromFEN("5r1k/4Qpq1/4p3/1p1p2P1/2p2P2/1p2P3/1K1P4/B7 w - - 0 1");

    // Test Pruning
    // 1r1r2k1/2p1qp1p/6p1/ppQB1b2/5Pn1/2R1P1P1/PP5P/R1B3K1 b ;bm Qe4

    // mate in 2 (4 plys)
    String fen =
      "1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - - 0 1"; // BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 2, 0, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    search.stopSearch();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.Value.CHECKMATE - (2 * 2), search.getLastSearchResult().resultValue);

    // mate in 4 (8 plys)
    fen = "r2r1n2/pp2bk2/2p1p2p/3q4/3PN1QP/2P3R1/P4PP1/5RK1 w - - 0 1";
    boardPosition = new BoardPosition(fen);

    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, null, false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.Value.CHECKMATE - (2 * 4), search.getLastSearchResult().resultValue);
  }

  @Test
  public void testMovesSearch() {

    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);

    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 2, Arrays.asList("h2h4"), false, false, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertEquals("h2h4", Move.toUCINotation(boardPosition, search.getLastSearchResult().bestMove));
  }

  @Test
  public void testPonderMissSearch() throws InterruptedException {
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    boardPosition.makeMove(Move.fromUCINotation(boardPosition, "e2e4"));

    SearchMode searchMode =
      new SearchMode(295000, 300000, 0, 0, 0, 0, 0, 0, 0, null, true, false, false);

    // set last ponder move
    boardPosition.makeMove(Move.fromUCINotation(boardPosition, "e7e5"));

    // Start pondering
    search.startSearch(boardPosition, searchMode);

    // wait a bit
    Thread.sleep(3000);

    // ponder miss
    // stop search
    search.stopSearch();
    boardPosition = new BoardPosition(fen);
    boardPosition.makeMove(Move.fromUCINotation(boardPosition, "e2e4"));
    boardPosition.makeMove(Move.fromUCINotation(boardPosition, "c7c5"));
    searchMode = new SearchMode(295000, 295000, 0, 0, 0, 0, 0, 0, 0, null, false, false, false);

    // Start search after miss
    search.startSearch(boardPosition, searchMode);

    // stop search
    search.stopSearch();
    // wait a bit
    Thread.sleep(3000);

    //    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    //    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    //    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testPonderHitSearch() throws InterruptedException {
    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    boardPosition.makeMove(Move.fromUCINotation(boardPosition, "e2e4"));

    SearchMode searchMode =
      new SearchMode(295000, 300000, 0, 0, 0, 0, 0, 0, 0, null, true, false, false);

    // set last ponder move
    boardPosition.makeMove(Move.fromUCINotation(boardPosition, "e7e5"));

    // Start pondering
    search.startSearch(boardPosition, searchMode);

    // wait a bit
    Thread.sleep(3000);

    // ponder miss
    // stop search
    search.ponderHit();

    // test search
    waitWhileSearching();

    //    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    //    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    //    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void perftTest() {

    LOG.info("Start PERFT Test for depth 5");

    String fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    //boardPosition.makeMove(Move.fromUCINotation(boardPosition,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0,
                                           0, 0, 5,
                                           0, 0, 0,
                                           null, false,
                                           false, true);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

    assertEquals(4865609, search.getSearchCounter().leafPositionsEvaluated);
    assertEquals(82719, search.getSearchCounter().captureCounter);
    assertEquals(258, search.getSearchCounter().enPassantCounter);
    assertEquals(27351, search.getSearchCounter().checkCounter);
    assertEquals(347, search.getSearchCounter().checkMateCounter);

    LOG.info("BOARDS: {}", String.format("%,d", search.getSearchCounter().leafPositionsEvaluated));
    LOG.info("PERFT Test for depth 5 successful.");

    // @formatter:off
    /*
    //N  Nodes      Captures EP     Checks  Mates
    { 0, 1,         0,       0,     0,      0},
    { 1, 20,        0,       0,     0,      0},
    { 2, 400,       0,       0,     0,      0},
    { 3, 8902,      34,      0,     12,     0},
    { 4, 197281,    1576,    0,     469,    8},
    { 5, 4865609,   82719,   258,   27351,  347},
    { 6, 119060324, 2812008, 5248,  809099, 10828},
    { 7, 3195901860L, 108329926, 319617, 33103848, 435816 }
    */
    // @formatter:on
  }

  @Test
  public void sizeOfSearchTreeTest() {

    final int depth = 4;

    LOG.info("Start SIZE Test for depth {}", depth);

    String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
//    fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0,
                                           0, 0, depth,
                                           0, 0, 0,
                                           null, false,
                                           true, false);

    List<String> values = new ArrayList<>();

    // turn off all optimizations to get a reference value of the search tree size
    search.config.USE_ROOT_MOVES_SORT = false;
    search.config.USE_ALPHABETA_PRUNING = false;
    search.config.USE_PVS = false;
    search.config.USE_TRANSPOSITION_TABLE = false;
    search.config.USE_EVALUATION_CACHE = false;
    search.config.MATE_DISTANCE_PRUNING = false;
    search.config.USE_MINOR_PROMOTION_PRUNING = false;
    search.config.USE_QUIESCENCE = false;
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE REFERENCE  : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));


    // turn on optimizations to get a compare value of the search tree size
    search.config.USE_ALPHABETA_PRUNING = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE AlphaBeta  : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    // turn on optimizations to get a compare value of the search tree size
    search.config.USE_ROOT_MOVES_SORT = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE RootSort   : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));


    // turn on optimizations to get a compare value of the search tree size
    search.config.USE_PVS = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE PVS        : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    // turn on optimizations to get a compare value of the search tree size
    search.config.USE_EVALUATION_CACHE = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE PVS+EC     : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    // turn on optimizations to get a compare value of the search tree size
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE PVS+TT     : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    // turn on optimizations to get a compare value of the search tree size
    search.config.MATE_DISTANCE_PRUNING = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE MDP        : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE MPP        : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    search.config.USE_TRANSPOSITION_TABLE = false;
    search.config.USE_EVALUATION_CACHE = false;
    search.config.USE_QUIESCENCE = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE QS-TT-EC   : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_EVALUATION_CACHE = true;
    search.config.USE_QUIESCENCE = true;
    search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE QS+TT+EC   : %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));


    // REPEAT
    // search.clearHashtables();
    search.startSearch(boardPosition, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE REPEAT w TT: %,14d >> %s",
                             search.getSearchCounter().leafPositionsEvaluated,
                             search.getSearchCounter().toString()));

    LOG.info("");
    for (String value : values) {
      LOG.info(value);
    }
  }

  private void waitWhileSearching() {
    while (search.isSearching()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Test
  public void evaluationTest() {
    final int depth = 28;

    LOG.info("Start SIZE Test for depth {}", depth);

    String fen = "k7/8/8/8/8/8/7P/K7 w - - 0 9";
    //    fen = BoardPosition.START_FEN;
    BoardPosition boardPosition = new BoardPosition(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0,
                                           0, 0, depth,
                                           0, 0, 0,
                                           null, false,
                                           true, false);

    search.startSearch(boardPosition, searchMode);

    waitWhileSearching();

    LOG.info("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue/100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));
  }

  @Test
  @Disabled
  public void testInfiniteSearch() {
    BoardPosition boardPosition = new BoardPosition();

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, false, true, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

  }


}
