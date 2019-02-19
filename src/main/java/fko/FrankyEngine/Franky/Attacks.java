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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Attacks
 *
 * This class determines all attacks for a given position and stores in various
 * bitboards and fields.
 */
public class Attacks {

  private static final Logger LOG = LoggerFactory.getLogger(Attacks.class);

  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  // the position we have been analysing for attacks
  private Position position           = null;
  private long     positionZobristKey = 0L;

  private boolean hasCheck = false;

  long[][] attacksTo = new long[2][64];

  long[][] attacksFrom = new long[2][64];
  long[]   allAttacks  = new long[2];
  int[]    mobility    = new int[2];

  public Attacks() {
  }

  public void computeAttacks(Position position) {
    // if we already computed this position we can simply return
    if (positionZobristKey == position.getZobristKey()) return;

    // new position - reset values and start
    resetAttacks();
    positionZobristKey = position.getZobristKey();
    this.position = position;

    List<Square> validSquares = Square.validSquares;
    for (int i = 0, validSquaresSize = validSquares.size(); i < validSquaresSize; i++) {

      final Square square = validSquares.get(i);
      final Piece pc = position.getPiece(square);
      final int sIdx = square.getIndex64();
      final long[] whitePieces = position.getPiecesBitboards(Color.WHITE);
      final long[] blackPieces = position.getPiecesBitboards(Color.WHITE);

      //      System.out.println("Square: " + square);

      // AttacksTo    @formatter:off
      // ===========================

      // Non-Sliding
      attacksTo[WHITE][sIdx] |= Bitboard.pawnAttacks[BLACK][sIdx] & whitePieces[PieceType.PAWN.ordinal()];
      attacksTo[BLACK][sIdx] |= Bitboard.pawnAttacks[WHITE][sIdx] & blackPieces[PieceType.PAWN.ordinal()];
      attacksTo[WHITE][sIdx] |= Bitboard.knightAttacks[sIdx] & whitePieces[PieceType.KNIGHT.ordinal()];
      attacksTo[BLACK][sIdx] |= Bitboard.knightAttacks[sIdx] & blackPieces[PieceType.KNIGHT.ordinal()];
      attacksTo[WHITE][sIdx] |= Bitboard.kingAttacks[sIdx] & whitePieces[PieceType.KING.ordinal()];
      attacksTo[BLACK][sIdx] |= Bitboard.kingAttacks[sIdx] & blackPieces[PieceType.KING.ordinal()];

      // Sliding - test reverse from the target square outgoing
      // does not matter for non pawns
      // rooks and queens
      final long rqMoves = Bitboard.getSlidingMovesRank(square, position) | Bitboard.getSlidingMovesFile(square, position);
      attacksTo[WHITE][sIdx] |= rqMoves & (whitePieces[PieceType.ROOK.ordinal()] | whitePieces[PieceType.QUEEN.ordinal()]);
      attacksTo[BLACK][sIdx] |= rqMoves & (blackPieces[PieceType.ROOK.ordinal()] | blackPieces[PieceType.QUEEN.ordinal()]);

      // bishop and queens
      final long bqMoves = Bitboard.getSlidingMovesDiagUp(square, position) | Bitboard.getSlidingMovesDiagDown(square, position);
      attacksTo[WHITE][sIdx] |= bqMoves & (whitePieces[PieceType.BISHOP.ordinal()] | whitePieces[PieceType.QUEEN.ordinal()]);
      attacksTo[BLACK][sIdx] |= bqMoves & (blackPieces[PieceType.BISHOP.ordinal()] | blackPieces[PieceType.QUEEN.ordinal()]);

      // TODO en passant

      // System.out.println("Black Attacks\n" + Bitboard.toString(attacksTo[BLACK][sIdx]));
      // System.out.println("White Attacks\n" + Bitboard.toString(attacksTo[WHITE][sIdx]));
      // @formatter:on

      // skip rest if there is no piece on square
      if (pc == Piece.NOPIECE) continue;

      final int color = pc.getColor().ordinal();
      final PieceType type = pc.getType();

      // AllAttacks, AttacksFrom, mobility
      long tmpAttacks;
      switch (type) {
        case PAWN:
          tmpAttacks = Bitboard.pawnAttacks[color][sIdx];
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          break;
        case KNIGHT:
          tmpAttacks = Bitboard.knightAttacks[sIdx];
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case ROOK:
          tmpAttacks = rqMoves;
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case BISHOP:
          tmpAttacks = bqMoves;
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case QUEEN:
          tmpAttacks = rqMoves | bqMoves;
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case KING:
          tmpAttacks = Bitboard.kingAttacks[sIdx];
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          break;
        case NOTYPE:
          break;
        default:
          break;
      }
    }

    // pre-compute if position has check
    hasCheck = (allAttacks[position.getOpponent().ordinal()] & position.getPiecesBitboards(
      position.getNextPlayer().ordinal(), PieceType.KING)) != 0;
  }

