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

/**
 * This class represents a move in the Omega Engine. The data structure is optimized for speed using
 * only int and enum.
 *
 * @author Frank Kopp
 */
public class Move {

  // NOMOVE
  /** */
  public static final int NOMOVE = -99;
  // MASKs
  private static final int SQUARE_bitMASK = 0x7F;
  private static final int PIECE_bitMASK = 0xF;
  private static final int MOVETYPE_bitMASK = 0x7;

  // Bit operation values
  private static final int START_SQUARE_SHIFT = 0;
  private static final int START_SQUARE_MASK = SQUARE_bitMASK << START_SQUARE_SHIFT;

  private static final int END_SQUARE_SHIFT = 7;
  private static final int END_SQUARE_MASK = SQUARE_bitMASK << END_SQUARE_SHIFT;

  private static final int PIECE_SHIFT = 14;
  private static final int PIECE_MASK = PIECE_bitMASK << PIECE_SHIFT;

  private static final int TARGET_SHIFT = 18;
  private static final int TARGET_MASK = PIECE_bitMASK << TARGET_SHIFT;

  private static final int PROMOTION_SHIFT = 22;
  private static final int PROMOTION_MASK = PIECE_bitMASK << PROMOTION_SHIFT;

  private static final int MOVETYPE_SHIFT = 26;
  private static final int MOVETYPE_MASK = MOVETYPE_bitMASK << MOVETYPE_SHIFT;

  // no instantiation of this class
  private Move() {}

  /** Create a Move. */
  static int createMove(
      MoveType movetype,
      Square start,
      Square end,
      Piece piece,
      Piece target,
      Piece promotion) {
    int move = 0;
    // Encode start
    move |= start.ordinal() << START_SQUARE_SHIFT;
    // Encode end
    move |= end.ordinal() << END_SQUARE_SHIFT;
    // Encode piece
    move |= piece.ordinal() << PIECE_SHIFT;
    // Encode target
    move |= target.ordinal() << TARGET_SHIFT;
    // Encode promotion
    move |= promotion.ordinal() << PROMOTION_SHIFT;
    // Encode move
    move |= movetype.ordinal() << MOVETYPE_SHIFT;
    return move;
  }

  /**
   * Get the start square from the move
   *
   * @param move the move.
   * @return start position of the move.
   */
  static Square getStart(int move) {
    assert move != NOMOVE;
    int position = (move & START_SQUARE_MASK) >>> START_SQUARE_SHIFT;
    assert (position & 0x88) == 0;
    return Square.getSquare(position);
  }

  /**
   * Get the end position from the move.
   *
   * @param move the move.
   * @return the end position of the move.
   */
  static Square getEnd(int move) {
    assert move != NOMOVE;
    int position = (move & END_SQUARE_MASK) >>> END_SQUARE_SHIFT;
    assert (position & 0x88) == 0;
    return Square.getSquare(position);
  }

  /**
   * Get the piece from the Move.
   *
   * @param move the IntMove.
   * @return the piece
   */
  static Piece getPiece(int move) {
    assert move != NOMOVE;
    int chessman = (move & PIECE_MASK) >>> PIECE_SHIFT;
    return Piece.values[chessman];
  }

  /**
   * Get the target piece from the move.
   *
   * @param move the move.
   * @return the target piece.
   */
  static Piece getTarget(int move) {
    assert move != NOMOVE;
    int chessman = (move & TARGET_MASK) >>> TARGET_SHIFT;
    return Piece.values[chessman];
  }

  /**
   * Get the promotion piece from the move.
   *
   * @param move the move.
   * @return the promotion piece.
   */
  static Piece getPromotion(int move) {
    assert move != NOMOVE;
    int promotion = ((move & PROMOTION_MASK) >>> PROMOTION_SHIFT);
    return Piece.values[promotion];
  }

  /**
   * Get the type from the move.
   *
   * @param move the move.
   * @return the type.
   */
  static MoveType getMoveType(int move) {
    assert move != NOMOVE : "STOP";
    int type = ((move & MOVETYPE_MASK) >>> MOVETYPE_SHIFT);
    return MoveType.values[type];
  }

