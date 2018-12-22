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

import fko.FrankyEngine.Franky.FrankyEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
  void setUp() throws IOException {

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

  }

  @AfterEach
  void tearDown()  {
  }

  @Test
  void uciCommand() throws IOException, InterruptedException {

    toHandlerPrinter.println("uci");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

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
  void setoptionCommand() throws InterruptedException {

    toHandlerPrinter.println("setoption name Hash value 4096");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
    assertEquals(4096, engine.getHashSizeOption());

    assertTrue(engine.getPonderOption());
    toHandlerPrinter.println("setoption name Ponder value false");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
    assertFalse(engine.getPonderOption());

    toHandlerPrinter.println("quit");
  }

  @Test
  void newGameCommand() throws InterruptedException {
    toHandlerPrinter.println("ucinewgame");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    toHandlerPrinter.println("quit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
  }

  @Test
  void debugCommand() throws InterruptedException {
    assertFalse(engine.getDebugMode());
    toHandlerPrinter.println("debug on");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getDebugMode());
    toHandlerPrinter.println("quit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
  }

  @Test
  void positionCommand() throws  InterruptedException {

    // promotion
    toHandlerPrinter.println("position fen 8/3P4/6K1/8/8/1k6/8/8 w - - 0 0 moves d7d8q");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals("3Q4/8/6K1/8/8/1k6/8/8 b - - 0 1", engine.getPosition().toFENString());

    // castling
    toHandlerPrinter.println(
      "position fen r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 0 moves e1g1");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals("r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 1 1",
                 engine.getPosition().toFENString());

    // normal
    toHandlerPrinter.println("position startpos moves e2e4 e7e5");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getPosition().toFENString());

    toHandlerPrinter.println("position moves e2e4 e7e5");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getPosition().toFENString());

    toHandlerPrinter.println(
      "position fen rnbqkbnr/8/8/8/8/8/8/RNBQKBNR w KQkq - 0 1 moves e1e2 e8e7");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals("rnbq1bnr/4k3/8/8/8/8/4K3/RNBQ1BNR w - - 2 2",
                 engine.getPosition().toFENString());

    toHandlerPrinter.println("quit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
  }

  @Test
  void goCommand() throws IOException, InterruptedException {

    toHandlerPrinter.println("go infinite");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go ponder");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isPonder());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go infinite searchmoves e2e4 d2d4");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 300000 btime 300000 movestogo 20");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(20, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 300000 btime 300000 movestogo 5");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(5, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 500000 btime 500000");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isTimeControl());
    assertEquals(500, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(0, engine.getSearchMode().getMovesToGo());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go wtime 300000 btime 300000 winc 2000 binc 2000");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals(300, engine.getSearchMode().getWhiteTime().getSeconds());
    assertEquals(2, engine.getSearchMode().getWhiteInc().getSeconds());
    assertEquals(2, engine.getSearchMode().getBlackInc().getSeconds());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go movetime 10000");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals(10, engine.getSearchMode().getMoveTime().getSeconds());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go mate 5");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(5, engine.getSearchMode().getMate());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go nodes 1000000000");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(1000000000, engine.getSearchMode().getNodes());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("go depth 10");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.getSearchMode().isTimeControl());
    assertEquals(10, engine.getSearchMode().getDepth());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("quit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
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
    toHandlerPrinter.println("go perft 5");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isPerft());
    assertEquals(5, engine.getSearchMode().getDepth());
    assertTrue(engine.isSearching());
    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }

    toHandlerPrinter.println("quit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
  }

  @Test
  void ponderHitCommand() throws InterruptedException, IOException, InterruptedException {

    // normal
    toHandlerPrinter.println("position startpos moves e2e4 e7e5");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                 engine.getPosition().toFENString());

    toHandlerPrinter.println("go ponder wtime 300000 btime 300000");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertTrue(engine.getSearchMode().isPonder());
    assertTrue(engine.isSearching());

    Thread.sleep(500);

    toHandlerPrinter.println("ponderhit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);


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
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
  }

  @Test
  void sendInfoToUCI() throws IOException, InterruptedException {

    toHandlerPrinter.println("go movetime 15000");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);


    while (engine.isSearching()) {
      while (fromHandlerReader.ready()) {
        final String line = fromHandlerReader.readLine();
        LOG.debug(" Received: " + line);
        assertTrue(line.startsWith("info ") || line.startsWith("bestmove "));
      }
    }

    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    assertFalse(engine.isSearching());

    toHandlerPrinter.println("quit");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);
  }

  @Test
  void testSetTest() throws IOException, InterruptedException {
    //    ucinewgame
    //    position fen 3rkb1r/1p3p2/p1n1p3/q5pp/2PpP3/1P4P1/P1Q1BPKP/R2N3R b k - 0 0
    //    go infinite

    toHandlerPrinter.println("ucinewgame");
    toHandlerPrinter.println(
      "position fen 3rkb1r/1p3p2/p1n1p3/q5pp/2PpP3/1P4P1/P1Q1BPKP/R2N3R b k - 0 0");
    toHandlerPrinter.println("go infinite");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    Thread.sleep(2000);

    assertTrue(engine.getSearchMode().isInfinite());
    assertTrue(engine.isSearching());

    Thread.sleep(2000);

    toHandlerPrinter.println("stop");
    semaphore.tryAcquire(2000, TimeUnit.MILLISECONDS);

    while (engine.isSearching()) {
      Thread.sleep(20);
      // clear buffer
      while (fromHandlerReader.ready()) {
        fromHandlerReader.readLine();
      }
    }
    assertFalse(engine.isSearching());

    // clear buffer
    while (fromHandlerReader.ready()) {
      fromHandlerReader.readLine();
    }
  }
}
