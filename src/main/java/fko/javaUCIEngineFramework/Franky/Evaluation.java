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

import static fko.javaUCIEngineFramework.Franky.EvaluationConfig.*;

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
 * TODO: Pawn Structure
 * TODO: King Safety
 * TODO: Board control
 * TODO: Piece Position
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

  // Game Phase
  private int gamePhaseFactor = GAME_PHASE_MAX;

  // Evaluation Results
  private int value                = 0;
  private int special              = 0;
  private int material             = 0;
  private int piecePosition        = 0;
  private int mobility             = 0;
  private int midGameMaterial      = 0;
  private int endGameMaterial      = 0;
  private int midGamePiecePosition = 0;
  private int endGamePiecePosition = 0;
  private int midGameMobility      = 0;
  private int endGameMobility      = 0;

  // Convenience fields - improve readability
  private Position     position;
  private int          nextToMove;
  private int          opponent;
  private SquareList[] pawnSquares;
  private SquareList[] knightSquares;
  private SquareList[] bishopSquares;
  private SquareList[] rookSquares;
  private SquareList[] queenSquares;
  private Square[]     kingSquares;


  /**
   * Creates an instance of the Evaluator
   */
  public Evaluation() {
  }

  /**
   * Sets the reference to the position this evaluation operates on.
   * <p>
   * Attention: This does not create a deep copy. If the position changes after
   * setting the reference the change will be reflected here. This does not
   * create a deep copy.
   *
   * @param position
   */
  public void setPosition(final Position position) {
    this.position = position;

    // convenience fields
    nextToMove = position.getNextPlayer().ordinal();
    opponent = position.getNextPlayer().getInverseColor().ordinal();
    pawnSquares = position.getPawnSquares();
    knightSquares = position.getKnightSquares();
    bishopSquares = position.getBishopSquares();
    rookSquares = position.getRookSquares();
    queenSquares = position.getQueenSquares();
    kingSquares = position.getKingSquares();
  }

  /**
   * @return the reference to the last position object used in evaluation
   */
  public Position getPosition() {
    return position;
  }

  /**
   * Sets the position to evaluate and evaluates the position.
   * Is equivalent to <code>setPosition(pos)</code> and <code>evaluate()</code>
   * <p>
   * Sets the reference to the position this evaluation operates on.
   * <p>
   * Attention: This does not create a deep copy. If the position changes after
   * setting the reference the change will be reflected here. This does not
   * create a deep copy.
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

    // Clear all evaluation values
    clearValues();

    // GamePhase - a value between 0 and 24 depending on officer midGameMaterial of
    // the position
    this.gamePhaseFactor = getGamePhaseFactor();

    /*
     * Ideally evaluate in 3 Stages to avoid doing certain loop multiple times
     * - 1. Static > O(1)
     * - 2. all pieces > O(#pieces)
     * - 3. all Squares > O(#squares)
     */

    // Stage 1
    staticEvaluations();
    // Stage 2
    iterateOverPieces();
    // Stage 3
    iterateOverSquares();

    // ######################################
    // Sum up per game phase
    material = midGameMaterial * (gamePhaseFactor / GAME_PHASE_MAX) +
               endGameMaterial * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    piecePosition = midGamePiecePosition * (gamePhaseFactor / GAME_PHASE_MAX) +
                    endGamePiecePosition * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    mobility = midGameMobility * (gamePhaseFactor / GAME_PHASE_MAX) +
               endGameMobility * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    // @formatter:off
    value = material      * MATERIAL_WEIGHT +
            piecePosition * POSITION_WEIGHT +
            mobility      * MOBILITY_WEIGHT +
            special;
    // @formatter:on

    // Sum up per game phase
    // ######################################
    return value;
  }

  private void clearValues() {
    value = 0;
    gamePhaseFactor = 0;
    special = 0;
    material = 0;
    piecePosition = 0;
    mobility = 0;
    midGameMaterial = 0;
    endGameMaterial = 0;
    midGamePiecePosition = 0;
    endGamePiecePosition = 0;
    midGameMobility = 0;
    endGameMobility = 0;
  }

  private void staticEvaluations() {

    // CHECK Bonus: Giving check or being in check has value as it forces evation moves
    special += position.isAttacked(position.getNextPlayer(), kingSquares[opponent])
               ? CHECK_VALUE
               : 0;
    special -= position.isAttacked(position.getOpponent(), kingSquares[nextToMove])
               ? CHECK_VALUE
               : 0;

    // TEMPO Bonus
    special += nextToMove == WHITE
               ? TEMPO * (gamePhaseFactor / GAME_PHASE_MAX)
               : -TEMPO * (gamePhaseFactor / GAME_PHASE_MAX);

    materialEvaluation();

  }

  private void materialEvaluation() {

    // midGameMaterial is incrementally counted in Position
    midGameMaterial = position.getNextPlayer().factor * (position.getMaterial(Color.WHITE) -
                                                         position.getMaterial(Color.BLACK));

    // bonus/malus for bishop pair
    if (bishopSquares[nextToMove].size() >= 2) {
      midGameMaterial += BISHOP_PAIR;
    }
    if (bishopSquares[opponent].size() >= 2) {
      midGameMaterial -= BISHOP_PAIR;
    }

    // bonus/malus for knight pair
    if (knightSquares[nextToMove].size() >= 2) {
      midGameMaterial += KNIGHT_PAIR;
    }
    if (knightSquares[opponent].size() >= 2) {
      midGameMaterial -= KNIGHT_PAIR;
    }

    // bonus/malus for rook pair
    if (rookSquares[nextToMove].size() >= 2) {
      midGameMaterial += ROOK_PAIR;
    }
    if (rookSquares[opponent].size() >= 2) {
      midGameMaterial -= ROOK_PAIR;
    }

    // for now they are always the same
    // TODO: e.g. should reflect that in endgames certain combinations are
    //  draws or mostly draws
    endGameMaterial = midGameMaterial;
  }

  /**
   * Iterates over all pieces for both colors and does all evaluations
   * for this piece.
   */
  private void iterateOverPieces() {

    // Pawns
    for (int i = 0; i < pawnSquares[nextToMove].size(); i++) {
      Square square = pawnSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(index).getType().equals(PieceType.PAWN));
      assert (position.getPiece(index).getColor().ordinal() == nextToMove);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += pawnsMidGame[tableIndex];
      this.endGamePiecePosition += pawnsEndGame[tableIndex];

    }
    for (int i = 0; i < pawnSquares[opponent].size(); i++) {
      Square square = pawnSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(index).getType().equals(PieceType.PAWN));
      assert (position.getPiece(index).getColor().ordinal() == opponent);

      // position
      final int tableIndex = opponent==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= pawnsMidGame[tableIndex];
      this.endGamePiecePosition -= pawnsEndGame[tableIndex];
    }

    // Knights
    for (int i = 0; i < knightSquares[nextToMove].size(); i++) {
      Square square = knightSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.KNIGHT));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += KNIGHTS_MOBILITY_FACTOR * mobilityForPiece(PieceType.KNIGHT, square,
                                                                    Square.knightDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += knightMidGame[tableIndex];
      this.endGamePiecePosition += knightEndGame[tableIndex];

    }
    for (int i = 0; i < knightSquares[opponent].size(); i++) {
      Square square = knightSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.KNIGHT));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= KNIGHTS_MOBILITY_FACTOR * mobilityForPiece(PieceType.KNIGHT, square,
                                                                    Square.knightDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= knightMidGame[tableIndex];
      this.endGamePiecePosition -= knightEndGame[tableIndex];
    }

    // Bishops
    for (int i = 0; i < bishopSquares[nextToMove].size(); i++) {
      Square square = bishopSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.BISHOP));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += BISHOP_MOBILITY_FACTOR * mobilityForPiece(PieceType.BISHOP, square,
                                                                   Square.bishopDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += bishopMidGame[tableIndex];
      this.endGamePiecePosition += bishopEndGame[tableIndex];

    }
    for (int i = 0; i < bishopSquares[opponent].size(); i++) {
      Square square = bishopSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.BISHOP));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= BISHOP_MOBILITY_FACTOR * mobilityForPiece(PieceType.BISHOP, square,
                                                                   Square.bishopDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= bishopMidGame[tableIndex];
      this.endGamePiecePosition -= bishopEndGame[tableIndex];
    }

    // Rooks
    for (int i = 0; i < rookSquares[nextToMove].size(); i++) {
      Square square = rookSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.ROOK));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += ROOK_MOBILITY_FACTOR * mobilityForPiece(PieceType.ROOK, square,
                                                                 Square.rookDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += rookMidGame[tableIndex];
      this.endGamePiecePosition += rookEndGame[tableIndex];
    }
    for (int i = 0; i < rookSquares[opponent].size(); i++) {
      Square square = rookSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.ROOK));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= ROOK_MOBILITY_FACTOR * mobilityForPiece(PieceType.ROOK, square,
                                                                 Square.rookDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= rookMidGame[tableIndex];
      this.endGamePiecePosition -= rookEndGame[tableIndex];
    }

    // Queens
    for (int i = 0; i < queenSquares[nextToMove].size(); i++) {
      Square square = queenSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.QUEEN));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += QUEEN_MOBILITY_FACTOR * mobilityForPiece(PieceType.QUEEN, square,
                                                                  Square.queenDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += queenMidGame[tableIndex];
      this.endGamePiecePosition += queenEndGame[tableIndex];
    }
    for (int i = 0; i < queenSquares[opponent].size(); i++) {
      Square square = queenSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square.ordinal()).getType().equals(PieceType.QUEEN));
      assert (position.getPiece(square.ordinal()).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= QUEEN_MOBILITY_FACTOR * mobilityForPiece(PieceType.QUEEN, square,
                                                                  Square.queenDirections);

      // position
      final int tableIndex = nextToMove==WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= queenMidGame[tableIndex];
      this.endGamePiecePosition -= queenEndGame[tableIndex];
    }

    // Kings
    {
      Square whiteKingSquare = kingSquares[nextToMove];
      final int index = whiteKingSquare.ordinal();
      assert (position.getPiece(whiteKingSquare.ordinal()).getType().equals(PieceType.KING));
      assert (position.getPiece(whiteKingSquare.ordinal()).getColor().ordinal() == nextToMove);

      // position
      final int tableIndex = nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += kingMidGame[tableIndex];
      this.endGamePiecePosition += kingEndGame[tableIndex];
    }
    {
      Square blackKingSquare = kingSquares[opponent];
      final int index = blackKingSquare.ordinal();
      assert (position.getPiece(blackKingSquare.ordinal()).getType().equals(PieceType.KING));
      assert (position.getPiece(blackKingSquare.ordinal()).getColor().ordinal() == opponent);

      // position
      final int tableIndex = nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= kingMidGame[tableIndex];
      this.endGamePiecePosition -= kingEndGame[tableIndex];
    }
    // for now they are always the same
    // TODO: different mobility for mid and end game
    endGameMobility = midGameMobility;

  }

  /**
   * Iterates over all squares an does evaluations specific to the square
   */
  private void iterateOverSquares() {

    for (Square square : Square.validSquares) {

    }

  }

  /**
   * Returns a value for the game phase between 0 and 24.
   * <p>
   * 24 is the standard opening position with all officer pieces present.<br>
   * 0 means no officer pieces present.
   * In rare cases were through pawn promotions more officers than the opening position
   * are present the value is at maximum 24.
   *
   * @return a value depending on officer midGameMaterial of both sides between 0 and 24
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
   * @return material balance from the view of the active player
   */
  public int material() {
    // clear old values
    evaluate();
    return material;
  }

  /**
   * @return
   */
  public int position() {
    // piece position is done in the piece iteration
    evaluate();
    return piecePosition;
  }

  /**
   * @return number of pseudo legal moves for the next player
   */
  public int mobility() {
    // midGameMobility is done in the squares iteration
    evaluate();
    return mobility;
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

  @Override
  public String toString() {
    // @formatter:off
    return "Evaluation{" +
           "value=" + value +
           ", gamePhaseFactor=" + gamePhaseFactor +
           ", special=" + special +
           ", material=" + material +
           ", midGameMaterial=" + midGameMaterial +
           ", endGameMaterial=" + endGameMaterial +
           ", piecePosition=" + piecePosition +
           ", midGamePiecePosition=" + midGamePiecePosition +
           ", endGamePiecePosition=" + endGamePiecePosition +
           ", mobility=" + mobility +
           ", midGameMobility=" + midGameMobility +
           ", endGameMobility=" + endGameMobility + '}';
    // @formatter:on
  }
}
