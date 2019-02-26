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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 *
 */
public class MoveTest {

  private static final Logger LOG = LoggerFactory.getLogger(MoveTest.class);

  @Test
  public void testValidMove() {
    int move = 100;
    assertFalse(Move.isValid(move));

    move = Move.createMove(MoveType.NORMAL, Square.e2, Square.d3, Piece.WHITE_PAWN,
                           Piece.BLACK_KNIGHT, Piece.NOPIECE);
    assertTrue(Move.isValid(move));

    move = Move.createMove(MoveType.NOMOVETYPE, Square.e2, Square.d3, Piece.WHITE_PAWN,
                           Piece.BLACK_KNIGHT, Piece.NOPIECE);
    assertFalse(Move.isValid(move));

    move = Move.createMove(MoveType.NOMOVETYPE, Square.e2, Square.d3, Piece.NOPIECE,
                           Piece.BLACK_KNIGHT, Piece.NOPIECE);
    assertFalse(Move.isValid(move));

    move = Move.createMove(MoveType.NOMOVETYPE, Square.NOSQUARE, Square.d3, Piece.WHITE_PAWN,
                           Piece.BLACK_KNIGHT, Piece.NOPIECE);
    assertFalse(Move.isValid(move));
  }

  @Test
  public void testCreateMove() {
    int move = Move.createMove(MoveType.NORMAL, Square.e2, Square.d3, Piece.WHITE_PAWN,
                               Piece.BLACK_KNIGHT, Piece.NOPIECE);
    LOG.debug("{}", Move.toString(move));
    assertEquals("NORMAL Pe2xnd3", Move.toString(move));
    move = Move.createMove(MoveType.PROMOTION, Square.e2, Square.f1, Piece.BLACK_PAWN,
                           Piece.WHITE_ROOK, Piece.BLACK_QUEEN);
    LOG.debug("{}", Move.toString(move));
    assertEquals("PROMOTION pe2xRf1q", Move.toString(move));
    move = Move.createMove(MoveType.CASTLING, Square.e1, Square.g1, Piece.WHITE_KING, Piece.NOPIECE,
                           Piece.NOPIECE);
    LOG.debug("{}", Move.toString(move));
    assertEquals("O-O", Move.toString(move));
  }

}