  public static long attacksTo(Position position, Square square, Color color) { // @formatter:off

    final int sIdx = square.getIndex64();
    final int opponent = color.getInverseColor().ordinal();

    final long[] piecesBitboards = position.getPiecesBitboards(color);

    long attacksToTemp = 0L;

    // Non-Sliding
    attacksToTemp |= Bitboard.pawnAttacks[opponent][sIdx] & piecesBitboards[PieceType.PAWN.ordinal()];
    attacksToTemp |= Bitboard.knightAttacks[sIdx] & piecesBitboards[PieceType.KNIGHT.ordinal()];
    attacksToTemp |= Bitboard.kingAttacks[sIdx] & piecesBitboards[PieceType.KING.ordinal()];

    // Sliding
    // test reverse from the target square outgoing
    // does not matter for non pawns
    // rooks and queens
    final long rqMoves = Bitboard.getSlidingMovesRank(square, position) | Bitboard.getSlidingMovesFile(square, position);
    attacksToTemp |= rqMoves & (piecesBitboards[PieceType.ROOK.ordinal()] | piecesBitboards[PieceType.QUEEN.ordinal()]);

    // bishop and queens
    final long bqMoves = Bitboard.getSlidingMovesDiagUp(square, position) | Bitboard.getSlidingMovesDiagDown(square, position);
    attacksToTemp |= bqMoves & (piecesBitboards[PieceType.BISHOP.ordinal()] | piecesBitboards[PieceType.QUEEN.ordinal()]);

    return attacksToTemp;

  } // @formatter:on


  public boolean hasCheck() {
    checkPosition();
    return hasCheck;
  }

  public boolean isAttacked(Square square) {
    checkPosition();
    // TODO
    return false;
  }

  public boolean isAttackedBy(Color attacker, Square square) {
    checkPosition();
    // TODO
    return false;
  }

  public void resetAttacks() {
    this.position = null;
    positionZobristKey = 0L;
    hasCheck = false;
    attacksTo = new long[2][64];
    attacksFrom = new long[2][64];
    allAttacks = new long[2];
    mobility = new int[2];
  }

  private void checkPosition() {
    if (position == null) {
      IllegalStateException e = new IllegalStateException(
        "Position not set. Call computeAttacks first.");
      LOG.error(e.getMessage(), e);
      throw e;
    }
  }

