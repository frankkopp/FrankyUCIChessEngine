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
 * Enumeration of all chess pieces with pieces types and color.
 */
public enum Piece {

    NOPIECE      (PieceType.NOTYPE, Color.NOCOLOR, ""),// 0
    WHITE_PAWN   (PieceType.PAWN, Color.WHITE, "P"), // 1
    WHITE_KNIGHT (PieceType.KNIGHT, Color.WHITE, "N"), // 2
    WHITE_BISHOP (PieceType.BISHOP, Color.WHITE, "B"), // 3
    WHITE_ROOK   (PieceType.ROOK, Color.WHITE, "R"), // 4
    WHITE_QUEEN  (PieceType.QUEEN, Color.WHITE, "Q"), // 5
    WHITE_KING   (PieceType.KING, Color.WHITE, "K"), // 6
    BLACK_PAWN   (PieceType.PAWN, Color.BLACK, "p"), // 7
    BLACK_KNIGHT (PieceType.KNIGHT, Color.BLACK, "n"), // 8
    BLACK_BISHOP (PieceType.BISHOP, Color.BLACK, "b"), // 9
    BLACK_ROOK   (PieceType.ROOK, Color.BLACK, "r"), // 10
    BLACK_QUEEN  (PieceType.QUEEN, Color.BLACK, "q"), // 11
    BLACK_KING   (PieceType.KING, Color.BLACK, "k"); // 12

    static final Piece[] values;

    private final PieceType _type;
    private final Color     _color;
    private final String    _shortName;

    static {
        values = Piece.values();
    }

    Piece(PieceType type, Color color, String shortName) {
        _type = type;
        _color = color;
        _shortName = shortName;
    }

    /**
     * @return the type
     */
    public PieceType getType() {
        return _type;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return _color;
    }

    /**
     * @return the shortName
     */
    public String getShortName() {
        return _shortName;
    }

    @Override
    public String toString() {
        return _shortName;
    }

    /**
     * Returns the Omega piece for this type and color.
     * @param type
     * @param color
     * @return matching Piece
     */
    public static Piece getPiece(PieceType type, Color color) {
        // this only works if the ordinal of all enums stay the same - if they change this
        // has to be changed as well
        return Piece.values[(color.ordinal() * 6) + type.ordinal() ];
    }

    /**
     * Convert this Piece to the matching GamePiece
     */
//    public GamePiece convertToGamePiece() {
//        switch (this) {
//            case WHITE_KING:   return King.create(GameColor.WHITE);
//            case WHITE_QUEEN:  return Queen.create(GameColor.WHITE);
//            case WHITE_ROOK:   return Rook.create(GameColor.WHITE);
//            case WHITE_BISHOP: return Bishop.create(GameColor.WHITE);
//            case WHITE_KNIGHT: return Knight.create(GameColor.WHITE);
//            case WHITE_PAWN:   return Pawn.create(GameColor.WHITE);
//            case BLACK_KING:   return King.create(GameColor.BLACK);
//            case BLACK_QUEEN:  return Queen.create(GameColor.BLACK);
//            case BLACK_ROOK:   return Rook.create(GameColor.BLACK);
//            case BLACK_BISHOP: return Bishop.create(GameColor.BLACK);
//            case BLACK_KNIGHT: return Knight.create(GameColor.BLACK);
//            case BLACK_PAWN:   return Pawn.create(GameColor.BLACK);
//            default:
//                throw new RuntimeException("Invalid Piece");
//        }
//    }

    /**
     * Convert e GamePiece to an Piece
     * @return matching Piece
     */
//    public static Piece convertFromGamePiece(GamePiece gp) {
//        if (gp == null) return Piece.NOPIECE;
//        assert (gp.isWhite() || gp.isBlack());
//        switch (gp.getType()) {
//            case KING:   return gp.isWhite() ? Piece.WHITE_KING : Piece.BLACK_KING;
//            case QUEEN:  return gp.isWhite() ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
//            case ROOK:   return gp.isWhite() ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
//            case BISHOP: return gp.isWhite() ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
//            case KNIGHT: return gp.isWhite() ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
//            case PAWN:   return gp.isWhite() ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
//            default:
//                throw new RuntimeException("Invalid GamePieceType: "+gp);
//        }
//    }

    /**
     * @param i
     * @return
     */
    public static boolean isValid(int i) {
        if (i<0 || i>12) return false;
        return true;
    }

}
