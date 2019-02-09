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

  private static final int MAX_SEARCH_DEPTH = Byte.MAX_VALUE;

  // defaults time control
  private Duration whiteTime;
  private Duration blackTime;
  private Duration whiteInc;
  private Duration blackInc;
  private int      movesToGo;
  private Duration moveTime;

  // extra limits
  private int          depth;
  private long         nodes;
  private List<String> moves;

  // no time control
  private int     mate;
  private boolean ponder;
  private boolean infinite;
  private boolean perft;

  // state
  private boolean timeControl = false;
  private int     startDepth  = 1;
  private int     maxDepth    = MAX_SEARCH_DEPTH;

  /**
   * Keeps the search mode state
   *  @param whiteTime remaining time in msec
   * @param blackTime remaining in msec
   * @param whiteInc time inc per move in msec
   * @param blackInc time inc in msec
   * @param movesToGo until next time control
   * @param moveTime time per move in msec
   * @param nodes max number of nodes to search
   * @param depth max depth of search
   * @param mate mate in x search
   * @param moves list of moves to search
   * @param ponder true or false
   * @param infinite true or false
   */
  public SearchMode(int whiteTime, int blackTime, int whiteInc, int blackInc, int movesToGo,
                    int moveTime, long nodes, int depth, int mate, List<String> moves,
                    boolean ponder, boolean infinite, boolean perft) {

    this.whiteTime = Duration.ofMillis(whiteTime);
    this.blackTime = Duration.ofMillis(blackTime);
    this.whiteInc = Duration.ofMillis(whiteInc);
    this.blackInc = Duration.ofMillis(blackInc);
    this.movesToGo = movesToGo;
    this.moveTime = Duration.ofMillis(moveTime);

    this.depth = depth;
    this.nodes = nodes;

    this.mate = mate;
    this.ponder = ponder;
    this.infinite = infinite;
    this.perft = perft;

    this.moves = moves;
    if (this.moves == null) {
      this.moves = new ArrayList<>();
    }
    setupLimits();

  }

  private synchronized void setupLimits() {
    // time management necessary and set start and max depth?
    if (this.perft){
      // no limits
      timeControl = false;
      startDepth = this.depth;
      maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
    } else if (this.infinite) {
      // limited by depth only (identical to depth limit in this case)
      timeControl = false;
      startDepth = 1;
      maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
    } else if (this.ponder) {
      // limits per depth only, start with 1
      timeControl = false;
      startDepth = 1;
      maxDepth =  this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
    } else if (this.mate > 0) {
      if (moveTime.toMillis() > 0) {
        // limits per mate depth and move time
        timeControl = true;
        startDepth = 1;
        maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
      } else {
        // limits per mate depth
        timeControl = false;
        startDepth = 1;
        maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
      }
    } else if (this.whiteTime.toMillis() > 0 && this.blackTime.toMillis() > 0) {
      // normal game with time for each player
      timeControl = true;
      startDepth = 1;
      // might be limited be depth as well
      maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
    } else if (this.moveTime.toMillis() > 0) {
      // normal game with time for each move
      timeControl = true;
      startDepth = 1;
      // might be limited be depth as well
      maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
    } else if (this.depth > 0 && this.nodes == 0) {
      // limited only by depth but still iterating
      timeControl = false;
      startDepth = 1;
      // might be limited be depth as well
      maxDepth = this.depth;
    } else if (this.nodes > 0) {
      // limited only by the number of nodes visited
      timeControl = false;
      startDepth = 1;
      // might be limited be depth as well
      maxDepth = this.depth > 0 ? this.depth : MAX_SEARCH_DEPTH;
    } else {
      // INVALID SearchMode
      String msg =
        "SearchMode is invalid as no mode could be deducted from settings: " + this.toString();
      IllegalArgumentException e = new IllegalArgumentException(msg);
      LOG.error(msg, e);
      throw e;
    }
  }

  public void ponderHit() {
    ponder = false;
    setupLimits();
  }

  public void ponderStop() {
    ponder = false;
  }

  public Duration getRemainingTime(Color color) {
    if (color == Color.NOCOLOR) {
      String msg = "Color must be either WHITE or BLACK";
      IllegalArgumentException e = new IllegalArgumentException(msg);
      LOG.error(msg, e);
      throw e;
    }
    return color == Color.WHITE ? whiteTime : blackTime;
  }

  public Duration getWhiteTime() {
    return whiteTime;
  }

  public Duration getBlackTime() {
    return blackTime;
  }

  public Duration getTimeInc(Color color) {
    if (color == Color.NOCOLOR) {
      String msg = "Color must be either WHITE or BLACK";
      IllegalArgumentException e = new IllegalArgumentException(msg);
      LOG.error(msg, e);
      throw e;
    }
    return color == Color.WHITE ? whiteInc : blackInc;
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

  public Duration getMoveTime() {
    return moveTime;
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

  public boolean isPerft() { return perft; }

  public boolean isInfinite() { return infinite && depth == 0; }

  public synchronized boolean isTimeControl() { return timeControl; }

  public synchronized int getStartDepth() { return startDepth; }

  public synchronized int getMaxDepth() {
    return maxDepth;
  }

  @Override
  public String toString() {
    return "SearchMode{" + "whiteTime=" + whiteTime + ", blackTime=" + blackTime + ", whiteInc=" +
           whiteInc + ", blackInc=" + blackInc + ", movesToGo=" + movesToGo + ", moveTime=" +
           moveTime + ", depth=" + depth + ", nodes=" + nodes + ", moves=" + moves + ", mate=" +
           mate + ", ponder=" + ponder + ", infinite=" + infinite + ", perft=" + perft +
           ", timeControl=" + timeControl + ", startDepth=" + startDepth + ", maxDepth=" +
           maxDepth + '}';
  }

}
