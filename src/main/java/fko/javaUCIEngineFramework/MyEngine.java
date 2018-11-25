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

import fko.javaUCIEngineFramework.UCI.IUCIEngine;
import fko.javaUCIEngineFramework.UCI.IUCISearchMode;
import fko.javaUCIEngineFramework.UCI.UCIOption;
import fko.javaUCIEngineFramework.UCI.UCISearchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * UCIEngine
 *
 * <p>TODO: implement commands
 */
public class UCIEngine implements IUCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(UCIEngine.class);

  // ID of engine
  private String iDName = "UCI Engine Example v0.1";
  private String iDAuthor = "Frank Kopp";

  // options of engine
  private int hashSizeOption = 16;
  private boolean ponderOption = true;
  private boolean useOwnBookOption = true;
  private boolean debugOption = false;

  List<IUCIEngine.IUCIOption> iUciOptions = new ArrayList<>();

  private IUCISearchMode searchMode = new UCISearchMode();

  /** Default Constructor */
  public UCIEngine() {
    initOptions();
  }

  private void initOptions() {
    iUciOptions.add(
        new UCIOption("Hash",
                IUCIEngine.UCIOptionType.spin,
                "" + hashSizeOption,
                "1",
                "4096",
                ""));
    iUciOptions.add(
        new UCIOption("Ponder",
                IUCIEngine.UCIOptionType.check,
                ponderOption ? "true" : "false",
                "",
                "",
                ""));
    iUciOptions.add(
        new UCIOption(
            "OwnBook",
                IUCIEngine.UCIOptionType.check,
                useOwnBookOption ? "true" : "false",
                "",
                "",
                ""));
    iUciOptions.add( // DUMMY for testing
        new UCIOption(
            "Style",
                IUCIEngine.UCIOptionType.combo,
                "Normal", "",
                "",
                "Solid Normal Risky"));
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
    LOG.info("Engine Hash Size set to " + hashSizeOption);
    this.hashSizeOption = hashSizeOption;
  }

  @Override
  public int getHashSizeOption() {
    return hashSizeOption;
  }

  @Override
  public void setPonderOption(final boolean ponderOption) {
    LOG.info("Engine Ponder set to " + (ponderOption ? "On" : "Off"));
    this.ponderOption = ponderOption;
  }

  @Override
  public boolean getPonderOption() {
    return ponderOption;
  }

  @Override
  public void newGame() {
    // TODO what need to be done for forgetting existing game
    LOG.info("Engine got New Game command");
  }

  @Override
  public void setPosition(final String fen) {
    LOG.info("Engine got Position command: "+fen);
    // TODO
  }

  @Override
  public void doMove(final String move) {
    LOG.info("Engine got doMove command: "+ move);
    // TODO
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setDebugOption(final boolean debugOption) {
    LOG.info("Engine Debug set to " + (debugOption ? "On" : "Off"));
    this.debugOption = debugOption;
  }

  @Override
  public boolean getDebugOption() {
    return debugOption;
  }

  @Override
  public void startSearch(final IUCISearchMode searchMode) {
    this.searchMode = searchMode;
    LOG.info("Engine Search start with "+this.searchMode.toString());
  }

  @Override
  public void stopSearch() {
    LOG.info("Engine Stop");
  }

  @Override
  public void ponderHit() {
    LOG.info("Engine PonderHit start with "+this.searchMode.toString());
  }
}
