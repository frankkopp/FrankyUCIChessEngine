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

package fko.FrankyEngine.util;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Simple and fast list class for integers.
 * Grows as needed.
 *
 * @author Frank Kopp
 */
public class SimpleIntList implements Iterable<Integer> {

  /**
   * Max entries of a MoveList
   */
  public static final int DEFAULT_MAX_ENTRIES = 64;

  /**
   * When growing the list this determines how many entries entries
   * shall be generated
   */
  public static final int DEFAULT_GROWTH_MARGIN = 10;

  protected int   _arraySize = DEFAULT_MAX_ENTRIES;
  protected int[] _list;
  protected int   _head      = 0;
  protected int   _tail      = 0;

  /**
   * Creates a list with a maximum of DEFAULT_MAX_ENTRIES elements
   */
  public SimpleIntList() {
    this(DEFAULT_MAX_ENTRIES);
  }

  /**
   * Creates a list with an initial size of <tt>size</tt> elements
   *
   * @param size
   */
  public SimpleIntList(int size) {
    this._arraySize = size;
    _list = new int[size];
  }

  /**
   * Creates a list as a copy of the provided list.
   *
   * @param old
   */
  public SimpleIntList(SimpleIntList old) {
    _list = Arrays.copyOfRange(old._list, old._head, old._list.length);
    this._arraySize = old._arraySize;
    this._head = 0;
    this._tail = old._tail - old._head;
  }

  /**
   * Clear the list
   */
  public void clear() {
    _tail = _head = 0;
  }

  /**
   * Gets entry at a specific index
   *
   * @param index
   * @return element at index
   */
  public int get(int index) {
    if (index < 0) {
      throw new ArrayIndexOutOfBoundsException("Index < 0");
    }
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    if (_head + index >= _tail) {
      throw new ArrayIndexOutOfBoundsException("Index too high");
    }
    return _list[_head + index];
  }

  /**
   * Gets entry at a first index
   *
   * @return first element
   */
  public int getFirst() {
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    return _list[_head];
  }

  /**
   * Gets entry at a last index
   *
   * @return last element
   */
  public int getLast() {
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    return _list[_tail - 1];
  }

  /**
   * Returns {@code true} if this list contains the specified element.
   * More formally, returns {@code true} if and only if this list contains
   * at least one element {@code e} such that
   * {@code s == e)}.
   *
   * @param s element whose presence in this list is to be tested
   * @return {@code true} if this list contains the specified element
   */
  public boolean contains(int s) {
    return indexOf(s) >= 0;
  }

