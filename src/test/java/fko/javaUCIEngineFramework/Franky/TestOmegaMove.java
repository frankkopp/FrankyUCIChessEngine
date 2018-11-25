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

import fko.chessly.game.GameBoard;
import fko.chessly.game.GameBoardImpl;
import fko.chessly.game.GameMove;
import fko.chessly.game.NotationHelper;
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

    @Test
    public void testConvertMove() {
        GameBoard gameBoard = new GameBoardImpl();
        String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";

        // normal
        gameBoard = new GameBoardImpl(testFen);
        GameMove gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"c4-a4");
        int move = OmegaMove.convertFromGameMove(gameMove);
        GameMove convertedMove = OmegaMove.convertToGameMove(move);
        assert(gameMove.equals(convertedMove));
        System.out.println(OmegaMove.toString(move));
        System.out.println(convertedMove);
        System.out.println();

        // pawn double
        testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
        gameBoard = new GameBoardImpl(testFen);
        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"h7-h5");
        move = OmegaMove.convertFromGameMove(gameMove);
        convertedMove = OmegaMove.convertToGameMove(move);
        assert(gameMove.equals(convertedMove));
        System.out.println(OmegaMove.toString(move));
        System.out.println(convertedMove);
        System.out.println();

        // castling
        testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
        gameBoard = new GameBoardImpl(testFen);
        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"e8-g8");
        move = OmegaMove.convertFromGameMove(gameMove);
        convertedMove = OmegaMove.convertToGameMove(move);
        assert(gameMove.equals(convertedMove));
        System.out.println(OmegaMove.toString(move));
        System.out.println(convertedMove);
        System.out.println();

        // promotion
        testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
        gameBoard = new GameBoardImpl(testFen);
        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"a2-a1");
        move = OmegaMove.convertFromGameMove(gameMove);
        convertedMove = OmegaMove.convertToGameMove(move);
        assert(gameMove.equals(convertedMove));
        System.out.println(OmegaMove.toString(move));
        System.out.println(convertedMove);
        System.out.println();

        // en passant
        testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
        gameBoard = new GameBoardImpl(testFen);
        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"f4-e3");
        move = OmegaMove.convertFromGameMove(gameMove);
        convertedMove = OmegaMove.convertToGameMove(move);
        assert(gameMove.equals(convertedMove));
        System.out.println(OmegaMove.toString(move));
        System.out.println(convertedMove);
        System.out.println();

    }

}
