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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

/**
 * @author Frank
 */
public class Timing_EnumSetIteratorVsArrayTest {

  final EnumSet<Square> _squareSet  = EnumSet.noneOf(Square.class);
  final SquareList      _squareList = new SquareList();


  @Test
  @Disabled
  public void testTiming() {

    prepare();

    int ROUNDS = 5;
    int DURATION = 5;

    int ITERATIONS = 0;

    Instant start;

    System.out.println("Running Timing Test Test 1 vs. Test 2");

    for (int j = 0; j < ROUNDS; j++) {

      System.gc();

      start = Instant.now();
      ITERATIONS = 0;
      while (true) {
        ITERATIONS++;
        test1();
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) break;
      }
      System.out.println(String.format("Test 1: %,7d runs/s", ITERATIONS / DURATION));

      start = Instant.now();
      ITERATIONS = 0;
      while (true) {
        ITERATIONS++;
        test2();
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) break;
      }
      System.out.println(String.format("Test 2: %,7d runs/s", ITERATIONS / DURATION));

    }

  }

  /**
   *
   */
  private void prepare() {
  }

  @SuppressWarnings("unused")
  private void test1() {

    for (Square s : Square.values) {
      if (s.isValidSquare()) {
        _squareSet.add(s);
      }
    }

    Square tempa, tempb;
    for (Square s : _squareSet) {
      for (Square b : _squareSet) {
        tempa = s;
        tempb = b;
      }
    }

    for (Square s : Square.values) {
      if (s.isValidSquare()) {
        _squareSet.remove(s);
      }
    }

  }

  @SuppressWarnings("unused")
  private void test2() {

    for (Square s : Square.values) {
      if (s.isValidSquare()) {
        _squareList.add(s);
      }
    }

    Square tempa, tempb;
    for (int i = 0; i < _squareList.size(); i++) {
      for (int j = 0; j < _squareList.size(); j++) {
        tempa = _squareList.get(i);
        tempb = _squareList.get(j);
      }
    }

    for (Square s : Square.values) {
      if (s.isValidSquare()) {
        _squareList.remove(s);
      }
    }


  }


}
