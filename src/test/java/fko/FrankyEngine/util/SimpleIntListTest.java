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

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** @author Frank */
public class SimpleIntListTest {

  /** Test boundaries of public methods */
  @Test
  public void testBoundaries() {
    // Constructor
    new SimpleIntList(100);
    new SimpleIntList(0);
    assertThrows(java.lang.NegativeArraySizeException.class, () -> new SimpleIntList(-1));
    assertThrows(java.lang.NullPointerException.class, () -> new SimpleIntList(null));
    // add
    final SimpleIntList list = new SimpleIntList();
    list.add(Integer.MIN_VALUE);
    list.add(Integer.MAX_VALUE);
    list.add(list);
    assertThrows(java.lang.NullPointerException.class, () -> list.add(null));
    list.addFront(list);
    assertThrows(java.lang.NullPointerException.class, () -> list.addFront(null));
    // get
    SimpleIntList listGet = new SimpleIntList(0);
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listGet.get(0));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listGet.get(-1));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listGet.get(10));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listGet.getFirst());
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listGet.getLast());
    listGet.add(Integer.MIN_VALUE);
    listGet.add(Integer.MAX_VALUE);
    listGet.get(1);
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listGet.get(2));
    // set
    SimpleIntList listSet = new SimpleIntList(0);
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSet.set(0,Integer.MAX_VALUE));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSet.set(-1, Integer.MAX_VALUE));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSet.set(10, Integer.MAX_VALUE));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSet.set(10, Integer.MAX_VALUE));
    listSet.add(Integer.MIN_VALUE);
    listSet.add(Integer.MAX_VALUE);
    listSet.set(1, Integer.MAX_VALUE-1);
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSet.set(2, Integer.MAX_VALUE));
    // remove
    SimpleIntList listRemove = new SimpleIntList(0);
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listRemove.removeFirst());
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listRemove.removeLast());
    // swap
    SimpleIntList listSwap = new SimpleIntList();
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSwap.swap(0, 0));
    listSwap.add(Integer.MIN_VALUE);
    listSwap.add(Integer.MAX_VALUE);
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSwap.swap(0, -1));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSwap.swap(-1, 0));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSwap.swap(0, 2));
    assertThrows(java.lang.ArrayIndexOutOfBoundsException.class, () -> listSwap.swap(2, 0));
    listSwap.swap(0,0);
    listSwap.swap(0,1);
    // sort
    SimpleIntList listSort = new SimpleIntList();
    listSort.add(Integer.MIN_VALUE);
    listSort.add(Integer.MAX_VALUE);
    listSort.sort(null);
  }


    /** Test Iterator in a simple case */
  @Test
  public void testGrowing() {

    SimpleIntList list = new SimpleIntList(80);

    // add many entries
    for (int i = 1; i <= 80; i++) {
      list.add((int) (Math.random() * Integer.MAX_VALUE));
    }
    assertEquals(80, list.size());

    for (int i = 1; i <= 10; i++) {
      list.add((int) (Math.random() * Integer.MAX_VALUE));
    }
    assertEquals(90, list.size());

    list.removeFirst();
    assertEquals(89, list.size());

    for (int i = 1; i <= 10; i++) {
      list.add((int) (Math.random() * Integer.MAX_VALUE));
    }
    assertEquals(99, list.size());
  }

  /** Test Iterator in a simple case */
  @Test
  public void testClone() {

    SimpleIntList list = new SimpleIntList();

    // add many entries
    for (int i = 1; i <= 100; i++) {
      list.add((int) (Math.random() * Integer.MAX_VALUE));
    }

    SimpleIntList clone = list.clone();
    for (int i = 0; i < list.size(); i++) {
      assertEquals(list.get(i), clone.get(i));
    }

    // delete some
    for (int i = 1; i <= 10; i++) {
      list.removeFirst();
    }

    // test again
    SimpleIntList clone2 = list.clone();
    for (int i = 0; i < list.size(); i++) {
      assertEquals(list.get(i), clone2.get(i));
    }
  }

  /** Test Iterator in a simple case */
  @Test
  public void testIterator_simpleIteration() {

    SimpleIntList list = new SimpleIntList();

    // add many entries
    for (int i = 1; i <= 100; i++) {
      list.add((int) (Math.random() * Integer.MAX_VALUE));
    }

    int counter = 0;
    for (int i : list) {
      System.out.println(i);
      counter++;
    }
    assertEquals(list.size(), counter);
  }

  /** Test modifications during Iterator use */
  @Test
  public void testIterator_simpleIterationModification() {

    SimpleIntList list = new SimpleIntList(1024);

    // add many entries
    for (int i = 1; i <= 100; i++) {
      list.add((int) (Math.random() * Integer.MAX_VALUE));
    }

    // remove during iteration
    try {
      for (int i : list) {
        list.removeFirst();
      }
      fail("");
    } catch (ConcurrentModificationException e) {
      // empty
    } catch (Exception e) {
      fail("");
    }

    // remove during iteration
    try {
      for (int i : list) {
        list.removeLast();
      }
      fail("");
    } catch (ConcurrentModificationException e) {
      // empty
    } catch (Exception e) {
      fail("");
    }

    // add during iteration}
    try {
      for (int i : list) {
        list.add(999);
      }
      fail("");
    } catch (ConcurrentModificationException e) {
      // empty
    } catch (Exception e) {
      fail("");
    }

    // change during iteration}
    try {
      int i = 0;
      for (Integer x : list) {
        list.set(i++, 999);
      }
    } catch (ConcurrentModificationException e) {
      fail("");
    }

    for (int x : list) {
      assertEquals(999, x);
    }
  }

  @Test
  public void testPushToHead() {
    SimpleIntList list = new SimpleIntList();

    // add 20 entries
    for (int i = 100; i < 120; i++) {
      list.add(i);
    }

    list.pushToHead(109);

    assertEquals(109, list.get(0));
    assertEquals(100, list.get(9));

    System.out.println(list);
  }

  @Test
  public void testPushToHeadStable() {
    SimpleIntList list = new SimpleIntList();

    // add 20 entries
    for (int i = 100; i < 120; i++) {
      list.add(i);
    }

    assertFalse(list.pushToHeadStable(99));

    assertTrue(list.pushToHeadStable(100));
    assertEquals(100, list.get(0));
    assertEquals(101, list.get(1));
    assertEquals(102, list.get(2));

    assertTrue(list.pushToHeadStable(109));
    assertEquals(109, list.get(0));
    assertEquals(100, list.get(1));
    assertEquals(101, list.get(2));
    assertEquals(108, list.get(9));
    assertEquals(110, list.get(10));

    assertEquals(109,list.removeFirst());
    assertEquals(19, list.size());
    assertTrue(list.pushToHeadStable(113));
    assertEquals(113, list.get(0));
    assertEquals(100, list.get(1));
    assertEquals(114, list.get(13));

    assertEquals(119,list.removeLast());
    assertEquals(18, list.size());
    assertTrue(list.pushToHeadStable(118));
    assertEquals(118, list.get(0));
    assertEquals(113, list.get(1));
    assertEquals(117, list.get(17));

    System.out.println(list);
  }

  @Test
  public void testAddList() {

    SimpleIntList list1 = new SimpleIntList();
    // add 10 entries
    for (int i = 100; i < 110; i++) {
      list1.add(i);
      assertEquals(i - 100 + 1, list1.size());
    }
    int size1 = list1.size();

    SimpleIntList list2 = new SimpleIntList();
    // add 10 entries
    for (int i = 200; i < 210; i++) {
      list2.add(i);
      assertEquals(i - 200 + 1, list2.size());
    }
    int size2 = list2.size();

    list1.add(list2);
    assertEquals(size1 + size2, list1.size());
    assertEquals(100, list1.get(0));
    assertEquals(200, list1.get(10));

  }

  @Test
  public void testAddListFront() {

    SimpleIntList list1 = new SimpleIntList();
    // add 10 entries
    for (int i = 100; i < 110; i++) {
      list1.add(i);
      assertEquals(i - 100 + 1, list1.size());
    }
    int size1 = list1.size();

    SimpleIntList list2 = new SimpleIntList();
    // add 10 entries
    for (int i = 200; i < 210; i++) {
      list2.add(i);
      assertEquals(i - 200 + 1, list2.size());
    }
    int size2 = list2.size();

    list1.addFront(list2);
    assertEquals(size1 + size2, list1.size());
    assertEquals(200, list1.get(0));
    assertEquals(100, list1.get(10));

    list1.add(300);
    assertEquals(300, list1.get(20));
  }

  @Test
  public void testListWithInts() {
    SimpleIntList list = new SimpleIntList();

    // empty list
    testEmptyList(list);

    // add one entry
    int value = 100;
    list.add(value);
    assertEquals(1, list.size());
    assertFalse(list.empty());
    assertEquals(list.get(0), value);
    assertEquals(list.getFirst(), value);
    assertEquals(list.getLast(), value);

    // remove one entry
    int element = list.removeLast();
    assertEquals(element, value);
    testEmptyList(list);
    list.add(value);
    element = list.removeFirst();
    assertEquals(element, value);
    testEmptyList(list);

    // add 10 entries
    for (int i = 100; i < 110; i++) {
      list.add(i);
      assertEquals(list.size(), i - 100 + 1);
      assertFalse(list.empty());
    }

    // get one entry
    assertEquals(104, list.get(4));

    // remove one entry
    element = list.removeLast();
    assertEquals(109, element);
    assertEquals(108, list.getLast());
    element = list.removeFirst();
    assertEquals(100, element);
    assertEquals(101, list.getFirst());
    assertEquals(8, list.size());

    // get one entry
    assertEquals(105, list.get(4));

    // get entry higher than size
    try {
      list.get(11);
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    // get entry lower zero
    try {
      list.get(-1);
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    // add list to list
    SimpleIntList list2 = new SimpleIntList();
    // add 10 entries
    for (int i = 200; i < 210; i++) {
      list2.add(i);
    }
    assertEquals(10, list2.size());

    // remove elements from list2
    element = list2.removeLast();
    assertEquals(209, element);
    assertEquals(208, list2.getLast());
    element = list2.removeFirst();
    assertEquals(200, element);
    assertEquals(201, list2.getFirst());
    assertEquals(8, list2.size());

    // concatenate lists
    list.add(list2);
    assertEquals(16, list.size());

    // add many elements
    list2 = new SimpleIntList();
    // add 10 entries
    for (int i = 200; i < 456; i++) {
      list2.add(i);
    }
    assertEquals(256, list2.size());

    // equals
    list = new SimpleIntList();
    list.add(list2);
    list.removeFirst();
    list2.removeFirst();
    assertEquals(list, list2);
    list2.removeFirst();
    assertNotEquals(list, list2);

    SimpleIntList cloneList = list2.clone();
    assertEquals(cloneList, list2);

    SimpleIntList sortList = new SimpleIntList();
    sortList.add(99);
    sortList.add(50);
    sortList.add(55);
    sortList.add(10);
    sortList.add(5);
    sortList.add(80);
    sortList.sort(Comparator.comparingInt(a -> a));
    cloneList.sort(Comparator.comparingInt(a -> a));

    // clear
    list.clear();
    assertTrue(list.size() == 0 && list.empty());
    list2.clear();
    assertTrue(list2.size() == 0 && list2.empty());

    System.out.println(cloneList);
  }


  /** @param list */
  private static void testEmptyList(SimpleIntList list) {
    // list is empty
    assertEquals(0, list.size());
    assertTrue(list.empty());

    // remove from empty list
    try {
      list.removeFirst();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
    // remove from empty list
    try {
      list.removeLast();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    // retrieve from empty list
    try {
      list.get(0);
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
    try {
      list.getFirst();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
    try {
      list.getLast();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
  }

  /** */
  @Test
  public void testIntStreamFromList() {
    SimpleIntList list = new SimpleIntList();

    // add 10 entries
    IntStream.rangeClosed(0, 100).forEach(list::add);

    System.out.println(list.stream().average());

    list.stream().filter(i -> i % 2 == 0).forEach(System.out::println);
  }

  @Test
  public void testSwap() {
    SimpleIntList list = new SimpleIntList();

    Random r = new Random();
    final IntConsumer intConsumer = (int a) -> list.add(r.nextInt());

    IntStream.rangeClosed(0, 100).forEach(intConsumer);

    int before1 = list.get(3);
    int before2 = list.get(97);
    list.swap(3,97);
    assertEquals(before1, list.get(97));
    assertEquals(before2, list.get(3));

  }

  @Test
  public void testRemoving() {
    SimpleIntList list = new SimpleIntList();

    IntStream.rangeClosed(0, 100).forEach(list::add);

    // test remove first
    int before1 = list.getFirst();
    assertEquals(before1, list.removeFirst());
    assertNotEquals(before1, list.getFirst());

    int before2 = list.getFirst();
    assertTrue(list.remove(before2));
    assertNotEquals(before2, list.getFirst());

    // test remove last
    int before3 = list.getLast();
    assertEquals(before3, list.removeLast());
    assertNotEquals(before3, list.getLast());

    int before4 = list.getLast();
    assertTrue(list.remove(before4));
    assertNotEquals(before4, list.getLast());

    // test remove
    int before5 = list.get(5);
    int beforeSize = list.size();
    assertTrue(list.remove(before5));
    assertNotEquals(before4, list.get(5));
    assertEquals(beforeSize-1, list.size());

  }

    /** */
  @Test
  public void testSorting() {

    SimpleIntList list = new SimpleIntList();

    Random r = new Random();
    final IntConsumer intConsumer = (int a) -> list.add(r.nextInt());

    IntStream.rangeClosed(0, 4000).forEach(intConsumer);
    list.sort(Comparator.comparingInt(a -> a));
    list.forEach(System.out::println);
    assertTrue(isSorted(list.toArray()));

    System.out.println();

    IntStream.rangeClosed(0, 40000).forEach(intConsumer);
    list.sort(Comparator.comparingInt(a -> a));
    list.forEach(System.out::println);
    assertTrue(isSorted(list.toArray()));

  }

  public boolean isSorted(int[] array) {
    int prev = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] < prev) {
        return false;
      }
      prev = array[i];
    }
    return true;
  }

}
