package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/** UCIProtocollHandler */
public class UCIProtocollHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(UCIProtocollHandler.class);

  // -- the handler runs in a separate thread --
  private Thread myThread = null;
  private Boolean running = false;

  // as long as we do not have the uci protocol confirmed we ignore all commands but the "uci"
  // command
  private Boolean uciProtocollConfirmed = false;

  public UCIProtocollHandler() {}

  /**
   * Start a new playroom thread to play one or multiple games<br>
   * The thread then calls run() to actually do the work.
   */
  public void startHandler() {
    running = true;
    // Now start the thread
    if (myThread == null) {
      myThread = new Thread(this, "Playroom");
      myThread.start();
    } else {
      throw new IllegalStateException("startHandler(): Handler thread already exists.");
    }
  }

  /** Stops the playroom thread and the running game.<br> */
  public void stopHandler() {
    if (myThread == null || !myThread.isAlive() || !running) {
      throw new IllegalStateException("stopHandler(): Handler thread is not running");
    }
    running = false;
    myThread.interrupt();
  }

  @Override
  public void run() {

    while (running) {

      LOG.debug("Listening...");

      BufferedReader in;
      try {
        in = new BufferedReader(new InputStreamReader(System.in));

        // wait until a line is ready to be read
        final String readLine = in.readLine();

        LOG.debug("Received: " + readLine);

        // returns a stream of token (words separated by white space)
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(readLine));
        // ignore case of tokens
        tokenizer.lowerCaseMode(true);
        tokenizer.parseNumbers();
        tokenizer.ordinaryChar('!');
        tokenizer.ordinaryChar('-');

        if (uciProtocollConfirmed) {
          while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            switch (tokenizer.ttype) {
              case StreamTokenizer.TT_WORD:
                LOG.debug("UCI Command WORD? " + tokenizer.sval);
                break;
              case StreamTokenizer.TT_NUMBER:
                LOG.debug("UCI Command NUMBER? " + tokenizer.nval);
                break;
              default:
                LOG.debug("UCI Command UNNKNOWN? " + tokenizer.ttype);
                break;
            }
          }
        } else {
          while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            if (tokenizer.sval.equals("uci")) {
              uciProtocollConfirmed = true;
              LOG.debug("UCI Protocol confirmed");
              break; // ignore other tokens
            } else {
              LOG.debug("UCI Protocol not yet confirmed");
            }
          }
        }

      } catch (IOException ex) {
        LOG.error("IO Exception at buffered read!!", ex);
        System.exit(-1);
      }
    }
  }
}
