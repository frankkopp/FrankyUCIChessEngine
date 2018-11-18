package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

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

        // Scanner parses the line to tokenize it
        Scanner scanner = new Scanner(readLine);
        if (scanner.hasNext()) {
          // while uci command has not been confirmed just look for uci and ignore rest
          if (uciProtocollConfirmed) {
            String command = scanner.next();
            LOG.debug("UCI Protocol Command? " + command);

            // interpreting the command
            switch (command) {
              case "quit":
                LOG.info("Received quit command");
                running=false;
                break;
              default:
                LOG.warn("Unknown command");
                break;
            }

            // interpreting the options
            while (scanner.hasNext()) {
              LOG.debug("UCI Protocol Command Option? " + scanner.next());
            }

          } else {
            if (scanner.next().equalsIgnoreCase("uci")) {
              uciProtocollConfirmed = true;
              LOG.info("UCI Protocol confirmed");
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
    LOG.debug("Quitting");
  }
}
