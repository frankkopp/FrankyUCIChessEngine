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

package fko.javaUCIEngineFramework.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** @author Frank */
public class TestSimpleIntList {

  private SimpleIntList _list_1;

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
    sortList.sort((a, b) -> a - b);

    cloneList.sort((a, b) -> a - b);

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
  public void testTiming() {

    prepare();

    int ROUNDS = 5;
    int DURATION = 5;

    int ITERATIONS = 0;

    Instant start;

    System.out.println("Running Timing Test Test 1 vs. Test 2");

    for (int j = 0; j < ROUNDS; j++) {

      System.gc();

      start = Instant.now();
      ITERATIONS = 0;
      for (; ; ) {
        ITERATIONS++;
        test1();
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) break;
      }
      System.out.println(String.format("Test 1 (copy): %,7d runs/s", ITERATIONS / DURATION));

      start = Instant.now();
      ITERATIONS = 0;
      for (; ; ) {
        ITERATIONS++;
        test2();
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) break;
      }
      System.out.println(String.format("Test 2 (clone): %,7d runs/s", ITERATIONS / DURATION));
    }
  }

  /** */
  private void prepare() {
    _list_1 = new SimpleIntList(1024);
    // add many entries
    for (int i = 0; i < 1024; i++) {
      _list_1.add((int) (Math.random() * Integer.MAX_VALUE));
    }
  }

  @SuppressWarnings("unused")
  private void test1() {
    SimpleIntList copy = new SimpleIntList(_list_1, false);
    assertEquals(_list_1, copy);
  }

  @SuppressWarnings("unused")
  private void test2() {
    SimpleIntList copy = new SimpleIntList(_list_1, true);
    assertEquals(_list_1, copy);
  }

}
