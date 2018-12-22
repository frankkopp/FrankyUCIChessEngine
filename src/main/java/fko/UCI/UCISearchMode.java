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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * UCISearchModes
 *
 * <p>start calculating on the current position set up with the "position" command.<br>
 * There are a number of commands that can follow this command, all will be sent in the same string.<br>
 * If one command is not sent its value should be interpreted as it would not influence the search.<br>
 * * searchmoves <move1> .... <movei><br>
 * restrict search to this moves only<br>
 * Example: After "position startpos" and "go infinite searchmoves e2e4 d2d4"<br>
 * the engine should only search the two moves e2e4 and d2d4 in the initial position.<br>
 * * ponder<br>
 * start searching in pondering mode.<br>
 * Do not exit the search in ponder mode, even if it's mate!<br>
 * This means that the last move sent in in the position string is the ponder move.<br>
 * The engine can do what it wants to do, but after a "ponderhit" command<br>
 * it should execute the suggested move to ponder on. This means that the pondern move sent by<br>
 * the GUI can be interpreted as a recommendation about which move to ponder. However, if the<br>
 * engine decides to ponder on a different move, it should not display any mainlines as they are<br>
 * likely to be misinterpreted by the GUI because the GUI expects the engine to ponder<br>
 * on the suggested move.<br>
 * * wtime <x><br>
 * white has x msec left on the clock<br>
 * * btime <x><br>
 * black has x msec left on the clock<br>
 * * winc <x><br>
 * white increment per move in mseconds if x > 0<br>
 * * binc <x><br>
 * black increment per move in mseconds if x > 0<br>
 * * movestogo <x><br>
 * there are x moves to the next time control,<br>
 * this will only be sent if x > 0,<br>
 * if you don't get this and get the wtime and btime it's sudden death<br>
 * * depth <x><br>
 * search x plies only.<br>
 * * nodes <x><br>
 * search x nodes only,<br>
 * * mate <x><br>
 * search for a mate in x moves<br>
 * * moveTime <x><br>
 * search exactly x mseconds<br>
 * * infinite<br>
 * search until the "stop" command. Do not exit the search without being told so in this mode!</p>
 */
public class UCISearchMode implements IUCISearchMode {

  private static final Logger LOG = LoggerFactory.getLogger(UCISearchMode.class);

  // defaults
  private int          whiteTime = 0;
  private int          blackTime = 0;
  private int          whiteInc  = 0;
  private int          blackInc  = 0;
  private int          movesToGo;
  private int          depth     = 0;
  private long         nodes     = 0;
  private int          mate      = 0;
  private int          movetime  = 0;
  private List<String> moves     = new ArrayList<>();

  private boolean ponder   = false;
  private boolean infinite = false;
  private boolean perft    = false;

  @Override
  public int getWhiteTime() {
    return whiteTime;
  }

  @Override
  public void setWhiteTime(final int whiteTime) {
    this.whiteTime = whiteTime;
  }

  @Override
  public void setMovesToGo(final int movesToGo) {
    this.movesToGo = movesToGo;
  }

  @Override
  public int getBlackTime() {
    return blackTime;
  }

  @Override
  public void setBlackTime(final int blackTime) {
    this.blackTime = blackTime;
  }

  @Override
  public int getWhiteInc() {
    return whiteInc;
  }

  @Override
  public void setWhiteInc(final int whiteInc) {
    this.whiteInc = whiteInc;
  }

  @Override
  public int getBlackInc() {
    return blackInc;
  }

  @Override
  public void setBlackInc(final int blackInc) {
    this.blackInc = blackInc;
  }

  @Override
  public int getDepth() {
    return depth;
  }

  @Override
  public void setDepth(final int depth) {
    this.depth = depth;
  }

  @Override
  public long getNodes() {
    return nodes;
  }

  @Override
  public void setNodes(final long nodes) {
    this.nodes = nodes;
  }

  @Override
  public int getMate() {
    return mate;
  }

  @Override
  public void setMate(final int mate) {
    this.mate = mate;
  }

  @Override
  public int getMoveTime() {
    return movetime;
  }

  @Override
  public void setMoveTime(final int movetime) {
    this.movetime = movetime;
  }

  @Override
  public List<String> getMoves() {
    return moves;
  }

  @Override
  public void setMoves(final List<String> moves) {
    this.moves = moves;
  }

  @Override
  public boolean isPonder() {
    return ponder;
  }

  @Override
  public int getMovesToGo() {
    return movesToGo;
  }

  @Override
  public void setPonder(final boolean ponder) {
    this.ponder = ponder;
  }

  @Override
  public boolean isInfinite() {
    return infinite;
  }

  @Override
  public void setInfinite(final boolean infinite) {
    this.infinite = infinite;
  }

  @Override
  public boolean isPerft() {
    return perft;
  }

  @Override
  public void setPerft(boolean perft) {
    this.perft = perft;
  }

  @Override
  public String toString() {
    return "UCISearchMode{" + "whiteTime=" + whiteTime + ", blackTime=" + blackTime +
           ", whiteInc=" + whiteInc + ", blackInc=" + blackInc + ", movesToGo=" + movesToGo +
           ", depth=" + depth + ", nodes=" + nodes + ", mate=" + mate + ", movetime=" + movetime +
           ", moves=" + moves + ", ponder=" + ponder + ", infinite=" + infinite + '}';
  }


}
