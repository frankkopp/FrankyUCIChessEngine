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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

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
    assertTrue(endTime < moveTime + 200);
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
  public void testIterativeSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 10, 0, null, false, true, false);
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
  public void testMovesSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, Collections.singletonList("h2h4"), false, true,
                     false);
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
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE
                 || search.getLastSearchResult().resultValue == -Evaluation.CHECKMATE);
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

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 0, null, false, false, false);
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
    assertNotEquals("g8f7", Move.toSimpleString(search.getLastSearchResult().bestMove));
    LOG.warn("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

  }

  @Test
  public void test3FoldRep2() {

    {
      // black can force repetition 1. ... Qe3+ 3.Kg2 Qe2+ 4.Kh3 Qh5+ 5.Kg2
      String fen = "6k1/p3q2p/1n1Q2pB/8/5P2/6P1/PP5P/3R2K1 b - -";
      Position position = new Position(fen);

      SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 0, null, false, false, false);
      search.startSearch(position, searchMode);
      search.waitWhileSearching();
      assertEquals("e7e3", Move.toSimpleString(search.getLastSearchResult().bestMove));
      assertEquals(-Evaluation.getGamePhaseFactor(position) * EvaluationConfig.CONTEMPT_FACTOR,
                   search.getLastSearchResult().resultValue);
      LOG.warn("Best Move: {} Value: {} Ponder {}",
               Move.toSimpleString(search.getLastSearchResult().bestMove),
               search.getLastSearchResult().resultValue / 100f,
               Move.toSimpleString(search.getLastSearchResult().ponderMove));
    }

    {
      // black can force repetition 1. ... Rd6 2.Qxd6 Qe3+ 3.Kg2 Qe2+ 4.Kh3 Qh5+ 5.Kg2
      String fen = "6k1/p3q2p/1nr3pB/8/3Q1P2/6P1/PP5P/3R2K1 b - -";
      Position position = new Position(fen);

    }

  }

  @Test
  void TT_Root_Test() {
    String fen = Position.STANDARD_BOARD_FEN;
    fen = "8/6R1/1rp1k3/6p1/3KPp1p/5P1P/8/8 b - -";
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 12, 0, null, false, true, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    //    position.makeMove(Move.fromSANNotation(position, "e4"));
    //    position.makeMove(Move.fromSANNotation(position, "e5"));

    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 14, 0, null, false, true, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    // What can be asserted here?
  }

  @Test
  void aspiration_search() {
    String fen;
    SearchMode searchMode;
    Position position;

    int maxDepth = 6;
    int moveTime = 0;
    int mateIn = 0;
    boolean infinite = true;
    searchMode =
      new SearchMode(0, 0, 0, 0, 0, moveTime, 0, maxDepth, mateIn, null, false, infinite, false);

    String result = "";

    fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    position = new Position(fen);

    search.config.USE_ASPIRATION_WINDOW = false;

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    result += String.format("%nSIZE: %,14d >> %-18s (%4d) >> nps %,.0f >> %s %n",
                            search.getSearchCounter().leafPositionsEvaluated,
                            Move.toString(search.getLastSearchResult().bestMove),
                            search.getLastSearchResult().resultValue,
                            (1e3 * search.getSearchCounter().nodesVisited)
                            / search.getSearchCounter().lastSearchTime,
                            search.getSearchCounter().toString());

    search.config.USE_ASPIRATION_WINDOW = true;

    search.clearHashTables();

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    result += String.format("SIZE: %,14d >> %-18s (%4d) >> nps %,.0f >> %s %n",
                            search.getSearchCounter().leafPositionsEvaluated,
                            Move.toString(search.getLastSearchResult().bestMove),
                            search.getLastSearchResult().resultValue,
                            (1e3 * search.getSearchCounter().nodesVisited)
                            / search.getSearchCounter().lastSearchTime,
                            search.getSearchCounter().toString());

    System.out.println(result);

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
  public void testAdvTimeControl() {
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
    //    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 4, 0, null, false, false, false);
    //    search.startSearch(position, searchMode);
    //    search.waitWhileSearching();
    //    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    //    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
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
  @Disabled
  public void sizeOfSearchTreeTest() {

    int depth = 6;
    List<String> resultStrings = new ArrayList<>();
    List<String> fens = new ArrayList<>();

    search.config.USE_BOOK = false;

    LOG.info("Start SIZE Test for depth {}", depth);

    fens.add(Position.STANDARD_BOARD_FEN);
    fens.add("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
    fens.add("1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - -");
    fens.add("r1bq1rk1/pp2bppp/2n2n2/3p4/3P4/2N2N2/PPQ1BPPP/R1B2RK1 b - -");
    fens.add("1r1r2k1/2p1qp1p/6p1/ppQB1b2/5Pn1/2R1P1P1/PP5P/R1B3K1 b - -");

    measureTreeSize(new Position(),
                    new SearchMode(0, 0, 0, 0, 0, 0, 0, depth, 0, null, false, true, false),
                    resultStrings, "WARM UP", true);

    search.clearHashTables();

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
    search.config.USE_PVS = false;
    search.config.USE_PVS_MOVE_ORDERING = false;
    search.config.USE_ASPIRATION_WINDOW = false;

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

    //    measureTreeSize(position, searchMode, values, "REFERENCE", true);

    search.config.USE_ALPHABETA_PRUNING = true;
    //    measureTreeSize(position, searchMode, values, "BASE", true);

    search.config.USE_TRANSPOSITION_TABLE = true;
    //    measureTreeSize(position, searchMode, values, "TT", true);

    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    //    measureTreeSize(position, searchMode, values, "PVS_ORDER", true);

    search.config.USE_TT_ROOT = true;
    //    measureTreeSize(position, searchMode, values, "TT_ROOT", false);
    //    search.config.USE_TT_ROOT = false;

    search.config.USE_KILLER_MOVES = true;
    //    measureTreeSize(position, searchMode, values, "KILLER_PUSH", true);

    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;
    //    measureTreeSize(position, searchMode, values, "MDP/MPP", true);

    search.config.USE_STATIC_NULL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = true;
    //    measureTreeSize(position, searchMode, values, "STATIC/RAZOR", true);

    search.config.USE_LMR = true;
    //    measureTreeSize(position, searchMode, values, "LMR", true);

    search.config.USE_NULL_MOVE_PRUNING = true;
    //    search.config.NULL_MOVE_DEPTH = 3;
    //    search.config.USE_VERIFY_NMP = true;
    //    search.config.NULL_MOVE_REDUCTION_VERIFICATION = 4;
    //    measureTreeSize(position, searchMode, values, "NMP", true);

    search.config.USE_QUIESCENCE = true;
    //    measureTreeSize(position, searchMode, values, "QS", true);

    //    search.config.USE_TT_ROOT = true;
    measureTreeSize(position, searchMode, values, "ALL", true);

    search.config.USE_ASPIRATION_WINDOW = true;
    measureTreeSize(position, searchMode, values, "ASPIRATION", true);

    // REPEAT
    //    measureTreeSize(position, searchMode, values, "REPEAT+TT", false);
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
                             (1e3 * search.getSearchCounter().nodesVisited)
                             / search.getSearchCounter().lastSearchTime,
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

    boolean infinite = false;
    int maxDepth = 0;
    int moveTime = 10000;
    int mateIn = 5;
    int pliesToMate = 7;
    String fen = "6K1/n1P2N1p/6pr/b1pp3b/n2Bp1k1/1R2R1Pp/3p1P2/2qN1B2 w - -";

    // these should not change result
    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_KILLER_MOVES = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;

    // these can change result
    // some of these make the search miss this mate at higher depths
    // might be a consequence from these "optimizations" might be a bug
    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.NULL_MOVE_DEPTH = 2;
    search.config.USE_VERIFY_NMP = true;
    search.config.NULL_MOVE_REDUCTION_VERIFICATION = 3;

    search.config.USE_RAZOR_PRUNING = true;
    search.config.USE_LMR = true;
    search.config.USE_QUIESCENCE = true;

    Position position = new Position(fen);
    SearchMode searchMode =
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

    search.config.USE_BOOK = false;

    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_MOVE_ORDERING = true;
    search.config.USE_KILLER_MOVES = true;
    search.config.USE_ASPIRATION_WINDOW = true;
    search.config.USE_MATE_DISTANCE_PRUNING = true;
    search.config.USE_MINOR_PROMOTION_PRUNING = true;

    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_TT_ROOT = true;

    search.config.USE_NULL_MOVE_PRUNING = true;
    search.config.NULL_MOVE_DEPTH = 2;
    search.config.USE_VERIFY_NMP = true;
    search.config.NULL_MOVE_REDUCTION_VERIFICATION = 3;

    search.config.USE_STATIC_NULL_PRUNING = true;
    search.config.USE_RAZOR_PRUNING = true;
    search.config.USE_LMR = true;

    search.config.USE_QUIESCENCE = true;

    int maxDepth = 8;
    int moveTime = 0;
    int mateIn = 0;
    boolean infinite = true;

    String result = "";

    fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    // fen = "r1bq1rk1/pp2bppp/2n2n2/3p4/3P4/2N2N2/PPQ1BPPP/R1B2RK1 b - -";
    // fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    // fen = "r2q1rk1/1p1nbppp/3p1n2/1Pp2b2/p1P5/2N1Pp1P/PBNPB1P1/R2Q1RK1 w - -";
    // fen = "4k3/4p3/8/8/8/8/8/3KQ3 w - -";
    //    fen = "7k/8/8/8/6p1/3N3N/P4p2/1K6 w - -";

    position = new Position(fen);
    searchMode =
      new SearchMode(0, 0, 0, 0, 0, moveTime, 0, maxDepth, mateIn, null, false, infinite, false);

    // results should be the same with or without aspiration windows
    //SIZE:      1.209.389 >> NORMAL bc8-g4      ( -83) >> nps 92.560 >> SearchCounter{nodesVisited=1209395, lastSearchTime=00:00:13.066, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=1209389, nonLeafPositionsEvaluated=0, checkCounter=49935, checkMateCounter=0, captureCounter=264419, enPassantCounter=140, positionsNonQuiet=0, nodeCache_Hits=0, nodeCache_Misses=0, movesGenerated=1232507, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=5986, pvs_cutoffs=982459, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=0}
    //SIZE:      1.438.298 >> NORMAL bc8-g4      ( -83) >> nps 106.906 >> SearchCounter{nodesVisited=1438312, lastSearchTime=00:00:13.454, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=1438298, nonLeafPositionsEvaluated=0, checkCounter=75913, checkMateCounter=0, captureCounter=362170, enPassantCounter=269, positionsNonQuiet=0, nodeCache_Hits=0, nodeCache_Misses=0, movesGenerated=1505868, pvs_root_researches=4, pvs_root_cutoffs=347, pvs_researches=6645, pvs_cutoffs=1118873, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=8}
    //
    //Killer
    //SIZE:        716.768 >> NORMAL bc8-g4      ( -83) >> nps 77.372 >> SearchCounter{nodesVisited=716774, lastSearchTime=00:00:09.264, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=716768, nonLeafPositionsEvaluated=0, checkCounter=38662, checkMateCounter=0, captureCounter=172954, enPassantCounter=110, positionsNonQuiet=0, nodeCache_Hits=0, nodeCache_Misses=0, movesGenerated=746895, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=2525, pvs_cutoffs=565213, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=0}
    //SIZE:        951.743 >> NORMAL bc8-g4      ( -83) >> nps 126.547 >> SearchCounter{nodesVisited=951757, lastSearchTime=00:00:07.521, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=951743, nonLeafPositionsEvaluated=0, checkCounter=56333, checkMateCounter=0, captureCounter=270340, enPassantCounter=238, positionsNonQuiet=0, nodeCache_Hits=0, nodeCache_Misses=0, movesGenerated=1022371, pvs_root_researches=4, pvs_root_cutoffs=347, pvs_researches=3236, pvs_cutoffs=707178, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=8}
    //
    //MP
    //SIZE:        716.768 >> NORMAL bc8-g4      ( -83) >> nps 77.364 >> SearchCounter{nodesVisited=716774, lastSearchTime=00:00:09.265, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=716768, nonLeafPositionsEvaluated=0, checkCounter=38662, checkMateCounter=0, captureCounter=172954, enPassantCounter=110, positionsNonQuiet=0, nodeCache_Hits=0, nodeCache_Misses=0, movesGenerated=746895, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=2525, pvs_cutoffs=565213, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=0}
    //SIZE:        951.743 >> NORMAL bc8-g4      ( -83) >> nps 130.467 >> SearchCounter{nodesVisited=951757, lastSearchTime=00:00:07.295, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=951743, nonLeafPositionsEvaluated=0, checkCounter=56333, checkMateCounter=0, captureCounter=270340, enPassantCounter=238, positionsNonQuiet=0, nodeCache_Hits=0, nodeCache_Misses=0, movesGenerated=1022371, pvs_root_researches=4, pvs_root_cutoffs=347, pvs_researches=3236, pvs_cutoffs=707178, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=8}
    //
    //TT
    //SIZE:        437.097 >> NORMAL bc8-g4      ( -83) >> nps 59.466 >> SearchCounter{nodesVisited=460626, lastSearchTime=00:00:07.746, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=437097, nonLeafPositionsEvaluated=0, checkCounter=21643, checkMateCounter=0, captureCounter=100515, enPassantCounter=76, positionsNonQuiet=0, nodeCache_Hits=63461, nodeCache_Misses=46505, movesGenerated=453512, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=24533, pvs_cutoffs=324897, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=0}
    //SIZE:        186.486 >> NORMAL bc8-g4      ( -83) >> nps 136.962 >> SearchCounter{nodesVisited=273376, lastSearchTime=00:00:01.996, currentBestRootMove=67263346, currentBestRootValue=-83, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=67246290, currentRootMoveNumber=36, leafPositionsEvaluated=186486, nonLeafPositionsEvaluated=0, checkCounter=29482, checkMateCounter=0, captureCounter=97200, enPassantCounter=132, positionsNonQuiet=0, nodeCache_Hits=180146, nodeCache_Misses=1336, movesGenerated=194035, pvs_root_researches=180, pvs_root_cutoffs=69, pvs_researches=86770, pvs_cutoffs=4794, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=0, nullMoveVerifications=0, lmrReductions=0, aspirationResearches=3}
    //
    //NULL
    //SIZE:        271.342 >> NORMAL bc8-g4      ( -51) >> nps 48.362 >> SearchCounter{nodesVisited=289401, lastSearchTime=00:00:05.984, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=271342, nonLeafPositionsEvaluated=0, checkCounter=12191, checkMateCounter=0, captureCounter=60232, enPassantCounter=24, positionsNonQuiet=0, nodeCache_Hits=46334, nodeCache_Misses=24318, movesGenerated=273738, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=18685, pvs_cutoffs=198437, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=931, nullMoveVerifications=99, lmrReductions=0, aspirationResearches=0}
    //SIZE:         60.903 >> NORMAL bc8-g4      ( -51) >> nps 106.440 >> SearchCounter{nodesVisited=86323, lastSearchTime=00:00:00.811, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=67246290, currentRootMoveNumber=36, leafPositionsEvaluated=60903, nonLeafPositionsEvaluated=0, checkCounter=8436, checkMateCounter=0, captureCounter=28987, enPassantCounter=20, positionsNonQuiet=0, nodeCache_Hits=54750, nodeCache_Misses=378, movesGenerated=59783, pvs_root_researches=176, pvs_root_cutoffs=73, pvs_researches=25332, pvs_cutoffs=5496, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=1691, nullMoveVerifications=8, lmrReductions=0, aspirationResearches=3}
    //
    //STATIC
    //SIZE:        263.500 >> NORMAL bc8-g4      ( -51) >> nps 51.343 >> SearchCounter{nodesVisited=278435, lastSearchTime=00:00:05.423, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=263500, nonLeafPositionsEvaluated=0, checkCounter=12069, checkMateCounter=0, captureCounter=52412, enPassantCounter=13, positionsNonQuiet=0, nodeCache_Hits=38088, nodeCache_Misses=29440, movesGenerated=265896, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=15561, pvs_cutoffs=201562, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=931, nullMoveVerifications=99, lmrReductions=0, aspirationResearches=0}
    //SIZE:         55.566 >> NORMAL bc8-g4      ( -51) >> nps 88.103 >> SearchCounter{nodesVisited=75945, lastSearchTime=00:00:00.862, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=67246290, currentRootMoveNumber=36, leafPositionsEvaluated=55566, nonLeafPositionsEvaluated=0, checkCounter=8183, checkMateCounter=0, captureCounter=23656, enPassantCounter=12, positionsNonQuiet=0, nodeCache_Hits=44639, nodeCache_Misses=5448, movesGenerated=54446, pvs_root_researches=176, pvs_root_cutoffs=73, pvs_researches=20291, pvs_cutoffs=10537, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=1691, nullMoveVerifications=8, lmrReductions=0, aspirationResearches=3}
    //
    //RAZOR
    //SIZE:        261.568 >> NORMAL bc8-g4      ( -51) >> nps 42.784 >> SearchCounter{nodesVisited=276382, lastSearchTime=00:00:06.460, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=261568, nonLeafPositionsEvaluated=0, checkCounter=12064, checkMateCounter=0, captureCounter=52277, enPassantCounter=13, positionsNonQuiet=0, nodeCache_Hits=37967, nodeCache_Misses=29474, movesGenerated=263782, pvs_root_researches=4, pvs_root_cutoffs=206, pvs_researches=15618, pvs_cutoffs=199516, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=931, nullMoveVerifications=99, lmrReductions=0, aspirationResearches=0}
    //SIZE:         55.566 >> NORMAL bc8-g4      ( -51) >> nps 88.206 >> SearchCounter{nodesVisited=75945, lastSearchTime=00:00:00.861, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=67246290, currentRootMoveNumber=36, leafPositionsEvaluated=55566, nonLeafPositionsEvaluated=0, checkCounter=8183, checkMateCounter=0, captureCounter=23656, enPassantCounter=12, positionsNonQuiet=0, nodeCache_Hits=44639, nodeCache_Misses=5448, movesGenerated=54446, pvs_root_researches=176, pvs_root_cutoffs=73, pvs_researches=20291, pvs_cutoffs=10537, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=1691, nullMoveVerifications=8, lmrReductions=0, aspirationResearches=3}
    //
    //LMR
    //SIZE:        497.206 >> NORMAL bc8-g4      ( -51) >> nps 46.703 >> SearchCounter{nodesVisited=515135, lastSearchTime=00:00:11.030, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=134341478, currentRootMoveNumber=36, leafPositionsEvaluated=497206, nonLeafPositionsEvaluated=0, checkCounter=13115, checkMateCounter=0, captureCounter=111286, enPassantCounter=7, positionsNonQuiet=0, nodeCache_Hits=55362, nodeCache_Misses=67804, movesGenerated=501967, pvs_root_researches=148, pvs_root_cutoffs=62, pvs_researches=43124, pvs_cutoffs=331264, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=598, nullMoveVerifications=11, lmrReductions=44611, aspirationResearches=0}
    //SIZE:        933.240 >> NORMAL bc8-g4      ( -51) >> nps 73.021 >> SearchCounter{nodesVisited=991473, lastSearchTime=00:00:13.578, currentBestRootMove=67263346, currentBestRootValue=-51, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=6, currentRootMove=67246290, currentRootMoveNumber=36, leafPositionsEvaluated=933240, nonLeafPositionsEvaluated=0, checkCounter=28995, checkMateCounter=0, captureCounter=218595, enPassantCounter=42, positionsNonQuiet=0, nodeCache_Hits=172982, nodeCache_Misses=90006, movesGenerated=912637, pvs_root_researches=154, pvs_root_cutoffs=133, pvs_researches=88157, pvs_cutoffs=629134, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=1353, nullMoveVerifications=250, lmrReductions=57569, aspirationResearches=5}
    //
    //QS
    //SIZE:        469.339 >> NORMAL bc8-g4      ( -32) >> nps 77.392 >> SearchCounter{nodesVisited=592205, lastSearchTime=00:00:07.652, currentBestRootMove=67263346, currentBestRootValue=-32, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=14, currentRootMove=67246290, currentRootMoveNumber=36, leafPositionsEvaluated=469339, nonLeafPositionsEvaluated=4, checkCounter=1265, checkMateCounter=0, captureCounter=97807, enPassantCounter=6, positionsNonQuiet=45578, nodeCache_Hits=307742, nodeCache_Misses=284482, movesGenerated=829051, pvs_root_researches=67, pvs_root_cutoffs=143, pvs_researches=124301, pvs_cutoffs=254405, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=737, nullMoveVerifications=57, lmrReductions=13306, aspirationResearches=0}
    //SIZE:         79.131 >> NORMAL bc8-g4      ( -32) >> nps 124.998 >> SearchCounter{nodesVisited=102623, lastSearchTime=00:00:00.821, currentBestRootMove=67263346, currentBestRootValue=-32, currentIterationDepth=6, currentSearchDepth=6, currentExtraSearchDepth=14, currentRootMove=67299571, currentRootMoveNumber=36, leafPositionsEvaluated=79131, nonLeafPositionsEvaluated=4, checkCounter=133, checkMateCounter=0, captureCounter=22338, enPassantCounter=1, positionsNonQuiet=10813, nodeCache_Hits=77019, nodeCache_Misses=25600, movesGenerated=112266, pvs_root_researches=224, pvs_root_cutoffs=59, pvs_researches=21784, pvs_cutoffs=31314, mateDistancePrunings=0, minorPromotionPrunings=0, nullMovePrunings=1926, nullMoveVerifications=27, lmrReductions=842, aspirationResearches=4}

    search.config.USE_ASPIRATION_WINDOW = false;

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    result += String.format("%nSIZE: %,14d >> %-18s (%4d) >> nps %,.0f >> %s %n",
                            search.getSearchCounter().leafPositionsEvaluated,
                            Move.toString(search.getLastSearchResult().bestMove),
                            search.getLastSearchResult().resultValue,
                            (1e3 * search.getSearchCounter().nodesVisited)
                            / search.getSearchCounter().lastSearchTime,
                            search.getSearchCounter().toString());

    int bestMove1 = search.getLastSearchResult().bestMove;

    search.config.USE_ASPIRATION_WINDOW = true;

    search.clearHashTables();

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    result += String.format("SIZE: %,14d >> %-18s (%4d) >> nps %,.0f >> %s %n",
                            search.getSearchCounter().leafPositionsEvaluated,
                            Move.toString(search.getLastSearchResult().bestMove),
                            search.getLastSearchResult().resultValue,
                            (1e3 * search.getSearchCounter().nodesVisited)
                            / search.getSearchCounter().lastSearchTime,
                            search.getSearchCounter().toString());

    int bestMove2 = search.getLastSearchResult().bestMove;

    System.out.println(result);

    assertEquals(bestMove1, bestMove2);
  }

  @Test
  @Disabled
  public void testManualMateSearch() {
    String fen;
    Position position;
    SearchMode searchMode;

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

  @Test
  @Disabled
  public void test() {
    ArrayList t = new ArrayList();
    t.listIterator().hasNext();
  }
}
