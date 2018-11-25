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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Frank
 *
 */
public class TestOmegaMoveList {

    /**
     *
     */
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
        assertTrue(list.size()==1);
        assertFalse(list.empty());
        assertTrue(list.get(0)==value);
        assertTrue(list.getFirst()==value);
        assertTrue(list.getLast()==value);

        // remove one entry
        int element = list.removeLast();
        assertTrue(element==value);
        testEmptyList(list);
        list.add(value);
        element = list.removeFirst();
        assertTrue(element==value);
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
            assertTrue(list.size()==i-100+1);
            assertFalse(list.empty());
        }

        // get one entry
        assertTrue(list.get(4)==OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.values[4],
                OmegaSquare.values[4],
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
                ));

        // remove one entry
        element = list.removeLast();
        assertTrue(element==OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.b2,
                OmegaSquare.b2,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
                ));
        assertTrue(list.getLast()==OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.a2,
                OmegaSquare.a2,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
                ));
        element = list.removeFirst();
        assertTrue(element==OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.a1,
                OmegaSquare.a1,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
                ));
        assertTrue(list.getFirst()==OmegaMove.createMove(
                OmegaMoveType.NORMAL,
                OmegaSquare.b1,
                OmegaSquare.b1,
                OmegaPiece.WHITE_PAWN,
                OmegaPiece.NOPIECE,
                OmegaPiece.NOPIECE
                ));
        assertTrue(list.size()==8);

        // get one entry
        assertTrue(list.get(4)==OmegaMove.createMove(
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
        assertTrue(list.size()==0);
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
