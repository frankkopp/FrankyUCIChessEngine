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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchMode
 */
public class SearchMode {

  private static final Logger LOG = LoggerFactory.getLogger(SearchMode.class);

  // defaults time control
  private Duration whiteTime = Duration.ZERO;
  private Duration blackTime = Duration.ZERO;
  private Duration whiteInc  = Duration.ZERO;
  private Duration blackInc  = Duration.ZERO;
  private int      movesToGo = 0;
  private Duration movetime  = Duration.ZERO;

  // extra limits
  private int          depth = 0;
  private long         nodes = 0;
  private List<String> moves = new ArrayList<>();

  // no time control
  private int     mate     = 0;
  private boolean ponder   = false;
  private boolean infinite = false;
  private boolean perft    = false;

  // state
  private boolean timeControl = false;

  /**
   * Keeps the search mode state
   *
   * @param whiteTime
   * @param blackTime
   * @param whiteInc
   * @param blackInc
   * @param movesToGo
   * @param depth
   * @param nodes
   * @param mate
   * @param movetime
   * @param moves
   * @param ponder
   * @param infinite
   */
  public SearchMode(int whiteTime, int blackTime, int whiteInc, int blackInc, int movesToGo,
                    int depth, long nodes, int mate, int movetime, List<String> moves,
                    boolean ponder, boolean infinite, boolean perft) {

    this.whiteTime = Duration.ofSeconds(whiteTime);
    this.blackTime = Duration.ofSeconds(blackTime);
    this.whiteInc = Duration.ofSeconds(whiteInc);
    this.blackInc = Duration.ofSeconds(blackInc);
    this.movesToGo = movesToGo;
    this.movetime = Duration.ofSeconds(movetime);

    this.depth = depth;
    this.nodes = nodes;

    this.mate = mate;
    this.ponder = ponder;
    this.infinite = infinite;
    this.perft = perft;

    this.moves = moves;

    timeControl = !this.ponder && !this.infinite && this.perft && this.mate <= 0;

  }

  public Duration getWhiteTime() {
    return whiteTime;
  }

  public Duration getBlackTime() {
    return blackTime;
  }

  public Duration getRemainingTime(Color color) {
    if (color == Color.NOCOLOR) {
      String msg = "Color ust be either WHITE or BLACK";
      IllegalArgumentException e = new IllegalArgumentException(msg);
      LOG.error(msg, e);
      throw e;
    }
    return color == Color.WHITE ? whiteInc : blackTime;
  }


  public Duration getWhiteInc() {
    return whiteInc;
  }

  public Duration getBlackInc() {
    return blackInc;
  }

  public int getMovesToGo() {
    return movesToGo;
  }

  public Duration getMovetime() {
    return movetime;
  }

  public int getDepth() {
    return depth;
  }

  public long getNodes() {
    return nodes;
  }

  public List<String> getMoves() {
    return moves;
  }

  public int getMate() {
    return mate;
  }

  public boolean isPonder() {
    return ponder;
  }

  public boolean isInfinite() {
    return infinite;
  }

  public boolean isTimeControl() {
    return timeControl;
  }

  public boolean isPerft() {
    return perft;
  }
}
