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
 * A cache for node results during AlphaBeta search.
 * Implementation uses a simple array of an Entry class. The array indexes
 * are calculated by using the modulo of the max number of entries from the key.
 * <code>entries[key%maxNumberOfEntries]</code>. As long as key is randomly distributed
 * this works just fine.
 */
public class TranspositionTable {

  static private final int MB = 1024;

  private       int _size;
  private final int _max_entries;

  private int  _numberOfEntries    = 0;
  private long _numberOfCollisions = 0L;

  private final TT_Entry[] entries;

  /**
   * Creates a hash table with a approximated number of entries calculated by
   * the size in MB divided by the entry size.<br>
   * The hash function is very simple using the modulo of number of entries on the key
   *
   * @param size in MB (1024^2)
   */
  public TranspositionTable(int size) {
    _size = size * MB * MB;

    // check available mem - add some head room
    System.gc();
    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long freeMemory = (Runtime.getRuntime().maxMemory() - usedMemory);
    int percentage = 10;
    if (freeMemory * percentage / 100 < _size) {
      System.err.println(
        String.format("Not enough memory for a %,dMB transposition cache - reducing to %,dMB", _size / (MB * MB),
                      (freeMemory * percentage / 100) / (MB * MB)));
      _size = (int) (freeMemory * percentage / 100); // % of memory
    }

    // size in byte divided by entry size plus size for array bucket
    _max_entries = _size / (TT_Entry.SIZE + Integer.BYTES);
    // create buckets for hash table
    entries = new TT_Entry[_max_entries];
    // initialize
    for (int i = 0; i < _max_entries; i++) {
      entries[i] = new TT_Entry();
    }
  }

  /**
   * Stores the node value and the depth it has been calculated at.
   *
   * @param position TODO
   * @param value
   * @param type
   * @param depth
   * @param moveList
   */
  public void put(BoardPosition position, int value, TT_EntryType type, int depth, MoveList moveList) {

    final int hash = getHash(position._zobristKey);

    // new value
    if (entries[hash].key == 0) {
      _numberOfEntries++;
      entries[hash].key = position._zobristKey;
      //entries[hash].fen = position.toFENString();
      entries[hash].value = value;
      entries[hash].type = type;
      entries[hash].depth = depth;
      entries[hash].move_list = moveList;
      //entries[hash].move_list = new SoftReference<MoveList>(moveList);

    }
    // different position - overwrite
    else if (position._zobristKey != entries[hash].key) {

      _numberOfCollisions++;
      entries[hash].key = position._zobristKey;
      //entries[hash].fen = position.toFENString();
      entries[hash].value = value;
      entries[hash].type = type;
      entries[hash].depth = depth;
      entries[hash].move_list = moveList;
      //entries[hash].move_list = new SoftReference<MoveList>(moveList);
    }
    // Collision or update
    else if (position._zobristKey == entries[hash].key  // same position
             && depth >= entries[hash].depth) { // Overwrite only when new value from deeper search

      // this asserts if key=key but fen!=fen ==> COLLISION!!!
      // DEBUG code
      //            final String fenCache = entries[hash].fen;
      //            final String fenNew = position.toFENString();
      //            final String fc = fenCache.replaceAll(" \\d+ \\d+$", "");
      //            final String fg = fenNew.replaceAll(" \\d+ \\d+$", "");
      //            if (!fc.equals(fg)) {
      //                System.err.println("key=key but fen!=fen");
      //                System.err.println("new  : "+fg);
      //                System.err.println("cache: "+fc);
      //                System.err.println();
      //            }

      _numberOfCollisions++;
      entries[hash].key = position._zobristKey;
      //entries[hash].fen = position.toFENString();
      entries[hash].value = value;
      entries[hash].type = type;
      entries[hash].depth = depth;
      entries[hash].move_list = moveList;
      //entries[hash].move_list = new SoftReference<MoveList>(moveList);
    }
    // ignore new values for cache
  }

  /**
   * This retrieves the cached value of this node from cache if the
   * cached value has been calculated at a depth equal or deeper as the
   * depth value provided.
   *
   * @param position TODO
   * @return value for key or <tt>Integer.MIN_VALUE</tt> if not found
   */
  public TT_Entry get(BoardPosition position) {
    final int hash = getHash(position._zobristKey);
    if (entries[hash].key == position._zobristKey) { // hash hit
      return entries[hash];
    }
    // cache miss or collision
    return null;
  }


  private int getHash(long key) {
    return (int) (key % _max_entries);
  }

  /**
   * Clears all entry by resetting the to key=0 and
   * value=Integer-MIN_VALUE
   */
  public void clear() {
    // initialize
    for (int i = 0; i < _max_entries; i++) {
      entries[i].key = 0L;
      //entries[i].fen = "";
      entries[i].value = Integer.MIN_VALUE;
      entries[i].depth = 0;
      entries[i].type = TT_EntryType.ALPHA;
    }
    _numberOfEntries = 0;
    _numberOfCollisions = 0;
  }

  /**
   * @return the numberOfEntries
   */
  public int getNumberOfEntries() {
    return this._numberOfEntries;
  }

  /**
   * @return the size in MB
   */
  public int getSize() {
    return this._size;
  }

  /**
   * @return the max_entries
   */
  public int getMaxEntries() {
    return this._max_entries;
  }

  /**
   * @return the numberOfCollisions
   */
  public long getNumberOfCollisions() {
    return _numberOfCollisions;
  }

  /**
   * Entry for transposition table.
   * Contains a key, value and an entry type.
   */
  public static final class TT_Entry {
    static final int SIZE = (Long.BYTES // key
                             + Integer.BYTES // value
                             + Integer.BYTES // depth
                            ) * 2 // 64bit?
                            + 8 // enum
      ;//+ 40; // SoftReference
    long         key       = 0L;
    //String fen = "";
    int          value     = Integer.MIN_VALUE;
    int          depth     = 0;
    TT_EntryType type      = TT_EntryType.ALPHA;
    MoveList     move_list = null;
    //SoftReference<MoveList> move_list = new SoftReference<MoveList>(null);
  }

  /**
   * Defines the type of transposition table entry for alpha beta search.
   */
  public enum TT_EntryType {EXACT, ALPHA, BETA}


}
