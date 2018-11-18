package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;


/** UCIEngine */
public class UCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(UCIEngine.class);

  public UCIEngine() {}

  /**
   * The main() method parses the command line arguments<br>
   *
   * @param args command line options
   */
  public static void main(final String[] args) {

    LOG.debug("Hello World! " + Instant.now());

    BufferedReader in;
    try {
      in = new BufferedReader(new InputStreamReader(System.in));
      while (in.ready()) {
        LOG.debug("INPUT: "+in.readLine());
      }
      // done, however you can choose to cycle over this line
      // in this thread or launch another to check for new input
      // in.close();
    } catch (IOException ex) {
      LOG.error("IO Exception at buffered read!!", ex);
      System.exit(-1);
    }

    PrintStream out;

    out = System.out;

    out.println("id name JavaUCI");
    out.println("id author Frank Kopp");
    out.println("uciok");

    // out.close();

  }
}
