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


import fko.UCI.IUCIEngine;
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
    search = ((FrankyEngine) engine).getSearch();

    search.config.USE_BOOK = false;

  }

  @Test
  public void testBookSearch() {
    search.config.USE_BOOK = true;
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);

    // timed search - should use book
    SearchMode searchMode =
      new SearchMode(300000, 300000, 0, 0, 0, 0, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertEquals(0, search.getSearchCounter().leafPositionsEvaluated);
    assertEquals(0, search.getSearchCounter().currentIterationDepth);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);

    // non timed search - should not use book
    searchMode = new SearchMode(0, 0, 0, 0, 0, 4, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);

  }

  @Test
  public void testDepthSearch() {

    //fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    //fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    //position.makeMove(Move.fromUCINotation(position,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 4, 0, 0, 0, null, false, false, false);

    search.startSearch(position, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testNodesSearch() {

    //fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    //fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    //position.makeMove(Move.fromUCINotation(position,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 5000000, 0, 0, null, false, false,
                                           false);

    search.startSearch(position, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(5000000, search.getSearchCounter().nodesVisited);
  }

  @Test
  public void testMultipleStartAndStopSearch() {

    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    //position.makeMove(Move.fromUCINotation(position,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, true, false, false);

    // test search
    waitWhileSearching();

    // Test start and stop search
    for (int i = 0; i < 20; i++) {
      search.startSearch(position, searchMode);
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
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);

    SearchMode searchMode = new SearchMode(300000, 300000, 0, 0, 0, 0, 0, 0, 0, null, false, false,
                                           false);

    search.startSearch(position, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testBasicTimeControl_RemainingTimeInc() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);

    SearchMode searchMode = new SearchMode(300000, 300000, 2000, 2000, 0, 0, 0, 0, 0, null, false,
                                           false, false);

    search.startSearch(position, searchMode);

    // test search
    waitWhileSearching();

    // TODO: Inc not implemented in search time estimations yet - so this is similar to non inc
    //  time control

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  /**
   *
   */
  @Test
  public void testBasicTimeControl_TimePerMove() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 5000, null, false, false, false);

    search.startSearch(position, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testMateSearch() {

    String fen;
    Position position;
    SearchMode searchMode;

//    search.config.USE_QUIESCENCE = true;
//    search.config.USE_MATE_DISTANCE_PRUNING = true;
//    search.config.USE_TRANSPOSITION_TABLE = true;

    // mate in 2 (3 plys)
    fen = "1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - - 0 1"; // Position
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 2, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    search.stopSearch();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 3, search.getLastSearchResult().resultValue);

    // mate in 3 (5 plys)
    fen = "4rk2/p5p1/1p2P2N/7R/nP5P/5PQ1/b6K/q7 w - - 0 1";
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 3, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 5, search.getLastSearchResult().resultValue);

     /*
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
    */
  }

  @Test
  @Disabled
  public void testExtMateSearch() {

//    testMateSearch();

    String fen;
    Position position;
    SearchMode searchMode;

    // mate in 4 (7 plys)
//    fen = "r2r1n2/pp2bk2/2p1p2p/3q4/3PN1QP/2P3R1/P4PP1/5RK1 w - - 0 1";
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, null, false, false, false);
//    search.startSearch(position, searchMode);
//    waitWhileSearching();
//    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
//    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
//    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
//    assertEquals(Evaluation.CHECKMATE - 7, search.getLastSearchResult().resultValue);

  }


  @Test
  public void testMovesSearch() {

    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 2, Arrays.asList("h2h4"), false,
                                           false, false);

    search.startSearch(position, searchMode);

    // test search
    waitWhileSearching();

    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertEquals("h2h4", Move.toUCINotation(position, search.getLastSearchResult().bestMove));
  }

  @Test
  public void testPonderMissSearch() throws InterruptedException {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));

    SearchMode searchMode = new SearchMode(295000, 300000, 0, 0, 0, 0, 0, 0, 0, null, true, false,
                                           false);

    // set last ponder move
    position.makeMove(Move.fromUCINotation(position, "e7e5"));

    // Start pondering
    search.startSearch(position, searchMode);

    // wait a bit
    Thread.sleep(3000);

    // ponder miss
    // stop search
    search.stopSearch();
    position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    position.makeMove(Move.fromUCINotation(position, "c7c5"));
    searchMode = new SearchMode(295000, 295000, 0, 0, 0, 0, 0, 0, 0, null, false, false, false);

    // Start search after miss
    search.startSearch(position, searchMode);

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
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));

    SearchMode searchMode = new SearchMode(295000, 300000, 0, 0, 0, 0, 0, 0, 0, null, true, false,
                                           false);

    // set last ponder move
    position.makeMove(Move.fromUCINotation(position, "e7e5"));

    // Start pondering
    search.startSearch(position, searchMode);

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

    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    //position.makeMove(Move.fromUCINotation(position,"e2e4"));

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 5, 0, 0, 0, null, false, false, true);

    search.startSearch(position, searchMode);

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
  @Disabled
  public void sizeOfSearchTreeTest() {

    int depth = 5;
    List<String> values = new ArrayList<>();
    List<String> fens = new ArrayList<>();

    LOG.info("Start SIZE Test for depth {}", depth);

    fens.add(Position.STANDARD_BOARD_FEN);
    fens.add("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
    fens.add("1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - - 0 1");
    fens.add("r1bq1rk1/pp2bppp/2n2n2/3p4/3P4/2N2N2/PPQ1BPPP/R1B2RK1 b - - 3 10");
    fens.add("1r1r2k1/2p1qp1p/6p1/ppQB1b2/5Pn1/2R1P1P1/PP5P/R1B3K1 b");

    for (String fen : fens) {
      values.add("");
      values.add(fen);
      featureMeasurements(depth, values, fen);
      values.add("");
    }

    LOG.info("");
    LOG.info("################## RESULTS ####################");
    for (String value : values) {
      LOG.info(value);
    }
  }

  private void featureMeasurements(final int depth, final List<String> values, final String fen) {
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);

    // turn off all optimizations to get a reference value of the search tree size
    search.config.USE_ROOT_MOVES_SORT = false;
    search.config.USE_ALPHABETA_PRUNING = false;
    search.config.USE_ASPIRATION_WINDOW = false;
    search.config.USE_PVS = false;
    search.config.USE_TRANSPOSITION_TABLE = false;
    search.config.USE_MATE_DISTANCE_PRUNING = false;
    search.config.USE_MINOR_PROMOTION_PRUNING = false;
    search.config.USE_QUIESCENCE = false;
    search.config.USE_NULL_MOVE_PRUNING = false;
    search.config.USE_PVS_MOVE_ORDERING = false;
    search.config.USE_EVAL_PRUNING = false;
    search.config.USE_RAZOR_PRUNING = false;

    measureTreeSize(position, searchMode, values, "REFERENCE", true);

//    search.config.USE_ASPIRATION_WINDOW = true;
//    measureTreeSize(position, searchMode, values, "Aspiration", true);

    search.config.USE_ALPHABETA_PRUNING = true;
    measureTreeSize(position, searchMode, values, "AlphaBeta", true);

    search.config.USE_ROOT_MOVES_SORT = true;
    measureTreeSize(position, searchMode, values, "RootMoveSort", true);

    search.config.USE_PVS = true;
    measureTreeSize(position, searchMode, values, "PVS", true);

    search.config.USE_MATE_DISTANCE_PRUNING = true;
    measureTreeSize(position, searchMode, values, "MDP", true);

    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    measureTreeSize(position, searchMode, values, "MPP", true);

    search.config.USE_PVS_MOVE_ORDERING = true;
    measureTreeSize(position, searchMode, values, "PVS_ORDER", true);

    search.config.USE_NULL_MOVE_PRUNING = true;
    measureTreeSize(position, searchMode, values, "NMP", true);

    search.config.USE_EVAL_PRUNING = true;
    measureTreeSize(position, searchMode, values, "EVALPRUN", true);

    search.config.USE_RAZOR_PRUNING = true;
    measureTreeSize(position, searchMode, values, "RAZOR", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    measureTreeSize(position, searchMode, values, "TT", true);

//    search.config.USE_TRANSPOSITION_TABLE = false;
//    search.config.USE_QUIESCENCE = true;
//    measureTreeSize(position, searchMode, values, "QS-TT", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_PVS_MOVE_ORDERING = false;
    measureTreeSize(position, searchMode, values, "QS-PVSO", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    measureTreeSize(position, searchMode, values, "QS+PVSO", true);


    // REPEAT
    measureTreeSize(position, searchMode, values, "REPEAT+TT", false);
  }

  private void measureTreeSize(final Position position, final SearchMode searchMode,
                               final List<String> values, final String feature,
                               final boolean clearTT) {

    if (clearTT) search.clearHashTables();
    search.startSearch(position, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE %-12s : %,14d >> %-14s (%4d) >> %s ", feature,
                             search.getSearchCounter().leafPositionsEvaluated,
                             Move.toString(search.getLastSearchResult().bestMove),
                             search.getLastSearchResult().resultValue,
                             search.getSearchCounter().toString()));
  }

  @Test
  public void evaluationTest() {
    int depth;
    String fen = Position.STANDARD_BOARD_FEN;
    SearchMode searchMode;
    Position position;

    search.config.USE_ROOT_MOVES_SORT = true;
    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_ASPIRATION_WINDOW = false;
    search.config.USE_PVS = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.USE_EVAL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = true;

    depth = 8;
    fen = "r1bnkbnr/ppppqppp/8/3Pp3/2B1P3/5N2/PPP2PPP/RNBQK2R b KQkq -"; // bm Bxd6+
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    LOG.info("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    LOG.debug(search.getSearchCounter().toString());

//    depth = 4;
//    fen = "1k3r2/pp6/3p4/8/8/n5B1/5PPP/5RK1 w - - 0 1"; // bm Bxd6+
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);
//    search.startSearch(position, searchMode);
//    waitWhileSearching();
//    LOG.info("Best Move: {} Value: {} Ponder {}",
//             Move.toSimpleString(search.getLastSearchResult().bestMove),
//             search.getLastSearchResult().resultValue / 100f,
//             Move.toSimpleString(search.getLastSearchResult().ponderMove));
//    assertEquals("g3d6", Move.toSimpleString(search.getLastSearchResult().bestMove));

    // Spanish Opening - next move should be 0-0
//    depth = 1;
//    fen = "r1bqkb1r/1ppp1ppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 5";
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);
//    search.startSearch(position, searchMode);
//    waitWhileSearching();
//    LOG.info("Best Move: {} Value: {} Ponder {}",
//             Move.toSimpleString(search.getLastSearchResult().bestMove),
//             search.getLastSearchResult().resultValue / 100f,
//             Move.toSimpleString(search.getLastSearchResult().ponderMove));
//    assertEquals("e1g1", Move.toSimpleString(search.getLastSearchResult().bestMove));

//    depth = 4;
//    fen = "rn1q1rk1/pbpp2pp/1p2pb2/4N3/3PN3/3B4/PPP2PPP/R2QK2R w KQ - 0 9"; // bm Dh5
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);
//    search.startSearch(position, searchMode);
//    waitWhileSearching();
//    LOG.info("Best Move: {} Value: {} Ponder {}",
//             Move.toSimpleString(search.getLastSearchResult().bestMove),
//             search.getLastSearchResult().resultValue / 100f,
//             Move.toSimpleString(search.getLastSearchResult().ponderMove));
//    assertEquals("d1h5", Move.toSimpleString(search.getLastSearchResult().bestMove));

    // 2kr4/ppq2pp1/2b1pn2/2P4r/2P5/3BQN1P/P4PP1/R4RK1 b - - 0 1
//    depth = 8;
//    fen = "2kr4/ppq2pp1/2b1pn2/2P4r/2P5/3BQN1P/P4PP1/R4RK1 b - - 0 1"; // bm Nf6-g4
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);
//    search.startSearch(position, searchMode);
//    waitWhileSearching();
//    LOG.info("Best Move: {} Value: {} Ponder {}",
//             Move.toSimpleString(search.getLastSearchResult().bestMove),
//             search.getLastSearchResult().resultValue / 100f,
//             Move.toSimpleString(search.getLastSearchResult().ponderMove));
//    assertEquals("f6g4", Move.toSimpleString(search.getLastSearchResult().bestMove));
  }

  @Test
  @Disabled
  public void testInfiniteSearch() {
    Position position = new Position();
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
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
