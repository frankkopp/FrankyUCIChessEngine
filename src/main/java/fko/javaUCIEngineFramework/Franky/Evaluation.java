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
 * TODO: Game Phase
 * TODO: Development (http://archive.gamedev.net/archive/reference/articles/article1208.html)
 * TODO: Piece Tables (http://www.chessbin.com/post/Chess-Board-Evaluation)
 * TODO: Tapered Eval (https://www.chessprogramming.org/Tapered_Eval)
 * TODO: Lazy Evaluation
 * TODO: Bishop Pair
 * TODO: Bishop vs. Knight
 * TODO: Center Control
 * TODO: Center Distance
 * TODO: Square Control
 * TODO: King Protection
 */
public class Evaluation {

  private static final boolean MATERIAL       = true;
  private static final boolean MOBILITY       = true;
  private static final boolean PIECE_POSITION = false;

  /**
   * Creates an instance of the Evaluator
   */
  public Evaluation() {
  }

  /**
   * Always from the view of the active (next) player.
   *
   * @param position
   * @return value of the position from active player's view.
   */
  public int evaluate(Position position) {

    int value = Evaluation.Value.DRAW;

    // Material
    if (MATERIAL) {
      value += material(position);
    }

    // Mobility
    if (MOBILITY) {
      value += mobility(position);
    }

    // Piece Position
    if (PIECE_POSITION) {
      value += position(position);
    }

    return value;
  }

  /**
   * @param board
   * @return material balance from the view of the active player
   */
  int material(final Position board) {
    int material = board.getNextPlayer().factor * (board.getMaterial(Color.WHITE) -
                                                   board.getMaterial(Color.BLACK));

    // bonus/malus for bishop pair
    if (board.getBishopSquares()[board.getNextPlayer().ordinal()].size() >= 2) {
      material += 50;
    }
    if (board.getBishopSquares()[board.getNextPlayer().getInverseColor().ordinal()].size() >= 2) {
      material -= 50;
    }

    return material;
  }

  /**
   * @param board
   * @return number of pseudo legal moves for the next player
   */
  int mobility(final Position board) {
    int mobility = 0;

    // to influence the weight of the piece type
    int factor = 1;

    final Color activePlayer = board.getNextPlayer();
    final Color passivePlayer = activePlayer.getInverseColor();

    // knights
    factor = 2;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.KNIGHT,
                                           board.getKnightSquares()[activePlayer.ordinal()],
                                           Square.knightDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.KNIGHT,
                                           board.getKnightSquares()[passivePlayer.ordinal()],
                                           Square.knightDirections);

    // bishops
    factor = 2;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.BISHOP,
                                           board.getBishopSquares()[activePlayer.ordinal()],
                                           Square.bishopDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.BISHOP,
                                           board.getBishopSquares()[passivePlayer.ordinal()],
                                           Square.bishopDirections);

    // rooks
    factor = 2;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.ROOK,
                                           board.getRookSquares()[activePlayer.ordinal()],
                                           Square.rookDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.ROOK,
                                           board.getRookSquares()[passivePlayer.ordinal()],
                                           Square.rookDirections);

    // queens
    factor = 1;
    mobility += factor * mobilityForPieces(board, activePlayer, PieceType.QUEEN,
                                           board.getQueenSquares()[activePlayer.ordinal()],
                                           Square.queenDirections);
    mobility -= factor * mobilityForPieces(board, passivePlayer, PieceType.QUEEN,
                                           board.getQueenSquares()[passivePlayer.ordinal()],
                                           Square.queenDirections);

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
  private static int mobilityForPieces(Position board, Color color, PieceType type,
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
  private static int mobilityForPiece(Position board, Color color, PieceType type,
                                      Square square, int[] pieceDirections) {
    int numberOfMoves = 0;
    for (int d : pieceDirections) {
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = board.getX88Board()[to];
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
  int position(Position board) {
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
    static public final int CHECKMATE = 10000;
  }

}
