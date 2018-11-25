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

package fko.javaUCIEngineFramework.Omega;

import fko.chessly.game.GameColor;
import fko.chessly.game.GamePiece;
import fko.chessly.game.pieces.*;

/**
 * Enumeration of all chess pieces with pieces types and color.
 */
@SuppressWarnings("javadoc")
public enum OmegaPiece {

    NOPIECE      (OmegaPieceType.NOTYPE, OmegaColor.NOCOLOR, ""),// 0
    WHITE_PAWN   (OmegaPieceType.PAWN,   OmegaColor.WHITE, "P"), // 1
    WHITE_KNIGHT (OmegaPieceType.KNIGHT, OmegaColor.WHITE, "N"), // 2
    WHITE_BISHOP (OmegaPieceType.BISHOP, OmegaColor.WHITE, "B"), // 3
    WHITE_ROOK   (OmegaPieceType.ROOK,   OmegaColor.WHITE, "R"), // 4
    WHITE_QUEEN  (OmegaPieceType.QUEEN,  OmegaColor.WHITE, "Q"), // 5
    WHITE_KING   (OmegaPieceType.KING,   OmegaColor.WHITE, "K"), // 6
    BLACK_PAWN   (OmegaPieceType.PAWN,   OmegaColor.BLACK, "p"), // 7
    BLACK_KNIGHT (OmegaPieceType.KNIGHT, OmegaColor.BLACK, "n"), // 8
    BLACK_BISHOP (OmegaPieceType.BISHOP, OmegaColor.BLACK, "b"), // 9
    BLACK_ROOK   (OmegaPieceType.ROOK,   OmegaColor.BLACK, "r"), // 10
    BLACK_QUEEN  (OmegaPieceType.QUEEN,  OmegaColor.BLACK, "q"), // 11
    BLACK_KING   (OmegaPieceType.KING,   OmegaColor.BLACK, "k"); // 12

    static final OmegaPiece[] values;

    private final OmegaPieceType _type;
    private final OmegaColor _color;
    private final String _shortName;

    static {
        values = OmegaPiece.values();
    }

    private OmegaPiece(OmegaPieceType type, OmegaColor color, String shortName) {
        _type = type;
        _color = color;
        _shortName = shortName;
    }

    /**
     * @return the type
     */
    public OmegaPieceType getType() {
        return _type;
    }

    /**
     * @return the color
     */
    public OmegaColor getColor() {
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
     * @return matching OmegaPiece
     */
    public static OmegaPiece getPiece(OmegaPieceType type, OmegaColor color) {
        // this only works if the ordinal of all enums stay the same - if they change this
        // has to be changed as well
        return OmegaPiece.values[ (color.ordinal()*6) + type.ordinal() ];
    }

    /**
     * Convert this OmegaPiece to the matching GamePiece
     */
    public GamePiece convertToGamePiece() {
        switch (this) {
            case WHITE_KING:   return King.create(GameColor.WHITE);
            case WHITE_QUEEN:  return Queen.create(GameColor.WHITE);
            case WHITE_ROOK:   return Rook.create(GameColor.WHITE);
            case WHITE_BISHOP: return Bishop.create(GameColor.WHITE);
            case WHITE_KNIGHT: return Knight.create(GameColor.WHITE);
            case WHITE_PAWN:   return Pawn.create(GameColor.WHITE);
            case BLACK_KING:   return King.create(GameColor.BLACK);
            case BLACK_QUEEN:  return Queen.create(GameColor.BLACK);
            case BLACK_ROOK:   return Rook.create(GameColor.BLACK);
            case BLACK_BISHOP: return Bishop.create(GameColor.BLACK);
            case BLACK_KNIGHT: return Knight.create(GameColor.BLACK);
            case BLACK_PAWN:   return Pawn.create(GameColor.BLACK);
            default:
                throw new RuntimeException("Invalid Piece");
        }
    }

    /**
     * Convert e GamePiece to an OmegaPiece
     * @return matching OmegaPiece
     */
    public static OmegaPiece convertFromGamePiece(GamePiece gp) {
        if (gp == null) return OmegaPiece.NOPIECE;
        assert (gp.isWhite() || gp.isBlack());
        switch (gp.getType()) {
            case KING:   return gp.isWhite() ? OmegaPiece.WHITE_KING : OmegaPiece.BLACK_KING;
            case QUEEN:  return gp.isWhite() ? OmegaPiece.WHITE_QUEEN : OmegaPiece.BLACK_QUEEN;
            case ROOK:   return gp.isWhite() ? OmegaPiece.WHITE_ROOK : OmegaPiece.BLACK_ROOK;
            case BISHOP: return gp.isWhite() ? OmegaPiece.WHITE_BISHOP : OmegaPiece.BLACK_BISHOP;
            case KNIGHT: return gp.isWhite() ? OmegaPiece.WHITE_KNIGHT : OmegaPiece.BLACK_KNIGHT;
            case PAWN:   return gp.isWhite() ? OmegaPiece.WHITE_PAWN : OmegaPiece.BLACK_PAWN;
            default:
                throw new RuntimeException("Invalid GamePieceType: "+gp);
        }
    }

    /**
     * @param i
     * @return
     */
    public static boolean isValid(int i) {
        if (i<0 || i>12) return false;
        return true;
    }

}
