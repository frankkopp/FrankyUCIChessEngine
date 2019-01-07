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

    MoveList m1 = new MoveList();
    MoveList m2 = new MoveList();
    final Position position =
      new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");

    int ROUNDS = 5;
    int ITERATIONS = 50;
    int REPETITIONS = 2000000;

    for (int round = 0; round < ROUNDS; round++) {
      long start = 0, end = 0, sum = 0;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);
      System.gc();

      int i = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;

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

      result.add(String.format("Round %d Test 1 avg: %,.3f sec", round, avg1));
      result.add(String.format("Round %d Test 2 avg: %,.3f sec", round, avg2));
    }

    System.out.println();
    System.out.printf("%-20s %-20s %n", "Test 1", "Test 2");
    for (int i = 0; i < m1.size(); i++) {
      System.out.printf("%-20s %-20s %n", Move.toString(m1.get(i)), Move.toString(m2.get(i)));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }

  }

  private void test1() {
    final long longValue = (long) Math.abs(Math.random() * Long.MAX_VALUE);
    int test = (int) (longValue % Integer.MAX_VALUE);
//    System.out.println("Test1: "+test);
  }

  private void test2() {
    final long longValue = (long) Math.abs(Math.random() * Long.MAX_VALUE);
    int test = (int) (longValue ^ (longValue >>> 32));
//    System.out.println("Test2: "+test);
  }
}
