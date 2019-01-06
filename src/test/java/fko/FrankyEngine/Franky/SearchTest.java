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


import fko.FrankyEngine.Franky.Search.TTHit;
import fko.UCI.IUCIEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
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
public class SearchTest {

  private static final Logger LOG = LoggerFactory.getLogger(SearchTest.class);

  private IUCIEngine engine;
  private Search     search;

  @BeforeEach
  void setUp() {

    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();
    search.config.USE_BOOK = false;

  }

  @Test
  public void testWaitWhileSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    final int moveTime = 5000;
    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, moveTime, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    long startTime = System.currentTimeMillis();
    search.waitWhileSearching();
    final long endTime = System.currentTimeMillis() - startTime;
    System.out.printf("MoveTime was %,d and Duration was %,d %n", moveTime, endTime);
    assertTrue(endTime < moveTime + 100);
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
    search.waitWhileSearching();
    assertEquals(0, search.getSearchCounter().leafPositionsEvaluated);
    assertEquals(0, search.getSearchCounter().currentIterationDepth);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);

    // non timed search - should not use book
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);

  }

  @Test
  public void testDepthSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
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
    search.waitWhileSearching();
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
    search.waitWhileSearching();
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
    search.waitWhileSearching();
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
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 2000, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testMateSearch() {
    String fen;
    Position position;
    SearchMode searchMode;

    // mate
    fen = "8/8/8/8/8/3K4/8/R2k4 b - -"; // Position
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    search.stopSearch();
    assertEquals(-Evaluation.CHECKMATE, search.getLastSearchResult().resultValue);

    System.out.println();

    // mate 1
    fen = "8/8/8/8/8/3K4/R7/3k4 w - -"; // Position
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    search.stopSearch();
    assertEquals(Evaluation.CHECKMATE - 1, search.getLastSearchResult().resultValue);

    System.out.println();

    // mate in 2 (3 plys)
    fen = "8/8/8/8/8/5K2/R7/7k w - -"; // Position
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    search.stopSearch();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 3, search.getLastSearchResult().resultValue);

    System.out.println();

    // mate in 2 (4 plys)
    fen = "8/8/8/8/8/5K2/R7/6k1 b - -"; // Position
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    search.stopSearch();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 1);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(-Evaluation.CHECKMATE + 4, search.getLastSearchResult().resultValue);

    System.out.println();

    // mate in 3 (5 plys)
    fen = "8/8/8/8/8/4K3/R7/6k1 w - -";
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(Evaluation.CHECKMATE - 5, search.getLastSearchResult().resultValue);

    System.out.println();

    // mate in 3 (6 plys)
    fen = "8/8/8/8/8/4K3/R7/5k2 b - -";
    position = new Position(fen);
    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 3, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getSearchCounter().currentIterationDepth > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
    assertEquals(-Evaluation.CHECKMATE + 6, search.getLastSearchResult().resultValue);
  }

  @Test
  @Disabled
  public void testInfiniteSearch() {
    Position position = new Position();
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
  }

  @Test
  public void testMovesSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 1000, 0, 0, 0, Arrays.asList("h2h4"), false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertEquals("h2h4", Move.toUCINotation(position, search.getLastSearchResult().bestMove));
  }

  @Test
  public void testMultipleStartAndStopSearch() throws InterruptedException {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    // Test start and stop search
    for (int i = 0; i < 10; i++) {
      SearchMode searchMode =
        new SearchMode(0, 0, 0, 0, 0, 10000, 0, 0, 0, null, false, false, false);
      search.startSearch(position, searchMode);

      Thread.sleep(new Random().nextInt(1000) + 100);

      search.stopSearch();

      assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
      if (search.getLastSearchResult().bestMove == Move.NOMOVE) {
        System.out.println(search.getLastSearchResult());
        System.out.println();
      }
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE ||
                 search.getLastSearchResult().resultValue == -Evaluation.CHECKMATE);
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
      Thread.sleep(new Random().nextInt(100) + 100);
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
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 3000, 0, 0, 0, null, true, false, false);
    position.makeMove(Move.fromUCINotation(position, "e7e5"));
    search.startSearch(position, searchMode);
    Thread.sleep(1000);
    search.stopSearch();
    // new search
    position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    position.makeMove(Move.fromUCINotation(position, "c7c5"));
    searchMode = new SearchMode(0, 0, 0, 0, 0, 2000, 0, 0, 0, null, false, false, false);
    // Start search after miss
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
  }

  @Test
  public void testPonderHitSearch() throws InterruptedException {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    // Test start and stop search
    for (int i = 0; i < 10; i++) {
      SearchMode searchMode =
        new SearchMode(0, 0, 0, 0, 0, 1000, 0, 0, 0, null, true, false, false);
      position.makeMove(Move.fromUCINotation(position, "e7e5"));
      search.startSearch(position, searchMode);
      Thread.sleep(new Random().nextInt(100));
      search.ponderHit();
      search.waitWhileSearching();
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
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 10000, 0, 0, 0, null, true, false, false);
    // set last ponder move
    position.makeMove(Move.fromUCINotation(position, "g1h1"));
    // Start pondering
    search.startSearch(position, searchMode);
    // wait a bit - ponder search will be finished during this time
    Thread.sleep(3000);
    search.ponderHit();
    // test search
    search.waitWhileSearching();
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
    search.waitWhileSearching();
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

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 1000, 0, 0, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
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
    search.waitWhileSearching();
    assertEquals("g7f8", Move.toSimpleString(search.getLastSearchResult().bestMove));
    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

  }

  @Test
  void TT_Root_Test() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 0, null, false, true, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    position.makeMove(Move.fromSANNotation(position, "e4"));
    position.makeMove(Move.fromSANNotation(position, "e5"));

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    // What can be asserted here?
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
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, depth, 0, null, false, false, true);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();

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

    search.config.USE_BOOK = false;

    LOG.info("Start SIZE Test for depth {}", depth);

    //fens.add(Position.STANDARD_BOARD_FEN);
    //fens.add("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
    //fens.add("1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - -");
    //fens.add("r1bq1rk1/pp2bppp/2n2n2/3p4/3P4/2N2N2/PPQ1BPPP/R1B2RK1 b - -");
    fens.add("1r1r2k1/2p1qp1p/6p1/ppQB1b2/5Pn1/2R1P1P1/PP5P/R1B3K1 b - -");

    measureTreeSize(new Position(),
                    new SearchMode(0, 0, 0, 0, 0, 0, 0, depth, 0, null, false, true, false),
                    resultStrings, "WARM UP", true);

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
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, depth, 0, null, false, true, false);

    // turn off all optimizations to get a reference value of the search tree size
    search.config.USE_ALPHABETA_PRUNING = false;
    search.config.USE_ROOT_MOVES_SORT = false;
    search.config.USE_PVS = false;
    search.config.USE_PVS_MOVE_ORDERING = false;

    search.config.USE_TRANSPOSITION_TABLE = false;

    search.config.USE_TT_ROOT = false;
    search.config.USE_MATE_DISTANCE_PRUNING = false;
    search.config.USE_MINOR_PROMOTION_PRUNING = false;
    search.config.USE_NULL_MOVE_PRUNING = false;
    search.config.USE_KILLER_MOVES = false;
    search.config.USE_STATIC_NULL_PRUNING = false;
    search.config.USE_RAZOR_PRUNING = false;
    search.config.USE_LMR = false;

    search.config.USE_QUIESCENCE = false;

    measureTreeSize(position, searchMode, values, "REFERENCE", true);

    search.config.USE_ALPHABETA_PRUNING = true;
    measureTreeSize(position, searchMode, values, "BASE", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    measureTreeSize(position, searchMode, values, "TT", true);

    search.config.USE_ROOT_MOVES_SORT = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    measureTreeSize(position, searchMode, values, "PVS_ORDER", true);

    search.config.USE_TT_ROOT = true;
    measureTreeSize(position, searchMode, values, "TT_ROOT", false);
    search.config.USE_TT_ROOT = false;

    search.config.USE_KILLER_MOVES = true;
    measureTreeSize(position, searchMode, values, "KILLER_PUSH", true);

    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    measureTreeSize(position, searchMode, values, "MDP/MPP", true);

    search.config.USE_STATIC_NULL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = true;
    measureTreeSize(position, searchMode, values, "STATIC/RAZOR", true);

    search.config.USE_LMR = true;
    measureTreeSize(position, searchMode, values, "LMR", true);

    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.NULL_MOVE_DEPTH = 3;
    search.config.USE_VERIFY_NMP = true;
    search.config.NULL_MOVE_REDUCTION_VERIFICATION = 4;
    measureTreeSize(position, searchMode, values, "NMP", true);

    search.config.USE_QUIESCENCE = true;
    measureTreeSize(position, searchMode, values, "QS", true);

    search.config.USE_TT_ROOT = true;
    measureTreeSize(position, searchMode, values, "TT_ROOT", false);

    // REPEAT
    measureTreeSize(position, searchMode, values, "REPEAT+TT", false);
  }

  private void measureTreeSize(final Position position, final SearchMode searchMode,
                               final List<String> values, final String feature,
                               final boolean clearTT) {

    System.out.println("Testing. " + feature);
    if (clearTT) {
      search.clearHashTables();
    }
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    values.add(String.format("SIZE %-12s : %,14d >> %-18s (%4d) >> nps %,.0f >> %s ", feature,
                             search.getSearchCounter().leafPositionsEvaluated,
                             Move.toString(search.getLastSearchResult().bestMove),
                             search.getLastSearchResult().resultValue,
                             (1e3 * search.getSearchCounter().nodesVisited) /
                             search.getSearchCounter().lastSearchTime,
                             search.getSearchCounter().toString()));
  }

  /**
   * Razor and LMR make the search miss this mate at higher depths
   * Might be a consequence from these "optimizations" might be a bug
   * TODO: investigate more
   */
  @Test
  @Disabled
  public void mateSearchIssueTest() {

    // problematic positions:

    // - finds mate in
    // "6K1/n1P2N1p/6pr/b1pp3b/n2Bp1k1/1R2R1Pp/3p1P2/2qN1B2 w - -"
    //    Franky-0.8:
    //        1/6	00:00	 81	40k	-2,66	Nxh6+ Kg5
    //        2/10	00:00	 510	255k	-2,66	Nxh6+ Kg5
    //        3/13	00:00	 6k	167k	-2,16	Nxh6+ Kg5 Nf7+ Kg4
    //        4/11	00:00	 9k	98k	-1,88	Nxh6+ Kg5 Nf7+ Kf5 Bxh3+ Bg4
    //        5/23	00:00	 224k	318k	+M3	Rb6 cxd4 Rf6 dxe3 Nxe3+ <<<<<<<<<<<<<<<<<<
    //        6/22	00:01	 402k	338k	-1,88	Nxh6+ Kg5 Nf7+ Kf5 Bxh3+ Bg4 <<<<<<<<<<<<<<<<<<
    //        7/26	00:03	 1.415k	398k	+2,02	f4 Kf5 Bxh3+ Bg4 Nxh6+ Ke6 Bxg4+
    //        8/26	00:08	 3.463k	432k	+2,02	f4 Kf5 Bxh3+ Bg4 Nxh6+ Ke6 Bxg4+ Kd6
    //        9/29	00:32	 15.919k	495k	+M4	Rf3 Bxc7 Bf6 e3 Nxh6+ Kxf3 Rxe3+
    //        10/25	00:38	 19.435k	501k	+M3	Rb6 cxd4 Rf6 dxe3 Nxe3+

    // can't find mate after 10 plies / very slow search
    // r3r3/p1p2p1k/3p2pp/2p5/2P2n2/2N2B2/PPR1PP1q/3RQK2 b - - 0 1
    //    Franky-0.8:
    //        6/15	00:00	 300k	434k	-1,25	Qh3+ Kg1 Rab8 Nd5 Nxd5 cxd5
    //        7/19	00:04	 2.369k	563k	+0,43	Rac8 Rdc1 Re3 Ne4 Rxf3 Ng5+ hxg5 exf3
    //        8/24	00:20	 10.987k	548k	-1,41	Re5 Ne4 Qh3+ Kg1 Rb8 b3 Qe6 Qc3
    //        9/28	02:15	 56.966k	421k	-0,71	Qh3+ Kg1 Rac8 Qd2 Qh4 e4 Nh3+ Kh2 Nxf2+ Kg1
    //        10/26	03:40	 85.856k	388k	-0,54	Re5 Ne4 Qh3+ Kg1 Qf5 Nf6+ Kg7 Qc3 Nxe2+ Rxe2 Qxf6

    // found M4 then went to M5 again
    // n7/3p1p2/NpkNp1p1/1p2P3/3Q4/6B1/b7/4K3 w - - 0 1
    // Franky-0.8:
    // 1/5	00:00	 73	36k	+12,51	Nb4+ Kc7 Nxb5+ Kc8 Nxa2
    // 2/5	00:00	 229	114k	+12,51	Nb4+ Kc7
    // 3/11	00:00	 3k	168k	+12,51	Nb4+ Kc7 Nxb5+
    // 4/9	00:00	 5k	283k	+12,51	Nb4+ Kc7 Nxb5+ Kd8 Nxa2
    // 5/15	00:00	 92k	529k	+12,51	Nb4+ Kc7 Nxb5+ Kc8 Nxa2
    // 6/14	00:00	 140k	493k	+12,91	Nb4+ Kc7 Nxa2 f5 Nb4 g5 Nxb5+ Kc8
    // 7/19	00:01	 1.060k	599k	+13,15	Nb4+ Kc7 Nxa2 f5 exf6 g5 Nxb5+ Kc6
    // 8/18	00:02	 1.618k	562k	+13,93	Nb4+ Kc7 Nxa2 f6 Qc3+ Kd8 Qf3 fxe5 Qxa8+ Ke7 Bxe5
    // 9/22	00:12	 8.081k	653k	+M5	Qa1 Bd5 Nb4+ Kc7 Nxd5+ exd5 Qxa8 d4 Nxb5+
    // 10/24	00:19	 12.505k	648k	+M5	Nb4+ Kc7 Qh4 f6 Qh8 fxe5 Bxe5 g5 Qc8+
    // 11/24	00:30	 19.145k	622k	+M4	Qg1 g5 Bf2 Kd5 Bxb6 Nxb6 Qc5+ <<<<<<<<<<<<<<<<<<
    // 12/26	00:57	 33.514k	580k	+M5	Qg1 Bc4 Qh1+ Bd5 Qh8 Nc7 Qc8 b4 Qxc7+ <<<<<<<<<<<<<<<<<<

    // finds M§ which is wrong!!
    // K6Q/1p6/pPq4P/P2p2P1/4pP1N/7k/n5R1/1n2BB2 w - - 0 1
    // Franky-0.8:
    // 1/6	00:00	 80	40k	+14,05	Rc2+ Kg4 Rxc6 bxc6
    // 2/11	00:00	 685	38k	+14,05	Rc2+ Kg4
    // 3/14	00:00	 9k	250k	+14,05	Rc2+ Kg4 Rxc6
    // 4/13	00:00	 19k	284k	+14,05	Rc2+ Kg4 Rxc6 bxc6
    // 5/18	00:00	 284k	393k	+19,70	Ng6 Nbc3 Ne5 Qe8+ Qxe8
    // 6/16	00:01	 491k	401k	+15,01	Qf8 Qe6 Rxa2+ Kg4 Rb2 Nc3 Bxc3 Kxh4 Kxb7
    // 7/20	00:02	 1.111k	444k	+M3	Rg1+ Kh2 Qb2+ Kxg1 Qg2+  <<<<<<<<<<<<<<<<<<
    // 8/22	00:04	 2.409k	505k	+M4	Rg1+ Kh2 Qb2+ Nd2 Qxd2+ Kxg1 Qg2+
    // 9/21	00:06	 3.814k	548k	+M4	Rg1+ Kh2 Qb2+ Nd2 Qxd2+ Kxg1 Qg2+
    // 10/23	00:23	 12.141k	512k	+M5	Rg1+ Kh2 Bg3+ Kxg1 Qd4+ e3 Qxe3+ Kxf1 Qf2+
    // 11/23	00:58	 27.067k	461k	+M4	Bxa6 bxa6+ b7 Qxh6 gxh6 Nd2 Qc8+

    // Found M5 in M4 which is wrong!!!
    // r2r4/1p1R3p/5pk1/b1B1Pp2/p4P2/P7/1P5P/1K1R4 w - - 0 1
    // Franky-0.8:
    // 1/5	00:00	 64	32k	+6,30	Rxd8
    // 2/5	00:00	 295	147k	+1,70	Rg1+ Kh6 Rxb7 fxe5 fxe5
    // 3/9	00:00	 2k	91k	+1,70	Rg1+ Kh6 Rxb7
    // 4/10	00:00	 6k	184k	+1,70	Rg1+ Kh6 Rxb7 fxe5 c3 fxe5
    // 5/13	00:00	 19k	299k	+1,58	Rg1+ Kh6 Rxb7 Bd2 Rb6
    // 6/13	00:00	 97k	361k	+1,68	Rg1+ Kh6 Rf7 fxe5 Rf6+ Kh5
    // 7/16	00:00	 233k	424k	+M4	Rg1+ Kh6 Bf8+ Rxf8 Rd3 fxe5 Rh3+
    // 8/16	00:01	 857k	553k	+1,68	Rg1+ Kh6 Rf7 fxe5 Rf6+ Kh5 Rxf5+ Kh4 fxe5
    // 9/20	00:03	 1.946k	610k	+M4	Rg1+ Kh6 Bf8+ Rxf8 Rd3 fxe5 Rh3+
    // 10/19	00:08	 5.514k	647k	+M5	Rg1+ Kh6 Bf8+ Rxf8 Rd3 Be1 Rh3+ Bh4 Rxh4+
    // 11/23	00:23	 14.678k	632k	+M5	Rg1+ Kh6 Bf8+ Rxf8 Rd3 Be1 Rh3+ Bh4 Rxh4+

    // Zugzwang Mate - not found
    // 8/8/8/p7/8/8/R6p/2K2Rbk w - - 0 1
    // Stockfish_10_x64:
    //  66/22	00:12	 21.952k	1.741k	+M5	Raf2 a4 Kd2 a3 Ra1 a2 Ke1 Bxf2+ Kxf2+
    // Franky-0.8:
    // 1/3	00:00	 38	19k	+6,37	Rxa5
    // 2/2	00:00	 68	34k	+6,07	Rxa5 Kg2
    // 3/5	00:00	 543	271k	+6,41	Kb1 a4 Rxa4
    // 4/6	00:00	 2k	956k	+6,41	Kb1 a4 Re1 a3 Rxa3
    // 5/8	00:00	 6k	339k	+6,89	Kc2 a4 Kd3 a3 Rxa3
    // 6/10	00:00	 19k	397k	+6,45	Re1 a4 Rd1 a3 Kc2 Kg2 Rxa3
    // 7/11	00:00	 32k	398k	+9,97	Re1 a4 Re5 Bc5 Rxc5 a3 Rxa3
    // 8/14	00:00	 144k	536k	+6,89	Rc2 a4 Kb2 a3+ Kb3 a2 Kc4 a1Q Rxa1
    // 9/14	00:00	 302k	601k	+6,95	Rc2 a4 Rd2 a3 Kc2 a2 Kc3 a1Q+ Rxa1
    // 10/16	00:02	 1.916k	665k	+6,45	Rc2 a4 Rf3 a3 Kb1 Bd4 Rf1+
    // 11/19	00:11	 9.299k	805k	+6,68	Rc2 a4 Kb2 a3+ Kb3 a2 Rc8 a1Q Rxa1 Kg2 Kc4
    // 12/20	00:20	 14.711k	729k	+6,87	Rc2 a4 Ra2 a3 Rf3 Bd4 Rf1+ Bg1 Raf2 a2 Kc2 a1Q Rxa1
    // 13/20	00:38	 27.806k	724k	+7,35	Rc2 a4 Re1 a3 Ree2 Bd4 Rxh2+ Kg1 Rh3 Kf1 Kd1 Kg1 Rxa3
    // 14/21	01:24	 52.191k	614k	+7,35	Rc2 a4 Re1 a3 Ree2 Bd4 Re1+
    // 15/24	04:33	 174.777k	638k	+7,35	Rc2 a4 Rf3 Bd4 Rf1+ Bg1

    String fen;
    SearchMode searchMode;
    Position position;

    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_ROOT_MOVES_SORT = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_KILLER_MOVES = true;

    search.config.USE_TRANSPOSITION_TABLE = true;

    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;

    // some of these make the search miss this mate at higher depths
    // might be a consequence from these "optimizations" might be a bug
    search.config.USE_NULL_MOVE_PRUNING = true;
    //    search.config.NULL_MOVE_DEPTH = 3;
    //    search.config.USE_VERIFY_NMP = true;
    //    search.config.NULL_MOVE_REDUCTION_VERIFICATION = 4;

    search.config.USE_RAZOR_PRUNING = true;
    search.config.USE_LMR = true;
    search.config.USE_QUIESCENCE = true;

    int maxDepth = 7;
    int moveTime = 0;
    int mateIn = 3;
    boolean infinite = true;

    // mate in 3
    //    final int pliesToMate = 5;
    //    fen = "4r1b1/1p4B1/pN2pR2/RB2k3/1P2N2p/2p3b1/n2P1p1r/5K1n w - -";
    // mate in 5
    final int pliesToMate = 5;
    fen = "6K1/n1P2N1p/6pr/b1pp3b/n2Bp1k1/1R2R1Pp/3p1P2/2qN1B2 w - -";
    position = new Position(fen);
    searchMode =
      new SearchMode(0, 0, 0, 0, 0, moveTime, 0, maxDepth, mateIn, null, false, infinite, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    LOG.warn(search.getSearchCounter().toString());

    assertEquals(Evaluation.CHECKMATE - pliesToMate, search.getLastSearchResult().resultValue);

  }

  @Test
  @Disabled
  public void evaluationTest() {
    String fen;
    SearchMode searchMode;
    Position position;

    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_ROOT_MOVES_SORT = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_KILLER_MOVES = true;

    search.config.USE_TRANSPOSITION_TABLE = true;

    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.USE_STATIC_NULL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = false;
    search.config.USE_LMR = false;

    search.config.USE_QUIESCENCE = false;

    int maxDepth = 6;
    int moveTime = 0;
    int mateIn = 0;
    boolean infinite = true;

    fen = "4r1b1/1p4B1/pN2pR2/RB2k3/1P2N2p/2p3b1/n2P1p1r/5K1n w - -";
    //    fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    //    fen = "r2q1rk1/1p1nbppp/3p1n2/1Pp2b2/p1P5/2N1Pp1P/PBNPB1P1/R2Q1RK1 w - -";
    //    fen = "4k3/4p3/8/8/8/8/8/3KQ3 w - -";
    position = new Position(fen);
    searchMode =
      new SearchMode(0, 0, 0, 0, 0, moveTime, 0, maxDepth, mateIn, null, false, infinite, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    LOG.warn(search.getSearchCounter().toString());

    assertEquals(Evaluation.CHECKMATE - 5, search.getLastSearchResult().resultValue);

  }

  @Test
  @Disabled
  public void testManualMateSearch() {
    String fen;
    Position position;
    SearchMode searchMode;

    search.config.USE_ROOT_MOVES_SORT = true;
    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_ASPIRATION_WINDOW = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.USE_STATIC_NULL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = true;
    search.config.USE_KILLER_MOVES = true;
    search.config.USE_LMR = true;

    // FIXME
    //  Sporadic fails here - suspect TT
    int i = 0;
    while (i++ < 1000) {
      System.out.println("Test NR " + i);
      fen = "8/8/8/8/4K3/8/R7/4k3 w - -"; // Position
      position = new Position(fen);
      searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 12, 5, null, false, false, false);
      search.startSearch(position, searchMode);
      search.waitWhileSearching();
      search.stopSearch();
      assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
      assertTrue(search.getSearchCounter().currentIterationDepth > 1);
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
      assertEquals(Evaluation.CHECKMATE - 9, search.getLastSearchResult().resultValue);
    }
  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    final Position position =
      new Position("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");

    final SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 0, null, false, true, false);

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
    //search.config.USE_SORT_ALL_MOVES = false;
    search.clearHashTables();
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
  }

  private void test2(final Position position, final SearchMode searchMode) {
    //search.config.USE_SORT_ALL_MOVES = true;
    search.clearHashTables();
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
  }

  @Test
  @Disabled
  public void showSize() {
    //System.out.println(VM.current().details());
    System.out.println(ClassLayout.parseClass(TTHit.class).toPrintable());
    //System.out.println(ClassLayout.parseClass(Search.class).toPrintable());
  }
}
