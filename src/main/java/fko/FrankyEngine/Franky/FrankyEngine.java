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
    option("Hash", UCIOptionType.spin, "" + config.HASH_SIZE, "1", "4096", "");
    option("Clear Hash", UCIOptionType.button, "", "", "", "");
    option("Ponder", UCIOptionType.check, Boolean.toString(config.PONDER), "", "", "");
    option("OwnBook", UCIOptionType.check, Boolean.toString(config.USE_BOOK), "", "", "");
    option("UCI_ShowCurrLine", UCIOptionType.check, Boolean.toString(config.UCI_ShowCurrLine), "", "", "");
    option("Use_TranspositionTable", UCIOptionType.check, Boolean.toString(config.USE_TRANSPOSITION_TABLE), "", "", "");
    option("Use_QSearch", UCIOptionType.check, Boolean.toString(config.USE_QUIESCENCE), "", "", "");
    option("Use_AlphaBeta_Pruning", UCIOptionType.check, Boolean.toString(config.USE_ALPHABETA_PRUNING), "", "", "");
    option("Use_Killer_Moves", UCIOptionType.check, Boolean.toString(config.USE_KILLER_MOVES), "", "", "");
    option("Number_Killer_Moves", UCIOptionType.spin, Integer.toString(config.NO_KILLER_MOVES), "0", "10", "");
    option("Use_PVS", UCIOptionType.check, Boolean.toString(config.USE_PVS), "", "", "");
    option("Use_PVS_Move_Ordering", UCIOptionType.check, Boolean.toString(config.USE_PVS_ORDERING), "", "", "");
    option("Use_Aspiration_Window_Search", UCIOptionType.check, Boolean.toString(config.USE_ASPIRATION_WINDOW), "", "", "");
    option("Aspiration_Start_Depth", UCIOptionType.spin, Integer.toString(config.ASPIRATION_START_DEPTH), "2", "2", "8");
    option("Use_Mate_Distance_Pruning", UCIOptionType.check, Boolean.toString(config.USE_MDP), "", "", "");
    option("Use_Minor_Promotion_Pruning", UCIOptionType.check, Boolean.toString(config.USE_MPP), "", "", "");
    option("Use_Reverse_Futility_Pruning", UCIOptionType.check, Boolean.toString(config.USE_RFP), "", "", "");
    option("RFP_Margin", UCIOptionType.spin, Integer.toString(config.RFP_MARGIN), "0", "1800", "");
    option("Use_Null_Move_Pruning", UCIOptionType.check, Boolean.toString(config.USE_NMP), "", "", "");
    option("Null_Move_Depth", UCIOptionType.spin, Integer.toString(config.NMP_DEPTH), "1", "3", "");
    option("Null_Move_Verification", UCIOptionType.check, Boolean.toString(config.USE_VERIFY_NMP), "", "", "");
    option("Null_Move_Verification_Depth", UCIOptionType.spin, Integer.toString(config.NMP_VERIFICATION_DEPTH), "1", "5", "");
    option("Use_Razor_Pruning", UCIOptionType.check, Boolean.toString(config.USE_RAZOR_PRUNING), "", "", "");
    option("Razor_Depth", UCIOptionType.spin, Integer.toString(config.RAZOR_DEPTH), "1", "5", "");
    option("Razor_Margin", UCIOptionType.spin, Integer.toString(config.RAZOR_MARGIN), "0", "1800", "");
    option("Use_IID", UCIOptionType.check, Boolean.toString(config.USE_IID), "", "", "");
    option("IID_Reduction", UCIOptionType.spin, Integer.toString(config.IID_REDUCTION), "0", "10", "");
    option("Use_Extensions", UCIOptionType.check, Boolean.toString(config.USE_EXTENSIONS), "", "", "");
    option("Use_Limited_Razoring", UCIOptionType.check, Boolean.toString(config.USE_LIMITED_RAZORING), "", "", "");
    option("Use_Extended_Futility_Pruning", UCIOptionType.check, Boolean.toString(config.USE_EXTENDED_FUTILITY_PRUNING), "", "", "");
    option("Use_Futility_Pruning", UCIOptionType.check, Boolean.toString(config.USE_FUTILITY_PRUNING), "", "", "");
    option("Use_QFutility_Pruning", UCIOptionType.check, Boolean.toString(config.USE_QFUTILITY_PRUNING), "", "", "");
    option("Use_Late_Move_Pruning", UCIOptionType.check, Boolean.toString(config.USE_LMP), "", "", "");
    option("LMP_Min_Depth", UCIOptionType.spin, Integer.toString(config.LMP_MIN_DEPTH), "2", "10", "");
    option("LMP_Min_Moves", UCIOptionType.spin, Integer.toString(config.LMP_MIN_MOVES), "1", "15", "");
    option("Use_Late_Move_Reduction", UCIOptionType.check, Boolean.toString(config.USE_LMR), "", "", "");
    option("LMR_Depth", UCIOptionType.spin, Integer.toString(config.LMR_MIN_DEPTH), "2", "10", "");
    option("LMR_Min_Moves", UCIOptionType.spin, Integer.toString(config.LMR_MIN_MOVES), "1", "15", "");
    option("LMR_Reduction", UCIOptionType.spin, Integer.toString(config.LMR_REDUCTION), "0", "5", "");
    // @formatter:on
  }

  private void option(String hash, UCIOptionType spin, String def, String min, String max,
                      String val) {
    iUciOptions.add(new UCIOption(hash, spin, def, min, max, val));
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
      case "UCI_ShowCurrLine":
        config.UCI_ShowCurrLine = Boolean.valueOf(value);
        msg = "Engine UCI_ShowCurrLine set to " + (config.UCI_ShowCurrLine ? "On" : "Off");
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
        msg = "Engine Book set to " + (config.USE_BOOK ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_TranspositionTable":
        config.USE_TRANSPOSITION_TABLE = Boolean.valueOf(value);
        msg = "Use Hashtable set to " + (config.USE_TRANSPOSITION_TABLE ? "On" : "Off");
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
      case "Use_PVS":
        config.USE_PVS = Boolean.valueOf(value);
        msg = "Use PVSearch set to " + (config.USE_PVS ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_PVS_Move_Ordering":
        config.USE_PVS_ORDERING = Boolean.valueOf(value);
        msg = "Use PVS Ordering set to " + (config.USE_PVS_ORDERING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Aspiration_Window_Search":
        config.USE_ASPIRATION_WINDOW = Boolean.valueOf(value);
        msg =
          "Use Aspiration Window Search set to " + (config.USE_ASPIRATION_WINDOW ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Aspiration_Start_Depth":
        config.ASPIRATION_START_DEPTH = Integer.valueOf(value);
        msg = "Aspiration Start Depth set to " + config.ASPIRATION_START_DEPTH;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Mate_Distance_Pruning":
        config.USE_MDP = Boolean.valueOf(value);
        msg = "Use Mate Distance Pruning set to " + (config.USE_MDP ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Minor_Promotion_Pruning":
        config.USE_MPP = Boolean.valueOf(value);
        msg = "Use Minor Promotion Pruning set to " + (config.USE_MPP ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Reverse_Futility_Pruning":
        config.USE_RFP = Boolean.valueOf(value);
        msg = "Use Reverse Futility Pruning set to " + (config.USE_RFP ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "RFP_Margin":
        config.RFP_MARGIN = Integer.valueOf(value);
        msg = "RFP Margin set to " + (config.RFP_MARGIN);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Null_Move_Pruning":
        config.USE_NMP = Boolean.valueOf(value);
        msg = "Use Null Move Pruning set to " + (config.USE_NMP ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Null_Move_Depth":
        config.NMP_DEPTH = Integer.valueOf(value);
        msg = "Null Move Depth set to " + (config.NMP_DEPTH);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Null_Move_Verification":
        config.USE_VERIFY_NMP = Boolean.valueOf(value);
        msg = "Null Move Verification set to " + (config.USE_VERIFY_NMP);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Null_Move_Verification_Depth":
        config.NMP_VERIFICATION_DEPTH = Integer.valueOf(value);
        msg = "Null Move Verification Depth set to " + (config.NMP_VERIFICATION_DEPTH);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Razor_Pruning":
        config.USE_RAZOR_PRUNING = Boolean.valueOf(value);
        msg = "Use Razor Pruning set to " + (config.USE_RAZOR_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Razor_Depth":
        config.RAZOR_DEPTH = Integer.valueOf(value);
        msg = "RFP Margin set to " + (config.RAZOR_DEPTH);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Razor_Margin":
        config.RFP_MARGIN = Integer.valueOf(value);
        msg = "Razor Margin set to " + (config.RAZOR_MARGIN);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_IID":
        config.USE_IID = Boolean.valueOf(value);
        msg = "Use_IID set to " + (config.USE_IID ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "IID_Reduction":
        config.IID_REDUCTION = Integer.valueOf(value);
        msg = "IID_Reduction set to " + (config.IID_REDUCTION);
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Extensions":
        config.USE_EXTENSIONS = Boolean.valueOf(value);
        msg = "Use Search Extensions set to " + (config.USE_EXTENSIONS ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Limited_Razoring":
        config.USE_LIMITED_RAZORING = Boolean.valueOf(value);
        msg = "Use_Limited_Razoring set to " + (config.USE_LIMITED_RAZORING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Extended_Futility_Pruning":
        config.USE_EXTENDED_FUTILITY_PRUNING = Boolean.valueOf(value);
        msg = "Use_Extended_Futility_Pruning set to " + (config.USE_EXTENDED_FUTILITY_PRUNING
                                                         ? "On"
                                                         : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Futility_Pruning":
        config.USE_FUTILITY_PRUNING = Boolean.valueOf(value);
        msg = "Use_Futility_Pruning set to " + (config.USE_FUTILITY_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_QFutility_Pruning":
        config.USE_QFUTILITY_PRUNING = Boolean.valueOf(value);
        msg = "Use_QFutility_Pruning set to " + (config.USE_QFUTILITY_PRUNING ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Late_Move_Pruning":
        config.USE_LMP = Boolean.valueOf(value);
        msg = "Use Late Move Pruning set to " + (config.USE_LMP ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "LMP_Min_Depth":
        config.LMP_MIN_DEPTH = Integer.valueOf(value);
        msg = "Late Move Pruning Min Depth set to " + config.LMP_MIN_DEPTH;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "LMP_Min_Moves":
        config.LMP_MIN_MOVES = Integer.valueOf(value);
        msg = "Late Move Pruning Min Moves set to " + config.LMP_MIN_MOVES;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "Use_Late_Move_Reduction":
        config.USE_LMR = Boolean.valueOf(value);
        msg = "Use Late Move Reduction set to " + (config.USE_LMR ? "On" : "Off");
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "LMR_Depth":
        config.LMR_MIN_DEPTH = Integer.valueOf(value);
        msg = "Late Move Reduction Min Depth set to " + config.LMR_MIN_DEPTH;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "LMR_Min_Moves":
        config.LMR_MIN_MOVES = Integer.valueOf(value);
        msg = "Late Move Reduction Min Moves set to " + config.LMR_MIN_MOVES;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
      case "LMR_Reduction":
        config.LMR_REDUCTION = Integer.valueOf(value);
        msg = "Late Move Reduction amount set to " + config.LMR_REDUCTION;
        LOG.info(msg);
        uciProtocolHandler.sendInfoStringToUCI(msg);
        break;
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
      LOG.error("Engine Best Move is invalid move: " + Move.toString(bestMove));
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
    }
    else {
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
    }
    else {
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
