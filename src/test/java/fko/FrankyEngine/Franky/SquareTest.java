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


import fko.FrankyEngine.Franky.Square.File;
import fko.FrankyEngine.Franky.Square.Rank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 *
 */
public class SquareTest {

    /**
     * Tests basic Square operations
     */
    @Test
    public void test() {
        // Square addressing
        assertEquals(Square.getSquare(0), Square.a1);
        assertEquals(Square.getSquare(119), Square.h8);
        assertEquals(Square.getSquare(8), Square.NOSQUARE);
        assertEquals(Square.getSquare(-1), Square.NOSQUARE);
        assertEquals(Square.getSquare(128), Square.NOSQUARE);
        assertTrue(Square.h8.isValidSquare());
        assertFalse(Square.i8.isValidSquare());
        assertFalse(Square.NOSQUARE.isValidSquare());

        // addressing with file and rank
        assertEquals(Square.getSquare(1, 1), Square.a1);
        assertEquals(Square.getSquare(8, 8), Square.h8);
        assertEquals(Square.getSquare(1, 9), Square.NOSQUARE);
        assertEquals(Square.getSquare(0, 8), Square.NOSQUARE);
        assertEquals(Square.getSquare(9, 9), Square.NOSQUARE);

        // getFile
        assertEquals(Square.a1.getFile(), File.a);
        assertEquals(Square.h8.getFile(), File.h);
        assertEquals(Square.j1.getFile(), File.NOFILE);
        assertEquals(Square.getSquare(0).getFile(), File.a);
        assertEquals(Square.getSquare(8).getFile(), File.NOFILE);
        assertEquals(Square.getSquare(128).getFile(), File.NOFILE);

        // getRank
        assertEquals(Square.a1.getRank(), Rank.r1);
        assertEquals(Square.h8.getRank(), Rank.r8);
        assertEquals(Square.j1.getRank(), Rank.NORANK);
        assertEquals(Square.getSquare(0).getRank(), Rank.r1);
        assertEquals(Square.getSquare(8).getRank(), Rank.NORANK);
        assertEquals(Square.getSquare(128).getRank(), Rank.NORANK);

        // base rows
        Square square = Square.a2;
        assertTrue(square.isWhitePawnBaseRow());
        assertFalse(square.isBlackPawnBaseRow());
        assertTrue(square.isPawnBaseRow(Color.WHITE));
        assertFalse(square.isPawnBaseRow(Color.BLACK));
        square = Square.e7;
        assertFalse(square.isWhitePawnBaseRow());
        assertTrue(square.isBlackPawnBaseRow());
        assertFalse(square.isPawnBaseRow(Color.WHITE));
        assertTrue(square.isPawnBaseRow(Color.BLACK));

        // iteration
        int counter = 0;
        for (Square sq : Square.values) {
            if (!sq.isValidSquare()) continue;
            counter++;
        }
        assertEquals(64, counter);

        // access through getValueList()
        List<Square> list = Square.getValueList();
        assertEquals(64, list.size());
        assertEquals(list.get(0), Square.a1);
        assertEquals(list.get(63), Square.h8);

        // check order by creating string
        StringBuilder sb = new StringBuilder();
        list.forEach(sb::append);
        assertEquals(
          "a1b1c1d1e1f1g1h1a2b2c2d2e2f2g2h2a3b3c3d3e3f3g3h3a4b4c4d4e4f4g4h4a5b5c5d5e5f5g5h5a6b6c6d6e6f6g6h6a7b7c7d7e7f7g7h7a8b8c8d8e8f8g8h8",
          sb.toString());

        counter = 0;
        for (Square.File f : Square.File.values()) {
            if (f == File.NOFILE) continue;
            counter++;
        }
        assertEquals(8, counter);

        counter = 0;
        for (Square.Rank r : Square.Rank.values()) {
            if (r == Rank.NORANK) continue;
            counter++;
        }
        assertEquals(8, counter);

    }

    @Test
    public void testDirections() {
        Square e4 = Square.e4;
        assertSame(e4.getNorth(), Square.e5);
        assertSame(e4.getSouth(), Square.e3);
        assertSame(e4.getEast(), Square.f4);
        assertSame(e4.getWest(), Square.d4);
    }

    @Test
    public void index64Test() {
        assertEquals(0, Square.a1.index64);
        assertEquals(7, Square.h1.index64);
        assertEquals(8, Square.a2.index64);
        assertEquals(56, Square.a8.index64);
        assertEquals(63, Square.h8.index64);
        assertEquals(-1, Square.i1.index64);
        assertEquals(-1, Square.p8.index64);
    }

}