  /**
   * Returns the index of the first occurrence of the specified element
   * in this list, or -1 if this list does not contain the element.
   * More formally, returns the lowest index {@code i} such that
   * {@code s == get(i)},
   * or -1 if there is no such index.
   */
  public int indexOf(final int s) {
    if (empty()) return -1;
    // look for number in list
    for (int i = _head; i < _tail; i++) {
      if (_list[i] == s) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Adds an element to the end of the list.
   *
   * @param integer
   */
  public void add(int integer) {
    if (_tail >= _list.length) {
      grow(1);
    }
    _list[_tail++] = integer;
  }

  /**
   * Adds an list to the end of the list.
   *
   * @param newList
   */
  public void add(SimpleIntList newList) {
    if (_tail + newList.size() > _list.length) {
      grow(_tail + newList.size() - _list.length);
    }
    System.arraycopy(newList._list, newList._head, this._list, this._tail, newList.size());
    this._tail += newList.size();
  }

  /**
   * Adds an element to the front of the list.
   *
   * @param integer
   */
  public void addFront(final int integer) {
    if (_tail >= _list.length) {
      grow(1);
    }
    System.arraycopy(_list, _head, _list, _head+1,_tail-_head);
    _list[0] = integer;
    _tail++;
  }

  /**
   * Adds an list to the front of the list.
   *
   * @param newList
   */
  public void addFront(SimpleIntList newList) {
    final int oldListSize = this.size();
    final int newListSize = newList.size();
    final int newSize = oldListSize + newListSize + DEFAULT_GROWTH_MARGIN;
    int[] tmpList = Arrays.copyOfRange(newList._list, newList._head, newSize);
    System.arraycopy(this._list, this._head, tmpList, newListSize, oldListSize);
    this._list = tmpList;
    this._head = 0;
    this._tail = oldListSize + newListSize;
  }

  /**
   * Sets entry at a specific index
   *
   * @param index
   * @param value
   * @return old value at index
   */
  public int set(int index, int value) {
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    if (index < 0) {
      throw new ArrayIndexOutOfBoundsException("Index < 0");
    }
    if (_head + index >= _tail) {
      throw new ArrayIndexOutOfBoundsException("Index too high");
    }
    int old = _list[_head + index];
    _list[_head + index] = value;
    return old;
  }

  /**
   * Removes the first entry and returns the value.
   * If the list is empty it throws a ArrayIndexOutOfBoundsException
   *
   * @return removed element
   */
  public int removeFirst() {
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    return _list[_head++];
  }

  /**
   * Removes the last entry and returns the value.
   * If the list is empty it throws ArrayIndexOutOfBoundsException
   *
   * @return removed element
   */
  public int removeLast() {
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    return _list[--_tail];
  }

  /**
   * Removes an entry.
   *
   * @return boolean true of element has been found and removed
   */
  public boolean remove(int toRemove) {
    if (empty()) return false;
    int element = -1;

    // look for number in list
    for (int i = _head; i < _tail; i++) {
      if (_list[i] == toRemove) {
        element = i;
        break;
      }
    }

    // not found
    if (element < 0) return false;

    // first?
    if (element == _head) {
      _head++;
      return true;
    }
    // last?
    else if (element == _tail - 1) {
      _tail--;
      return true;
    }
    // remove element and move all elements behind one forward
    else {
      System.arraycopy(_list, element+1, _list, element, _tail-element-1);
      _tail--;
      return true;
    }
  }

  /**
   * Exchanges/swaps two entries
   *
   * @param i
   * @param j
   */
  public void swap(int i, int j) {
    if (i < 0) {
      throw new ArrayIndexOutOfBoundsException("Index i < 0");
    }
    if (j < 0) {
      throw new ArrayIndexOutOfBoundsException("Index j < 0");
    }
    if (_tail <= _head) {
      throw new ArrayIndexOutOfBoundsException("List is empty");
    }
    if (_head + i >= _tail) {
      throw new ArrayIndexOutOfBoundsException("Index i too high");
    }
    if (_head + j >= _tail) {
      throw new ArrayIndexOutOfBoundsException("Index j too high");
    }
    exchange(_head + i, _head + j);
  }

  /**
   * Swap the first occurrence of number with the first element.
   * If the number is not in the list nothing happens.
   *
   * @param number
   * @return true if number has been found and pushed, false otherwise
   */
  public boolean pushToHead(int number) {
    if (empty()) return false;
    int element = -1;
    // look for number in list
    for (int i = _head; i < _tail; i++) {
      if (_list[i] == number) {
        element = i;
        break;
      }
    }
    // if found swap with first
    if (element > -1) {
      final int tmp = _list[_head];
      _list[_head] = _list[element];
      _list[element] = tmp;
      return true;
    }
    return false;
  }

  /**
   * Puts the first occurrence of number as first element. Keeps the current order stable.
   * <p>
   * Moves all other elements one up until the former place of the element.
   * <p>
   * Creates a new list with size <code>oldList.size() + DEFAULT_GROWTH_MARGIN</code>.
   * <p>
   * If the number is not in the list nothing happens.
   *
   * @param number to push to the head
   * @return true if number has been found and pushed, false otherwise
   */
  public boolean pushToHeadStable(int number) {
    if (empty()) return false;
    int element = -1;
    // look for number in list
    for (int i = _head; i < _tail; i++) {
      if (_list[i] == number) {
        element = i;
        break;
      }
    }
    // already first?
    if (element == _head) {
      return true;
    }
    // put element to the front and copy other elements behind it in stable order
    else if (element > -1) {
      System.arraycopy(_list, _head, _list, _head + 1, element - _head);
      _list[_head] = number;
      return true;
    }
    return false;
  }

  /**
   * Returns the size of the list
   *
   * @return number of elements
   */
  public int size() {
    return _tail - _head;
  }

  /**
   * Returns true is size==0
   *
   * @return true if empty
   */
  public boolean empty() {
    return _tail - _head == 0;
  }

  /**
   * Returns a number of how many elements can be added to this list before it is full.
   *
   * @return number of available slots for elements to add
   */
  public int getAvailableCapacity() {
    return _list.length - size() - _head;
  }

  /**
   * A primitve int type comperator. Avoids boxing and unboxing of int to Integer
   */
  public interface IntComparator {
    int compare(int a1, int a2);
  }

  /**
   * fast sort of list
   *
   * @param comparator
   */
  public void sort(IntComparator comparator) {
    if (this.empty()) return;
    sort(_head, _tail, comparator);
  }

  /**
   * fast sort of list
   *
   * @param comparator
   */
  public void sort(Comparator<Integer> comparator) {
    if (this.empty()) return;
    sort(_head, _tail, comparator);
  }

  /**
   * Returns a copy of the list.
   *
   * @return copy of list as int[]
   */
  public int[] toArray() {
    return Arrays.copyOfRange(_list, _head, _tail);
  }

  /**
   * Creates a stream of int from this list
   *
   * @return IntStream from list
   */
  public IntStream stream() {
    return Arrays.stream(_list, _head, _tail);
  }

  /**
   * clones the list
   */
  @Override
  public SimpleIntList clone() {
    return new SimpleIntList(this);
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder(
      "List size=" + size() + " available capacity=" + getAvailableCapacity() + " [");
    for (int i = _head; i < _tail; i++) {
      s.append(_list[i]);
      if (i < _tail - 1) {
        s.append(",");
      }
    }
    s.append("]");
    return s.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.toArray());
    return result;
  }

  /**
   * A MoveList is equal to another MoveList when they have the same
   * elements in the same order independent from internal implementation.
   *
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof SimpleIntList)) {
      return false;
    }
    SimpleIntList other = (SimpleIntList) obj;
    return Arrays.equals(this.toArray(), other.toArray());
  }

  /**
   * @param extra_size
   */
  private void grow(int extra_size) {
    _arraySize = _arraySize + extra_size + DEFAULT_GROWTH_MARGIN;
    _list = Arrays.copyOfRange(_list, _head, _arraySize);
    this._tail = _tail - _head;
    this._head = 0;
  }

  /**
   * Sort implementation to order the list according to the given int comparator.<br>
   * Using the IntComperator avoids boxing/unboxing if int to Integer
   *
   * @param head       (including)
   * @param tail       (excluding)
   * @param comparator
   */
  private void sort(int head, int tail, IntComparator comparator) {
    insertionSort(head, tail, comparator);
  }

  /**
   * Insertionsort algorithm for smaller arrays.
   * Using the IntComperator avoids boxing/unboxing if int to Integer
   * @param head
   * @param tail
   * @param comparator
   */
  private void insertionSort(int head, int tail, IntComparator comparator) {
    int temp;
    for (int i = head + 1; i < tail; i++) {
      for (int j = i; j > head; j--) {
        if (comparator.compare(_list[j], _list[j - 1]) < 0) {
          temp = _list[j];
          _list[j] = _list[j - 1];
          _list[j - 1] = temp;
        }
      }
    }
  }

  /**
   * Sort implementation to order the list according to the given comparator.<br>
   *
   * @param head       (including)
   * @param tail       (excluding)
   * @param comparator
   */
  private void sort(int head, int tail, Comparator<Integer> comparator) {
    // Bigger arrays will be sorted with Arrays.sort. As Arrays.sort
    // only accepts object[] when using a comparator we need to convert.
    // Therefore small arrays will be sorted with local insertion sort as convertion
    // is too expensive.
    // TODO: 5000 are arbitrarily chosen - needs timing tests
    if (tail - head < 5000 && comparator!=null) {
      insertionSort(head, tail, comparator);
    } else {
      final Integer[] tmp = Arrays.stream(_list).boxed().toArray(Integer[]::new);
      Arrays.sort(tmp, _head, _tail, comparator);
      _list = Arrays.stream(tmp).mapToInt(o -> o).toArray();
    }
  }

  /**
   * Insertionsort algorithm for smaller arrays.
   *
   * @param head
   * @param tail
   * @param comparator
   */
  private void insertionSort(int head, int tail, Comparator<Integer> comparator) {
    int temp;
    for (int i = head + 1; i < tail; i++) {
      for (int j = i; j > head; j--) {
        if (comparator.compare(_list[j], _list[j - 1]) < 0) {
          temp = _list[j];
          _list[j] = _list[j - 1];
          _list[j - 1] = temp;
        }
      }
    }
  }

  /**
   * @param i
   * @param j
   */
  private void exchange(int i, int j) {
    int t = _list[i];
    _list[i] = _list[j];
    _list[j] = t;
  }

  /**
   * Returns an iterator over the elements contained in this list.  The
   * iterator traverses the elements in their <i>natural order</i>.
   * <p>
   * Using a for loop instead the iterator is about 15% faster
   * See units tests
   * <p>
   * <pre>
   * for (int i=0; i<_squareList.size(); i++) {
   *  for (int j=0; j<_squareList.size(); j++) {
   *    tempa = _squareList.get(i);
   *    tempb = _squareList.get(j);
   *  }
   * }
   * </pre>
   *
   * @return an iterator over the elements contained in this list
   */
  @Override
  public Iterator<Integer> iterator() {
    return new SimpleIntListIterator();
  }
  private class SimpleIntListIterator implements Iterator<Integer> {


    // detect modifications
    final int initialHead = _head;
    final int initialTail = _tail;

    int cursor = _head;

    /**
     * @see Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
      if (modified()) {
        throw new ConcurrentModificationException();
      }
      return cursor < _tail;
    }

    /**
     * @see Iterator#next()
     */
    public int nextInt() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return _list[cursor++];

    }

    /**
     * @see Iterator#next()
     */
    @Override
    public Integer next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return _list[cursor++];

    }

    /**
     * @return
     */
    private boolean modified() {
      return initialTail != _tail || initialHead != _head;
    }

  }
}