  /* OLD VERSION */
  public void computeAttacks2(Position position) {
    // if we already computed this position we can simply return
    if (positionZobristKey == position.getZobristKey()) return;

    // new position - reset values and start
    resetAttacks();
    this.position = position;

    List<Square> validSquares = Square.validSquares;
    for (int i = 0, validSquaresSize = validSquares.size(); i < validSquaresSize; i++) {

      final Square square = validSquares.get(i);
      final Piece pc = position.getPiece(square);
      final int sIdx = square.getIndex64();
      final long allOccupiedBitboard = position.getAllOccupiedBitboard();
      final long[] whitePieces = position.getPiecesBitboards(Color.WHITE);
      final long[] blackPieces = position.getPiecesBitboards(Color.WHITE);

      //      System.out.println("Square: " + square);

      // AttacksTo    @formatter:off
      // ===================
      // Non-Sliding
      attacksTo[WHITE][sIdx] |= Bitboard.pawnAttacks[BLACK][sIdx]
                                & whitePieces[PieceType.PAWN.ordinal()];
      attacksTo[BLACK][sIdx] |= Bitboard.pawnAttacks[WHITE][sIdx]
                                & blackPieces[PieceType.PAWN.ordinal()];
      attacksTo[WHITE][sIdx] |= Bitboard.knightAttacks[sIdx]
                                & whitePieces[PieceType.KNIGHT.ordinal()];
      attacksTo[BLACK][sIdx] |= Bitboard.knightAttacks[sIdx]
                                & blackPieces[PieceType.KNIGHT.ordinal()];
      attacksTo[WHITE][sIdx] |= Bitboard.kingAttacks[sIdx]
                                & whitePieces[PieceType.KING.ordinal()];
      attacksTo[BLACK][sIdx] |= Bitboard.kingAttacks[sIdx]
                                & blackPieces[PieceType.KING.ordinal()];
      // Sliding - test reverse from the target square outgoing
      // does not matter for non pawns
      // rooks and queens
      final long rqMoves = Bitboard.getSlidingMovesRank(square, allOccupiedBitboard)
                           | Bitboard.getSlidingMovesFile(square, allOccupiedBitboard);
      attacksTo[WHITE][sIdx] |= rqMoves &
                                 (whitePieces[PieceType.ROOK.ordinal()]
                                  | whitePieces[PieceType.QUEEN.ordinal()]);
      attacksTo[BLACK][sIdx] |= rqMoves &
                                 (blackPieces[PieceType.ROOK.ordinal()]
                                  | blackPieces[PieceType.QUEEN.ordinal()]);

      // bishop and queens
      final long bqMoves = Bitboard.getSlidingMovesDiagUp(square, allOccupiedBitboard)
                      | Bitboard.getSlidingMovesDiagDown(square, allOccupiedBitboard);
      attacksTo[WHITE][sIdx] |= bqMoves & (whitePieces[PieceType.BISHOP.ordinal()]
                                | whitePieces[PieceType.QUEEN.ordinal()]);
      attacksTo[BLACK][sIdx] |= bqMoves & (blackPieces[PieceType.BISHOP.ordinal()]
                                | blackPieces[PieceType.QUEEN.ordinal()]);

      // System.out.println("Black Attacks\n" + Bitboard.toString(attacksTo[BLACK][sIdx]));
      // System.out.println("White Attacks\n" + Bitboard.toString(attacksTo[WHITE][sIdx]));
      // @formatter:on

      // skip rest if there is no piece on square
      if (pc == Piece.NOPIECE) continue;

      final int color = pc.getColor().ordinal();
      final PieceType type = pc.getType();

      // AllAttacks, AttacksFrom, mobility
      long tmpAttacks;
      switch (type) {
        case PAWN:
          tmpAttacks = Bitboard.pawnAttacks[color][sIdx];
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          break;
        case KNIGHT:
          tmpAttacks = Bitboard.knightAttacks[sIdx];
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case ROOK:
          tmpAttacks = rqMoves;
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case BISHOP:
          tmpAttacks = bqMoves;
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case QUEEN:
          tmpAttacks = rqMoves | bqMoves;
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          mobility[color] += Long.bitCount(tmpAttacks);
          break;
        case KING:
          tmpAttacks = Bitboard.kingAttacks[sIdx];
          allAttacks[color] |= tmpAttacks;
          attacksFrom[color][sIdx] |= tmpAttacks;
          break;
        case NOTYPE:
          break;
        default:
          break;
      }
    }

    // pre-compute if position has check
    hasCheck = (allAttacks[position.getOpponent().ordinal()] & position.getPiecesBitboards(
      position.getNextPlayer().ordinal(), PieceType.KING)) != 0;
  }
}
