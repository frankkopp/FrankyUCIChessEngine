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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static fko.FrankyEngine.Franky.Square.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Frank
 *
 */
public class SquareListTest {

  static SquareList list;

  @BeforeAll
  static public void setUp() {
    list = new SquareList();
  }

  @Test
  public final void testAdd() {

    System.out.println(list);
    list.add(a8);
    System.out.println(list);
    list.add(b2);
    System.out.println(list);
    list.add(a3);
    System.out.println(list);
    list.add(c1);
    System.out.println(list);
    list.add(a1);
    System.out.println(list);

    assertEquals(5, list.size());
    assertEquals(a1, list.get(3));

    System.out.println(list);
    list.remove(a4);
    System.out.println(list);
    list.remove(a1);
    System.out.println(list);
    list.remove(b2);
    System.out.println(list);
    list.remove(a3);
    System.out.println(list);

    list.remove(a4);
    System.out.println(list);

    list.remove(a2);
    System.out.println(list);

  }

  @Test
  public final void testIterate() {

    list.add(a1);
    list.add(a2);
    list.add(a3);
    list.add(a4);

    System.out.println(list);
    for (int i = 0; i < list.size(); i++) {
      System.out.print(list.get(i) + " ");
      // remove and add element - should not influence the iteration
      // (had a bug because of this)
      list.remove(a1);
      list.add(a1);
    }
    System.out.println();
    System.out.println(list);

    list.remove(a1);
    list.remove(a3);

    System.out.println(list);
    for (int i = 0; i < list.size(); i++) {
      System.out.print(list.get(i) + " ");
    }
    System.out.println();
  }

}
