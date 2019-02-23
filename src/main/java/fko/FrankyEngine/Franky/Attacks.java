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

import static fko.FrankyEngine.Franky.Square.NOSQUARE;

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

  /**
   * Determines all attacks to a specific square and returns them as a bitboard of attackers
   * @param position
   * @param square
   * @param color
   * @return bitboard with all attackers
   */
  public static long attacksTo(Position position, Square square, Color color) {

    final int sIdx = square.getIndex64();
    final int opponent = color.inverse().ordinal();

    final long[] piecesBitboards = position.getPiecesBitboards(color);

    long attacksToTemp = 0L;

    // Non-Sliding
    attacksToTemp |= Bitboard.pawnAttacks[opponent][sIdx]
      & piecesBitboards[PieceType.PAWN.ordinal()];
    attacksToTemp |= Bitboard.knightAttacks[sIdx] & piecesBitboards[PieceType.KNIGHT.ordinal()];
    attacksToTemp |= Bitboard.kingAttacks[sIdx] & piecesBitboards[PieceType.KING.ordinal()];

    // Sliding
    // test reverse from the target square outgoing
    // does not matter for non pawns
    // rooks and queens
    final long rqMoves = Bitboard.getSlidingMovesRank(square, position)
      | Bitboard.getSlidingMovesFile(square, position);
    attacksToTemp |= rqMoves & (piecesBitboards[PieceType.ROOK.ordinal()]
      | piecesBitboards[PieceType.QUEEN.ordinal()]);

    // bishop and queens
    final long bqMoves = Bitboard.getSlidingMovesDiagUp(square, position)
      | Bitboard.getSlidingMovesDiagDown(square, position);
    attacksToTemp |= bqMoves & (piecesBitboards[PieceType.BISHOP.ordinal()]
      | piecesBitboards[PieceType.QUEEN.ordinal()]);

    return attacksToTemp;

  }

  // max 32 captures with 32 pieces
  private static int[] gain = new int[32];

  /**
   * Evaluates the SEE score for the given move which has not been made on the position
   * yet.<p>
   * En-passant captures will always return a score of +100 and should therefore not be
   * cut-off.
   *
   * Credit; https://www.chessprogramming.org/SEE_-_The_Swap_Algorithm
   *
   * @param position
   * @param move
   * @return
   */
  public static int see(Position position, int move) {

    // en passants are ignored in a sense that it will be winning
    // capture and therefore should lead to no cut-offs when using see()
    if (Move.getMoveType(move) == MoveType.ENPASSANT) return 100;

    // no captures done with this move types -> return 0
    Piece capturedPiece = Move.getTarget(move);
    if (capturedPiece == Piece.NOPIECE) return 0;

    // clear gain list
    for (int i = 0, intListLength = gain.length; i < intListLength; i++) gain[i] = 0;
    int d = 0;

    final Square toSquare = Move.getEnd(move);

    Square fromSquare = Move.getStart(move);
    Color nextPlayer = position.getNextPlayer();
    Piece movedPiece = Move.getPiece((move));

    // get the initial bitboards
    long fromSet = fromSquare.getBitBoard();
    long occSet = position.getAllOccupiedBitboard();

    // get the attacks from both colors
    long remainingAttacks = attacksTo(position, toSquare, nextPlayer)
      | attacksTo(position, toSquare, nextPlayer.inverse());

    final int capturedValue;
    final int promotionValue = Move.getPromotion(move).getType().getValue();
    if (promotionValue > 0) capturedValue = promotionValue;
    else capturedValue = capturedPiece.getType().getValue();
    gain[d] = capturedValue;

    //    System.out.printf("%d. Move: %12s on %s captures %12s on %s: score=%d%n",
    //                      d,
    //                      movedPiece.name(),
    //                      fromSquare,
    //                      capturedPiece.name(),
    //                      toSquare,
    //                      gain[d]);

    while (remainingAttacks != 0) {
      // next depth and side
      d++;
      nextPlayer = nextPlayer.inverse();

      // determine value also in case of promotion
      final int movedPieceValue;
      if (movedPiece.getType() == PieceType.PAWN
        && toSquare.getRank() == (nextPlayer.isWhite() ? Square.Rank.r8 : Square.Rank.r1)) {
        movedPieceValue = PieceType.QUEEN.getValue();
      }
      else {
        movedPieceValue = movedPiece.getType().getValue();
      }
      // speculative store, if defended
      gain[d] = movedPieceValue - gain[d - 1];

      //      System.out.printf("%d. Move: %12s on %s captures %12s on %s: score=%d (risk=%d)%n", d,
      //                        movedPiece.name(), fromSquare, capturedPiece.name(), toSquare,
      //                        gain[d - 1],
      //                        movedPieceValue);
      //      capturedPiece = movedPiece;

      /* */
      // pruning
      // pruning does influence result on promotions an certain other positions
      // if (Math.max(-gain[d - 1], gain[d]) < 0) break;
      remainingAttacks ^= fromSet; // reset bit in set to traverse
      occSet ^= fromSet; // reset bit in temporary occupancy (for x-Rays)

      // reevaluate attacks to reveal attacks after removing the moving piece
      remainingAttacks = revealedAttacks(position, toSquare, occSet, remainingAttacks);

      // determine next capture
      fromSquare = getLeastValuablePiece(position, remainingAttacks, nextPlayer);
      if (fromSquare == NOSQUARE) break;
      fromSet = fromSquare.getBitBoard();
      movedPiece = position.getPiece(fromSquare);

      // kings are checked last and should not move into check
      if (movedPiece.getType() == PieceType.KING && Long.bitCount(remainingAttacks) > 1) break;
    }

    // find the gain in the moves played with a minimax like algorithm
    // the final gain will only be used if we could not stop before
    // a player wouldn't play a capture if it was for sure bad
    // "minimaxing" a unary tree
    while (--d > 0) gain[d - 1] = -Math.max(-gain[d - 1], gain[d]);

    return gain[0];
  }
  /**
   * Returns the given attacks bitboard with revealed attacks added.
   *
   * @param position
   * @param square
   * @param occSet
   * @param attacks
   * @return given attacks bitboard with revealed attacks added
   */
  private static long revealedAttacks(Position position, Square square, long occSet,
                                      long attacks) {

    final long[] whitePieces = position.getPiecesBitboards(Color.WHITE);
    final long[] blackPieces = position.getPiecesBitboards(Color.BLACK);

    // Only sliders can be revealed

    // rooks and queens
    attacks |= (Bitboard.getSlidingMovesRank(square, occSet)
      | Bitboard.getSlidingMovesFile(square, occSet)) & (whitePieces[PieceType.ROOK.ordinal()]
      | whitePieces[PieceType.QUEEN.ordinal()]
      | blackPieces[PieceType.ROOK.ordinal()] | blackPieces[PieceType.QUEEN.ordinal()]) & occSet;

    // bishop and queens
    attacks |= (Bitboard.getSlidingMovesDiagUp(square, occSet)
      | Bitboard.getSlidingMovesDiagDown(square, occSet)) & (whitePieces[PieceType.BISHOP.ordinal()]
      | whitePieces[PieceType.QUEEN.ordinal()]
      | blackPieces[PieceType.BISHOP.ordinal()] | blackPieces[PieceType.QUEEN.ordinal()]) & occSet;

    return attacks;
  }

  private static Square getLeastValuablePiece(Position position, long bitboard, Color c) {

    final long[][] pieceBB = position.getPiecesBitboards();

    // check all piece types with increasing value
    if ((bitboard & pieceBB[c.ordinal()][PieceType.PAWN.ordinal()]) != 0)
      return Square.getFirstSquare(bitboard & pieceBB[c.ordinal()][PieceType.PAWN.ordinal()]);

    if ((bitboard & pieceBB[c.ordinal()][PieceType.KNIGHT.ordinal()]) != 0)
      return Square.getFirstSquare(bitboard & pieceBB[c.ordinal()][PieceType.KNIGHT.ordinal()]);

    if ((bitboard & pieceBB[c.ordinal()][PieceType.BISHOP.ordinal()]) != 0)
      return Square.getFirstSquare(bitboard & pieceBB[c.ordinal()][PieceType.BISHOP.ordinal()]);

    if ((bitboard & pieceBB[c.ordinal()][PieceType.ROOK.ordinal()]) != 0)
      return Square.getFirstSquare((bitboard & pieceBB[c.ordinal()][PieceType.ROOK.ordinal()]));

    if ((bitboard & pieceBB[c.ordinal()][PieceType.QUEEN.ordinal()]) != 0)
      return Square.getFirstSquare(bitboard & pieceBB[c.ordinal()][PieceType.QUEEN.ordinal()]);

    if ((bitboard & pieceBB[c.ordinal()][PieceType.KING.ordinal()]) != 0)
      return Square.getFirstSquare(bitboard & pieceBB[c.ordinal()][PieceType.KING.ordinal()]);

    return NOSQUARE;
  }

  /**
   * Computes all attacksTo and attacksFrom for a given position
   * @param position
   */
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

  public boolean hasCheck() {
    checkPosition();
    return hasCheck;
  }

  //  public boolean isAttacked(Square square) {
  //    checkPosition();
  //    // TODO
  //    return false;
  //  }
  //
  //  public boolean isAttackedBy(Color attacker, Square square) {
  //    checkPosition();
  //    // TODO
  //    return false;
  //  }

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
}
