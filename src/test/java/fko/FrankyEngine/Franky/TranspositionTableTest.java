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

import fko.FrankyEngine.Franky.TranspositionTable.TT_EntryType;
import fko.UCI.IUCIEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank
 */
public class TranspositionTableTest {

  private static final Logger LOG = LoggerFactory.getLogger(TranspositionTableTest.class);

  private IUCIEngine engine;
  private Search     search;

  @BeforeEach
  void setUp() {
  }

  @Test
  public final void test_Cache() {
    TranspositionTable cache = new TranspositionTable(32);
    Position position = new Position();
    assertEquals(32 * 1024 * 1024, cache.getSize());
    cache.put(position.getZobristKey(), (short) 999, TT_EntryType.EXACT, (byte) 5);
    assertEquals(1, cache.getNumberOfEntries());
    assertEquals(999, cache.get(position.getZobristKey()).value);
    cache.put(position.getZobristKey(), (short) 1111, TT_EntryType.EXACT, (byte) 15);
    assertEquals(1111, cache.get(position.getZobristKey()).value);
    assertEquals(1, cache.getNumberOfEntries());
    cache.clear();
    assertEquals(0, cache.getNumberOfEntries());
  }

  @Test
  public void collisionTest() {
    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();

    final int depth = 10;

    LOG.info("Start COLLISION Test for depth {}", depth);

    String fen = "7k/8/8/8/8/8/P7/K7 b - - 0 1";
    Position position = new Position();

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, depth, 0, null, false, true, false);

    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    LOG.info("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    if (search.config.USE_TRANSPOSITION_TABLE) {
      if (search.getTranspositionTable().getNumberOfEntries() > 0) {
        LOG.info(String.format("TT Objects: %,d (%,d)",
                               search.getTranspositionTable().getNumberOfEntries(),
                               search.getTranspositionTable().getMaxEntries()));
        LOG.info(String.format("TT Collisions: %,d Updates: %,d",
                               search.getTranspositionTable().getNumberOfCollisions(),
                               search.getTranspositionTable().getNumberOfUpdates()));
      }
    }
    System.out.println();
  }


  /**
   * General test of TT usage
   */
  @Test
  public void TTUsageTest() {

    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();

    final int depth = 6;

    LOG.info("Start SIZE Test for depth {}", depth);

    Position position = new Position();

    search.config.USE_TRANSPOSITION_TABLE = true;

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, depth, 0, null, false, true, false);
    search.startSearch(position, searchMode);
    search.waitWhileSearching();

    LOG.info("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    if (search.getTranspositionTable().getNumberOfEntries() > 0) {
      LOG.info("TT Objects: {} ({})", search.getTranspositionTable().getNumberOfEntries(),
               search.getTranspositionTable().getMaxEntries());
      LOG.info("TT Collisions: {}", search.getTranspositionTable().getNumberOfCollisions());
    }
    LOG.info(search.getSearchCounter().toString());

    assertTrue(search.getTranspositionTable().getNumberOfEntries() > 0);
    assertTrue(search.getTranspositionTable().getNumberOfUpdates() > 0);
    //    assertEquals(0, search.getTranspositionTable().getNumberOfCollisions());
    assertTrue(search.getSearchCounter().tt_Hits > 0);
    assertTrue(search.getSearchCounter().tt_Misses > 0);
  }

  @Test
  public void testSize() {

    System.out.println("Testing Transposition Table size:");
    int[] megabytes = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 2048};
    for (int i : megabytes) {
      System.gc();
      long usedMemoryBefore =
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      TranspositionTable tt = new TranspositionTable(i);
      System.gc();
      long usedMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      long hashAllocation = (usedMemoryAfter - usedMemoryBefore) / (1024 * 1024);
      System.out.format("TT Size (config): %dMB = %dMB real size - Nodes: %d%n", i, hashAllocation,
                        tt.getMaxEntries());
      tt=null; // GC help
    }
  }

  @Test
  @Disabled
  public void showSize() {
    //System.out.println(VM.current().details());
    System.out.println(ClassLayout.parseClass(TranspositionTable.TT_Entry.class).toPrintable());
  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    int ROUNDS = 5;
    int ITERATIONS = 10;
    int REPETITIONS = 20;

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
    for (String s : result) {
      System.out.println(s);
    }

  }

  TranspositionTable tt = new TranspositionTable(1024);

  private void test1() {
    tt.ageEntries();
  }

  private void test2() {
   //tt.ageEntries2();
  }

}
