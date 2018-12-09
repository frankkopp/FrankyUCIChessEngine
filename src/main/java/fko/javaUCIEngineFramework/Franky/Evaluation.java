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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(Evaluation.class);

  // Constants for evaluations
  public static final int NOVALUE   = Integer.MIN_VALUE;
  public static final int INFINITE  = Integer.MAX_VALUE;
  public static final int DRAW      = 0;
  public static final int CHECKMATE = 10000;

  // Convenience constants
  private static final int GAME_PHASE_MAX = 24;
  private static final int WHITE          = Color.WHITE.ordinal();
  private static final int BLACK          = Color.BLACK.ordinal();

  // CONFIGURATION
  private static final boolean MATERIAL       = true;
  private static final boolean MOBILITY       = true;
  private static final boolean PIECE_POSITION = false;

  // Game Phase
  private int gamePhaseFactor = GAME_PHASE_MAX;

  // Evaluation Results
  private int material      = 0;
  private int piecePosition = 0;
  private int mobility      = 0;


  // Convenience fields - improve readability
  private Position     position;
  private int          nextToMove;
  private int          opponent;
  private SquareList[] knightSquares;
  private SquareList[] bishopSquares;
  private SquareList[] rookSquares;
  private SquareList[] queenSquares;


  /**
   * Creates an instance of the Evaluator
   */
  public Evaluation() {
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(final Position position) {
    this.position = position;

    // convenience fields
    nextToMove = position.getNextPlayer().ordinal();
    opponent = position.getNextPlayer().getInverseColor().ordinal();
    knightSquares = position.getKnightSquares();
    bishopSquares = position.getBishopSquares();
    rookSquares = position.getRookSquares();
    queenSquares = position.getQueenSquares();
  }

  /**
   * Sets the position to evaluate and evaluates the position.
   * Is equivalent to <code>setPosition(pos)</code> and <code>evaluate()</code>
   *
   * @param position
   * @return value of the position from active player's view.
   */
  public int evaluate(Position position) {
    setPosition(position);
    return evaluate();
  }

  /**
   * Sets the position to evaluate and evaluates the position.
   * Is equivalent to <code>setPosition(pos)</code> and <code>evaluate()</code>
   *
   * @return value of the position from active player's view.
   */

  public int evaluate() {

    // protect against null position
    if (position == null) {
      IllegalStateException e = new IllegalStateException();
      LOG.error("No position to evaluate. Set position before calling this", e);
      throw e;
    }

    // GamePhase - a value between 0 and 24 depending on officer material of
    // the position
    this.gamePhaseFactor = getGamePhaseFactor();

    /*
     * Ideally evaluate in 3 Stages to avoid doing certain loop multiple times
     * - 1. Static > O(1)
     * - 2. all pieces > O(#pieces)
     * - 3. all Squares > O(#squares)
     */

    // ###########
    // Stage 1
    // ###########

    // Material
    if (MATERIAL) {
      this.material = material();
    }

    // TODO: Pawn Structure
    // TODO: King Safety

    // ###########
    // Stage 2
    // ###########

    // Piece positions
    if (PIECE_POSITION) {
      this.piecePosition += position(position);
    }

    // ###########
    // Stage 3
    // ###########

    // Mobility
    if (MOBILITY) {
      this.mobility = mobility();
    }

    // TODO: Board control

    // TODO: Piece Position

    // Endgames

    // Sum up per game phase
    // @formatter:off
    // @formatter:on
    return material + piecePosition + mobility;
  }

  /**
   * Returns a value for the game phase between 0 and 24.
   * <p>
   * 24 is the standard opening position with all officer pieces present.<br>
   * 0 means no officer pieces present.
   * In rare cases were through pawn promotions more officers than the opening position
   * are present the value is at maximum 24.
   *
   * @return a value depending on officer material of both sides between 0 and 24
   */
  public int getGamePhaseFactor() {

    // protect against null position
    if (position == null) {
      IllegalStateException e = new IllegalStateException();
      LOG.error("No position to evaluate. Set position before calling this", e);
      throw e;
    }

    // @formatter:off
    return Math.min(GAME_PHASE_MAX,
                    knightSquares[WHITE].size() +
                    knightSquares[BLACK].size() +
                    bishopSquares[WHITE].size() +
                    bishopSquares[BLACK].size() +
                    2 * rookSquares[WHITE].size() +
                    2 * rookSquares[BLACK].size() +
                    4 * queenSquares[WHITE].size() +
                    4 * queenSquares[BLACK].size());
    // @formatter:on
  }

  /**
   * @param position
   * @return
   */
  public int position(Position position) {
    return 0;
  }

  /**
   * @return material balance from the view of the active player
   */
  public int material() {

    // protect against null position
    if (position == null) {
      IllegalStateException e = new IllegalStateException();
      LOG.error("No position to evaluate. Set position before calling this", e);
      throw e;
    }

    // material is incrementally counted in Position
    int material = position.getNextPlayer().factor * (position.getMaterial(Color.WHITE) -
                                                      position.getMaterial(Color.BLACK));

    // bonus/malus for bishop pair
    if (bishopSquares[nextToMove].size() >= 2) {
      material += Value.BISHOP_PAIR;
    }
    if (bishopSquares[opponent].size() >= 2) {
      material -= Value.BISHOP_PAIR;
    }

    // bonus/malus for knight pair
    if (knightSquares[nextToMove].size() >= 2) {
      material += Value.KNIGHT_PAIR;
    }
    if (knightSquares[opponent].size() >= 2) {
      material -= Value.KNIGHT_PAIR;
    }

    // bonus/malus for rook pair
    if (rookSquares[nextToMove].size() >= 2) {
      material += Value.ROOK_PAIR;
    }
    if (rookSquares[opponent].size() >= 2) {
      material -= Value.ROOK_PAIR;
    }

    return material;
  }

  /**
   * @return number of pseudo legal moves for the next player
   */
  public int mobility() {

    // protect against null position
    if (position == null) {
      IllegalStateException e = new IllegalStateException();
      LOG.error("No position to evaluate. Set position before calling this", e);
      throw e;
    }

    int mobility = 0;

    // to influence the weight of the piece type
    int factor = 1;

    // knights
    factor = Value.KNIGHTS_MOBILITY_FACTOR;
    mobility += factor * mobilityForPieces(PieceType.KNIGHT, knightSquares[nextToMove],
                                           Square.knightDirections);
    mobility -= factor * mobilityForPieces(PieceType.KNIGHT, knightSquares[opponent],
                                           Square.knightDirections);

    // bishops
    factor = Value.BISHOP_MOBILITY_FACTOR;
    mobility += factor * mobilityForPieces(PieceType.BISHOP, bishopSquares[nextToMove],
                                           Square.bishopDirections);
    mobility -= factor * mobilityForPieces(PieceType.BISHOP, bishopSquares[opponent],
                                           Square.bishopDirections);

    // rooks
    factor = Value.ROOK_MOBILITY_FACTOR;
    mobility += factor * mobilityForPieces(PieceType.ROOK, rookSquares[nextToMove],
                                           Square.rookDirections);
    mobility -= factor * mobilityForPieces(PieceType.ROOK, rookSquares[opponent],
                                           Square.rookDirections);

    // queens
    factor = Value.QUEEN_MOBILITY_FACTOR;
    mobility += factor * mobilityForPieces(PieceType.QUEEN, queenSquares[nextToMove],
                                           Square.queenDirections);
    mobility -= factor * mobilityForPieces(PieceType.QUEEN, queenSquares[opponent],
                                           Square.queenDirections);

    return mobility;
  }

  /**
   * @param type
   * @param squareList
   * @param pieceDirections
   * @return
   */
  private int mobilityForPieces(PieceType type, SquareList squareList, int[] pieceDirections) {
    int numberOfMoves = 0;
    // iterate over all squares where we have a piece
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      Square square = squareList.get(i);
      numberOfMoves += mobilityForPiece(type, square, pieceDirections);
    }
    return numberOfMoves;
  }

  /**
   * @param type
   * @param square
   * @param pieceDirections
   */
  private int mobilityForPiece(PieceType type, Square square, int[] pieceDirections) {

    int numberOfMoves = 0;
    for (int d : pieceDirections) {
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = position.getPiece(to);
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
   * Predefined values for Evaluation of positions.
   */
  private static class Value {

    public static final int BISHOP_PAIR = 30;
    public static final int KNIGHT_PAIR = 10;
    public static final int ROOK_PAIR   = 15;

    public static final int KNIGHTS_MOBILITY_FACTOR = 2;
    public static final int BISHOP_MOBILITY_FACTOR  = 2;
    public static final int ROOK_MOBILITY_FACTOR    = 2;
    public static final int QUEEN_MOBILITY_FACTOR   = 1;
  }

}
