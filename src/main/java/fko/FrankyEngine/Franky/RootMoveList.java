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

import java.util.ArrayList;

/**
 * List of Move with additional information such as value.<br>
 * Moves and Value are encapsulated in a class Entry.<br>
 * <br>
 * List with NOMOVE entries created when this class is instantiated.
 * This saves time during usage of this list.
 */
public class RootMoveList extends ArrayList<RootMoveEntry> {

  private static final long serialVersionUID = -8905465753105752609L;

  /**
   * @param move
   * @param value
   */
  public void add(int move, int value) {
    final RootMoveEntry e = new RootMoveEntry(move, value);
    this.add(e);
  }

  /**
   * @param i
   * @param move
   * @param value
   */
  public void set(int i, int move, int value) {
    final RootMoveEntry e = new RootMoveEntry(move, value);
    this.set(i, e);
  }

  /**
   * @param i
   * @return move
   */
  public int getMove(int i) {
    return this.get(i).move;
  }

  /**
   * @param i
   * @return value
   */
  public int getValue(int i) {
    return this.get(i).value;
  }


  /**
   * Sorts the list according to value.
   */
  public void sort() {
    this.sort((a, b) -> Integer.compare(b.value, a.value));
  }

  /**
   * @see Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    this.stream().forEach((i) -> {
      s.append(Move.toSimpleString(i.move));
      s.append(" (");
      s.append(i.value);
      s.append(") ");
    });
    return s.toString();
  }

  /**
   * Pushes the first entry in the list which equals entry to index 0
   *
   * @param move
   */
  public void pushToHead(int move) {
    RootMoveEntry element = null;
    for (RootMoveEntry e : this) {
      if (e.move == move) {
        element = e;
        break;
      }
    }
    if (element != null) {
      this.remove(element);
      this.add(0, element);
    }
  }

}
