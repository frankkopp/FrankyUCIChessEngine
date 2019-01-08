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

/**
 * @author Frank
 *
 */
public class SquareListTest {

    static SquareList list;

    /**
     * @throws Exception
     */
    @BeforeAll
    static public void setUp() throws Exception {

        list = new SquareList();

    }

    /**
     *
     */
    @Test
    public final void testAdd() {

        System.out.println(list);
        list.add(Square.a8);
        System.out.println(list);
        list.add(Square.b2);
        System.out.println(list);
        list.add(Square.a3);
        System.out.println(list);
        list.add(Square.c1);
        System.out.println(list);
        list.add(Square.a1);
        System.out.println(list);

        System.out.println(list);
        list.remove(Square.a4);
        System.out.println(list);
        list.remove(Square.a1);
        System.out.println(list);
        list.remove(Square.b2);
        System.out.println(list);
        list.remove(Square.a3);
        System.out.println(list);


        list.remove(Square.a4);
        System.out.println(list);

        list.remove(Square.a2);
        System.out.println(list);

    }

    /**
     *
     */
    @Test
    public final void testIterate() {

        list.add(Square.a1);
        list.add(Square.a2);
        list.add(Square.a3);
        list.add(Square.a4);

        System.out.println(list);
        for (int i=0; i<list.size(); i++) {
            System.out.print(list.get(i) + " ");
            // remove and add element - should not influence the iteration
            // (we had a bug because of this)
            list.remove(Square.a1);
            list.add(Square.a1);
        }
        System.out.println();
        System.out.println(list);

        list.remove(Square.a1);
        list.remove(Square.a3);

        System.out.println(list);
        for (int i=0; i<list.size(); i++) {
            System.out.print(list.get(i) + " ");
        }
        System.out.println();
    }


}