  /**
   * String representation of move
   *
   * @param move
   * @return String for move
   */
  static String toString(int move) {
    if (move == NOMOVE) return "NOMOVE";
    String s = "";
    if (getMoveType(move) == MoveType.CASTLING) {
      switch (getEnd(move)) {
        case g1:
          s += "O-O";
          break;
        case c1:
          s += "O-O-O";
          break;
        case g8:
          s += "o-o";
          break;
        case c8:
          s += "o-o-o";
          break;
        default:
          break;
      }
    } else {
      s += getMoveType(move) + " " + getPiece(move) + getStart(move);
      s += getTarget(move) == Piece.NOPIECE ? "-" : "x" + getTarget(move).toString();
      s += getEnd(move).toString() + getPromotion(move).toString();
    }
    return s;
  }

  /**
   * Simple String representation of move
   *
   * @param move
   * @return String for move
   */
  public static String toSimpleString(int move) {
    if (move == NOMOVE) return "NOMOVE";
    String s = "";
    s += getStart(move).toString();
    s += getEnd(move).toString();
    return s;
  }

  public static int fromUCINotation(final Position position, final String move) {
    Square from = Square.fromUCINotation(move.substring(0, 2));
    Square to = Square.fromUCINotation(move.substring(2, 4));
    String promotion = "";
    if (move.length() > 4) {
      promotion = move.substring(4, 5);
    }

    // to find the move type it is easiest to generate all legal moves and then look
    // for a move with the same from and to
    MoveGenerator omg = new MoveGenerator();
    MoveList moves = omg.getLegalMoves(position);

    for (int m : moves) {
      Square f = Move.getStart(m);
      Square t = Move.getEnd(m);
      if (from.equals(f) && to.equals(t)) {
        if (promotion.isEmpty()) {
          return m;
        }
        if (Move.getMoveType(m).equals(MoveType.PROMOTION)
            && Move.getPromotion(m).getShortName().toLowerCase().equals(promotion)) {
          return m;
        }
      }
    }
    return NOMOVE;
  }

  public static String toUCINotation(final Position position, int move) {
    String promotion = "";
    if (Move.getMoveType(move) == MoveType.PROMOTION) {
      promotion = Move.getPromotion(move).getType().getShortName().toLowerCase();
    }
    return Move.getStart(move).toString() + Move.getEnd(move).toString() + promotion;
  }

  /**
   * Lightweight check if the given int is a valid int representing a move.<br>
   * <b>This does not check if this is a legal move</b>.<br>
   * It simply checks if the we can extract a valid Square as "from" and "to" and valid
   * OmegaPieces for piece (without NOPIECE), target and promotion.
   *
   * @param move
   * @return true if we could extract valid squares and pieces
   */
  static boolean isValid(int move) {
    // is it a valid move type (excludes NOMOVETYPE
    int type = ((move & MOVETYPE_MASK) >>> MOVETYPE_SHIFT);
    if (type == 0 || !MoveType.isValid(type)) return false;
    // is there a valid from (start) square
    int start = ((move & START_SQUARE_MASK) >>> START_SQUARE_SHIFT);
    if ((start & 0x88) != 0) return false;
    // is there a valid to (end) square
    int end = ((move & END_SQUARE_MASK) >>> END_SQUARE_SHIFT);
    if ((end & 0x88) != 0) return false;
    // is the piece a valid piece excluding NOPIECE
    int piece = (move & PIECE_MASK) >>> PIECE_SHIFT;
    if (piece == 0 || !Piece.isValid(piece)) return false;
    // is the target and the promotion a valid piece including NOPIECE
    int target = (move & TARGET_MASK) >>> TARGET_SHIFT;
    if (!Piece.isValid(target)) return false;
    int promotion = (move & PROMOTION_MASK) >>> PROMOTION_SHIFT;
    if (!Piece.isValid(promotion)) return false;
    return true;
  }
}
