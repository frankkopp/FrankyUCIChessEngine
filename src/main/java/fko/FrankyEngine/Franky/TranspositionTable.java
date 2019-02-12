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
public class TranspositionTable {

  private static final Logger LOG = LoggerFactory.getLogger(TranspositionTable.class);

  private static final int KB = 1024;
  private static final int MB = KB * KB;
  private static final int ENTRY_SIZE = 8;

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
  private long[] keys; // zobrist key
  private long[] data; // tt entry data

  /**
   * Creates a hash table with a approximated number of entries calculated by
   * the size in KB divided by the entry size.<br>
   * The hash function is very simple using the modulo of number of entries on the key
   *
   * @param size in MB (1024B^2)
   */
  public TranspositionTable(int size) {
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
    LOG.debug("{}", String.format("Memory available for TT:     %,5d MB", ttMemory / MB));

    if (ttMemory < sizeInByte) {
      LOG.warn("{}", String.format(
        "Not enough memory for a %,dMB transposition cache - reducing to %,dMB", sizeInByte / MB,
        (ttMemory) / MB));
      sizeInByte = (int) (ttMemory); // % of memory
    }

    // size in byte divided by entry size plus size for array bucket
    maxNumberOfEntries = (int) (sizeInByte / (ENTRY_SIZE + Integer.BYTES));

    // create buckets for hash table
    keys = new long[maxNumberOfEntries];
    data = new long[maxNumberOfEntries];

    LOG.info("{}", String.format("Transposition Table Size:    %,5d MB", sizeInByte / (KB * KB)));
    LOG.info("{}", String.format("Transposition Table Entries: %,d", maxNumberOfEntries));
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   *
   * @param key
   * @param value
   * @param type
   * @param depth
   */
  public void put(final long key, final short value, final byte type, final byte depth) {
    put(key, value, type, depth, Move.NOMOVE, false);
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   * @param key
   * @param value
   * @param type
   * @param depth
   * @param bestMove
   * @param mateThreat
   */
  public void put(final long key, final short value, final byte type, final byte depth,
                  final int bestMove, final boolean mateThreat) {
    put(false, key, value, type, depth, bestMove, mateThreat);
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   * @param forced when true skips age check
   * @param key
   * @param value
   * @param type
   * @param depth
   * @param bestMove
   * @param mateThreat
   */
  public void put(boolean forced, final long key, final short value, final byte type,
                  final byte depth, final int bestMove, final boolean mateThreat) {

    assert depth >= 0;
    assert type > 0;
    assert value > Evaluation.NOVALUE;

    //final TTEntry ttEntry = entries[getHash(key)];
    final int hashKey = getHash(key);
    final long entryKey = keys[hashKey];
    long entryData = data[hashKey];

    numberOfPuts++;

    // New hash
    if (entryKey == 0) {
      numberOfEntries++;
      keys[hashKey] = key;
      entryData = resetAge(entryData);
      entryData = setMateThreat(entryData, mateThreat);
      entryData = setValue(entryData, value);
      entryData = setType(entryData, type);
      entryData = setDepth(entryData, depth);
      entryData = setBestMove(entryData, bestMove);
      data[hashKey] = entryData;
    }
    // Same hash but different position
    // overwrite if
    // - the new entry's depth is higher or equal
    // - the previous entry has not been used (is aged)
    // @formatter:off
    else if (entryKey != key
             && depth >= getDepth(entryData)
             && (forced || getAge(entryData) > 0)
    ) { // @formatter:on
      numberOfCollisions++;
      keys[hashKey] = key;
      entryData = resetAge(entryData);
      entryData = setMateThreat(entryData, mateThreat);
      entryData = setValue(entryData, value);
      entryData = setType(entryData, type);
      entryData = setDepth(entryData, depth);
      entryData = setBestMove(entryData, bestMove);
      data[hashKey] = entryData;
    }
    // Same hash and same position -> update entry?
    else if (entryKey == key) {

      // if from same depth only update when quality of new entry is better
      // e.g. don't replace EXACT with ALPHA or BETA
      if (depth == getDepth(entryData)) {
        numberOfUpdates++;

        entryData = resetAge(entryData);
        entryData = setMateThreat(entryData, mateThreat);

        // old was not EXACT - update
        if (getType(entryData) != TT_EntryType.EXACT) {
          entryData = setValue(entryData, value);
          entryData = setType(entryData, type);
          entryData = setDepth(entryData, depth);
        }
        // old entry was exact, the new entry is also EXACT -> assert that they are identical
        else assert type != TT_EntryType.EXACT || getValue(entryData) == value;

        // overwrite bestMove only with a valid move
        if (bestMove != Move.NOMOVE) entryData = setBestMove(entryData, bestMove);

        data[hashKey] = entryData;
      }
      // if depth is greater then update in any case
      else if (getDepth(entryData) < depth) {
        numberOfUpdates++;
        entryData = resetAge(entryData);
        entryData = setMateThreat(entryData, mateThreat);
        entryData = setValue(entryData, value);
        entryData = setType(entryData, type);
        entryData = setDepth(entryData, depth);

        // overwrite bestMove only with a valid move
        if (bestMove != Move.NOMOVE) entryData = setBestMove(entryData, bestMove);

        data[hashKey] = entryData;
      }
      // overwrite bestMove if there wasn't any before
      else if (getBestMove(entryData) == Move.NOMOVE) {
        entryData = setBestMove(entryData, bestMove);
        data[hashKey] = entryData;
      }
    }
  }

  /**
   * This retrieves the cached value of this node from cache if the
   * cached value has been calculated at a depth equal or deeper as the
   * depth value provided.
   *
   * @param key
   * @return value for key or <tt>Integer.MIN_VALUE</tt> if not found
   */
  public long get(final long key) {
    numberOfProbes++;
    final int hashKey = getHash(key);
    final long entryKey = keys[hashKey];
    long entryData = data[hashKey];

    if (entryKey == key) { // hash hit
      numberOfHits++;
      // decrease age of entry until 0
      entryData = decreaseAge(entryData);
      data[hashKey] = entryData;
      return entryData;
    }
    else numberOfMisses++;
    // cache miss or collision
    return 0;
  }

  /**
   * Clears all entries
   */
  public void clear() {
    // tests show for() is about 60% slower than lambda parallel()
    keys = new long[maxNumberOfEntries];
    data = new long[maxNumberOfEntries];
    numberOfEntries = 0;
    numberOfPuts = 0;
    numberOfCollisions = 0;
    numberOfUpdates = 0;
    numberOfProbes = 0;
    numberOfMisses = 0;
    numberOfHits = 0;
  }

  /**
   * Age all entries to clear them for overwriting
   */
  public void ageEntries() {
    // tests show for() is about 60% slower than lambda parallel()
    IntStream.range(0, data.length)
             .parallel()
             .filter(i -> keys[i] != 0)
             .forEach(i -> data[i] = increaseAge(data[i]));
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
    return String.format("TranspositionTable'{'" + "Size: %,d MB, max entries: %,d "
                         + "numberOfEntries: %,d (%,.1f%%), " + "numberOfPuts: %,d, "
                         + "numberOfCollisions: %,d (%,.1f%%), "
                         + "numberOfUpdates: %,d (%,.1f%%), " + "numberOfProbes: %,d, "
                         + "numberOfHits: %,d (%,.1f%%), numberOfMisses: %,d (%,.1f%%)" + "'}'",
                         sizeInByte / MB, maxNumberOfEntries, numberOfEntries,
                         maxNumberOfEntries == 0
                         ? 0
                         : 100 * ((double) numberOfEntries / maxNumberOfEntries), numberOfPuts,
                         numberOfCollisions,
                         numberOfPuts == 0 ? 0 : 100 * ((double) numberOfCollisions / numberOfPuts),
                         numberOfUpdates,
                         numberOfPuts == 0 ? 0 : 100 * ((double) numberOfUpdates / numberOfPuts),
                         numberOfProbes, numberOfHits,
                         numberOfHits == 0 ? 0 : 100 * ((double) numberOfHits / numberOfProbes),
                         numberOfMisses, numberOfMisses == 0
                                         ? 0
                                         : 100 * ((double) numberOfMisses / numberOfProbes));
  }
  // ###########################################################################
  // Bit operations for data
  // data:       length position values
  // ----------------------------------------------------
  // move:       31 bit  0-30    (only positive integers)
  // Value:      15 bit 31-45    (only positive shorts)
  // Depth:       7 bit 46-52    (only positive bytes)
  // Age:         3 bit 53-55    (0-7)
  // Type:        2 bit 56-57    (0-3)
  // MateThread:  1 bit 58       (0-1)
  // Free:        5 bit
  // ###########################################################################

  // MASKs
  private static final long MOVE_bitMASK  = 0b1111111111111111111111111111111L;
  private static final long VALUE_bitMASK = 0b111111111111111L;
  private static final long DEPTH_bitMASK = 0b1111111L;
  private static final long AGE_bitMASK   = 0b111L;
  private static final long TYPE_bitMASK  = 0b11L;
  private static final long MATE_bitMASK  = 0b1L;

  // Bit operation values
  private static final long MOVE_SHIFT  = 0;
  private static final long MOVE_MASK   = MOVE_bitMASK << MOVE_SHIFT;
  private static final long VALUE_SHIFT = 31;
  private static final long VALUE_MASK  = VALUE_bitMASK << VALUE_SHIFT;
  private static final long DEPTH_SHIFT = 46;
  private static final long DEPTH_MASK  = DEPTH_bitMASK << DEPTH_SHIFT;
  private static final long AGE_SHIFT   = 53;
  private static final long AGE_MASK    = AGE_bitMASK << AGE_SHIFT;
  private static final long TYPE_SHIFT  = 56;
  private static final long TYPE_MASK   = TYPE_bitMASK << TYPE_SHIFT;
  private static final long MATE_SHIFT  = 58;
  private static final long MATE_MASK   = MATE_bitMASK << MATE_SHIFT;

  /**
   * Encodes the given move into the bit representation of the long.
   * Accepts any positive int and does not check if this is a valid move.
   * Any negative values will be set to 0.
   *
   * @param data
   * @param bestMove
   * @return long with value encoded
   */
  public static long setBestMove(long data, int bestMove) {
    if (bestMove < 0) bestMove = 0;
    // reset old move
    data &= ~MOVE_MASK;
    return data | bestMove << MOVE_SHIFT;
  }

  /**
   * Decodes the move from the long. Does not have to be a valid move as it
   * will ont be checked during storing or retrieving.
   *
   * @param data
   * @return decoded value
   */
  public static int getBestMove(long data) {
    return (int) ((data & MOVE_MASK) >>> MOVE_SHIFT);
  }

  /**
   * Encodes the given value into the bit representation of the long.
   * Accepts any short and does not check if this is a valid value.
   *
   * As our bit encoding does not allow negative values due to Java not
   * supporting unsigned primitive numbers we need to shift the value.
   * Therefore our max/min evaluation values should be smaller/greater than
   * Short.MAX/2 or Short.MIN/2. Anything bigger or smaller will be set to
   * MAX/MIN here!
   * Internally the value will then be shifted to a positive short and when
   * retrieved shifted back to its original value
   *
   * @param data
   * @param value
   * @return long with value encoded
   */
  public static long setValue(long data, short value) {
    if (value < Evaluation.NOVALUE) value = -Evaluation.INFINITE;
    else if (value > Evaluation.INFINITE) value = Evaluation.INFINITE;
    // shift to positive short to avoid signed short setting the negative bit
    value += -Evaluation.NOVALUE;
    assert value > 0;
    data &= ~VALUE_MASK;
    return data | (long) value << VALUE_SHIFT;
  }

  /**
   * Decodes the value from the long. Does not have to be a valid value
   * as it will ont be checked during storing or retrieving.
   *
   * As our bit encoding does not allow negative values due to Java not
   * supporting unsigned primitive numbers we need to shift the value.
   * Therefore our max/min evaluation values should be smaller/greater than
   * Short.MAX/2 or Short.MIN/2. Anything bigger or smaller will be set to
   * MAX/MIN here!
   * Internally the value will then be shifted to a positive short and when
   * retrieved shifted back to its original value
   *
   * @param data
   * @return decoded value
   */
  public static short getValue(long data) {
    // shift value back to original value
    return (short) ((short) ((data & VALUE_MASK) >>> VALUE_SHIFT) + Evaluation.NOVALUE);
  }

  /**
   * Encodes the given depth into the bit representation of the long.
   * Accepts any positive byte. Any negative values will be set to 0.
   *
   * @param data
   * @param depth
   * @return long with value encoded
   */
  public static long setDepth(long data, byte depth) {
    if (depth < 0) depth = 0;
    data &= ~DEPTH_MASK;
    return data | (long) depth << DEPTH_SHIFT;
  }

  /**
   * Decodes the depth from the long. Does not have to be a valid value
   * as it will ont be checked during storing or retrieving.
   *
   * @param data
   * @return decoded value
   */
  public static byte getDepth(long data) {
    return (byte) ((data & DEPTH_MASK) >>> DEPTH_SHIFT);
  }

  /**
   * Encodes the given age into the bit representation of the long.
   * Accepts any positive byte. Any negative values will be set to 0.
   *
   * @param data
   * @param age
   * @return long with value encoded
   */
  public static long setAge(long data, byte age) {
    if (age < 0) age = 0;
    data &= ~AGE_MASK;
    return data | (long) age << AGE_SHIFT;
  }

  /**
   * Decodes the age from the long.
   *
   * @param data
   * @return decoded value
   */
  public static byte getAge(long data) {
    return (byte) ((data & AGE_MASK) >>> AGE_SHIFT);
  }

  /**
   * Encodes default age (1) into the bit representation of the long.
   *
   * @param data
   * @return long with value encoded
   */
  public static long resetAge(long data) {
    return setAge(data, (byte) 1);
  }

  /**
   * Encodes age+1 into the bit representation of the long.
   * Maximum age of 7 is ensured.
   *
   * @param data
   * @return long with value encoded
   */
  public static long increaseAge(long data) {
    return setAge(data, (byte) Math.min(7, getAge(data) + 1));
  }

  /**
   * Encodes age-1 into the bit representation of the long.
   * Minimum age of 0 is ensured.
   *
   * @param data
   * @return long with value encoded
   */
  public static long decreaseAge(long data) {
    return setAge(data, (byte) (getAge(data) - 1));
  }

  /**
   * Encodes the given type into the bit representation of the long.
   * Accepts values 0 =< value <= 3. Other values will be set to 0.
   *
   * @param data
   * @param type
   * @return long with value encoded
   */
  public static long setType(long data, byte type) {
    if (type > 3 || type < 0) type = 0;
    data &= ~TYPE_MASK;
    return data | (long) type << TYPE_SHIFT;
  }

  /**
   * Decodes the type from the long.
   *
   * @param data
   * @return decoded value
   */
  public static byte getType(long data) {
    return (byte) ((data & TYPE_MASK) >>> TYPE_SHIFT);
  }

  /**
   * Encodes the given mateThreat into the bit representation of the long.
   *
   * @param data
   * @param mateThreat
   * @return long with value encoded
   */
  public static long setMateThreat(long data, boolean mateThreat) {
    if (mateThreat) return data | 1L << MATE_SHIFT;
    else return data & ~MATE_MASK;
  }

  /**
   * Decodes the mateThreat from the long.
   *
   * @param data
   * @return decoded value
   */
  public static boolean hasMateThreat(long data) {
    return (data & MATE_MASK) >>> MATE_SHIFT == 1;
  }

  public static String printBitString(long data) {
    StringBuilder bitBoardLine = new StringBuilder();
    for (int i = 0; i < Long.numberOfLeadingZeros(data); i++) bitBoardLine.append('0');
    String binaryString = Long.toBinaryString(data);
    if (binaryString.equals("0")) binaryString = "";
    return bitBoardLine.append(binaryString).toString();
  }

  /**
   * Constants for the TT Entry types NONE, EXACT, ALPHA, BETA.
   * Implemented as byte data types to save space
   */
  public static class TT_EntryType {

    public static final byte NONE  = 0;
    public static final byte EXACT = 1;
    public static final byte ALPHA = 2;
    public static final byte BETA  = 3;

    public static String toString(int type) {
      switch (type) {
        case EXACT:
          return "EXACT";
        case ALPHA:
          return "ALPHA";
        case BETA:
          return "BETA";
        default:
          return "NONE";
      }
    }
  }

}
