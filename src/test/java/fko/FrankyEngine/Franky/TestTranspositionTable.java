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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Frank
 */
public class TestTranspositionTable {

  private static final Logger LOG = LoggerFactory.getLogger(TestTranspositionTable.class);

  private IUCIEngine engine;
  private Search     search;

  @BeforeEach
  void setUp() {
  }

  /**
   *
   */
  @Test
  public final void test_Cache() {
    TranspositionTable cache = new TranspositionTable(32);
    Position position = new Position();
    //    assertEquals(762600, cache.getMaxEntries());
    assertEquals(32 * 1024 * 1024, cache.getSize());
    cache.put(position, 999, TT_EntryType.EXACT, 5);
    assertEquals(1, cache.getNumberOfEntries());
    assertEquals(999, cache.get(position).value);
    assertEquals(999, cache.get(position).value);
    cache.put(position, 1111, TT_EntryType.EXACT, 15);
    assertEquals(1111, cache.get(position).value);
    assertEquals(1, cache.getNumberOfEntries());
    cache.clear();
    assertEquals(0, cache.getNumberOfEntries());
  }

  /**
   *
   */
  @Test
  public void testSize() {

    //    System.out.println(VM.current().details());
    //    System.out.println(ClassLayout.parseClass(TranspositionTable.TT_Entry.class).toPrintable());
    //    System.out.println(ClassLayout.parseClass(TranspositionTable.class).toPrintable());

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
      tt = null;
    }

  }

  @Test
  public void collisionTest() {
    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();

    final int depth = 10;

    LOG.info("Start SIZE Test for depth {}", depth);

    String fen = "7k/8/8/8/8/8/P7/K7 b - - 0 1";
    //    fen = Position.START_FEN;
    Position position = new Position(fen);

    search.config.USE_ROOT_MOVES_SORT = false;
    search.config.USE_ALPHABETA_PRUNING = false;
    search.config.USE_PVS = false;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MATE_DISTANCE_PRUNING = false;
    search.config.USE_MINOR_PROMOTION_PRUNING = false;
    search.config.USE_QUIESCENCE = false;
    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, depth, 0, 0, 0, null, false, true, false);

    search.startSearch(position, searchMode);

    waitWhileSearching();

    LOG.info("Best Move: {} Value: {} Ponder {}",
             Move.toSimpleString(search.getLastSearchResult().bestMove),
             search.getLastSearchResult().resultValue / 100f,
             Move.toSimpleString(search.getLastSearchResult().ponderMove));

    if (search.config.USE_TRANSPOSITION_TABLE) {
      if (search.getTranspositionTable().getNumberOfEntries() > 0) {
        LOG.info("TT Objects: {} ({})", search.getTranspositionTable().getNumberOfEntries(),
                 search.getTranspositionTable().getMaxEntries());
        LOG.info("TT Collisions: {} Updates: {}",
                 search.getTranspositionTable().getNumberOfCollisions(),
                 search.getTranspositionTable().getNumberOfUpdates());
      }
    }
    System.out.println();
  }

  @Test
  public void TTUsageTest() {

    engine = new FrankyEngine();
    search = ((FrankyEngine) engine).getSearch();

    final int depth = 10;

    LOG.info("Start SIZE Test for depth {}", depth);

    String fen = "7k/8/8/8/8/8/P7/K7 w - - 0 1";
    //    fen = Position.START_FEN;
    Position position = new Position(fen);

    search.config.USE_ROOT_MOVES_SORT = false;
    search.config.USE_ALPHABETA_PRUNING = false;
    search.config.USE_PVS = false;
    search.config.USE_TRANSPOSITION_TABLE = true;
    search.config.USE_MATE_DISTANCE_PRUNING = false;
    search.config.USE_MINOR_PROMOTION_PRUNING = false;
    search.config.USE_QUIESCENCE = false;
    SearchMode searchMode =
      new SearchMode(0, 0, 0, 0, 0,
                     depth, 0, 0, 0, null,
                     false, true, false);

    search.startSearch(position, searchMode);

    waitWhileSearching();

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

  }

  private void waitWhileSearching() {
    while (search.isSearching()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignored) {
      }
    }
  }

}
