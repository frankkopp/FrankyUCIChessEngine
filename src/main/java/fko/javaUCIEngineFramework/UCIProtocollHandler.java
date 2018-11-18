package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;

/** UCIProtocollHandler */
public class UCIProtocollHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(UCIProtocollHandler.class);

  // back reference to the engine
  private final UCIEngine uciEngine;

  // input and outputStream of command stream
  private final InputStream inputStream;
  private final PrintStream outputStream;

  // -- the handler runs in a separate thread --
  private Thread myThread = null;
  private Boolean running = false;

  // as long as we do not have the uci protocol confirmed we ignore all commands but the "uci"
  // command
  private Boolean uciProtocollConfirmed = false;


  public UCIProtocollHandler(final UCIEngine uciEngine) {
    this.uciEngine = uciEngine;
    this.inputStream = System.in;
    this.outputStream = System.out;
  }

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
        in = new BufferedReader(new InputStreamReader(inputStream));

        // wait until a line is ready to be read
        final String readLine = in.readLine();
        LOG.debug("Received: " + readLine);

        // Scanner parses the line to tokenize it
        Scanner scanner = new Scanner(readLine);
        if (scanner.hasNext()) {
          // while uci command has not been confirmed just look for uci and ignore rest
          if (uciProtocollConfirmed) {
            String command = scanner.next().toLowerCase();

            // interpreting the command
            switch (command) {
              case "isready":
                LOG.info("Received isready command");
                send("readyok");
                break;
              case "setoption":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "ucinewgame":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "position":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "go":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "stop":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "ponderhit":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "register":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "debug":
                LOG.warn("UCI Protocol Command: " + command + " not yet implemented!");
                break;
              case "quit":
                LOG.info("Received quit command");
                running=false;
                break;
              default:
                LOG.warn("UCI Protocol Unknown Command: " + command);
                break;
            }

            // interpreting the options
            while (scanner.hasNext()) {
              LOG.debug("UCI Protocol Command Option? " + scanner.next());
            }

          } else {
            if (scanner.next().equalsIgnoreCase("uci")) {
              send("id name " + uciEngine.getiDName());
              send("id author " + uciEngine.getiDAuthor());
              // TODO Options
              send("uciok");
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

  private void send(final String msg) {
    LOG.debug("Send: "+msg);
    outputStream.println(msg);
  }
}
