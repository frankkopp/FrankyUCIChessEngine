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
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 4, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testNodesSearch() {
    final int nodes = 500000;
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 0, nodes, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(nodes, search.getSearchCounter().nodesVisited);
  }


  @Test
  public void testBasicTimeControl_RemainingTime() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode =
      new SearchMode(100000, 100000, 0, 0, 0, 0, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testBasicTimeControl_RemainingTimeInc() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode =
      new SearchMode(100000, 100000, 2000, 2000, 0, 0, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    // TODO: Inc not implemented in search time estimations yet - so this is similar to non inc
    //  time control
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testBasicTimeControl_TimePerMove() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 2000, null, false, false, false);
    search.startSearch(position, searchMode);
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

    // mate in 2 (3 plys)
    fen = "1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - - 0 1"; // Position
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 2, 2000, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    search.stopSearch();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    //assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 3, search.getLastSearchResult().resultValue);

    // mate in 3 (5 plys)
    fen = "4rk2/p5p1/1p2P2N/7R/nP5P/5PQ1/b6K/q7 w - - 0 1";
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 6, 0, 3, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 5, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testInfiniteSearch() {
    Position position = new Position();
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
  }

  @Test
  public void testMovesSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 1000, Arrays.asList("h2h4"), false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertEquals("h2h4", Move.toUCINotation(position, search.getLastSearchResult().bestMove));
  }

  @Test
  public void testMultipleStartAndStopSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    // Test start and stop search
    for (int i = 0; i < 10; i++) {
      SearchMode searchMode =
        new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 10000, null, false, false, false);
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
  public void testMultipleStartAndStopPondering() throws InterruptedException {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    // Test start and stop search
    for (int i = 0; i < 10; i++) {
      SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, true, false, false);
      search.startSearch(position, searchMode);
      Thread.sleep(new Random().nextInt(100));
      search.stopSearch();
      assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    }
  }

  @Test
  public void testPonderMissSearch() throws InterruptedException {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 3000, null, true, false, false);
    position.makeMove(Move.fromUCINotation(position, "e7e5"));
    search.startSearch(position, searchMode);
    Thread.sleep(1000);
    search.stopSearch();
    // new search
    position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    position.makeMove(Move.fromUCINotation(position, "c7c5"));
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 2000, null, false, false, false);
    // Start search after miss
    search.startSearch(position, searchMode);
    waitWhileSearching();
  }

  @Test
  public void testPonderHitSearch() throws InterruptedException {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    // Test start and stop search
    for (int i = 0; i < 10; i++) {
      SearchMode searchMode =
        new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 1000, null, true, false, false);
      position.makeMove(Move.fromUCINotation(position, "e7e5"));
      search.startSearch(position, searchMode);
      Thread.sleep(new Random().nextInt(100));
      search.ponderHit();
      waitWhileSearching();
      assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
      position.undoMove();
    }
  }

  @Test
  public void testPonderHitFinishedSearch() throws InterruptedException {
    String fen = "8/8/8/8/8/4K3/R7/6k1 w - - 0 6"; // 9999
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e3f3"));
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 10000, null, true, false, false);
    // set last ponder move
    position.makeMove(Move.fromUCINotation(position, "g1h1"));
    // Start pondering
    search.startSearch(position, searchMode);
    // wait a bit - ponder search will be finished during this time
    Thread.sleep(3000);
    search.ponderHit();
    // test search
    waitWhileSearching();
  }

  @Test
  public void testPonderStopFinishedSearch() throws InterruptedException {
    String fen = "8/8/8/8/8/4K3/R7/6k1 w - - 0 6"; // 9999
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e3f3"));
    SearchMode searchMode =
      new SearchMode(295000, 300000, 0, 0, 0, 0, 0, 0, 0, null, true, false, false);
    // set last ponder move
    position.makeMove(Move.fromUCINotation(position, "g1h1"));
    // Start pondering
    search.startSearch(position, searchMode);
    // wait a bit - ponder search will be finished during this time
    Thread.sleep(3000);
    search.stopSearch();
    // test search
    waitWhileSearching();
  }

  @Test
  public void test3FoldRep() {
    String fen = "8/p3Q1bk/1p4p1/5q2/P1N2p2/1P5p/2b4P/6K1 w - - 0 38";
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e7h4")); // #1
    position.makeMove(Move.fromUCINotation(position, "h7g8"));
    position.makeMove(Move.fromUCINotation(position, "h4d8"));
    position.makeMove(Move.fromUCINotation(position, "g8h7"));
    position.makeMove(Move.fromUCINotation(position, "d8h4")); // #2
    position.makeMove(Move.fromUCINotation(position, "h7g8"));
    position.makeMove(Move.fromUCINotation(position, "h4d8"));
    position.makeMove(Move.fromUCINotation(position, "g8h7"));
    // next white move would be 3-fold draw

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 1000, null, false, false, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertEquals("d8h4", Move.toSimpleString(search.getLastSearchResult().bestMove));
    assertEquals(Evaluation.getGamePhaseFactor(position) * EvaluationConfig.CONTEMPT_FACTOR,
                 search.getLastSearchResult().resultValue);
    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e7h4")); // #1
    position.makeMove(Move.fromUCINotation(position, "h7g8"));
    position.makeMove(Move.fromUCINotation(position, "h4d8"));
    position.makeMove(Move.fromUCINotation(position, "g8h7"));
    position.makeMove(Move.fromUCINotation(position, "d8h4")); // #2
    position.makeMove(Move.fromUCINotation(position, "h7g8"));
    position.makeMove(Move.fromUCINotation(position, "h4d8"));
    // black should not move Kg8 as this would enable white to  3-fold repetition
    // although black is winning

    search.startSearch(position, searchMode);
    waitWhileSearching();
    assertEquals("g7f8", Move.toSimpleString(search.getLastSearchResult().bestMove));
    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

  }

  @Test
  public void perftTest() {

    long[][] perftResults = {
      // @formatter:off
      //N  Nodes        Captures   EP      Checks    Mates
      { 0, 1,           0,         0,      0,        0},
      { 1, 20,          0,         0,      0,        0},
      { 2, 400,         0,         0,      0,        0},
      { 3, 8902,        34,        0,      12,       0},
      { 4, 197281,      1576,      0,      469,      8},
      { 5, 4865609,     82719,     258,    27351,    347},
      { 6, 119060324,   2812008,   5248,   809099,   10828},
      { 7, 3195901860L, 108329926, 319617, 33103848, 435816 }};
      // @formatter:on

    final int depth = 5;
    LOG.info("Start PERFT Test for depth {}", depth);

    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, false, true);
    search.startSearch(position, searchMode);
    waitWhileSearching();

    assertEquals(perftResults[depth][1], search.getSearchCounter().leafPositionsEvaluated);
    assertEquals(perftResults[depth][2], search.getSearchCounter().captureCounter);
    assertEquals(perftResults[depth][3], search.getSearchCounter().enPassantCounter);
    assertEquals(perftResults[depth][4], search.getSearchCounter().checkCounter);
    assertEquals(perftResults[depth][5], search.getSearchCounter().checkMateCounter);

    LOG.info("BOARDS: {}", String.format("%,d", search.getSearchCounter().leafPositionsEvaluated));
    LOG.info("PERFT Test for depth 5 successful.");
  }

  @Test
  @Disabled
  public void sizeOfSearchTreeTest() {

    int depth = 5;
    List<String> resultStrings = new ArrayList<>();
    List<String> fens = new ArrayList<>();

    LOG.info("Start SIZE Test for depth {}", depth);

    fens.add(Position.STANDARD_BOARD_FEN);
    fens.add("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
    fens.add("1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - -");
    fens.add("r1bq1rk1/pp2bppp/2n2n2/3p4/3P4/2N2N2/PPQ1BPPP/R1B2RK1 b - -");
    fens.add("1r1r2k1/2p1qp1p/6p1/ppQB1b2/5Pn1/2R1P1P1/PP5P/R1B3K1 b - -");

    for (String fen : fens) {
      resultStrings.add("");
      resultStrings.add(fen);
      featureMeasurements(depth, resultStrings, fen);
      resultStrings.add("");
    }

    LOG.info("");
    LOG.info("################## RESULTS ####################");
    for (String value : resultStrings) {
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
    search.config.USE_KILLER_MOVES = false;
    search.config.USE_STATIC_NULL_PRUNING = false;
    search.config.USE_RAZOR_PRUNING = false;
    search.config.USE_SORT_ALL_MOVES = false;

    measureTreeSize(position, searchMode, values, "WARM UP", true);
    measureTreeSize(position, searchMode, values, "REFERENCE", true);

    //    search.config.USE_ASPIRATION_WINDOW = true;
    //    measureTreeSize(position, searchMode, values, "Aspiration", true);

    search.config.USE_ALPHABETA_PRUNING = true;
    measureTreeSize(position, searchMode, values, "AlphaBeta", true);

    search.config.USE_ROOT_MOVES_SORT = true;
    measureTreeSize(position, searchMode, values, "RootMoveSort", true);

    search.config.USE_PVS = true;
    measureTreeSize(position, searchMode, values, "PVS", true);

    search.config.USE_SORT_ALL_MOVES = true;
    measureTreeSize(position, searchMode, values, "SORTALL", true);

    search.config.USE_MATE_DISTANCE_PRUNING = true;
    measureTreeSize(position, searchMode, values, "MDP", true);

    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    measureTreeSize(position, searchMode, values, "MPP", true);

    search.config.USE_PVS_MOVE_ORDERING = true;
    measureTreeSize(position, searchMode, values, "PVS_ORDER", true);

    search.config.USE_KILLER_MOVES = true;
    measureTreeSize(position, searchMode, values, "KILLER_PUSH", true);

    search.config.USE_NULL_MOVE_PRUNING = true;
    measureTreeSize(position, searchMode, values, "NMP", true);

    search.config.USE_STATIC_NULL_PRUNING = true;
    measureTreeSize(position, searchMode, values, "STATIC_NULL", true);

    search.config.USE_RAZOR_PRUNING = true;
    measureTreeSize(position, searchMode, values, "RAZOR", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    measureTreeSize(position, searchMode, values, "TT", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_QUIESCENCE = true;
    measureTreeSize(position, searchMode, values, "QS", true);

    // REPEAT
    measureTreeSize(position, searchMode, values, "REPEAT+TT", false);
  }

  private void measureTreeSize(final Position position, final SearchMode searchMode,
                               final List<String> values, final String feature,
                               final boolean clearTT) {

    if (clearTT) {
      search.clearHashTables();
    }
    search.startSearch(position, searchMode);
    waitWhileSearching();
    values.add(String.format("SIZE %-12s : %,14d >> %-14s (%4d) >> nps %,.0f >> %s ", feature,
                             search.getSearchCounter().leafPositionsEvaluated,
                             Move.toString(search.getLastSearchResult().bestMove),
                             search.getLastSearchResult().resultValue,
                             (1e3 * search.getSearchCounter().nodesVisited) /
                             search.getSearchCounter().lastSearchTime,
                             search.getSearchCounter().toString()));
  }

  @Test
  @Disabled
  public void evaluationTest() {
    String fen = Position.STANDARD_BOARD_FEN;
    SearchMode searchMode;
    Position position;

    search.config.USE_ROOT_MOVES_SORT = true;
    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_ASPIRATION_WINDOW = true;
    search.config.USE_PVS = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.USE_STATIC_NULL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = true;
    search.config.USE_KILLER_MOVES = true;

    int maxDepth = 6;
    int moveTime = 0;
    int mateIn = 0;
    boolean infinite = true;

    fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    position = new Position(fen);
    searchMode =
      new SearchMode(0, 0, 0, 0, 0, maxDepth, 0, mateIn, moveTime, null, false, infinite, false);
    search.startSearch(position, searchMode);
    waitWhileSearching();
    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));
    LOG.warn(search.getSearchCounter().toString());

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

  private void waitWhileSearching() {
    while (search.isSearching()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    final Position position =
      new Position("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");

    final SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 8, 0, 0, 0, null, false, true, false);

    prepare();

    int ROUNDS = 3;
    int ITERATIONS = 5;

    for (int round = 0; round < ROUNDS; round++) {
      long start = 0, end = 0, sum = 0;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);
      System.gc();

      int i = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        test1(position, searchMode);
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;

      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        test2(position, searchMode);
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 1 avg: %,.3f sec", round, avg1));
      result.add(String.format("Round %d Test 2 avg: %,.3f sec", round, avg2));
    }

    for (String s : result) {
      System.out.println(s);
    }

  }

  private void prepare() {
    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();
    search.config.USE_BOOK = false;
  }

  private void test1(final Position position, final SearchMode searchMode) {
    search.config.USE_SORT_ALL_MOVES = false;
    search.clearHashTables();
    search.startSearch(position, searchMode);
    waitWhileSearching();
  }

  private void test2(final Position position, final SearchMode searchMode) {
    search.config.USE_SORT_ALL_MOVES = true;
    search.clearHashTables();
    search.startSearch(position, searchMode);
    waitWhileSearching();
  }

}
