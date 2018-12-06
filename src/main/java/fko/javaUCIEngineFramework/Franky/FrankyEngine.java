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

package fko.javaUCIEngineFramework.Franky;

import fko.javaUCIEngineFramework.UCI.IUCIEngine;
import fko.javaUCIEngineFramework.UCI.IUCIProtocolHandler;
import fko.javaUCIEngineFramework.UCI.IUCISearchMode;
import fko.javaUCIEngineFramework.UCI.UCIOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Franky Engine for UCI GUIs
 */
public class FrankyEngine implements IUCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(FrankyEngine.class);

  // back reference to the uci protocol handler for sending messages to the UCI UI
  private IUCIProtocolHandler uciProtocolHandler = null;

  // configuration parameters
  private final Configuration config = new Configuration();

  // the current search instance
  private Search search;

  // ID of engine
  private String iDName   = "Franky v0.1";
  private String iDAuthor = "Frank Kopp";

  // options of engine
  private List<IUCIEngine.IUCIOption> iUciOptions;

  // engine state
  private BoardPosition boardPosition;
  private SearchMode searchMode;

  /**
   * Default Constructor
   */
  public FrankyEngine() {
    initOptions();
    search = new Search(this, config);
    boardPosition = new BoardPosition(); // default is standard start board
  }

  /**
   * Return the last board position the engine has received through <code>setPosition</code>.
   * Used for Unit testing.
   *
   * @return the last position the engine has received through <code>setPosition</code>
   */
  public BoardPosition getBoardPosition() {
    return boardPosition;
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
        new UCIOption("Ponder",
                UCIOptionType.check,
                Boolean.toString(config.PONDER),
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
//    iUciOptions.add( // DUMMY for testing
//        new UCIOption(
//            "OwnBook",
//                UCIOptionType.check,
//                useOwnBookOption ? "true" : "false",
//                "",
//                "",
//                ""));
//    iUciOptions.add( // DUMMY for testing
//        new UCIOption(
//            "Style",
//                UCIOptionType.combo,
//                "Normal", "",
//                "",
//                "Solid Normal Risky"));
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
    switch (name.toLowerCase()) {
      case "hash":
        setHashSizeOption(value);
        break;
      case "ponder":
        setPonderOption(value);
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
    // TODO what need to be done for forgetting existing game
    LOG.info("Engine got New Game command");
    search.newGame();
  }

  @Override
  public void setPosition(final String fen) {
    LOG.info("Engine got Position command: {}", fen);
    boardPosition = new BoardPosition(fen);
  }

  @Override
  public void doMove(final String move) {
    LOG.info("Engine got doMove command: " + move);
    final int omegaMove = Move.fromUCINotation(boardPosition, move);
    boardPosition.makeMove(omegaMove);
  }

  @Override
  public boolean isReady() {
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

    IUCISearchMode uciSearchMode1 = uciSearchMode;
    LOG.info("Engine got Start Search Command with " + uciSearchMode1.toString());

    searchMode = new SearchMode(uciSearchMode.getWhiteTime(), uciSearchMode.getBlackTime(),
                                uciSearchMode.getWhiteInc(), uciSearchMode.getBlackInc(),
                                uciSearchMode.getMovesToGo(), uciSearchMode.getDepth(),
                                uciSearchMode.getNodes(), uciSearchMode.getMate(),
                                uciSearchMode.getMoveTime(), uciSearchMode.getMoves(),
                                uciSearchMode.isPonder(), uciSearchMode.isInfinite(),
                                uciSearchMode.isPerft());

    search.startSearch(boardPosition, searchMode);
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
    LOG.info("Engine got Best Move: " + Move.toSimpleString(bestMove) + " [Ponder " +
             Move.toSimpleString(ponderMove) + "]");

    if (ponderMove == Move.NOMOVE) {
      if (uciProtocolHandler != null) {
        uciProtocolHandler.sendResultToUCI(Move.toUCINotation(boardPosition, bestMove));
      }
    } else {
      if (uciProtocolHandler != null) {
        uciProtocolHandler.sendResultToUCI(Move.toUCINotation(boardPosition, bestMove),
                                           Move.toUCINotation(boardPosition, ponderMove));
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

  private void setPonderOption(final String value) {
    config.PONDER = Boolean.valueOf(value);
    final String msg = "Engine Ponder set to " + (config.PONDER ? "On" : "Off");
    LOG.info(msg);
    uciProtocolHandler.sendInfoStringToUCI(msg);
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


}
