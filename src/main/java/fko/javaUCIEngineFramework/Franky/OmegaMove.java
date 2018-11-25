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
public class OmegaMove {

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
  private OmegaMove() {}

  /** Create a OmegaMove. */
  static int createMove(
      OmegaMoveType movetype,
      OmegaSquare start,
      OmegaSquare end,
      OmegaPiece piece,
      OmegaPiece target,
      OmegaPiece promotion) {
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
  static OmegaSquare getStart(int move) {
    assert move != NOMOVE;
    int position = (move & START_SQUARE_MASK) >>> START_SQUARE_SHIFT;
    assert (position & 0x88) == 0;
    return OmegaSquare.getSquare(position);
  }

  /**
   * Get the end position from the move.
   *
   * @param move the move.
   * @return the end position of the move.
   */
  static OmegaSquare getEnd(int move) {
    assert move != NOMOVE;
    int position = (move & END_SQUARE_MASK) >>> END_SQUARE_SHIFT;
    assert (position & 0x88) == 0;
    return OmegaSquare.getSquare(position);
  }

  /**
   * Get the piece from the Move.
   *
   * @param move the IntMove.
   * @return the piece
   */
  static OmegaPiece getPiece(int move) {
    assert move != NOMOVE;
    int chessman = (move & PIECE_MASK) >>> PIECE_SHIFT;
    return OmegaPiece.values[chessman];
  }

  /**
   * Get the target piece from the move.
   *
   * @param move the move.
   * @return the target piece.
   */
  static OmegaPiece getTarget(int move) {
    assert move != NOMOVE;
    int chessman = (move & TARGET_MASK) >>> TARGET_SHIFT;
    return OmegaPiece.values[chessman];
  }

  /**
   * Get the promotion piece from the move.
   *
   * @param move the move.
   * @return the promotion piece.
   */
  static OmegaPiece getPromotion(int move) {
    assert move != NOMOVE;
    int promotion = ((move & PROMOTION_MASK) >>> PROMOTION_SHIFT);
    return OmegaPiece.values[promotion];
  }

  /**
   * Get the type from the move.
   *
   * @param move the move.
   * @return the type.
   */
  static OmegaMoveType getMoveType(int move) {
    assert move != NOMOVE : "STOP";
    int type = ((move & MOVETYPE_MASK) >>> MOVETYPE_SHIFT);
    return OmegaMoveType.values[type];
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
    if (getMoveType(move) == OmegaMoveType.CASTLING) {
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
      s += getTarget(move) == OmegaPiece.NOPIECE ? "-" : "x" + getTarget(move).toString();
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

  public static int fromUCINotation(final OmegaBoardPosition position, final String move) {
    OmegaSquare from = OmegaSquare.fromUCINotation(move.substring(0, 2));
    OmegaSquare to = OmegaSquare.fromUCINotation(move.substring(2, 4));
    String promotion = "";
    if (move.length() > 4) {
      promotion = move.substring(4, 5);
    }

    // to find the move type it is easiest to generate all legal moves and then look
    // for a move with the same from and to
    OmegaMoveGenerator omg = new OmegaMoveGenerator();
    OmegaMoveList moves = omg.getLegalMoves(position, false);

    for (int m : moves) {
      OmegaSquare f = OmegaMove.getStart(m);
      OmegaSquare t = OmegaMove.getEnd(m);
      if (from.equals(f) && to.equals(t)) {
        if (promotion.isEmpty()) {
          return m;
        }
        if (OmegaMove.getMoveType(m).equals(OmegaMoveType.PROMOTION)
                && OmegaMove.getPromotion(m).getShortName().toLowerCase().equals(promotion)) {
          return m;
        }
      }
    }
    return NOMOVE;
  }

  /**
   * Converts move to GameMove. If the move is invalid or NOMOVE it returns null:
   *
   * @param move
   * @return the matching GameMove
   */
  //    static GameMove convertToGameMove(int move) {
  //        if (move == NOMOVE || !OmegaMove.isValid(move)) return null;
  //        GameMove gameMove = new GameMoveImpl(
  //                getStart(move).convertToGamePosition(),
  //                getEnd(move).convertToGamePosition(),
  //                getPiece(move).convertToGamePiece()
  //                );
  //        switch (getMoveType(move)) {
  //            case NORMAL:
  //                if (getTarget(move) != OmegaPiece.NOPIECE) {
  //                    gameMove.setCapturedPiece(getTarget(move).convertToGamePiece());
  //                }
  //                break;
  //            case PAWNDOUBLE:
  //                break;
  //            case ENPASSANT:
  //                break;
  //            case CASTLING:
  //                break;
  //            case PROMOTION:
  //                gameMove.setPromotedTo(getPromotion(move).convertToGamePiece());
  //                break;
  //                //return new GameMove(Square.valueOfIntPosition(start),
  // Square.valueOfIntPosition(end), Piece.valueOfIntChessman(getPromotion(move)));
  //            case NOMOVETYPE:
  //            default:
  //                throw new IllegalArgumentException();
  //        }
  //        return gameMove;
  //    }

  /**
   * Converts GameMove to move integer
   *
   * @param gm
   * @return integer representing matching move for the GameMove
   */
  //    static int convertFromGameMove(GameMove move) {
  //        assert move != null;
  //
  //        if (move.getPromotedTo() != null) {
  //            return createMove(
  //                    OmegaMoveType.PROMOTION,
  //                    OmegaSquare.convertFromGamePosition(move.getFromField()),
  //                    OmegaSquare.convertFromGamePosition(move.getToField()),
  //                    OmegaPiece.convertFromGamePiece(move.getMovedPiece()),
  //                    OmegaPiece.convertFromGamePiece(move.getCapturedPiece()),
  //                    OmegaPiece.convertFromGamePiece(move.getPromotedTo()));
  //
  //        } else if (move.isEnPassantNextMovePossible()) {
  //            //} else if (isPawnDouble(move, board)) {
  //            return createMove(
  //                    OmegaMoveType.PAWNDOUBLE,
  //                    OmegaSquare.convertFromGamePosition(move.getFromField()),
  //                    OmegaSquare.convertFromGamePosition(move.getToField()),
  //                    OmegaPiece.convertFromGamePiece(move.getMovedPiece()),
  //                    OmegaPiece.NOPIECE,
  //                    OmegaPiece.NOPIECE);
  //
  //        } else if (move.getWasEnPassantCapture()) {
  //            //} else if (isEnPassant(move, board)) {
  //            return createMove(
  //                    OmegaMoveType.ENPASSANT,
  //                    OmegaSquare.convertFromGamePosition(move.getFromField()),
  //                    OmegaSquare.convertFromGamePosition(move.getToField()),
  //                    OmegaPiece.convertFromGamePiece(move.getMovedPiece()),
  //                    OmegaPiece.convertFromGamePiece(move.getCapturedPiece()),
  //                    OmegaPiece.NOPIECE);
  //
  //        } else if (move.getCastlingType() != GameCastling.NOCASTLING) {
  //            //} else if (isCastling(move, board)) {
  //            return createMove(
  //                    OmegaMoveType.CASTLING,
  //                    OmegaSquare.convertFromGamePosition(move.getFromField()),
  //                    OmegaSquare.convertFromGamePosition(move.getToField()),
  //                    OmegaPiece.convertFromGamePiece(move.getMovedPiece()),
  //                    OmegaPiece.NOPIECE,
  //                    OmegaPiece.NOPIECE);
  //
  //        } else {
  //            return createMove(
  //                    OmegaMoveType.NORMAL,
  //                    OmegaSquare.convertFromGamePosition(move.getFromField()),
  //                    OmegaSquare.convertFromGamePosition(move.getToField()),
  //                    OmegaPiece.convertFromGamePiece(move.getMovedPiece()),
  //                    OmegaPiece.convertFromGamePiece(move.getCapturedPiece()),
  //                    OmegaPiece.NOPIECE);
  //        }
  //    }

  /**
   * Lightweight check if the given int is a valid int representing a move.<br>
   * <b>This does not check if this is a legal move</b>.<br>
   * It simply checks if the we can extract a valid OmegaSquare as "from" and "to" and valid
   * OmegaPieces for piece (without NOPIECE), target and promotion.
   *
   * @param move
   * @return true if we could extract valid squares and pieces
   */
  static boolean isValid(int move) {
    // is it a valid move type (excludes NOMOVETYPE
    int type = ((move & MOVETYPE_MASK) >>> MOVETYPE_SHIFT);
    if (type == 0 || !OmegaMoveType.isValid(type)) return false;
    // is there a valid from (start) square
    int start = ((move & START_SQUARE_MASK) >>> START_SQUARE_SHIFT);
    if ((start & 0x88) != 0) return false;
    // is there a valid to (end) square
    int end = ((move & END_SQUARE_MASK) >>> END_SQUARE_SHIFT);
    if ((end & 0x88) != 0) return false;
    // is the piece a valid piece excluding NOPIECE
    int piece = (move & PIECE_MASK) >>> PIECE_SHIFT;
    if (piece == 0 || !OmegaPiece.isValid(piece)) return false;
    // is the target and the promotion a valid piece including NOPIECE
    int target = (move & TARGET_MASK) >>> TARGET_SHIFT;
    if (!OmegaPiece.isValid(target)) return false;
    int promotion = (move & PROMOTION_MASK) >>> PROMOTION_SHIFT;
    if (!OmegaPiece.isValid(promotion)) return false;
    return true;
  }
}
