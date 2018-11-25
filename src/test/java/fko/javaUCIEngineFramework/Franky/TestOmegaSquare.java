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


import org.junit.jupiter.api.Test;
import fko.javaUCIEngineFramework.Franky.OmegaSquare.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 *
 */
public class TestOmegaSquare {

    /**
     * Tests basic OmegaSquare operations
     */
    @Test
    public void test() {
        // Square addressing
        assertEquals(OmegaSquare.getSquare(0), OmegaSquare.a1);
        assertEquals(OmegaSquare.getSquare(119), OmegaSquare.h8);
        assertEquals(OmegaSquare.getSquare(8), OmegaSquare.NOSQUARE);
        assertEquals(OmegaSquare.getSquare(-1), OmegaSquare.NOSQUARE);
        assertEquals(OmegaSquare.getSquare(128), OmegaSquare.NOSQUARE);
        assertTrue(OmegaSquare.h8.isValidSquare());
        assertFalse(OmegaSquare.i8.isValidSquare());
        assertFalse(OmegaSquare.NOSQUARE.isValidSquare());

        // addressing with file and rank
        assertEquals(OmegaSquare.getSquare(1, 1), OmegaSquare.a1);
        assertEquals(OmegaSquare.getSquare(8, 8), OmegaSquare.h8);
        assertEquals(OmegaSquare.getSquare(1, 9), OmegaSquare.NOSQUARE);
        assertEquals(OmegaSquare.getSquare(0, 8), OmegaSquare.NOSQUARE);
        assertEquals(OmegaSquare.getSquare(9, 9), OmegaSquare.NOSQUARE);

        // getFile
        assertEquals(OmegaSquare.a1.getFile(), File.a);
        assertEquals(OmegaSquare.h8.getFile(), File.h);
        assertEquals(OmegaSquare.j1.getFile(), File.NOFILE);
        assertEquals(OmegaSquare.getSquare(0).getFile(), File.a);
        assertEquals(OmegaSquare.getSquare(8).getFile(), File.NOFILE);
        assertEquals(OmegaSquare.getSquare(128).getFile(), File.NOFILE);

        // getRank
        assertEquals(OmegaSquare.a1.getRank(), Rank.r1);
        assertEquals(OmegaSquare.h8.getRank(), Rank.r8);
        assertEquals(OmegaSquare.j1.getRank(), Rank.NORANK);
        assertEquals(OmegaSquare.getSquare(0).getRank(), Rank.r1);
        assertEquals(OmegaSquare.getSquare(8).getRank(), Rank.NORANK);
        assertEquals(OmegaSquare.getSquare(128).getRank(), Rank.NORANK);

        // base rows
        OmegaSquare square = OmegaSquare.a2;
        assertTrue(square.isWhitePawnBaseRow());
        assertFalse(square.isBlackPawnBaseRow());
        assertTrue(square.isPawnBaseRow(OmegaColor.WHITE));
        assertFalse(square.isPawnBaseRow(OmegaColor.BLACK));
        square = OmegaSquare.e7;
        assertFalse(square.isWhitePawnBaseRow());
        assertTrue(square.isBlackPawnBaseRow());
        assertFalse(square.isPawnBaseRow(OmegaColor.WHITE));
        assertTrue(square.isPawnBaseRow(OmegaColor.BLACK));

        // iteration
        int counter = 0;
        for ( OmegaSquare sq : OmegaSquare.values ) {
            if (!sq.isValidSquare()) continue;
            counter++;
        }
        assertEquals(64, counter);

        // access through getValueList()
        List<OmegaSquare> list = OmegaSquare.getValueList();
        assertEquals(64, list.size());
        assertEquals(list.get(0), OmegaSquare.a1);
        assertEquals(list.get(63), OmegaSquare.h8);

        // check order by creating string
        StringBuilder sb = new StringBuilder();
        list.forEach(c -> sb.append(c));
        assertEquals("a1b1c1d1e1f1g1h1a2b2c2d2e2f2g2h2a3b3c3d3e3f3g3h3a4b4c4d4e4f4g4h4a5b5c5d5e5f5g5h5a6b6c6d6e6f6g6h6a7b7c7d7e7f7g7h7a8b8c8d8e8f8g8h8", sb.toString());

        counter = 0;
        for ( OmegaSquare.File f : OmegaSquare.File.values() ) {
            if (f == File.NOFILE) continue;
            counter++;
        }
        assertEquals(8, counter);

        counter = 0;
        for ( OmegaSquare.Rank r : OmegaSquare.Rank.values() ) {
            if (r == Rank.NORANK) continue;
            counter++;
        }
        assertEquals(8, counter);

    }

    /**
     *
     */
    @Test
    public void testDirections() {
        OmegaSquare e4 = OmegaSquare.e4;
        assertSame(e4.getNorth(), OmegaSquare.e5);
        assertSame(e4.getSouth(), OmegaSquare.e3);
        assertSame(e4.getEast(), OmegaSquare.f4);
        assertSame(e4.getWest(), OmegaSquare.d4);
    }
}
