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

import fko.javaUCIEngineFramework.UCI.IUCIEngine;
import fko.javaUCIEngineFramework.UCI.IUCIProtocolHandler;
import fko.javaUCIEngineFramework.UCI.UCIProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Main
 */
public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  /**
   * The main() method parses the command line arguments<br>
   *
   * @param args command line options
   */
  public static void main(final String[] args) {

    LOG.debug("Start UCI Engine Framework" + Instant.now());

    final IUCIEngine uciEngine = new FrankyEngine();
    final UCIProtocolHandler handler = new UCIProtocolHandler(uciEngine);

    handler.startHandler();

    LOG.debug("Started UCI Engine Framework" + Instant.now());


  }

}
