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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * UCIEngine
 *
 * TODO: implement commands
 */
public class UCIEngine implements IUCIEngine {

  private static final Logger LOG = LoggerFactory.getLogger(UCIEngine.class);

  /**
   * UCIEngine
   */

  // ID of engine
  private String iDName = "MyEngine v0.1";
  private String iDAuthor = "Frank Kopp";

  // options of engine
  private int hashSize = 16;
  private boolean ponder = true;


  List<IUCIEngine.IUCIOption> iUciOptions = new ArrayList<>();

  /**
   * Default Constructor
   */
  public UCIEngine() {
    initOptions();
  }

  private void initOptions() {
    iUciOptions.add(new UCIOption("Hash",
            IUCIEngine.UCIOptionType.spin,
            ""+hashSize,
            "1",
            "4096",
            ""));
    iUciOptions.add(new UCIOption("Ponder",
            IUCIEngine.UCIOptionType.check,
            ponder ? "true" : "false",
            "",
            "",
            ""));
    iUciOptions.add(new UCIOption("Style",
            IUCIEngine.UCIOptionType.combo,
            "Normal",
            "",
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
  public void setHashSize(final int hashSize) {
    LOG.info("Engine Hash Size set to " + hashSize);
    this.hashSize = hashSize;
  }

  @Override
  public void setPonder(final boolean ponder) {
    LOG.info("Engine Ponder set to " + (ponder ? "On" : "Off"));
    this.ponder = ponder;
  }

  @Override
  public boolean getPonder() {
    return ponder;
  }

  @Override
  public int getHashSize() { return hashSize; }

}
