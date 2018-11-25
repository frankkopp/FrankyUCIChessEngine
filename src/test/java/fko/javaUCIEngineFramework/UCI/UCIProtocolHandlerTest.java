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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class UCIProtocolHandlerTest {

  private UCIProtocolHandler handler;

  private PrintStream toHandlerPrinter;
  private BufferedReader fromHandlerReader;

  private IUCIEngine engine;

  @BeforeEach
  void setUp() throws IOException {

    final PipedOutputStream toHandler = new PipedOutputStream();
    final InputStream handlerInput = new PipedInputStream(toHandler);
    toHandlerPrinter = new PrintStream(toHandler);

    final PipedInputStream fromHandler = new PipedInputStream();
    final PipedOutputStream handlerOutput = new PipedOutputStream(fromHandler);
    fromHandlerReader = new BufferedReader(new InputStreamReader(fromHandler));

    engine = new MyEngine();
    handler = new UCIProtocolHandler(engine, handlerInput, handlerOutput);
    handler.startHandler();
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    while (handler.isRunning()) {
      Thread.sleep(100);
    }
  }

  @Test
  void uciCommand() throws IOException {

    toHandlerPrinter.println("uci");

    // check id
    String line = fromHandlerReader.readLine();
    System.out.println("OUTPUT: " + line);
    assertTrue(line.startsWith("id name "));

    // check author
    line = fromHandlerReader.readLine();
    System.out.println("OUTPUT: " + line);
    assertTrue(line.startsWith("id author "));

    while ((line = fromHandlerReader.readLine()).startsWith("option ")) {
      System.out.println("OUTPUT: " + line);
    }

    // check uciok
    System.out.println("OUTPUT: " + line);
    assertTrue(line.startsWith("uciok"));

    toHandlerPrinter.println("quit");
  }

  @Test
  void setoptionCommand() {

    assertEquals(16, engine.getHashSizeOption());
    toHandlerPrinter.println("setoption name Hash value 32");
    handler.waitUntilProcessed();
    assertEquals(32, engine.getHashSizeOption());

    assertTrue(engine.getPonderOption());
    toHandlerPrinter.println("setoption name Ponder value false");
    handler.waitUntilProcessed();
    assertFalse(engine.getPonderOption());

    toHandlerPrinter.println("quit");
  }

  @Test
  void newGameCommand() {
    toHandlerPrinter.println("ucinewgame");
    handler.waitUntilProcessed();
    toHandlerPrinter.println("quit");
  }

  @Test
  void debugCommand() {
    assertFalse(engine.getDebugOption());
    toHandlerPrinter.println("debug on");
    handler.waitUntilProcessed();
    assertTrue(engine.getDebugOption());
    toHandlerPrinter.println("quit");
  }

  @Test
  void positionCommand() {

    // promotion
    toHandlerPrinter.println(
            "position fen 8/3P4/6K1/8/8/1k6/8/8 w - - 0 0 moves d7d8q");
    handler.waitUntilProcessed();
    assertEquals("3Q4/8/6K1/8/8/1k6/8/8 b - - 0 1",engine.getBoardPosition().toFENString());

    // castling
    toHandlerPrinter.println(
            "position fen r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 0 moves e1g1");
    handler.waitUntilProcessed();
    assertEquals("r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 1 1",engine.getBoardPosition().toFENString());

    // normal
    toHandlerPrinter.println("position startpos moves e2e4 e7e5");
    handler.waitUntilProcessed();
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",engine.getBoardPosition().toFENString());

    toHandlerPrinter.println("position moves e2e4 e7e5");
    handler.waitUntilProcessed();
    assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",engine.getBoardPosition().toFENString());

    toHandlerPrinter.println(
        "position fen rnbqkbnr/8/8/8/8/8/8/RNBQKBNR w KQkq - 0 1 moves e1e2 e8e7");
    handler.waitUntilProcessed();
    assertEquals("rnbq1bnr/4k3/8/8/8/8/4K3/RNBQ1BNR w - - 2 2",engine.getBoardPosition().toFENString());

    toHandlerPrinter.println("quit");
  }

  @Test
  void goCommand() {

    // TODO: add asserts when board and do move works
    toHandlerPrinter.println("go infinite");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go ponder");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go infinite searchmoves e2e4 d2d4");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go wtime 120 btime 110 movestogo 25");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go wtime 60 btime 50 winc 2 binc 2");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go movetime 10");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go mate 5");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go nodes 1000000000");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("go depth 10");
    handler.waitUntilProcessed();

    toHandlerPrinter.println("quit");
  }

  @Test
  void ponderHitCommand() {
    // TODO: add asserts when board and do move works
    toHandlerPrinter.println("ponderhit");
    handler.waitUntilProcessed();
    toHandlerPrinter.println("quit");
  }

  @Test
  void stopCommand() {
    // TODO: add asserts when board and do move works
    toHandlerPrinter.println("stop");
    handler.waitUntilProcessed();
    toHandlerPrinter.println("quit");
  }
}
