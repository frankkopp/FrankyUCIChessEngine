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

/**
 * A cache for node results during AlphaBeta search.
 * Implementation uses a simple array of an Entry class. The array indexes
 * are calculated by using the modulo of the max number of entries from the key.
 * <code>entries[key%maxNumberOfEntries]</code>. As long as key is randomly distributed
 * this works just fine.
 */
public class TranspositionTable {

  private static final Logger LOG = LoggerFactory.getLogger(TranspositionTable.class);

  private static final int KB = 1024;

  private long sizeInByte;
  private int  maxNumberOfEntries;

  private int  numberOfEntries    = 0;
  private long numberOfCollisions = 0L;
  private long numberOfUpdates    = 0L;

  private final TT_Entry[] entries;

  /**
   * Creates a hash table with a approximated number of entries calculated by
   * the size in KB divided by the entry size.<br>
   * The hash function is very simple using the modulo of number of entries on the key
   *
   * @param size in MB (1024B^2)
   */
  public TranspositionTable(int size) {
    sizeInByte = (long) size * KB * KB;

    // check available mem - add some head room
    System.gc();
    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long freeMemory = (Runtime.getRuntime().maxMemory() - usedMemory);
    int percentage = 10;
    if (freeMemory * percentage / 100 < sizeInByte) {
      LOG.warn(
        String.format("Not enough memory for a %,dMB transposition cache - reducing to %,dMB",
                      sizeInByte / (KB * KB), (freeMemory * percentage / 100) / (KB * KB)));
      sizeInByte = (int) (freeMemory * percentage / 100); // % of memory
    }

    // size in byte divided by entry size plus size for array bucket
    maxNumberOfEntries = (int) (sizeInByte / (TT_Entry.SIZE + Integer.BYTES));

    // create buckets for hash table
    entries = new TT_Entry[maxNumberOfEntries];
    // initialize to not create objects during usage
    for (int i = 0; i < maxNumberOfEntries; i++) {
      entries[i] = new TT_Entry();
    }

    LOG.info("Transposition Table Size: {}MB", String.format("%,d", sizeInByte / (KB * KB)));
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   *  @param position
   * @param value
   * @param type
   * @param depth
   */
  public void put(Position position, int value, TT_EntryType type, int depth) {
    put(position, value, type, depth, Move.NOMOVE);
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   *  @param position
   * @param value
   * @param type
   * @param depth
   * @param bestMove
   */
  public void put(Position position, int value, TT_EntryType type, int depth, int bestMove) {

    final int hash = getHash(position.getZobristKey());

    // new value
    if (entries[hash].key == 0) {
      numberOfEntries++;
      entries[hash].key = position.getZobristKey();
      //entries[hash].fen = position.toFENString();
      entries[hash].value = value;
      entries[hash].type = type;
      entries[hash].depth = depth;
      entries[hash].bestMove = bestMove;
    }
    // different position - overwrite
    else if (position.getZobristKey() != entries[hash].key) {
      numberOfCollisions++;
      entries[hash].key = position.getZobristKey();
      //entries[hash].fen = position.toFENString();
      entries[hash].value = value;
      entries[hash].type = type;
      entries[hash].depth = depth;
      entries[hash].bestMove = bestMove;
    }
    // Update
    else if (position.getZobristKey() == entries[hash].key  // same position
             && depth > entries[hash].depth) { // Overwrite only when new value from deeper search
      numberOfUpdates++;
      entries[hash].key = position.getZobristKey();
      //entries[hash].fen = position.toFENString();
      entries[hash].value = value;
      entries[hash].type = type;
      entries[hash].depth = depth;
      entries[hash].bestMove = bestMove;
    }
    // ignore new values for cache
  }

  /**
   * This retrieves the cached value of this node from cache if the
   * cached value has been calculated at a depth equal or deeper as the
   * depth value provided.
   *
   * @param position
   * @return value for key or <tt>Integer.MIN_VALUE</tt> if not found
   */
  public TT_Entry get(Position position) {
    final int hash = getHash(position.getZobristKey());
    if (entries[hash].key == position.getZobristKey()) { // hash hit
      return entries[hash];
    }
    // cache miss or collision
    return null;
  }


  /**
   * TODO: better hash function?
   * @param key
   * @return returns a hash key
   */
  private int getHash(long key) {
    return (int) (key % maxNumberOfEntries);
  }

  /**
   * Clears all entry by resetting the to key=0 and
   * value=Integer-MIN_VALUE
   */
  public void clear() {
    // initialize
    for (int i = 0; i < maxNumberOfEntries; i++) {
      entries[i].key = 0L;
      //entries[i].fen = "";
      entries[i].value = Integer.MIN_VALUE;
      entries[i].depth = 0;
      entries[i].type = TT_EntryType.ALPHA;
    }
    numberOfEntries = 0;
    numberOfCollisions = 0;
    numberOfUpdates = 0;
  }

  /**
   * @return the numberOfEntries
   */
  public int getNumberOfEntries() {
    return this.numberOfEntries;
  }

  /**
   * @return the size in KB
   */
  public long getSize() {
    return this.sizeInByte;
  }

  /**
   * @return the max_entries
   */
  public int getMaxEntries() {
    return this.maxNumberOfEntries;
  }

  /**
   * @return the numberOfCollisions
   */
  public long getNumberOfCollisions() {
    return numberOfCollisions;
  }


  /**
   * @return number of entry updates of same position but deeper search
   */
  public long getNumberOfUpdates() {
    return numberOfUpdates;
  }

  /**
   * Entry for transposition table.
   * Contains a key, value and an entry type.
   */
  public static final class TT_Entry {

    // @formatter:off
    /**
     * fko.FrankyEngine.Franky.TranspositionTable$TT_Entry object internals:
     *
     *  OFFSET  SIZE TYPE DESCRIPTION
     *       0    12                (object header)
     *      12     4            int TT_Entry.value
     *      16     8           long TT_Entry.key
     *      24     4            int TT_Entry.depth
     *      28     4   TT_EntryType TT_Entry.type
     * Instance size: 32 bytes
     *
     */
    // @formatter:on
    static final int SIZE = 40;

    long         key      = 0L;
    int          value    = Evaluation.NOVALUE;
    int          depth    = 0;
    TT_EntryType type     = TT_EntryType.ALPHA;
    int          bestMove = Move.NOMOVE;
  }

  /**
   * Defines the type of transposition table entry for alpha beta search.
   */
  public enum TT_EntryType {EXACT, ALPHA, BETA}


}
