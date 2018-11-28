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

package fko.javaUCIEngineFramework.UCI;

import fko.javaUCIEngineFramework.MyEngine;
import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class UCIProtocolHandlerTest {

  private static final Logger LOG = LoggerFactory.getLogger(UCIProtocolHandlerTest.class);

  private UCIProtocolHandler handler;

  private PrintStream    toHandlerPrinter;
  private BufferedReader fromHandlerReader;

  private IUCIEngine engine;
  private Waiter     waiter;

  @BeforeEach
  void setUp() throws IOException {

    System.out.println("SETUP");

    waiter = new Waiter();

    final PipedOutputStream toHandler = new PipedOutputStream();
    final InputStream handlerInput = new PipedInputStream(toHandler);
    toHandlerPrinter = new PrintStream(toHandler);

    final PipedInputStream fromHandler = new PipedInputStream();
    final PipedOutputStream handlerOutput = new PipedOutputStream(fromHandler);
    fromHandlerReader = new BufferedReader(new InputStreamReader(fromHandler), 1000000);

    engine = new MyEngine();
    handler = new UCIProtocolHandler(engine, handlerInput, handlerOutput);
    engine.registerProtocolHandler(handler);
    handler.setWaiterForProcessing(waiter);
    handler.startHandler();
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    //    toHandlerPrinter.println("quit");
    //    while (handler.isRunning()) {
    //      Thread.sleep(100);
    //    }
    System.out.println("TEAR_DOWN");
  }

  @Test
  void uciCommand() throws IOException, TimeoutException {

    toHandlerPrinter.println("uci");
    waiter.await(2000);

    // check id
    String line = fromHandlerReader.readLine();
    assertTrue(line.startsWith("id name "));

    // check author
    line = fromHandlerReader.readLine();
    assertTrue(line.startsWith("id author "));

    while ((line = fromHandlerReader.readLine()).startsWith("option ")) {
    }

    // check uciok
    assertTrue(line.startsWith("uciok"));

    toHandlerPrinter.println("quit");
  }

  @Test
  void setoptionCommand() throws TimeoutException {

    assertEquals(16, engine.getHashSizeOption());
    toHandlerPrinter.println("setoption name Hash value 4096");
    waiter.await(2000);
    assertEquals(4096, engine.getHashSizeOption());

    assertTrue(engine.getPonderOption());
    toHandlerPrinter.println("setoption name Ponder value false");
    waiter.await(2000);
    assertFalse(engine.getPonderOption());

    toHandlerPrinter.println("quit");
  }

  @Test
  void newGameCommand() throws TimeoutException {
    toHandlerPrinter.println("ucinewgame");
    waiter.await(2000);
    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }

  @Test
  void debugCommand() throws InterruptedException, TimeoutException {
    assertFalse(engine.getDebugOption());
    toHandlerPrinter.println("debug on");
    waiter.await(2000);
    assertTrue(engine.getDebugOption());
    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }

  @Test
  void positionCommand() throws InterruptedException, TimeoutException {

    // promotion
    toHandlerPrinter.println("position fen 8/3P4/6K1/8/8/1k6/8/8 w - - 0 0 moves d7d8q");
    waiter.await(2000);
    assertEquals("3Q4/8/6K1/8/8/1k6/8/8 b - - 0 1", engine.getBoardPosition().toFENString());

    // castling
    toHandlerPrinter.println(
      "position fen r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 0 moves e1g1");
    waiter.await(2000);
    assertEquals("r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 1 1",
                 engine.getBoardPosition().toFENString());

    // normal
    toHandlerPrinter.println("position startpos moves e2e4 e7e5");
    waiter.await(2000);
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getBoardPosition().toFENString());

    toHandlerPrinter.println("position moves e2e4 e7e5");
    waiter.await(2000);
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getBoardPosition().toFENString());

    toHandlerPrinter.println(
      "position fen rnbqkbnr/8/8/8/8/8/8/RNBQKBNR w KQkq - 0 1 moves e1e2 e8e7");
    waiter.await(2000);
    assertEquals("rnbq1bnr/4k3/8/8/8/8/4K3/RNBQ1BNR w - - 2 2",
                 engine.getBoardPosition().toFENString());

    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }

  @Test
  void goCommand() throws IOException, TimeoutException {

    toHandlerPrinter.println("go infinite");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go ponder");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isPonder());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go infinite searchmoves e2e4 d2d4");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 300000 btime 300000 movestogo 20");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(20, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 300000 btime 300000 movestogo 5");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(5, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 500000 btime 500000");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(500, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(0, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 300000 btime 300000 winc 2000 binc 2000");
    waiter.await(2000);
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(2, engine.getSearchMode().getWhiteInc().getSeconds());
    assertEquals(2, engine.getSearchMode().getBlackInc().getSeconds());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go movetime 10000");
    waiter.await(2000);
    assertEquals(10, engine.getSearchMode().getMoveTime().getSeconds());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go mate 5");
    waiter.await(2000);
    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(5, engine.getSearchMode().getMate());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go nodes 1000000000");
    waiter.await(2000);
    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(1000000000, engine.getSearchMode().getNodes());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go depth 10");
    waiter.await(2000);
    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(10, engine.getSearchMode().getDepth());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }

  @Test
  void perftCommand() throws IOException, TimeoutException {
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
    toHandlerPrinter.println("go perft 5");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isPerft());
    assertEquals(5, engine.getSearchMode().getDepth());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }

  @Test
  void ponderHitCommand() throws TimeoutException, IOException, InterruptedException {

    // normal
    toHandlerPrinter.println("position startpos moves e2e4 e7e5");
    waiter.await(2000);
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getBoardPosition().toFENString());

    toHandlerPrinter.println("go ponder wtime 300000 btime 300000");
    waiter.await(2000);
    assertTrue(engine.getSearchMode().isPonder());
    assertTrue(engine.isSearching());

    Thread.sleep(500);

    toHandlerPrinter.println("ponderhit");
    waiter.await(2000);

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    while (engine.isSearching()) {
      Thread.sleep(50);
      // clear buffer
      while (fromHandlerReader.ready()) {
        fromHandlerReader.readLine();
      }
    }

    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }

  @Test
  void sendInfoToUCI() throws IOException, TimeoutException {

    toHandlerPrinter.println("go movetime 15000");
    waiter.await(2000);

    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = fromHandlerReader.readLine();
        LOG.debug(" Received: " + line);
        assertTrue(line.startsWith("info depth ") || line.startsWith("bestmove "));
      }
    }

    toHandlerPrinter.println("stop");
    waiter.await(2000);
    assertFalse(engine.isSearching());

    toHandlerPrinter.println("quit");
    waiter.await(2000);
  }
}
