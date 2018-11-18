package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;


/** UCIEngine */
public class UCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(UCIEngine.class);

  private final UCIProtocollHandler handler;

  /**
   * Default Contructor
   */
  public UCIEngine() {
    handler = new UCIProtocollHandler();
    handler.startHandler();
  }

  /**
   * The main() method parses the command line arguments<br>
   *
   * @param args command line options
   */
  public static void main(final String[] args) {

    LOG.debug("Start UCI Engine Framework" + Instant.now());

    UCIEngine uciEngine = new UCIEngine();

    LOG.debug("Started UCI Engine Framework" + Instant.now());

  }
}
