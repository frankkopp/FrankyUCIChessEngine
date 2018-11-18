package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;


/** UCIEngine */
public class UCIEngine implements IUCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(UCIEngine.class);

  private final UCIProtocolHandler handler;

  // ID of engine
  private String iDName = "MyEngine v0.1";
  private String iDAuthor = "Frank Kopp";

  /**
   * Default Constructor
   */
  public UCIEngine() {
    handler = new UCIProtocolHandler(this);
    handler.startHandler();
  }

  /**
   * Contructor for unit tests
   * @param handler
   */
  public UCIEngine(UCIProtocolHandler handler) {
    this.handler = handler;
    handler.startHandler();
  }

  @Override
  public String getiDName() {
    return iDName;
  }

  @Override
  public String getiDAuthor() {
    return iDAuthor;
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
