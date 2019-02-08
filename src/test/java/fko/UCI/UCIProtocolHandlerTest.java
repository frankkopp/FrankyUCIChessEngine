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

package fko.UCI;

import fko.FrankyEngine.Franky.FrankyEngine;
import fko.FrankyEngine.Franky.Move;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UCIProtocolHandlerTest {

  private static final Logger LOG = LoggerFactory.getLogger(UCIProtocolHandlerTest.class);

  private UCIProtocolHandler handler;

  private PrintStream    toHandlerPrinter;
  private BufferedReader fromHandlerReader;

  private FrankyEngine engine;
  private Semaphore    semaphore;

  @BeforeEach
  void setUp() throws IOException, InterruptedException {

    final PipedOutputStream toHandler = new PipedOutputStream();
    final InputStream handlerInput = new PipedInputStream(toHandler);
    toHandlerPrinter = new PrintStream(toHandler);

    final PipedInputStream fromHandler = new PipedInputStream();
    final PipedOutputStream handlerOutput = new PipedOutputStream(fromHandler);
    fromHandlerReader = new BufferedReader(new InputStreamReader(fromHandler), 1000000);

    engine = new FrankyEngine();
    handler = new UCIProtocolHandler(engine, handlerInput, handlerOutput);
    engine.registerProtocolHandler(handler);

    semaphore = new Semaphore(0, true);
    handler.setSemaphoreForSynchronization(semaphore);

    handler.startHandler();

    Thread.sleep(1000);

    clearBuffer();

  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void uciCommand() throws IOException, InterruptedException {

    commandToEngine("uci");

    // check id
    String line = getResponseFromEngine();
    assertTrue(line.startsWith("id name "));

    // check author
    line = getResponseFromEngine();
    assertTrue(line.startsWith("id author "));

    while ((line = getResponseFromEngine()).startsWith("option ")) {
    }

    // check uciok
    assertTrue(line.startsWith("uciok"));

    commandToEngine("quit");

  }

  @Test
  void setoptionCommand() throws InterruptedException {

    commandToEngine("setoption name Hash value 4096");
    assertEquals(4096, engine.getHashSizeOption());

    assertTrue(engine.getPonderOption());
    commandToEngine("setoption name Ponder value false");
    assertFalse(engine.getPonderOption());

    commandToEngine("setoption name Clear_Hash");

    commandToEngine("quit");
  }

  @Test
  void newGameCommand() throws InterruptedException {
    commandToEngine("ucinewgame");
    commandToEngine("quit");
  }

  @Test
  void debugCommand() throws InterruptedException {
    assertFalse(engine.getDebugMode());
    commandToEngine("debug on");
    assertTrue(engine.getDebugMode());
    commandToEngine("quit");
  }

  @Test
  void positionCommand() throws InterruptedException {

    // promotion
    commandToEngine("position fen 8/3P4/6K1/8/8/1k6/8/8 w - - 0 0 moves d7d8q");
    assertEquals("3Q4/8/6K1/8/8/1k6/8/8 b - - 0 1", engine.getPosition().toFENString());

    // castling
    commandToEngine(
      "position fen r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 0 moves e1g1");
    assertEquals("r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 1 1",
                 engine.getPosition().toFENString());

    // normal
    commandToEngine("position startpos moves e2e4 e7e5");
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getPosition().toFENString());

    commandToEngine("position moves e2e4 e7e5");
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getPosition().toFENString());

    commandToEngine("position fen rnbqkbnr/8/8/8/8/8/8/RNBQKBNR w KQkq - 0 1 moves e1e2 e8e7");
    assertEquals("rnbq1bnr/4k3/8/8/8/8/4K3/RNBQ1BNR w - - 2 2", engine.getPosition().toFENString());

    commandToEngine("quit");
  }

  @Test
  void goCommand() throws IOException, InterruptedException {

    engine.getConfig().USE_BOOK = false;

    commandToEngine("go infinite");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go ponder movetime 10000");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isPonder());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go infinite searchmoves e2e4 d2d4");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go wtime 300000 btime 300000 movestogo 20");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(20, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go wtime 300000 btime 300000 movestogo 5");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(5, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go wtime 500000 btime 500000");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(500, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(0, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go wtime 300000 btime 300000 winc 2000 binc 2000");
    waitUntilSearching();
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(2, engine.getSearchMode().getWhiteInc().getSeconds());
    assertEquals(2, engine.getSearchMode().getBlackInc().getSeconds());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go movetime 10000");
    waitUntilSearching();
    assertEquals(10, engine.getSearchMode().getMoveTime().getSeconds());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go mate 5");
    waitUntilSearching();
    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(5, engine.getSearchMode().getMate());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go nodes 500000");
    waitUntilSearching();
    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(500000, engine.getSearchMode().getNodes());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("go depth 10");
    waitUntilSearching();
    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(10, engine.getSearchMode().getDepth());
    assertTrue(engine.isSearching());
    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    // clear buffer
    clearBuffer();

    commandToEngine("quit");
  }

  @Test
  void test3FoldRepetitionTest1() throws InterruptedException, IOException {
    // position startpos moves b2b4 c7c6 e2e3 g8f6 c1b2 a7a5 b4b5 a5a4 g1f3 d7d6 f1e2 e7e5 c2c4 f8e7 e1g1 e8g8 d2d4 d8c7 d4e5 d6e5 b2e5 c7a5 e5c3 e7b4 c3b4 a5b4 a2a3 b4b2 b1d2 c6b5 c4b5 c8e6 d1c1 b2c1 a1c1 b8d7 f3d4 f8c8 f2f4 d7c5 e2f3 e6a2 d4f5 c8d8 c1c5 d8d2 f3b7 a8b8 f1c1 a2e6 f5d4 d2d3 d4e6 f7e6 c5c8 b8c8 c1c8 g8f7 c8c7 f7g6 g1f2 d3a3 f2f3 a3a1 c7e7 a4a3 b7e4 g6h6 g2g4 a3a2 g4g5 h6h5 e7a7 a1f1 f3g2 a2a1r a7a1 f1a1 g5f6 g7f6 e4h7 a1a2 g2f3 a2h2 h7g8 e6e5 g8f7 h5h6 f3e4 e5f4 e3f4 h2b2 e4f5 h6g7 f7c4 b2b4 c4e2 g7f7 e2f1 f7e7 f1d3 e7d6 d3e4 b4b5 f5f6 b5b8 f6g5 d6e6 g5g4 b8b3 e4h7 e6f7 h7f5 f7f6 f5h7 f6f7 h7f5 f7f6 f5h7 b3c3 h7g8 c3c7 g8d5 c7c3 d5a2 c3d3 a2g8 d3e3 g8d5 e3d3 d5c4 d3e3 c4d5 e3d3 d5c4 d3a3 c4d5 a3c3 d5a2 c3d3 a2g8 d3c3
    // go infinite

    commandToEngine("setoption name OwnBook value false");

    // Ba2 and Bd5 are r3-fold repetition
    commandToEngine("position startpos moves b2b4 c7c6 e2e3 g8f6 c1b2 a7a5 b4b5 a5a4 g1f3 d7d6 "
                    + "f1e2 e7e5 c2c4 f8e7 e1g1 e8g8 d2d4 d8c7 d4e5 d6e5 b2e5 c7a5 e5c3 e7b4 c3b4 "
                    + "a5b4 a2a3 b4b2 b1d2 c6b5 c4b5 c8e6 d1c1 b2c1 a1c1 b8d7 f3d4 f8c8 f2f4 d7c5 "
                    + "e2f3 e6a2 d4f5 c8d8 c1c5 d8d2 f3b7 a8b8 f1c1 a2e6 f5d4 d2d3 d4e6 f7e6 c5c8 "
                    + "b8c8 c1c8 g8f7 c8c7 f7g6 g1f2 d3a3 f2f3 a3a1 c7e7 a4a3 b7e4 g6h6 g2g4 a3a2 "
                    + "g4g5 h6h5 e7a7 a1f1 f3g2 a2a1r a7a1 f1a1 g5f6 g7f6 e4h7 a1a2 g2f3 a2h2 h7g8 "
                    + "e6e5 g8f7 h5h6 f3e4 e5f4 e3f4 h2b2 e4f5 h6g7 f7c4 b2b4 c4e2 g7f7 e2f1 f7e7 "
                    + "f1d3 e7d6 d3e4 b4b5 f5f6 b5b8 f6g5 d6e6 g5g4 b8b3 e4h7 e6f7 h7f5 f7f6 f5h7 "
                    + "f6f7 h7f5 f7f6 f5h7 b3c3 h7g8 c3c7 g8d5 c7c3 d5a2 c3d3 a2g8 d3e3 g8d5 e3d3 "
                    + "d5c4 d3e3 c4d5 e3d3 d5c4 d3a3 c4d5 a3c3 d5a2 c3d3 a2g8 d3c3 g8d5");

    assertEquals("8/8/5k2/3B4/5PK1/2r5/8/8 b - - 38 72", engine.getPosition().toFENString());

    // white can draw with d8h4 b/c 3-fold-repetition with Ba2 and Bd5
    commandToEngine("go depth 6 searchmoves c3d3");
    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = getResponseFromEngine();
        assertTrue(line.startsWith("info ") || line.startsWith("bestmove "));
        if (line.startsWith("bestmove ")) {
          System.out.println(line);
          assertEquals("c3d3",
                       Move.toSimpleString(engine.getSearch().getLastSearchResult().bestMove));
          assertEquals(0, engine.getSearch().getLastSearchResult().resultValue);
          break;
        }
      }
    }
  }

  @Test
  void test3FoldRepetitionTest2() throws InterruptedException, IOException {


    commandToEngine("position startpos moves b2b3 b7b6 c1b2 c8b7 e2e3 g8f6 "
                    + "f2f4 g7g6 g1f3 f8g7 f1e2 e8g8 e1g1 c7c5 a2a4 b8c6 b1c3 "
                    + "d7d5 d2d4 f6g4 b2c1 c5d4 e3d4 g4f6 f3e5 f6e4 c3e4 d5e4 "
                    + "c1e3 d8d6 d1d2 a8d8 f1d1 f7f6 e5c4 d6c7 d2c3 h7h5 g1f2 "
                    + "e7e5 d4e5 f6e5 d1d8 c7d8 f4f5 f8f5 f2g3 c6d4 c3d2 d4e2 "
                    + "d2e2 h5h4 g3h3 b7c8 g2g4 f5f3 h3g2 c8g4 a1d1 d8f8 e3g5 "
                    + "h4h3 g2g1 f3f4 e2e3 g4d1 g5f4 e5f4 e3e4 f8f5 e4e8 g8h7 "
                    + "e8e7 d1c2 e7h4 h7g8 h4d8 g8h7 d8h4 h7g8 h4d8 g8h7");

    assertEquals("3Q4/p5bk/1p4p1/5q2/P1N2p2/1P5p/2b4P/6K1 w - - 8 42",
                 engine.getPosition().toFENString());

    // white can draw with d8h4 b/c 3-fold-repetition
    commandToEngine("go movetime 1000");
    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = getResponseFromEngine();
        assertTrue(line.startsWith("info ") || line.startsWith("bestmove "));
        if (line.startsWith("bestmove ")) {
          System.out.println(line);
          assertEquals("d8h4",
                       Move.toSimpleString(engine.getSearch().getLastSearchResult().bestMove));
          assertEquals(0, engine.getSearch().getLastSearchResult().resultValue);
          break;
        }
      }
    }

    // black should avoid 3-fold repetition
    commandToEngine("position startpos moves b2b3 b7b6 c1b2 c8b7 e2e3 g8f6 "
                    + "f2f4 g7g6 g1f3 f8g7 f1e2 e8g8 e1g1 c7c5 a2a4 b8c6 b1c3 "
                    + "d7d5 d2d4 f6g4 b2c1 c5d4 e3d4 g4f6 f3e5 f6e4 c3e4 d5e4 "
                    + "c1e3 d8d6 d1d2 a8d8 f1d1 f7f6 e5c4 d6c7 d2c3 h7h5 g1f2 "
                    + "e7e5 d4e5 f6e5 d1d8 c7d8 f4f5 f8f5 f2g3 c6d4 c3d2 d4e2 "
                    + "d2e2 h5h4 g3h3 b7c8 g2g4 f5f3 h3g2 c8g4 a1d1 d8f8 e3g5 "
                    + "h4h3 g2g1 f3f4 e2e3 g4d1 g5f4 e5f4 e3e4 f8f5 e4e8 g8h7 "
                    + "e8e7 d1c2 e7h4 h7g8 h4d8 g8h7 d8h4 h7g8 h4d8");
    assertEquals("3Q2k1/p5b1/1p4p1/5q2/P1N2p2/1P5p/2b4P/6K1 b - - 7 41",
                 engine.getPosition().toFENString());

    // white can draw with d8h4 b/c 3-fold-repetition
    // block should avoid it with Bf8
    commandToEngine("go movetime 1000");
    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = getResponseFromEngine();
        assertTrue(line.startsWith("info ") || line.startsWith("bestmove "));
        if (line.startsWith("bestmove ")) {
          System.out.println(line);
          assertEquals("g7f8",
                       Move.toSimpleString(engine.getSearch().getLastSearchResult().bestMove));
          break;
        }
      }
    }


  }

  @Test
  void perftCommand() throws IOException, InterruptedException {
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
    commandToEngine("go perft 5");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isPerft());
    assertEquals(5, engine.getSearchMode().getDepth());
    assertTrue(engine.isSearching());

    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("quit");
  }

  @Test
  void ponderHitCommand() throws InterruptedException, IOException, InterruptedException {

    commandToEngine("position startpos moves e2e4 e7e5");
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getPosition().toFENString());

    commandToEngine("go ponder wtime 3000 btime 3000");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isPonder());
    assertTrue(engine.isSearching());

    Thread.sleep(100);

    commandToEngine("ponderhit");
    waitWhileSearching();

    commandToEngine("quit");
  }

  @Test
  void sendInfoToUCI() throws IOException, InterruptedException {

    engine.getConfig().USE_BOOK = false;

    commandToEngine("go movetime 5000");
    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = getResponseFromEngine();
        LOG.debug(" Received: " + line);
        assertTrue(line.startsWith("info ") || line.startsWith("bestmove "));
      }
    }

    commandToEngine("stop");
    assertFalse(engine.isSearching());

    commandToEngine("quit");
  }

  @Test
  void testSetTest() throws IOException, InterruptedException {

    commandToEngine("ucinewgame");
    commandToEngine("position fen 3rkb1r/1p3p2/p1n1p3/q5pp/2PpP3/1P4P1/P1Q1BPKP/R2N3R b k - 0 0");
    commandToEngine("go infinite");
    waitUntilSearching();
    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());

    Thread.sleep(2000);

    commandToEngine("stop");
    waitWhileSearching();
    assertFalse(engine.isSearching());

    commandToEngine("quit");
  }

  @Test
  @Disabled
  void debuggingTest() throws InterruptedException, IOException {
    commandToEngine("ucinewgame");
    clearBuffer();


    /*
    Trying to find a 3-fold repetition bug
     */

    commandToEngine(
      "position startpos moves a2a3 g8f6 g1f3 d7d5 d2d4 c7c6 c1f4 c8g4 f3e5 g4h5 f2f3 f6d7 b1c3 e7e6 e2e4 d7e5 f4e5 b8d7 e5f4 d5e4 c3e4 f7f5 e4d6 f8d6 f4d6 d7f6 d6e5 d8b6 b2b4 a7a5 b4a5 a8a5 f1c4 a5e5 d4e5 f6e4 d1e2 b6a5 e1f1 e4d2 f1f2 d2e4 f2f1 e4d2 f1f2 a5c5 e2e3 c5e3 f2e3 d2c4 e3d4 b7b5 d4c5 e8e7 c5c6 c4e5 c6b5 e7f6 b5b4 h8c8 b4b3 f5f4 a1c1 c8b8 b3c3 b8a8 c1a1 a8c8 c3b3 c8b8 b3c3 b8c8 c3b3 c8b8 b3c3 b8c8");
    commandToEngine("go infinite");
    waitUntilSearching();
    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = getResponseFromEngine();
        if (line.startsWith("info depth 15 ")) {
          //          commandToEngine("stop");
          //          break;
        }
      }
    }
    clearBuffer();

    //    commandToEngine(
    //      "position startpos moves d2d3 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3 c8b7 f1g2 f8b4 b1c3 e8g8 e1g1 b4c3 b2c3 d7d5 a1b1 d8e7 f3e5 b8c6 e5c6 b7c6 c1f4 e6e5 f4e3 e7e6 d1b3 d5c4 b3c4 e6c4 d3c4 c6g2 g1g2 f6e4 g2f3 f7f5 b1b3 e4d6 c4c5 d6e4 c5b6 a7b6 a2a3 e4d6 f1a1 d6c4 h2h4 g8f7 a3a4 c7c5 b3b1 a8a7 h4h5 f8a8 h5h6 g7g6 b1h1 a7a4 a1a4 a8a4 e3g5 f7e6 e2e4 a4a2 g5e3 a2c2 h1a1 f5e4 f3e4 c2c3 a1a8 c4d6 e4f3 c3b3 a8a7 d6f7 f3e2 e5e4 e2d2 b3d3 d2e2 d3b3 e2d2 c5c4 a7a6 e6d5 a6a7 d5e6 a7a6");
    //    commandToEngine("go infinite");
    //    waitUntilSearching();
    //    while (engine.isSearching()) {
    //      while (fromHandlerReader.ready()) {
    //        final String line = getResponseFromEngine();
    //        if (line.startsWith("info depth 15 ")) {
    //          commandToEngine("stop");
    //          break;
    //        }
    //      }
    //    }
    //    clearBuffer();
    //
    //    commandToEngine(
    //      "position startpos moves d2d3 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3 c8b7 f1g2 f8b4 b1c3 e8g8 e1g1 b4c3 b2c3 d7d5 a1b1 d8e7 f3e5 b8c6 e5c6 b7c6 c1f4 e6e5 f4e3 e7e6 d1b3 d5c4 b3c4 e6c4 d3c4 c6g2 g1g2 f6e4 g2f3 f7f5 b1b3 e4d6 c4c5 d6e4 c5b6 a7b6 a2a3 e4d6 f1a1 d6c4 h2h4 g8f7 a3a4 c7c5 b3b1 a8a7 h4h5 f8a8 h5h6 g7g6 b1h1 a7a4 a1a4 a8a4 e3g5 f7e6 e2e4 a4a2 g5e3 a2c2 h1a1 f5e4 f3e4 c2c3 a1a8 c4d6 e4f3 c3b3 a8a7 d6f7 f3e2 e5e4 e2d2 b3d3 d2e2 d3b3 e2d2 c5c4 a7a6 e6d5 a6a7 d5e6 a7a6 e6d5");
    //    commandToEngine("go infinite");
    //    waitUntilSearching();
    //    while (engine.isSearching()) {
    //      while (fromHandlerReader.ready()) {
    //        final String line = getResponseFromEngine();
    //        if (line.startsWith("info depth 15 ")) {
    //          commandToEngine("stop");
    //          break;
    //        }
    //      }
    //    }
    //    clearBuffer();
    //
    //    commandToEngine(
    //      "position startpos moves d2d3 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3 c8b7 f1g2 f8b4 b1c3 e8g8 e1g1 b4c3 b2c3 d7d5 a1b1 d8e7 f3e5 b8c6 e5c6 b7c6 c1f4 e6e5 f4e3 e7e6 d1b3 d5c4 b3c4 e6c4 d3c4 c6g2 g1g2 f6e4 g2f3 f7f5 b1b3 e4d6 c4c5 d6e4 c5b6 a7b6 a2a3 e4d6 f1a1 d6c4 h2h4 g8f7 a3a4 c7c5 b3b1 a8a7 h4h5 f8a8 h5h6 g7g6 b1h1 a7a4 a1a4 a8a4 e3g5 f7e6 e2e4 a4a2 g5e3 a2c2 h1a1 f5e4 f3e4 c2c3 a1a8 c4d6 e4f3 c3b3 a8a7 d6f7 f3e2 e5e4 e2d2 b3d3 d2e2 d3b3 e2d2 c5c4 a7a6 e6d5 a6a7 d5e6 a7a6 e6d5 a6a7");
    //    commandToEngine("go infinite");
    //    waitUntilSearching();
    //    while (engine.isSearching()) {
    //      while (fromHandlerReader.ready()) {
    //        final String line = getResponseFromEngine();
    //        if (line.startsWith("info depth 15 ")) {
    //          commandToEngine("stop");
    //          break;
    //        }
    //      }
    //    }
  }

  private void commandToEngine(String s) throws InterruptedException {
    System.out.println("COMMAND  >> " + s);
    toHandlerPrinter.println(s);
    semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS);
  }

  private void waitWhileSearching() throws InterruptedException, IOException {
    while (engine.isSearching()) {
      Thread.sleep(10);
      clearBuffer();
    }
  }

  private void waitUntilSearching() throws InterruptedException, IOException {
    while (!engine.isSearching()) {
      Thread.sleep(10);
      clearBuffer();
    }
  }

  private void clearBuffer() throws IOException {
    while (fromHandlerReader.ready()) {
      getResponseFromEngine();
    }
  }

  private String getResponseFromEngine() throws IOException {
    final String readLine = fromHandlerReader.readLine();
    System.out.println("RESPONSE << " + readLine);
    return readLine;
  }
}
