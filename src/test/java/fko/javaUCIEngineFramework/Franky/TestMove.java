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
public class TestMove {

    @Test
    public void testValidMove() {
        int move = 100;
        assertFalse(Move.isValid(move));
        move = Move.createMove(MoveType.NORMAL, Square.e2, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertTrue(Move.isValid(move));
        move = Move.createMove(MoveType.NOMOVETYPE, Square.e2, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertFalse(Move.isValid(move));
        move = Move.createMove(MoveType.NOMOVETYPE, Square.e2, Square.d3, Piece.NOPIECE, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertFalse(Move.isValid(move));
        move = Move.createMove(MoveType.NOMOVETYPE, Square.j1, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertFalse(Move.isValid(move));
    }

    @Test
    public void testCreateMove() {
        int move = Move.createMove(MoveType.NORMAL, Square.e2, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        System.out.println(Move.toString(move));
        move = Move.createMove(MoveType.PROMOTION, Square.e2, Square.f1, Piece.BLACK_PAWN, Piece.WHITE_ROOK, Piece.BLACK_QUEEN);
        System.out.println(Move.toString(move));
        move = Move.createMove(MoveType.CASTLING, Square.e1, Square.g1, Piece.WHITE_KING, Piece.NOPIECE, Piece.NOPIECE);
        System.out.println(Move.toString(move));
    }


}
