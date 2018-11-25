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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 *
 */
public class TestOmegaMoveList {

    @Test
    public void testListWithInts() {
        OmegaMoveList list = new OmegaMoveList();

        // empty list
        testEmptyList(list);

        // add one entry
        int move1 = OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.b1,
                OmegaSquare.c3,
                OmegaPiece.WHITE_KNIGHT,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
                );

        int value = move1;

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
        for (int i=100; i<110; i++) {
            int move = OmegaMove.createMove(
                    OmegaMoveType.NORMAL,
                    OmegaSquare.getValueList().get(i-100),
                    OmegaSquare.getValueList().get(i-100),
                    OmegaPiece.WHITE_PAWN,
                    OmegaPiece.NOPIECE,
                    OmegaPiece.NOPIECE
                    );
            list.add(move);
            assertEquals(list.size(), i - 100 + 1);
            assertFalse(list.empty());
        }

        // get one entry
        assertEquals(list.get(4), OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.values[4],
                OmegaSquare.values[4],
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
        ));

        // remove one entry
        element = list.removeLast();
        assertEquals(element, OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.b2,
                OmegaSquare.b2,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
        ));
        assertEquals(list.getLast(), OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.a2,
                OmegaSquare.a2,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
        ));
        element = list.removeFirst();
        assertEquals(element, OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.a1,
                OmegaSquare.a1,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
        ));
        assertEquals(list.getFirst(), OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.b1,
                OmegaSquare.b1,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
        ));
        assertEquals(8, list.size());

        // get one entry
        assertEquals(list.get(4), OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.f1,
                OmegaSquare.f1,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
        ));

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

    }

    /**
     * @param list
     */
    private static void testEmptyList(OmegaMoveList list) {
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

}
