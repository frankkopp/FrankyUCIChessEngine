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

package fko.FrankyEngine;

import ch.qos.logback.core.joran.spi.JoranException;
import fko.FrankyEngine.Franky.FrankyEngine;
import fko.UCI.IUCIEngine;
import fko.UCI.UCIProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

/**
 * Main
 */
public class Main {

  static {
    String PROJECT_PROPERTIES = "project.properties";
    final Properties properties = new Properties();
    try {
      properties.load(Objects.requireNonNull(
        Main.class.getClassLoader().getResourceAsStream(PROJECT_PROPERTIES)));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    String name = properties.getProperty("artifactId") + "_v" + properties.getProperty("version");
    System.setProperty("application-name", name);
  }

  /**
   * The main() method parses the command line arguments<br>
   *
   * @param args command line options
   */
  public static void main(final String[] args) throws JoranException {

    Logger LOG = LoggerFactory.getLogger(Main.class);

    LOG.debug("Start UCI Engine Framework " + Instant.now());

    final IUCIEngine uciEngine = new FrankyEngine();
    final UCIProtocolHandler handler = new UCIProtocolHandler(uciEngine);

    handler.startHandler();

    LOG.debug("Started UCI Engine Framework " + Instant.now());
  }

}
