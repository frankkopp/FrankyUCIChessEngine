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

import java.util.ArrayList;

/** @author Frank */
public class TimingTests {

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    int ROUNDS = 5;
    int ITERATIONS = 50;
    int REPETITIONS = 5000000;

    for (int round = 0; round < ROUNDS; round++) {
      long start, end, sum;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);
      System.gc();

      int i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;
      result.add(String.format("Round %d Test 1 avg: %,.3f sec", round, avg1));

      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test2();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;
      result.add(String.format("Round %d Test 2 avg: %,.3f sec", round, avg2));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }
    System.out.println(t);

  }

  long t = 0;

  private void test1() {
    t += Long.numberOfLeadingZeros((long) (2147483647L + Math.random()));
  }

  private void test2() {
    t += Long.numberOfTrailingZeros((long) (2147483647L + Math.random()));
  }
}
