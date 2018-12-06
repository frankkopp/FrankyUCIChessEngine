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

/**
 * A cache for board evaluation values to reduce evaluation calculation during
 * search. Implementation uses a simple array of an Entry class. The array indexes
 * are calculated by using the modulo of the max number of entries from the key.
 * <code>entries[key%maxNumberOfEntries]</code>. As long as key is randomly distributed
 * this works just fine.
 */
public class EvaluationCache {

  static private final int KB = 1024;

  private long sizeInBytes;
  private int  maxEntries;

  private int  numberOfEntries    = 0;
  private long numberOfCollisions = 0L;

  private final Entry[] entries;

  /**
   * Creates a hash table with a approximated number of entries calculated by
   * the sizeInBytes in KB divided by the entry sizeInBytes.<br>
   * The hash function is very simple using the modulo of number of entries on the key
   *
   * @param sizeInBytes in KB (1024^2)
   */
  public EvaluationCache(int sizeInBytes) {
    this.sizeInBytes = (long) sizeInBytes * KB * KB;

    // check available mem - add some head room
    System.gc();
    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long freeMemory = (Runtime.getRuntime().maxMemory() - usedMemory);
    int percentage = 10;
    if (freeMemory * percentage / 100 < this.sizeInBytes) {
      System.err.println(
        String.format("Not enough memory for a %,dMB evaluation cache - reducing to %,dMB",
                      this.sizeInBytes / (KB * KB), (freeMemory * percentage / 100) / (KB * KB)));
      this.sizeInBytes = (int) (freeMemory * percentage / 100); // % of memory
    }

    // sizeInBytes in byte divided by entry sizeInBytes plus sizeInBytes for array bucket
    maxEntries = (int) this.sizeInBytes / (Entry.SIZE + Integer.BYTES);

    // create buckets for hash table
    entries = new Entry[maxEntries];
    // initialize
    for (int i = 0; i < maxEntries; i++) {
      entries[i] = new Entry();
    }
  }

  /**
   * @param key
   * @param value
   */
  public void put(long key, int value) {
    if (maxEntries==0) return;
    final int hash = getHash(key);
    if (entries[hash].key == 0) { // new value
      numberOfEntries++;
    } else { // collision
      numberOfCollisions++;
    }
    entries[hash].key = key;
    entries[hash].value = value;
  }

  /**
   * @param key
   * @return value for key or Integer.MIN_VALUE if not found
   */
  public int get(long key) {
    if (maxEntries==0) return Integer.MIN_VALUE;
    final int hash = getHash(key);
    if (entries[hash].key == key) { // hash hit
      return entries[hash].value;
    }
    // cache miss or collision
    return Integer.MIN_VALUE;
  }

  private int getHash(long key) {
    return (int) (key % maxEntries);
  }

  /**
   * Clears all entry by resetting the to key=0 and
   * value=Integer-MIN_VALUE
   */
  public void clear() {
    // initialize
    for (int i = 0; i < maxEntries; i++) {
      entries[i].key = 0;
      entries[i].value = Integer.MIN_VALUE;
    }
    numberOfEntries = 0;
    numberOfCollisions = 0;
  }

  /**
   * @return the numberOfEntries
   */
  public int getNumberOfEntries() {
    return this.numberOfEntries;
  }

  /**
   * @return the sizeInBytes in KB
   */
  public long getSizeInBytes() {
    return this.sizeInBytes;
  }

  /**
   * @return the max_entries
   */
  public int getMaxEntries() {
    return this.maxEntries;
  }

  /**
   * @return the numberOfCollisions
   */
  public long getNumberOfCollisions() {
    return numberOfCollisions;
  }

  private static final class Entry {
    static final int SIZE = (Long.BYTES + Integer.BYTES) * 2;
    long key   = 0L;
    int  value = Integer.MIN_VALUE;
  }


}
