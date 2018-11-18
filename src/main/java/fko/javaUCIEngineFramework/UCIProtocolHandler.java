package fko.javaUCIEngineFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;

/** UCIProtocolHandler */
public class UCIProtocolHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(UCIProtocolHandler.class);

  // back reference to the engine
  private final IUCIEngine uciEngine;

  // input and outputStream of command stream
  private final InputStream inputStream;
  private final PrintStream outputStream;

  // -- the handler runs in a separate thread --
  private Thread myThread = null;
  private Boolean running = false;

  // as long as we do not have the uci protocol confirmed we ignore all commands but the "uci"
  // command
  private Boolean uciProtocollConfirmed = false;

  /**
   * Default contructor<p>
   *   Uses System.in and System.out as input and output streams.
   * @param uciEngine
   */
  public UCIProtocolHandler(final IUCIEngine uciEngine) {
    this.uciEngine = uciEngine;
    this.inputStream = System.in;
    this.outputStream = System.out;
  }

  /**
   * Contructor for unit testing
   * @param uciEngine
   * @param in
   * @param out
   */
  public UCIProtocolHandler(final IUCIEngine uciEngine, InputStream in, PrintStream out) {
    this.uciEngine = uciEngine;
    this.inputStream = in;
    this.outputStream = out;
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

  /**
   * Loop for protocol scanning
   */
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
                commandIsReady(scanner);
                break;
              case "setoption":
                commandSetOption(scanner);
                break;
              case "ucinewgame":
                commandUciNewGame(scanner);
                break;
              case "position":
                commandPosition(scanner);
                break;
              case "go":
                commandGo(scanner);
                break;
              case "stop":
                commandStop(scanner);
                break;
              case "ponderhit":
                commandPonderhit(scanner);
                break;
              case "register":
                commandRegister(scanner);
                break;
              case "debug":
                commandDebug(scanner);
                break;
              case "quit":
                commandQuit(scanner);
                break;
              default:
                LOG.warn("UCI Protocol Unknown Command: " + command);
                break;
            }

          } else {
            if (scanner.next().equalsIgnoreCase("uci")) {
              commandUCI(scanner);
            } else {
              LOG.warn("UCI Protocol not yet confirmed");
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

  private void commandUCI(final Scanner scanner) {
    send("id name " + uciEngine.getiDName());
    send("id author " + uciEngine.getiDAuthor());
    // TODO Options
    send("uciok");
    uciProtocollConfirmed = true;
    LOG.info("UCI Protocol confirmed");
  }

  private void commandIsReady(final Scanner scanner) {
    LOG.info("Received isready command");
    send("readyok");
  }

  private void commandSetOption(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: setoption not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandUciNewGame(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: ucinewgame not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandDebug(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: debug not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandRegister(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: register not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandPosition(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: position not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandGo(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: go not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandStop(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: stop not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandPonderhit(final Scanner scanner) {
    LOG.warn("UCI Protocol Command: ponderhit not yet implemented!");
    // interpreting the options
    while (scanner.hasNext()) {
      LOG.debug("UCI Protocol Command Option? " + scanner.next());
    }
  }

  private void commandQuit(final Scanner scanner) {
    LOG.info("Received quit command");
    running=false;
  }

  private void send(final String msg) {
    LOG.debug("Send: "+msg);
    outputStream.println(msg);
  }
}
