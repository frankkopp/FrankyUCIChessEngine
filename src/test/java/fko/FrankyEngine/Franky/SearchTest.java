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
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.text.WordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
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
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 0, null, false, false, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);
    assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE);
  }

  @Test
  public void testIterativeSearch() {
    String fen = Position.STANDARD_BOARD_FEN;
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 5, 0, null, false, true, false);
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
//    fen = "8/8/8/8/8/3K4/8/R2k4 b - -"; // Position
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
//    search.startSearch(position, searchMode);
//    search.waitWhileSearching();
//    search.stopSearch();
//    assertEquals(-Evaluation.CHECKMATE, search.getLastSearchResult().resultValue);
//
//    System.out.println();
//
//    // mate 1
//    fen = "8/8/8/8/8/3K4/R7/3k4 w - -"; // Position
//    position = new Position(fen);
//    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 3, null, false, false, false);
//    search.startSearch(position, searchMode);
//    search.waitWhileSearching();
//    search.stopSearch();
//    assertEquals(Evaluation.CHECKMATE - 1, search.getLastSearchResult().resultValue);
//
//    System.out.println();

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
    fen = "8/2P5/1P2r3/8/1bBP2Rp/4k2P/5r2/3K4 w - -";
    Position position = new Position(fen);

    // Test start and stop search
    for (int i = 0; i < 10; i++) {
      SearchMode searchMode =
        new SearchMode(0, 0, 0, 0, 0, 5000, 0, 0, 0, null, false, false, false);

      search.startSearch(position, searchMode);
      Thread.sleep(new Random().nextInt(1000) + 100);
      search.stopSearch();

      assertTrue(search.getSearchCounter().leafPositionsEvaluated > 0);

      if (search.getLastSearchResult().bestMove == Move.NOMOVE) {
        System.out.println(search.getLastSearchResult());
        System.out.println();
      }
      assertTrue(search.getLastSearchResult().bestMove != Move.NOMOVE
                 || search.getLastSearchResult().resultValue == Evaluation.MIN);
      assertTrue(search.getLastSearchResult().resultValue != Evaluation.MIN);
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

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
    assertEquals("d8h4", Move.toSimpleString(search.getLastSearchResult().bestMove));
    assertEquals((int) (position.getGamePhaseFactor() * EvaluationConfig.CONTEMPT_FACTOR),
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

      SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 0, null, false, true, false);
      search.startSearch(position, searchMode);
      search.waitWhileSearching();
      assertEquals("e7e3", Move.toSimpleString(search.getLastSearchResult().bestMove));
      //      assertEquals(-Evaluation.getGamePhaseValue(position) * EvaluationConfig.CONTEMPT_FACTOR,
      //                   search.getLastSearchResult().resultValue);
      LOG.warn("Best Move: {} Value: {} Ponder {}",
               Move.toSimpleString(search.getLastSearchResult().bestMove),
               search.getLastSearchResult().resultValue / 100f,
               Move.toSimpleString(search.getLastSearchResult().ponderMove));
    }

    {
      // black can force repetition 1. ... Rd6 2.Qxd6 Qe3+ 3.Kg2 Qe2+ 4.Kh3 Qh5+ 5.Kg2
      String fen = "6k1/p3q2p/1nr3pB/8/3Q1P2/6P1/PP5P/3R2K1 b - -";
      Position position = new Position(fen);

      SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 8, 0, null, false, true, false);
      search.startSearch(position, searchMode);
      search.waitWhileSearching();
      assertEquals("c6d6", Move.toSimpleString(search.getLastSearchResult().bestMove));
      //      assertEquals(-Evaluation.getGamePhaseValue(position) * EvaluationConfig.CONTEMPT_FACTOR,
      //                   search.getLastSearchResult().resultValue);
      LOG.warn("Best Move: {} Value: {} Ponder {}",
               Move.toSimpleString(search.getLastSearchResult().bestMove),
               search.getLastSearchResult().resultValue / 100f,
               Move.toSimpleString(search.getLastSearchResult().ponderMove));

    }

  }

  @Test
  void TT_Root_Test() {

    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_TT_ROOT = true;

    String fen = Position.STANDARD_BOARD_FEN;
    fen = "8/6R1/1rp1k3/6p1/3KPp1p/5P1P/8/8 b - -";
    Position position = new Position(fen);
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 6, 0, null, false, true, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    //    position.makeMove(Move.fromSANNotation(position, "e4"));
    //    position.makeMove(Move.fromSANNotation(position, "e5"));

    searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 7, 0, null, false, true, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    // What can be asserted here?
  }

  @Test
  void aspiration_search() {
    String fen;
    SearchMode searchMode;
    Position position;

    int maxDepth = 4;
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

    LOG.info("PERFT        : Test for depth {} successful.", depth);
    LOG.info("BOARDS       : {}",
             String.format("%,d", search.getSearchCounter().leafPositionsEvaluated));
    LOG.info("NODES VISITED: {}", String.format("%,d", search.getSearchCounter().nodesVisited));
    LOG.info("TIME         : {}", String.format("%s", DurationFormatUtils.formatDurationHMS(
      search.getSearchCounter().lastSearchTime)));
    LOG.info("PERFT NPS    : {} ", String.format("%,.0f",
                                             (1e3 * search.getSearchCounter().nodesVisited) / search
                                               .getSearchCounter().lastSearchTime));
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
  public void testInfiniteSearch() throws InterruptedException {
    Position position = new Position();
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();
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
    search.config.USE_PVS_ORDERING = true;
    search.config.USE_KILLER_MOVES = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MDP = true;
    search.config.USE_MPP = true;

    // these can change result
    // some of these make the search miss this mate at higher depths
    // might be a consequence from these "optimizations" might be a bug
    search.config.USE_NMP = true;
    search.config.NMP_DEPTH = 2;
    search.config.USE_VERIFY_NMP = true;
    search.config.NMP_VERIFICATION_DEPTH = 3;

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
    String fen = Position.STANDARD_BOARD_FEN;
    SearchMode searchMode;
    Position position;

    search.config.USE_BOOK = false;
    search.config.USE_ALPHABETA_PRUNING = false;
    search.config.USE_TRANSPOSITION_TABLE = false;
    search.config.USE_TT_ROOT = false;
    search.config.USE_PVS = false;
    search.config.USE_PVS_ORDERING = false;
    search.config.USE_KILLER_MOVES = false;
    search.config.USE_ASPIRATION_WINDOW = false;
    search.config.USE_MTDf = false;
    search.config.USE_MDP = false;
    search.config.USE_MPP = false;
    search.config.USE_RFP = false;
    search.config.USE_NMP = false;
    search.config.USE_RAZOR_PRUNING = false;
    search.config.USE_IID = false;
    search.config.USE_EXTENSIONS = false;
    search.config.USE_LIMITED_RAZORING = false;
    search.config.USE_EXTENDED_FUTILITY_PRUNING = false;
    search.config.USE_FUTILITY_PRUNING = false;
    search.config.USE_QFUTILITY_PRUNING = false;
    search.config.USE_LMP = false;
    search.config.USE_LMR = false;
    search.config.USE_QUIESCENCE = false;

    int whiteTime = 0;
    int blackTime = 0;
    int whiteInc = 0;
    int blackInc = 0;
    int movesToGo = 0;
    int moveTime = 0;
    int depth = 1;
    int mateIn = 0;
    boolean infinite = false;

    String result = "";

    fen = "r1bq1rk1/pp1p1ppp/2n2b2/2p1p3/4P3/2NP1NP1/PPP2PBP/R2QK2R w KQ -";
    // fen = "1r2kb1r/2Rn4/p4p2/4pN1p/4N1p1/6B1/P4PPP/3R2K1 w k -";
    //fen = "r2qkb1r/p1p1pppp/2pp1n2/5b2/P2P4/2N5/1PP1PPPP/R1BQKB1R b KQkq d3";
    // fen = "1b1qrr2/1p4pk/1np4p/p3Np1B/Pn1P4/R1N3B1/1Pb2PPP/2Q1R1K1 b - -";
    // fen = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1";
    // fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    // fen = "r1bq1rk1/pp2bppp/2n2n2/3p4/3P4/2N2N2/PPQ1BPPP/R1B2RK1 b - -";
    // fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    // fen = "r2q1rk1/1p1nbppp/3p1n2/1Pp2b2/p1P5/2N1Pp1P/PBNPB1P1/R2Q1RK1 w - -";
    // fen = "4k3/4p3/8/8/8/8/8/3KQ3 w - -";
    // fen = "7k/8/8/8/6p1/3N3N/P4p2/1K6 w - -";

    position = new Position(fen);
    searchMode =
      new SearchMode(whiteTime, blackTime, whiteInc, blackInc, movesToGo, moveTime, 0, depth,
                     mateIn, null, false, infinite, false);

    search.config.USE_ALPHABETA_PRUNING = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_TT_ROOT = true;
    search.config.USE_PVS = true;
    search.config.USE_PVS_ORDERING = true;
    search.config.USE_KILLER_MOVES = true;
    search.config.USE_ASPIRATION_WINDOW = true;
    search.config.USE_MDP = true;
    search.config.USE_MPP = true;
    search.config.USE_RFP = true;
    search.config.USE_NMP = true;
    search.config.USE_IID = true;
    search.config.USE_EXTENSIONS = true;
    search.config.USE_LIMITED_RAZORING = true;
    search.config.USE_EXTENDED_FUTILITY_PRUNING = true;
    search.config.USE_FUTILITY_PRUNING = true;
    search.config.USE_QFUTILITY_PRUNING = true;
    search.config.USE_LMP = false;
    search.config.USE_LMR = true;
    search.config.USE_QUIESCENCE = true;

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    System.out.println();

    result += String.format("%nSIZE: %,14d >> %-18s (%4d) >> nps %,.0f >> %s %n",
                            search.getSearchCounter().leafPositionsEvaluated,
                            Move.toString(search.getLastSearchResult().bestMove),
                            search.getLastSearchResult().resultValue,
                            (1e3 * search.getSearchCounter().nodesVisited)
                            / search.getSearchCounter().lastSearchTime,
                            search.getSearchCounter().toString());

    int bestMove1 = search.getLastSearchResult().bestMove;

    System.out.println(WordUtils.wrap(result, 120));

    //    // MTDf - just for debugging for now
    //    search.config.USE_PVS = false;
    //    search.config.USE_PVS_ORDERING = false;
    //    search.config.USE_MTDf = true;
    //    search.config.MTDf_START_DEPTH = 2;
    //
    //    search.clearHashTables();
    //    search.startSearch(position, searchMode);
    //    search.waitWhileSearching();
    //
    //    result += String.format("SIZE: %,14d >> %-18s (%4d) >> nps %,.0f >> %s %n",
    //                            search.getSearchCounter().leafPositionsEvaluated,
    //                            Move.toString(search.getLastSearchResult().bestMove),
    //                            search.getLastSearchResult().resultValue,
    //                            (1e3 * search.getSearchCounter().nodesVisited)
    //                            / search.getSearchCounter().lastSearchTime,
    //                            search.getSearchCounter().toString());
    //
    //    int bestMove2 = search.getLastSearchResult().bestMove;
    //
    //    System.out.println(result);
    //
    //    assertEquals(bestMove1, bestMove2);
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
    search.config.USE_PVS_ORDERING = true;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MDP = true;
    search.config.USE_MPP = true;
    search.config.USE_QUIESCENCE = true;
    search.config.USE_NMP = true;
    search.config.USE_RFP = true;
    search.config.USE_RAZOR_PRUNING = true;
    search.config.USE_KILLER_MOVES = true;
    search.config.USE_LMR = true;

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
