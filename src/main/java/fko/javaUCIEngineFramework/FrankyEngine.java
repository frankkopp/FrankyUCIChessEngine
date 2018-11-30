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

package fko.javaUCIEngineFramework;

import fko.javaUCIEngineFramework.Franky.BoardPosition;
import fko.javaUCIEngineFramework.Franky.Move;
import fko.javaUCIEngineFramework.Franky.Search;
import fko.javaUCIEngineFramework.Franky.SearchMode;
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

  // the current search instance
  private Search search;

  // ID of engine
  private String iDName   = "Franky v0.1";
  private String iDAuthor = "Frank Kopp";

  // options of engine
  private int     hashSizeOption   = 16;
  private boolean ponderOption     = true;
  private boolean useOwnBookOption = false;
  private boolean debugOption      = false;
  private List<IUCIEngine.IUCIOption> iUciOptions;

  // engine state
  private BoardPosition  boardPosition;
  private IUCISearchMode uciSearchMode;

  private SearchMode searchMode;

  /**
   * Default Constructor
   */
  public FrankyEngine() {
    initOptions();
    search = new Search(this, hashSizeOption);
    boardPosition = new BoardPosition(); // default is standard start board
  }

  /**
   * Return the last board position the engine has received through <code>setPosition</code>.
   * Used for Unit testing.
   * @return the last position the engine has received through <code>setPosition</code>
   */
  public BoardPosition getBoardPosition() {
    return boardPosition;
  }

  /**
   * Return the last search mode the engine has received through <code>startSearch</code>.
   * Used for Unit testing.
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
                "" + hashSizeOption,
                "1",
                "4096",
                ""));
    iUciOptions.add(
        new UCIOption("Ponder",
                UCIOptionType.check,
                ponderOption ? "true" : "false",
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption(
            "OwnBook",
                UCIOptionType.check,
                useOwnBookOption ? "true" : "false",
                "",
                "",
                ""));
    iUciOptions.add( // DUMMY for testing
        new UCIOption(
            "Style",
                UCIOptionType.combo,
                "Normal", "",
                "",
                "Solid Normal Risky"));
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
  public List<IUCIOption> getOptions() {
    return iUciOptions;
  }

  @Override
  public void setHashSizeOption(final int hashSizeOption) {
    this.hashSizeOption = hashSizeOption;
    final String msg = "Hash Size set to " + this.hashSizeOption;
    LOG.info(msg);
    search.setHashSize(hashSizeOption);
    uciProtocolHandler.sendInfoStringToUCI(msg);
  }

  @Override
  public int getHashSizeOption() {
    return hashSizeOption;
  }

  @Override
  public void setPonderOption(final boolean ponderOption) {
    this.ponderOption = ponderOption;
    final String msg = "Engine Ponder set to " + (this.ponderOption ? "On" : "Off");
    LOG.info(msg);
    uciProtocolHandler.sendInfoStringToUCI(msg);
  }

  @Override
  public boolean getPonderOption() {
    return ponderOption;
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
  public void setDebugOption(final boolean debugOption) {
    this.debugOption = debugOption;
    final String msg = "Engine Debug set to " + (this.debugOption ? "On" : "Off");
    LOG.info(msg);
    uciProtocolHandler.sendInfoStringToUCI(msg);
  }

  @Override
  public boolean getDebugOption() {
    return debugOption;
  }

  @Override
  public void startSearch(final IUCISearchMode uciSearchMode) {

    if (search.isSearching()) {
      LOG.warn("Previous search was still running. Stopping to start new search!");
      search.stopSearch();
    }

    this.uciSearchMode = uciSearchMode;
    LOG.info("Engine got Start Search Command with " + this.uciSearchMode.toString());

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
        uciProtocolHandler.resultToUCI(Move.toUCINotation(boardPosition, bestMove));
      }
    } else {
      if (uciProtocolHandler != null) {
        uciProtocolHandler.resultToUCI(Move.toUCINotation(boardPosition, bestMove),
                                       Move.toUCINotation(boardPosition, ponderMove));
      }
    }
  }

  @Override
  public void sendInfoToUCI(String s) {
    if (uciProtocolHandler != null) uciProtocolHandler.sendInfoToUCI(s);
    else LOG.info("Engine >>>> " + s);
  }

}
