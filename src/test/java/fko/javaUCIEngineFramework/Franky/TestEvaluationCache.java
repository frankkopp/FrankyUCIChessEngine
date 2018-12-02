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

import fko.javaUCIEngineFramework.UCI.IUCIProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Frank
 */
public class TestEvaluationCache {

  private String          fenStandard;
  private BoardPosition   boardPosition;
  private Evaluation      evaluation;
  private EvaluationCache evaluationCache;

  /**
   * @throws Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    fenStandard = IUCIProtocolHandler.START_FEN;
    boardPosition = new BoardPosition(fenStandard);
    evaluation = new Evaluation();
    evaluationCache = new EvaluationCache(500);
  }

  /**
   *
   */
  @Test
  public final void test_Cache() {
    EvaluationCache cache = new EvaluationCache(32);
    assertEquals(1198372, cache.getMaxEntries());
    assertEquals(33554432, cache.getSizeInBytes());
    cache.put(123412341234L, 999);
    assertEquals(1, cache.getNumberOfEntries());
    assertEquals(999, cache.get(123412341234L));
    assertEquals(Integer.MIN_VALUE, cache.get(1234L));
    cache.put(123412341234L, 1111);
    assertEquals(1111, cache.get(123412341234L));
    assertEquals(1, cache.getNumberOfEntries());
    cache.clear();
    assertEquals(0, cache.getNumberOfEntries());
  }

  /**
   *
   */
  @Test
  public void testSize() {
    System.out.println("Testing Transposition Table size:");
    int[] megabytes = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 20048};
    for (int i : megabytes) {
      System.gc();
      long usedMemoryBefore =
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      EvaluationCache oec = new EvaluationCache(i);
      System.gc();
      long usedMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      long hashAllocation = (usedMemoryAfter - usedMemoryBefore) / (1024 * 1024);
      System.out.format("TT Size (config): %dMB = %dMB real size - Nodes: %d%n", i, hashAllocation,
                        oec.getMaxEntries());
      oec = null;
    }
  }

  @Test
  @Disabled
  public void testTiming() {

    int ROUNDS = 5;
    int DURATION = 2;

    int ITERATIONS = 0;

    Instant start;

    System.out.println("Running Timing Test");

    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    boardPosition = new BoardPosition(fen);
    int eval = evaluation.evaluate(boardPosition);
    evaluationCache.put(boardPosition.getZobristKey(), eval);

    System.out.println("Running Timing Test Test 1 vs. Test 2");

    for (int j = 0; j < ROUNDS; j++) {

      System.gc();

      start = Instant.now();
      ITERATIONS = 0;
      do {
        ITERATIONS++;
        test1();
      } while (Duration.between(start, Instant.now()).getSeconds() < DURATION);
      System.out.println(String.format("Test 1: %,7d runs/s", ITERATIONS / DURATION));

      start = Instant.now();
      ITERATIONS = 0;
      do {
        ITERATIONS++;
        test2();
      } while (Duration.between(start, Instant.now()).getSeconds() < DURATION);
      System.out.println(String.format("Test 2: %,7d runs/s", ITERATIONS / DURATION));
    }
  }

  private void test1() {
    evaluation.evaluate(boardPosition);
  }

  private void test2() {
    evaluationCache.get(boardPosition.getZobristKey());
  }

}
