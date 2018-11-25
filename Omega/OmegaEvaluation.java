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

/**
 * Omega Evaluation
 *
 * Features/Ideas:
 *      DONE: Material
 *      DONE: Mobility
 *      TODO: Piece Tables
 *      TODO: Game Phase
 *      TODO: Tapered Eval
 *      TODO: Lazy Evaluation
 *      TODO: Bishop Pair
 *      TODO: Bishop vs. Knight
 *      TODO: Center Control
 *      TODO: Center Distance
 *      TODO: Square Control
 *      TODO: King Protection
 */
public class OmegaEvaluation {

    static private final boolean MATERIAL = true;
    static private final boolean MOBILITY = true;
    static private final boolean PIECE_POSITION = false;

    @SuppressWarnings("unused")
    private final OmegaMoveGenerator _omegaMoveGenerator;
    @SuppressWarnings("unused")
    private final OmegaEngine _omegaEngine;

    /**
     * Creates an instance of the OmegaEvaluator using a new Engine
     * and a new Move Generator.
     */
    public OmegaEvaluation() {
        this._omegaEngine = new OmegaEngine();
        this._omegaMoveGenerator = new OmegaMoveGenerator();
    }

    /**
     * @param omegaEngine
     * @param omegaMoveGenerator
     */
    public OmegaEvaluation(OmegaEngine omegaEngine, OmegaMoveGenerator omegaMoveGenerator) {
        this._omegaEngine = omegaEngine;
        this._omegaMoveGenerator = omegaMoveGenerator;
    }

    /**
     * Always from the view of the active (next) player.
     *
     * @param board
     * @return value of the position from active player's view.
     */
    public int evaluate(OmegaBoardPosition board) {

        int value = OmegaEvaluation.Value.DRAW;

        // Material
        if (MATERIAL)
            value += material(board);

        // Mobility
        if (MOBILITY)
            value += mobility(board);

        // Piece Position
        if (PIECE_POSITION)
            value += position(board);

        return value;
    }

    /**
     * @param board
     * @return material balance from the view of the active player
     */
    int material(final OmegaBoardPosition board) {
        int material = board._nextPlayer.factor * (board.getMaterial(OmegaColor.WHITE) - board.getMaterial(OmegaColor.BLACK));

        // bonus/malus for bishop pair
        if (board._bishopSquares[board._nextPlayer.ordinal()].size() >= 2) material += 50;
        if (board._bishopSquares[board._nextPlayer.getInverseColor().ordinal()].size() >= 2) material -= 50;

        return material;
    }

    /**
     * @param board
     * @return number of pseudo legal moves for the next player
     */
    int mobility(final OmegaBoardPosition board) {
        int mobility = 0;

        // to influence the weight of the piece type
        int factor = 1;

        final OmegaColor activePlayer = board._nextPlayer;
        final OmegaColor passivePlayer = activePlayer.getInverseColor();

        // knights
        factor = 2;
        mobility += factor * mobilityForPieces(board, activePlayer, OmegaPieceType.KNIGHT, board._knightSquares[activePlayer.ordinal()], OmegaSquare.knightDirections);
        mobility -= factor * mobilityForPieces(board, passivePlayer, OmegaPieceType.KNIGHT, board._knightSquares[passivePlayer.ordinal()], OmegaSquare.knightDirections);

        // bishops
        factor = 2;
        mobility += factor * mobilityForPieces(board, activePlayer, OmegaPieceType.BISHOP, board._bishopSquares[activePlayer.ordinal()], OmegaSquare.bishopDirections);
        mobility -= factor * mobilityForPieces(board, passivePlayer, OmegaPieceType.BISHOP, board._bishopSquares[passivePlayer.ordinal()], OmegaSquare.bishopDirections);

        // rooks
        factor = 2;
        mobility += factor * mobilityForPieces(board, activePlayer, OmegaPieceType.ROOK, board._rookSquares[activePlayer.ordinal()], OmegaSquare.rookDirections);
        mobility -= factor * mobilityForPieces(board, passivePlayer, OmegaPieceType.ROOK, board._rookSquares[passivePlayer.ordinal()], OmegaSquare.rookDirections);

        // queens
        factor = 1;
        mobility += factor * mobilityForPieces(board, activePlayer, OmegaPieceType.QUEEN, board._queenSquares[activePlayer.ordinal()], OmegaSquare.queenDirections);
        mobility -= factor * mobilityForPieces(board, passivePlayer, OmegaPieceType.QUEEN, board._queenSquares[passivePlayer.ordinal()], OmegaSquare.queenDirections);

        return mobility;
    }

    /**
     * @param board
     * @param color
     * @param type
     * @param squareList
     * @param pieceDirections
     * @return
     */
    private static int mobilityForPieces(OmegaBoardPosition board, OmegaColor color, OmegaPieceType type, OmegaSquareList squareList, int[] pieceDirections) {
        int numberOfMoves = 0;
        // iterate over all squares where we have a piece
        final int size = squareList.size();
        for (int i=0; i<size; i++) {
            OmegaSquare square = squareList.get(i);
            numberOfMoves += mobilityForPiece(board, color, type, square, pieceDirections);
        }
        return numberOfMoves;
    }

    /**
     * @param board
     * @param color
     * @param type
     * @param square
     * @param pieceDirections
     */
    private static int mobilityForPiece(OmegaBoardPosition board, OmegaColor color, OmegaPieceType type, OmegaSquare square, int[] pieceDirections) {
        int numberOfMoves = 0;
        int[] directions = pieceDirections;
        for (int d : directions) {
            int to = square.ordinal() + d;
            while ((to & 0x88) == 0) { // slide while valid square
                final OmegaPiece target = board._x88Board[to];
                // free square - non capture
                if (target == OmegaPiece.NOPIECE) numberOfMoves++;
                // occupied square - capture if opponent and stop sliding
                else {
                    /*
                     * Either only count moves which capture an opponent's piece or also
                     * count moves which defend one of our own piece
                     */
                    //if (target.getColor() == color.getInverseColor())
                    numberOfMoves++;
                    break; // stop sliding;
                }
                if (type.isSliding()) to += d; // next sliding field in this direction
                else break; // no sliding piece type
            }
        }
        return numberOfMoves;
    }

    /**
     * @param board
     * @return
     */
    int position(OmegaBoardPosition board) {
        return 0;
    }

    /**
     * Predefined values for Evaluation of positions.
     */
    @SuppressWarnings("javadoc")
    public static class Value {
        static public final int NOVALUE = Integer.MIN_VALUE;
        static public final int INFINITE = Integer.MAX_VALUE;
        static public final int MIN_VALUE = -200000;
        static public final int DRAW = 0;
        static public final int CHECKMATE = 100000;
    }

}
