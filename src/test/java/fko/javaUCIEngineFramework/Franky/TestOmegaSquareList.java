/**
 * The MIT License (MIT)
 *
 * "Chessly by Frank Kopp"
 *
 * mail-to:frank@familie-kopp.de
 *
 * Copyright (c) 2016 Frank Kopp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package fko.chessly.player.computer.Omega;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Frank
 *
 */
public class TestOmegaSquareList {

    static OmegaSquareList list;

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static public void setUp() throws Exception {

        list = new OmegaSquareList();

    }

    /**
     *
     */
    @Test
    public final void testAdd() {

        System.out.println(list);
        list.add(OmegaSquare.a8);
        System.out.println(list);
        list.add(OmegaSquare.b2);
        System.out.println(list);
        list.add(OmegaSquare.a3);
        System.out.println(list);
        list.add(OmegaSquare.c1);
        System.out.println(list);
        list.add(OmegaSquare.a1);
        System.out.println(list);

        System.out.println(list);
        list.remove(OmegaSquare.a4);
        System.out.println(list);
        list.remove(OmegaSquare.a1);
        System.out.println(list);
        list.remove(OmegaSquare.b2);
        System.out.println(list);
        list.remove(OmegaSquare.a3);
        System.out.println(list);


        list.remove(OmegaSquare.a4);
        System.out.println(list);

        list.remove(OmegaSquare.a2);
        System.out.println(list);

    }

    /**
     *
     */
    @Test
    public final void testIterate() {

        list.add(OmegaSquare.a1);
        list.add(OmegaSquare.a2);
        list.add(OmegaSquare.a3);
        list.add(OmegaSquare.a4);

        System.out.println(list);
        for (int i=0; i<list.size(); i++) {
            System.out.print(list.get(i) + " ");
            // remove and add element - should not influence the iteration
            // (we had a bug because of this)
            list.remove(OmegaSquare.a1);
            list.add(OmegaSquare.a1);
        }
        System.out.println();
        System.out.println(list);

        list.remove(OmegaSquare.a1);
        list.remove(OmegaSquare.a3);

        System.out.println(list);
        for (int i=0; i<list.size(); i++) {
            System.out.print(list.get(i) + " ");
        }
        System.out.println();
    }


}
