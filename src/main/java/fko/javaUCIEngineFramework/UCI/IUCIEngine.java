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

package fko.javaUCIEngineFramework.UCI;

import fko.javaUCIEngineFramework.Franky.BoardPosition;

import java.util.List;

/** Interface for UCI Engines */
public interface IUCIEngine {

  void registerProtocolHandler(UCIProtocolHandler uciProtocolHandler);

  BoardPosition getBoardPosition();

  String getIDName();

  String getIDAuthor();

  List<IUCIOption> getOptions();

  int getHashSizeOption();

  void setHashSizeOption(int hashSizeOption);

  void setPonderOption(boolean ponderOn);

  boolean getPonderOption();

  void newGame();

  void setPosition(String startFen);

  void doMove(String move);

  boolean isReady();

  void setDebugOption(boolean debugOption);

  boolean getDebugOption();

  void startSearch(IUCISearchMode searchMode);

  void stopSearch();

  void ponderHit();

    /**
   * An Option for a MyEngine
   */
  interface IUCIOption {

    String getNameID();

    UCIOptionType getOptionType();

    String getDefaultValue();

    String getMinValue();

    String getMaxValue();

    String getVarValue();
  }

  /**
   * UCI Option can have these types
   */
  enum UCIOptionType {
    check,
    spin,
    combo,
    button,
    string
  }
}
