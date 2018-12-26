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

package fko.UCI;

import java.util.List;

public interface IUCISearchMode {

  int getBlackInc();

  int getBlackTime();

  int getDepth();

  int getMate();

  List<String> getMoves();

  int getMoveTime();

  long getNodes();

  int getWhiteInc();

  int getWhiteTime();

  boolean isInfinite();

  boolean isPonder();

  int getMovesToGo();

  void setBlackInc(int blackInc);

  void setBlackTime(int blackTime);

  void setDepth(int depth);

  void setInfinite(boolean infinite);

  void setMate(int mate);

  void setMoves(List<String> moves);

  void setMoveTime(int movetime);

  void setNodes(long nodes);

  void setPonder(boolean ponder);

  void setWhiteInc(int whiteInc);

  void setWhiteTime(int whiteTime);

  void setMovesToGo(int movesToGo);

  boolean isPerft();

  void setPerft(boolean perft);
}
