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

package fko.javaUCIEngineFramework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class UCIProtocolHandlerTest {

  private UCIProtocolHandler handler;

  private InputStream handlerInput;
  private PipedOutputStream toHandler;
  private PrintStream toHandlerPrinter;

  private PipedOutputStream handlerOutput;
  private PipedInputStream fromHandler;
  private BufferedReader fromHandlerReader;
  private IUCIEngine engine;

  @BeforeEach
  void setUp() throws IOException {

    toHandler = new PipedOutputStream();
    handlerInput = new PipedInputStream(toHandler);
    toHandlerPrinter = new PrintStream(toHandler);

    fromHandler = new PipedInputStream();
    handlerOutput = new PipedOutputStream(fromHandler);
    fromHandlerReader = new BufferedReader(new InputStreamReader(fromHandler));

    engine = new UCIEngine();
    handler = new UCIProtocolHandler(engine, handlerInput, handlerOutput);
    handler.startHandler();
  }

  @Test
  void uciCommand() throws IOException {

    toHandlerPrinter.println("uci");

    String line;

    // check id
    line = fromHandlerReader.readLine();
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
  }

  @Test
  void setoptionCommand() throws InterruptedException {

    assertEquals(16, engine.getHashSize());
    toHandlerPrinter.println("setoption name Hash value 32");
    handler.waitUntilProcessed();
    assertEquals(32, engine.getHashSize());

    assertTrue(engine.getPonder());
    toHandlerPrinter.println("setoption name Ponder value false");
    handler.waitUntilProcessed();
    assertFalse(engine.getPonder());

    toHandlerPrinter.println("quit");
    while (handler.isRunning()) {
      Thread.sleep(100);
    }

  }

  @Test
  void smokeTest() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
