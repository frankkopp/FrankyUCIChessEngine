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

package fko.FrankyEngine.Franky;

import fko.UCI.IUCIEngine;
import fko.UCI.IUCIProtocolHandler;
import fko.UCI.IUCISearchMode;
import fko.UCI.UCIOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Franky Engine for UCI GUIs
 * <p>
 * TODO:
 * Testing / Bug fixing
 * --------------------------------
 * Arena shows Illegal Moves
 * Arena shows forfeit for time
 * Mates found in lower depths lost in higher depths
 * <p>
 * <p>
 * TODO
 * Performance
 * --------------------------------
 * <p>
 * TODO
 * Features
 * --------------------------------
 */
public class FrankyEngine implements IUCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(FrankyEngine.class);

  private static final String PROJECT_PROPERTIES = "project.properties";

  // back reference to the uci protocol handler for sending messages to the UCI UI
  private IUCIProtocolHandler uciProtocolHandler = null;

  // engine/search configuration
  private final Configuration config = new Configuration();

  // the current search instance
  private final Search search;

  // ID of engine
  private final String iDName;
  private final String iDAuthor;

  // options of engine
  private List<IUCIEngine.IUCIOption> iUciOptions;

  // engine state
  private Position   position;
  private SearchMode searchMode;

  /**
   * Default Constructor
   */
  public FrankyEngine() {

    final Properties properties = new Properties();
    try {
      properties.load(Objects.requireNonNull(
        this.getClass().getClassLoader().getResourceAsStream(PROJECT_PROPERTIES)));
    } catch (IOException e) {
      LOG.error("Could load properties file: {}", PROJECT_PROPERTIES);
      LOG.error("Could load properties file: " + PROJECT_PROPERTIES, e);
      e.printStackTrace();
      System.exit(1);
    }
    iDName = properties.getProperty("artifactId") + " v" + properties.getProperty("version");
    iDAuthor = properties.getProperty("author");

    initOptions();

    search = new Search(this, config);
    position = new Position();

    // @formatter:off
    System.out.printf("%n===================================================%n"
                      + "%s%n"
                      + "Java UCI Chess Engine%n"
                      + "MIT License%n"
                      + "Copyright (c) 2018 by %s %n"
                      + "===================================================%n%n",
                      iDName,
                      iDAuthor
                     );
   // @formatter:on

  }

  /**
   * Return the last board position the engine has received through <code>setPosition</code>.
   * Used for Unit testing.
   *
   * @return the last position the engine has received through <code>setPosition</code>
   */
  public Position getPosition() {
    return position;
  }

  /**
   * Return the last search mode the engine has received through <code>startSearch</code>.
   * Used for Unit testing.
   *
   * @return last search mode
   */
  public SearchMode getSearchMode() {
    return searchMode;
  }

  private void initOptions() {
    iUciOptions = new ArrayList<>();
    // @formatter:off
    iUciOptions.add(
        new UCIOption("Hash",
                UCIOptionType.spin,
                "" + config.HASH_SIZE,
                "1",
                "512",
                ""));
    iUciOptions.add(
        new UCIOption("Clear Hash",
                UCIOptionType.button,
                "",
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Ponder",
                UCIOptionType.check,
                Boolean.toString(config.PONDER),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("OwnBook",
                UCIOptionType.check,
                Boolean.toString(config.USE_BOOK),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("UCI_ShowCurrLine",
                UCIOptionType.check,
                Boolean.toString(config.UCI_ShowCurrLine),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Use_QSearch",
                UCIOptionType.check,
                Boolean.toString(config.USE_QUIESCENCE),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Use_AlphaBeta_Pruning",
                UCIOptionType.check,
                Boolean.toString(config.USE_ALPHABETA_PRUNING),
                "",
                "",
                ""));
//    iUciOptions.add(
//        new UCIOption("Use_Aspiration_Window_Search",
//                UCIOptionType.check,
//                Boolean.toString(config.USE_ASPIRATION_WINDOW),
//                "",
//                "",
//                ""));
    iUciOptions.add(
        new UCIOption("Use_PVS",
                UCIOptionType.check,
                Boolean.toString(config.USE_PVS),
                "",
                "",
                ""));
     iUciOptions.add(
        new UCIOption("Use_PVS_Move_Ordering",
                UCIOptionType.check,
                Boolean.toString(config.USE_PVS_MOVE_ORDERING),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Use_TranspositionTable",
                UCIOptionType.check,
                Boolean.toString(config.USE_TRANSPOSITION_TABLE),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Use_Mate_Distance_Pruning",
                UCIOptionType.check,
                Boolean.toString(config.USE_MATE_DISTANCE_PRUNING),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Use_Minor_Promotion_Pruning",
                UCIOptionType.check,
                Boolean.toString(config.USE_MINOR_PROMOTION_PRUNING),
                "",
                "",
                ""));
     iUciOptions.add(
        new UCIOption("Use_Null_Move_Pruning",
                UCIOptionType.check,
                Boolean.toString(config.USE_NULL_MOVE_PRUNING),
                "",
                "",
                ""));
     iUciOptions.add(
        new UCIOption("Null_Move_Depth",
                UCIOptionType.spin,
                Integer.toString(config.NULL_MOVE_DEPTH),
                "1",
                "3",
                ""));
     iUciOptions.add(
        new UCIOption("Use_Eval_Pruning",
                UCIOptionType.check,
                Boolean.toString(config.USE_STATIC_NULL_PRUNING),
                "",
                "",
                ""));
     iUciOptions.add(
        new UCIOption("Use_Razor_Pruning",
                UCIOptionType.check,
                Boolean.toString(config.USE_RAZOR_PRUNING),
                "",
                "",
                ""));
     iUciOptions.add(
        new UCIOption("Use_Killer_Moves",
                UCIOptionType.check,
                Boolean.toString(config.USE_KILLER_MOVES),
                "",
                "",
                ""));
     iUciOptions.add(
        new UCIOption("Number_Killer_Moves",
                UCIOptionType.spin,
                Integer.toString(config.NO_KILLER_MOVES),
                "0",
                "10",
                ""));
//     iUciOptions.add(
//        new UCIOption("Use_Full_Move_Ordering",
//                UCIOptionType.check,
//                Boolean.toString(config.USE_SORT_ALL_MOVES),
//                "",
//                "",
//                ""));
    iUciOptions.add(
        new UCIOption("Use_Late_Move_Reduction",
                UCIOptionType.check,
                Boolean.toString(config.USE_LMR),
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption("Late_Move_Reduction_Min_Depth",
                UCIOptionType.spin,
                Integer.toString(config.LMR_MIN_DEPTH),
                "2",
                "10",
                ""));
    iUciOptions.add(
        new UCIOption("Late_Move_Reduction",
                UCIOptionType.spin,
                Integer.toString(config.LMR_REDUCTION),
                "0",
                "5",
                ""));
   // @formatter:on
  }

  @Override
  public void registerProtocolHandler(IUCIProtocolHandler uciProtocolHandler) {
    this.uciProtocolHandler = uciProtocolHandler;
  }

  @Override
  public String getIDName() {
    return iDName;
  }

  @Override
  public String getIDAuthor() {
    return iDAuthor;
  }

  @Override
  public void setOption(String name, String value) {
    final String msg;
    switch (name) {
      case "Hash":
        setHashSizeOption(value);
        break;
      case "Clear Hash":
        search.clearHashTables();
        msg = "Hash cleared";
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Ponder":
        config.PONDER = Boolean.valueOf(value);
        msg = "Engine Ponder set to " + (config.PONDER ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "OwnBook":
        config.USE_BOOK = Boolean.valueOf(value);
        msg = "Engine Book set to " + (config.PONDER ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_QSearch":
        config.USE_QUIESCENCE = Boolean.valueOf(value);
        msg = "Use Quiescence Search set to " + (config.USE_QUIESCENCE ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_AlphaBeta_Pruning":
        config.USE_ALPHABETA_PRUNING = Boolean.valueOf(value);
        msg = "Use AlphaBeta Pruning set to " + (config.USE_ALPHABETA_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_PVS":
        config.USE_PVS = Boolean.valueOf(value);
        msg = "Use PVSearch set to " + (config.USE_PVS ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_PVS_Move_Ordering":
        config.USE_PVS_MOVE_ORDERING = Boolean.valueOf(value);
        msg = "Use PVS Ordering set to " + (config.USE_PVS_MOVE_ORDERING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_TranspositionTable":
        config.USE_TRANSPOSITION_TABLE = Boolean.valueOf(value);
        msg = "Use Hashtable set to " + (config.USE_TRANSPOSITION_TABLE ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Mate_Distance_Pruning":
        config.USE_MATE_DISTANCE_PRUNING = Boolean.valueOf(value);
        msg =
          "Use Mate Distance Pruning set to " + (config.USE_MATE_DISTANCE_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Minor_Promotion_Pruning":
        config.USE_MINOR_PROMOTION_PRUNING = Boolean.valueOf(value);
        msg = "Use Minor Promotion Pruning set to " + (config.USE_MINOR_PROMOTION_PRUNING
                                                       ? "On"
                                                       : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Null_Move_Pruning":
        config.USE_NULL_MOVE_PRUNING = Boolean.valueOf(value);
        msg = "Use Null Move Pruning set to " + (config.USE_NULL_MOVE_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Null_Move_Depth":
        config.NULL_MOVE_DEPTH = Integer.valueOf(value);
        msg = "Null Move Depth set to " + (config.NULL_MOVE_DEPTH);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Razor_Pruning":
        config.USE_RAZOR_PRUNING = Boolean.valueOf(value);
        msg = "Use Razor Pruning set to " + (config.USE_RAZOR_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Eval_Pruning":
        config.USE_STATIC_NULL_PRUNING = Boolean.valueOf(value);
        msg = "Use Eval Pruning set to " + (config.USE_STATIC_NULL_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Killer_Moves":
        config.USE_KILLER_MOVES = Boolean.valueOf(value);
        msg = "Use Killer Moves set to " + (config.USE_KILLER_MOVES ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Number_Killer_Moves":
        config.NO_KILLER_MOVES = Integer.valueOf(value);
        msg = "Number of Killer Moves set to " + config.NO_KILLER_MOVES;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Late_Move_Reduction":
        config.USE_LMR = Boolean.valueOf(value);
        msg = "Use Late Move Reduction set to " + (config.USE_LMR ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Late_Move_Reduction_Min_Depth":
        config.LMR_MIN_DEPTH = Integer.valueOf(value);
        msg = "Late Move Reduction Min Depth set to " + config.LMR_MIN_DEPTH;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Late_Move_Reduction":
        config.LMR_REDUCTION = Integer.valueOf(value);
        msg = "Late Move Reduction amount set to " + config.LMR_REDUCTION;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      //      case "Use_Aspiration_Window_Search":
      //        config.USE_ASPIRATION_WINDOW = Boolean.valueOf(value);
      //        msg =
      //          "Use Aspiration Window Search set to " + (config.USE_ASPIRATION_WINDOW ? "On" : "Off");
      //        LOG.info(msg);
      //        uciProtocolHandler.sendInfoStringToUCI(msg);
      //        break;
      default:
        LOG.error("Unknown option: {}", name);
        break;
    }
  }

  @Override
  public List<IUCIOption> getOptions() {
    return iUciOptions;
  }

  @Override
  public void newGame() {
    LOG.info("Engine got New Game command");
    LOG.info(config.toString());
    uciProtocolHandler.sendInfoStringToUCI(config.toString());
    search.newGame();
  }

  @Override
  public void setPosition(final String fen) {
    LOG.info("Engine got Position command: {}", fen);
    position = new Position(fen);
  }

  @Override
  public void doMove(final String moveUCI) {
    LOG.debug("Engine got doMove command: " + moveUCI);
    final int move = Move.fromUCINotation(position, moveUCI);
    position.makeMove(move);
  }

  @Override
  public boolean isReady() {
    LOG.info(config.toString());
    uciProtocolHandler.sendInfoStringToUCI(config.toString());
    return true;
  }

  @Override
  public void setDebugMode(final boolean debugOption) {
    this.config.DEBUG = debugOption;
    final String msg = "Engine Debug set to " + (this.config.DEBUG ? "On" : "Off");
    LOG.info(msg);
    uciProtocolHandler.sendInfoStringToUCI(msg);
  }

  @Override
  public boolean getDebugMode() {
    return config.DEBUG;
  }

  @Override
  public void startSearch(final IUCISearchMode uciSearchMode) {

    if (search.isSearching()) {
      LOG.warn("Previous search was still running. Stopping to start new search!");
      search.stopSearch();
    }

    LOG.info("Engine got Start Search Command with " + uciSearchMode.toString());

    // @formatter:off
    searchMode = new SearchMode(uciSearchMode.getWhiteTime(),
                                uciSearchMode.getBlackTime(),
                                uciSearchMode.getWhiteInc(),
                                uciSearchMode.getBlackInc(),
                                uciSearchMode.getMovesToGo(),
                                uciSearchMode.getMoveTime(),uciSearchMode.getNodes(),uciSearchMode.getDepth(),
                                uciSearchMode.getMate(),
                                uciSearchMode.getMoves(),
                                config.PONDER && uciSearchMode.isPonder(),
                                uciSearchMode.isInfinite(),
                                uciSearchMode.isPerft());
    // @formatter:on

    search.startSearch(position, searchMode);
  }

  @Override
  public boolean isSearching() {
    return search.isSearching();
  }

  @Override
  public void stopSearch() {
    LOG.info("Engine got Stop Search Command");
    search.stopSearch();
  }

  @Override
  public void ponderHit() {
    LOG.info("Engine got Ponderhit Command");
    search.ponderHit();
  }

  @Override
  public void sendResult(int bestMove, int ponderMove) {
    if (!Move.isValid(bestMove)) {
      LOG.error("Engine Best Move is invalid move!" + Move.toString(bestMove));
      LOG.error("Position: " + position.toFENString());
      LOG.error("Last Move: " + position.getLastMove());
    }

    LOG.info(
      "Engine got Best Move: " + Move.toSimpleString(bestMove) + " [Ponder " + Move.toSimpleString(
        ponderMove) + "]");

    if (ponderMove == Move.NOMOVE) {
      if (uciProtocolHandler != null) {
        uciProtocolHandler.sendResultToUCI(Move.toUCINotation(position, bestMove));
      }
    } else {
      if (uciProtocolHandler != null) {
        uciProtocolHandler.sendResultToUCI(Move.toUCINotation(position, bestMove),
                                           Move.toUCINotation(position, ponderMove));
      }
    }
  }

  @Override
  public void sendInfoToUCI(String s) {
    if (uciProtocolHandler != null) {
      uciProtocolHandler.sendInfoToUCI(s);
    } else {
      LOG.info("Engine >>>> " + s);
    }
  }

  private void setHashSizeOption(final String value) {
    this.config.HASH_SIZE = Integer.valueOf(value);
    final String msg = "Hash Size set to " + this.config.HASH_SIZE;
    LOG.info(msg);
    search.setHashSize(this.config.HASH_SIZE);
    uciProtocolHandler.sendInfoStringToUCI(msg);
  }

  /**
   * @return the current hash size setting of the engine
   */
  public int getHashSizeOption() {
    return this.config.HASH_SIZE;
  }

  /**
   * @return the current ponder option of the engine
   */
  public boolean getPonderOption() {
    return config.PONDER;
  }

  /**
   * @return the search object of this engine
   */
  public Search getSearch() {
    return search;
  }

  /**
   * @return the configuration object this engine uses
   */
  public Configuration getConfig() {
    return config;
  }


}
