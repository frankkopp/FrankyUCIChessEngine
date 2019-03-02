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

import java.util.stream.IntStream;

/**
 * A cache for node results during AlphaBeta search.
 * <p>
 * Implementation uses a simple array of an TT_Entry class. The array indexes
 * are calculated by using the modulo of the max number of entries from the key.
 * <code>entries[key%maxNumberOfEntries]</code>. As long as key is randomly distributed
 * this works just fine.
 * <p>
 * The TT_Entry elements are tailored for small memory footprint and use primitive data types
 * for value (short), depth (byte), type (byte), age (byte).
 */
public class EvalCache {

  private static final Logger LOG = LoggerFactory.getLogger(EvalCache.class);

  private static final int KB = 1024;
  private static final int MB = KB * KB;

  private static final int ENTRY_SIZE = 5;

  // size and fill info
  private long sizeInByte;
  private int  maxNumberOfEntries;
  private int  numberOfEntries = 0;

  // statistics
  private long numberOfPuts       = 0L;
  private long numberOfCollisions = 0L;
  private long numberOfUpdates    = 0L;
  private long numberOfProbes     = 0L;
  private long numberOfHits       = 0L;
  private long numberOfMisses     = 0L;

  // these two longs hold the actual entries for the transposition table
  private long[]  keys; // zobrist key
  private short[] data; // tt entry data

  /**
   * Creates a hash table with a approximated number of entries calculated by
   * the size in KB divided by the entry size.<br>
   * The hash function is very simple using the modulo of number of entries on the key
   *
   * @param size in MB (1024B^2)
   */
  public EvalCache(int size) {
    if (size < 1) {
      final String msg = "Hashtable must a least be 1 MB in size";
      IllegalArgumentException e = new IllegalArgumentException(msg);
      LOG.error(msg, e);
      throw e;
    }

    sizeInByte = (long) size * MB;

    // check available mem - add some head room
    System.gc();
    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long freeMemory = (Runtime.getRuntime().maxMemory() - usedMemory);
    long ttMemory = (long) (freeMemory * 0.9);

    LOG.debug("{}", String.format("Max JVM memory:              %,5d MB",
                                  Runtime.getRuntime().maxMemory() / MB));
    LOG.debug("{}", String.format("Current total memory:        %,5d MB",
                                  Runtime.getRuntime().totalMemory() / MB));
    LOG.debug("{}", String.format("Current used memory:         %,5d MB", usedMemory / MB));
    LOG.debug("{}", String.format("Current free memory:         %,5d MB", freeMemory / MB));
    LOG.debug("{}", String.format("Memory for Eval Cache:       %,5d MB", ttMemory / MB));

    if (ttMemory < sizeInByte) {
      LOG.warn("{}", String.format("Not enough memory for a %,dMB eval cache - reducing to %,dMB",
                                   sizeInByte / MB,
        (ttMemory) / MB));
      sizeInByte = (int) (ttMemory); // % of memory
    }

    // size in byte divided by entry size plus size for array bucket
    maxNumberOfEntries = (int) (sizeInByte / (ENTRY_SIZE + Integer.BYTES));

    // create buckets for hash table
    keys = new long[maxNumberOfEntries];
    data = new short[maxNumberOfEntries];

    LOG.info("{}", String.format("EvalCache Size:              %,5d MB", sizeInByte / (KB * KB)));
    LOG.info("{}", String.format("EvalCache Entries:           %,d", maxNumberOfEntries));
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   * @param key
   * @param value
   */
  public void put(final long key, final short value) {
    final int hashKey = getHash(key);
    final long entryKey = keys[hashKey];

    numberOfPuts++;

    keys[hashKey] = key;
    data[hashKey] = value;

    // stats
    if (entryKey == 0) numberOfEntries++;
    else if (entryKey != key) numberOfCollisions++;
    else numberOfUpdates++;

  }

  /**
   * This retrieves the cached value of this node from cache if the
   * cached value has been calculated at a depth equal or deeper as the
   * depth value provided.
   *
   * @param key
   * @return value for key or <tt>Integer.MIN_VALUE</tt> if not found
   */
  public short get(final long key) {
    final int hashKey = getHash(key);
    final long entryKey = keys[hashKey];

    numberOfProbes++;

    if (entryKey == key) { // hash hit
      numberOfHits++;
      return data[hashKey];
    }
    else numberOfMisses++;

    // cache miss or collision
    return Evaluation.NOVALUE;
  }

  /**
   * Clears all entries
   */
  public void clear() {
    // tests show for() is about 60% slower than lambda parallel()
    keys = new long[maxNumberOfEntries];
    data = new short[maxNumberOfEntries];
    numberOfEntries = 0;
    numberOfPuts = 0;
    numberOfCollisions = 0;
    numberOfUpdates = 0;
    numberOfProbes = 0;
    numberOfMisses = 0;
    numberOfHits = 0;
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
   * @return number of queries
   */
  public long getNumberOfProbes() {
    return numberOfProbes;
  }

  /**
   * @return number of hits when queried
   */
  public long getNumberOfHits() {
    return numberOfHits;
  }

  /**
   * @return number of misses when queried
   */
  public long getNumberOfMisses() {
    return numberOfMisses;
  }

  /**
   * @param key
   * @return returns a hash key
   */
  private int getHash(long key) {
    return (int) (key % maxNumberOfEntries);
  }

  @Override
  public String toString() {
    return String.format(
      "EvalCache'{'" + "Size: %,d MB, max entries: %,d " + "numberOfEntries: %,d (%,.1f%%), "
      + "numberOfPuts: %,d, " + "numberOfCollisions: %,d (%,.1f%%), "
      + "numberOfUpdates: %,d (%,.1f%%), " + "numberOfProbes: %,d, "
      + "numberOfHits: %,d (%,.1f%%), numberOfMisses: %,d (%,.1f%%)" + "'}'", sizeInByte / MB,
      maxNumberOfEntries, numberOfEntries,
      maxNumberOfEntries == 0 ? 0 : 100 * ((double) numberOfEntries / maxNumberOfEntries),
      numberOfPuts, numberOfCollisions,
      numberOfPuts == 0 ? 0 : 100 * ((double) numberOfCollisions / numberOfPuts), numberOfUpdates,
      numberOfPuts == 0 ? 0 : 100 * ((double) numberOfUpdates / numberOfPuts), numberOfProbes,
      numberOfHits, numberOfHits == 0 ? 0 : 100 * ((double) numberOfHits / numberOfProbes),
      numberOfMisses, numberOfMisses == 0 ? 0 : 100 * ((double) numberOfMisses / numberOfProbes));
  }
}
