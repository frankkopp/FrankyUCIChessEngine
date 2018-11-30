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
 * Omega Evaluation
 * <p>
 * Features/Ideas:
 * DONE: Material
 * DONE: Mobility
 * TODO: Piece Tables
 * TODO: Game Phase
 * TODO: Tapered Eval
 * TODO: Lazy Evaluation
 * TODO: Bishop Pair
 * TODO: Bishop vs. Knight
 * TODO: Center Control
 * TODO: Center Distance
 * TODO: Square Control
 * TODO: King Protection
 */
public class Evaluation {

  static private final boolean MATERIAL       = true;
  static private final boolean MOBILITY       = true;
  static private final boolean PIECE_POSITION = false;


  /**
   * Creates an instance of the OmegaEvaluator using a new Engine
   * and a new Move Generator.
   */
  public Evaluation() {
  }

  /**
   * Always from the view of the active (next) player.
   *
   * @param board
   * @return value of the position from active player's view.
   */
  public int evaluate(BoardPosition board) {

    int value = Evaluation.Value.DRAW;

    // Material
    if (MATERIAL) value += material(board);

    // Mobility
    if (MOBILITY) value += mobility(board);

    // Piece Position
    if (PIECE_POSITION) value += position(board);

    return value;
  }

  /**
   * @param board
   * @return material balance from the view of the active player
   */
  int material(final BoardPosition board) {
    int material =
      board._nextPlayer.factor * (board.getMaterial(Color.WHITE) - board.getMaterial(Color.BLACK));

    // bonus/malus for bishop pair
    if (board._bishopSquares[board._nextPlayer.ordinal()].size() >= 2) material += 50;
    if (board._bishopSquares[board._nextPlayer.getInverseColor().ordinal()].size() >= 2) material -= 50;

    return material;
  }

  /**
   * @param board
   * @return number of pseudo legal moves for the next player
   */
  int mobility(final BoardPosition board) {
    int mobility = 0;

    // to influence the weight of the piece type
    int factor = 1;

    final Color activePlayer = board._nextPlayer;
    final Color passivePlayer = activePlayer.getInverseColor();

    // knights
    factor = 2;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.KNIGHT,
                                           board._knightSquares[activePlayer.ordinal()], Square.knightDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.KNIGHT,
                                           board._knightSquares[passivePlayer.ordinal()], Square.knightDirections);

    // bishops
    factor = 2;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.BISHOP,
                                           board._bishopSquares[activePlayer.ordinal()], Square.bishopDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.BISHOP,
                                           board._bishopSquares[passivePlayer.ordinal()], Square.bishopDirections);

    // rooks
    factor = 2;
    mobility += factor *
                mobilityForPieces(board, activePlayer, PieceType.ROOK, board._rookSquares[activePlayer.ordinal()],
                                  Square.rookDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.ROOK,
                                           board._rookSquares[passivePlayer.ordinal()], Square.rookDirections);

    // queens
    factor = 1;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.QUEEN,
                                           board._queenSquares[activePlayer.ordinal()], Square.queenDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.QUEEN,
                                           board._queenSquares[passivePlayer.ordinal()], Square.queenDirections);

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
  private static int mobilityForPieces(BoardPosition board, Color color, PieceType type,
                                       SquareList squareList, int[] pieceDirections) {
    int numberOfMoves = 0;
    // iterate over all squares where we have a piece
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      Square square = squareList.get(i);
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
  private static int mobilityForPiece(BoardPosition board, Color color, PieceType type,
                                      Square square, int[] pieceDirections) {
    int numberOfMoves = 0;
    for (int d : pieceDirections) {
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = board._x88Board[to];
        // free square - non capture
        if (target == Piece.NOPIECE) {
          numberOfMoves++;
        }
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
        if (type.isSliding()) {
          to += d; // next sliding field in this direction
        } else {
          break; // no sliding piece type
        }
      }
    }
    return numberOfMoves;
  }

  /**
   * @param board
   * @return
   */
  int position(BoardPosition board) {
    return 0;
  }

  /**
   * Predefined values for Evaluation of positions.
   */
  @SuppressWarnings("javadoc")
  public static class Value {
    static public final int NOVALUE   = Integer.MIN_VALUE;
    static public final int INFINITE  = Integer.MAX_VALUE;
    static public final int MIN_VALUE = -200000;
    static public final int DRAW      = 0;
    static public final int CHECKMATE = 100000;
  }

}
