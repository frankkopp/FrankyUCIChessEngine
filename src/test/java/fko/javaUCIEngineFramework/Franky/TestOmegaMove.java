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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank
 *
 */
public class TestOmegaMove {

    @Test
    public void testValidMove() {
        int move = 100;
        assertFalse(OmegaMove.isValid(move));
        move = OmegaMove.createMove(OmegaMoveType.NORMAL, OmegaSquare.e2, OmegaSquare.d3, OmegaPiece.WHITE_PAWN, OmegaPiece.BLACK_KNIGHT, OmegaPiece.NOPIECE);
        assertTrue(OmegaMove.isValid(move));
        move = OmegaMove.createMove(OmegaMoveType.NOMOVETYPE, OmegaSquare.e2, OmegaSquare.d3, OmegaPiece.WHITE_PAWN, OmegaPiece.BLACK_KNIGHT, OmegaPiece.NOPIECE);
        assertFalse(OmegaMove.isValid(move));
        move = OmegaMove.createMove(OmegaMoveType.NOMOVETYPE, OmegaSquare.e2, OmegaSquare.d3, OmegaPiece.NOPIECE, OmegaPiece.BLACK_KNIGHT, OmegaPiece.NOPIECE);
        assertFalse(OmegaMove.isValid(move));
        move = OmegaMove.createMove(OmegaMoveType.NOMOVETYPE, OmegaSquare.j1, OmegaSquare.d3, OmegaPiece.WHITE_PAWN, OmegaPiece.BLACK_KNIGHT, OmegaPiece.NOPIECE);
        assertFalse(OmegaMove.isValid(move));
    }

    @Test
    public void testCreateMove() {
        int move = OmegaMove.createMove(OmegaMoveType.NORMAL, OmegaSquare.e2, OmegaSquare.d3, OmegaPiece.WHITE_PAWN, OmegaPiece.BLACK_KNIGHT, OmegaPiece.NOPIECE);
        System.out.println(OmegaMove.toString(move));
        move = OmegaMove.createMove(OmegaMoveType.PROMOTION, OmegaSquare.e2, OmegaSquare.f1, OmegaPiece.BLACK_PAWN, OmegaPiece.WHITE_ROOK, OmegaPiece.BLACK_QUEEN);
        System.out.println(OmegaMove.toString(move));
        move = OmegaMove.createMove(OmegaMoveType.CASTLING, OmegaSquare.e1, OmegaSquare.g1, OmegaPiece.WHITE_KING, OmegaPiece.NOPIECE, OmegaPiece.NOPIECE);
        System.out.println(OmegaMove.toString(move));
    }


}
