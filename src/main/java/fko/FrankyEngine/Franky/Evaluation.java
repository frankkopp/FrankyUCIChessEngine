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

import static fko.FrankyEngine.Franky.EvaluationConfig.*;
import static fko.FrankyEngine.Franky.Piece.*;
import static fko.FrankyEngine.Franky.PieceType.PAWN;
import static fko.FrankyEngine.Franky.Square.File.*;
import static fko.FrankyEngine.Franky.Square.Rank.r1;
import static fko.FrankyEngine.Franky.Square.Rank.r8;
import static fko.FrankyEngine.Franky.Square.*;

/**
 * Omega Evaluation
 * <p>
 * Features/Ideas:
 * DONE: Material
 * DONE: Mobility
 * DONE: Game Phase
 * TODO: Development (http://archive.gamedev.net/archive/reference/articles/article1208.html)
 * DONE: Piece Tables (http://www.chessbin.com/post/Chess-Board-Evaluation)
 * DONE: Tapered Eval (https://www.chessprogramming.org/Tapered_Eval)
 * TODO: Lazy Evaluation
 * DONE: Bishop Pair
 * TODO: Bishop vs. Knight
 * TODO: Center Control
 * TODO: Center Distance
 * TODO: Square Control
 * TODO: King Protection
 * TODO: Pawn Structure
 * TODO: King Safety
 * TODO: Board control
 */
public class Evaluation {

  private static final Logger LOG = LoggerFactory.getLogger(Evaluation.class);

  // Constants for evaluations
  public static final int NOVALUE             = Short.MIN_VALUE; // TT uses shorts
  public static final int INFINITE            = Short.MAX_VALUE; // TT uses shorts
  public static final int MIN                 = -10000;
  public static final int MAX                 = 10000;
  public static final int DRAW                = 0;
  public static final int CHECKMATE           = MAX;
  public static final int CHECKMATE_THRESHOLD = CHECKMATE - Byte.MAX_VALUE;

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
  private int kingSafety           = 0;
  private int midGameMaterial      = 0;
  private int endGameMaterial      = 0;
  private int midGamePiecePosition = 0;
  private int endGamePiecePosition = 0;
  private int midGameMobility      = 0;
  private int endGameMobility      = 0;
  private int midGameKingSafety    = 0;
  private int endGameKingSafety    = 0;


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
   *
   * @param position
   */
  private void setPosition(final Position position) {
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
   * Evaluates the position.
   *
   * @return value of the position from active player's view.
   */

  private int evaluate() {

    // protect against null position
    if (position == null) {
      IllegalStateException e = new IllegalStateException();
      LOG.error("No position to evaluate. Set position before calling this", e);
      throw e;
    }

    // if not enough material on the board for a win then it is a draw
    if (position.checkInsufficientMaterial()) return Evaluation.DRAW;

    // Clear all evaluation values
    clearValues();

    // GamePhase - a value between 0 and 24 depending on officer midGameMaterial of
    // the position
    gamePhaseFactor = getGamePhaseFactor(position);

    /*
     * Ideally evaluate in 3 Stages to avoid doing certain loop multiple times
     * - 1. Static > O(1)
     * - 2. all pieces > O(#pieces)
     * - 3. all Squares > O(#squares)
     */

    // Stage 1
    staticEvaluations();

    material = midGameMaterial * (gamePhaseFactor / GAME_PHASE_MAX) +
               endGameMaterial * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    // TODO: LAZY EVALUATION

    // Stage 2
    iterateOverPieces();

    piecePosition = midGamePiecePosition * (gamePhaseFactor / GAME_PHASE_MAX) +
                    endGamePiecePosition * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    mobility = midGameMobility * (gamePhaseFactor / GAME_PHASE_MAX) +
               endGameMobility * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    kingSafety = midGameKingSafety * (gamePhaseFactor / GAME_PHASE_MAX) +
                 endGameKingSafety * (1 - gamePhaseFactor / GAME_PHASE_MAX);

    // TODO: LAZY EVALUATION

    // Stage 3
    iterateOverSquares();

    // ######################################
    // Sum up
    // @formatter:off
    value = material      * MATERIAL_WEIGHT +
            piecePosition * POSITION_WEIGHT +
            mobility      * MOBILITY_WEIGHT +
            kingSafety    * KING_SAFETY_WEIGHT +
            special;
    // @formatter:on
    // Sum up per game phase
    // ######################################

    assert (Evaluation.MIN < value && value < Evaluation.MAX);
    return value;
  }

  private void clearValues() {
    value = 0;
    gamePhaseFactor = 0;

    special = 0;
    material = 0;
    piecePosition = 0;
    mobility = 0;
    kingSafety = 0;

    midGameMaterial = 0;
    endGameMaterial = 0;

    midGamePiecePosition = 0;
    endGamePiecePosition = 0;

    midGameMobility = 0;
    endGameMobility = 0;

    midGameKingSafety = 0;
    endGameKingSafety = 0;
  }

  private void staticEvaluations() {

    // CHECK Bonus: Giving check or being in check has value as it forces evasion moves
    special +=
      position.isAttacked(position.getNextPlayer(), kingSquares[opponent]) ? CHECK_VALUE : 0;
    special -=
      position.isAttacked(position.getOpponent(), kingSquares[nextToMove]) ? CHECK_VALUE : 0;

    // TEMPO Bonus
    special += nextToMove == WHITE
               ? TEMPO * (gamePhaseFactor / GAME_PHASE_MAX)
               : -TEMPO * (gamePhaseFactor / GAME_PHASE_MAX);

    materialEvaluation();

  }

  private void materialEvaluation() {

    // midGameMaterial is incrementally counted in Position
    midGameMaterial = position.getNextPlayer().factor *
                      (position.getMaterial(Color.WHITE) - position.getMaterial(Color.BLACK));

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
    evalPawns();

    // Knights
    evalKnights();

    // Bishops
    evalBishops();

    // Rooks
    evalRooks();

    // Queens
    evalQueens();

    // Kings
    evalKings();

    // for now they are always the same
    // TODO: different mobility for mid and end game
    endGameMobility = midGameMobility;

  }

  private void evalKings() {
    { // ME
      Square nextToMoveKingSquare = kingSquares[nextToMove];
      final int index = nextToMoveKingSquare.ordinal();
      assert (position.getPiece(nextToMoveKingSquare).getType() == PieceType.KING);
      assert (position.getPiece(nextToMoveKingSquare).getColor().ordinal() == nextToMove);

      // position
      final int tableIndex =
        nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += kingMidGame[tableIndex];
      this.endGamePiecePosition += kingEndGame[tableIndex];

      // king safety - skip in endgame
      if (gamePhaseFactor > GAME_PHASE_MAX / 2) {

        // king safety WHITE
        if (nextToMove == WHITE && kingSquares[nextToMove].getRank() == r1) {

          if (kingSquares[nextToMove].getFile().get() > f.get()) {
            // king side castle

            // rook in the corner penalty
            if (position.getPiece(h1) == WHITE_ROOK) {
              midGamePiecePosition += CORNERED_ROOK_PENALTY;
            }
            // pawns in front
            if (position.getPiece(f2) == WHITE_PAWN) {
              midGameKingSafety += 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(g2) == WHITE_PAWN || position.getPiece(g3) == WHITE_PAWN) {
              midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(h2) == WHITE_PAWN || position.getPiece(h3) == WHITE_PAWN) {
              midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
          } else if (kingSquares[nextToMove].getFile().get() < d.get()) {
            // queen side castle

            // queen side castle is weaker as king is more exposed
            this.midGameKingSafety += -KING_SAFETY_PAWNSHIELD;

            // rook in the corner penalty
            if (position.getPiece(a1) == WHITE_ROOK || position.getPiece(b1) == WHITE_ROOK) {
              midGamePiecePosition += EvaluationConfig.CORNERED_ROOK_PENALTY;
            }
            // extra bonus for queen side castle and king on b or a file
            if (kingSquares[nextToMove].getFile().get() < c.get()) {
              this.midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
            // pawns in front
            if (position.getPiece(c2).getType() == PAWN) {
              this.midGameKingSafety += 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(b2).getType() == PAWN
                || position.getPiece(b3).getType() == PAWN) {
              this.midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(a2).getType() == PAWN
                || position.getPiece(a3).getType() == PAWN) {
              this.midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
          }
        }
        // king safety BLACK
        else if (nextToMove == BLACK && kingSquares[nextToMove].getRank() == r8) {

          if (kingSquares[nextToMove].getFile().get() > e.get()) {
            // king side castle

            // rook in the corner penalty
            if (position.getPiece(h8) == BLACK_ROOK) {
              midGamePiecePosition += CORNERED_ROOK_PENALTY;
            }
            // pawns in front
            if (position.getPiece(f7) == BLACK_PAWN) {
              midGameKingSafety += 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(g7) == BLACK_PAWN || position.getPiece(g6) == BLACK_PAWN) {
              midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(h7) == BLACK_PAWN || position.getPiece(h6) == BLACK_PAWN) {
              midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
          } else if (kingSquares[nextToMove].getFile().get() < d.get()) {
            // queen side castle

            // queen side castle is weaker as king is more exposed
            this.midGameKingSafety += -KING_SAFETY_PAWNSHIELD;

            // rook in the corner penalty
            if (position.getPiece(a8) == BLACK_ROOK || position.getPiece(b8) == BLACK_ROOK) {
              midGamePiecePosition += EvaluationConfig.CORNERED_ROOK_PENALTY;
            }
            // extra bonus for queen side castle and king on b or a file
            if (kingSquares[nextToMove].getFile().get() < c.get()) {
              this.midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
            // pawns in front
            if (position.getPiece(c7) == BLACK_PAWN) {
              this.midGameKingSafety += 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(b7) == BLACK_PAWN || position.getPiece(b6) == BLACK_PAWN) {
              this.midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(a7) == BLACK_PAWN || position.getPiece(a6) == BLACK_PAWN) {
              this.midGameKingSafety += KING_SAFETY_PAWNSHIELD;
            }
          }
        }
      }
    }

    { // OPPONENT
      Square opponentKingSquare = kingSquares[opponent];
      final int index = opponentKingSquare.ordinal();
      assert (position.getPiece(opponentKingSquare).getType() == PieceType.KING);
      assert (position.getPiece(opponentKingSquare).getColor().ordinal() == opponent);

      // position
      final int tableIndex =
        opponent == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= kingMidGame[tableIndex];
      this.endGamePiecePosition -= kingEndGame[tableIndex];

      // king safety - skip in endgame
      if (gamePhaseFactor > GAME_PHASE_MAX / 2) {

        // king safety WHITE
        if (opponent == WHITE && kingSquares[opponent].getRank() == r1) {

          if (kingSquares[opponent].getFile().get() > e.get()) {
            // king side castle

            // rook in the corner penalty
            if (position.getPiece(h1) == WHITE_ROOK) {
              midGamePiecePosition -= CORNERED_ROOK_PENALTY;
            }
            // pawns in front
            if (position.getPiece(f2) == WHITE_PAWN) {
              midGameKingSafety -= 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(g2) == WHITE_PAWN || position.getPiece(g3) == WHITE_PAWN) {
              midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(h2) == WHITE_PAWN || position.getPiece(h3) == WHITE_PAWN) {
              midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
          } else if (kingSquares[opponent].getFile().get() < e.get()) {
            // queen side castle

            // queen side castle is weaker as king is more exposed
            this.midGameKingSafety += -KING_SAFETY_PAWNSHIELD;

            // rook in the corner penalty
            if (position.getPiece(a1) == WHITE_ROOK || position.getPiece(b1) == WHITE_ROOK) {
              midGamePiecePosition -= EvaluationConfig.CORNERED_ROOK_PENALTY;
            }
            // extra bonus for queen side castle and king on b or a file
            if (kingSquares[opponent].getFile().get() < c.get()) {
              this.midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
            // pawns in front
            if (position.getPiece(c2).getType() == PAWN) {
              this.midGameKingSafety -= 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(b2).getType() == PAWN
                || position.getPiece(b3).getType() == PAWN) {
              this.midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(a2).getType() == PAWN
                || position.getPiece(a3).getType() == PAWN) {
              this.midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
          }
        }
        // king safety BLACK
        else if (opponent == BLACK && kingSquares[opponent].getRank() == r8) {

          if (kingSquares[opponent].getFile().get() > e.get()) {
            // king side castle

            // rook in the corner penalty
            if (position.getPiece(h8) == BLACK_ROOK) {
              midGamePiecePosition -= CORNERED_ROOK_PENALTY;
            }
            // pawns in front
            if (position.getPiece(f7) == BLACK_PAWN) {
              midGameKingSafety -= 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(g7) == BLACK_PAWN || position.getPiece(g6) == BLACK_PAWN) {
              midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(h7) == BLACK_PAWN || position.getPiece(h6) == BLACK_PAWN) {
              midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
          } else if (kingSquares[opponent].getFile().get() < d.get()) {
            // queen side castle

            // queen side castle is weaker as king is more exposed
            this.midGameKingSafety -= -KING_SAFETY_PAWNSHIELD;

            // rook in the corner penalty
            if (position.getPiece(a8) == BLACK_ROOK || position.getPiece(b8) == BLACK_ROOK) {
              midGamePiecePosition -= EvaluationConfig.CORNERED_ROOK_PENALTY;
            }
            // extra bonus for queen side castle and king on b or a file
            if (kingSquares[opponent].getFile().get() < c.get()) {
              this.midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
            // pawns in front
            if (position.getPiece(c7) == BLACK_PAWN) {
              this.midGameKingSafety -= 2 * KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(b7) == BLACK_PAWN || position.getPiece(b6) == BLACK_PAWN) {
              this.midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
            if (position.getPiece(a7) == BLACK_PAWN || position.getPiece(a6) == BLACK_PAWN) {
              this.midGameKingSafety -= KING_SAFETY_PAWNSHIELD;
            }
          }
        }
      }
    }
  }

  private void evalQueens() {
    for (int i = 0; i < queenSquares[nextToMove].size(); i++) {
      Square square = queenSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.QUEEN);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility +=
        QUEEN_MOBILITY_FACTOR * mobilityForPiece(PieceType.QUEEN, square, queenDirections);

      // position
      final int tableIndex =
        nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += queenMidGame[tableIndex];
      this.endGamePiecePosition += queenEndGame[tableIndex];
    }
    for (int i = 0; i < queenSquares[opponent].size(); i++) {
      Square square = queenSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.QUEEN);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -=
        QUEEN_MOBILITY_FACTOR * mobilityForPiece(PieceType.QUEEN, square, queenDirections);

      // position
      final int tableIndex =
        opponent == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= queenMidGame[tableIndex];
      this.endGamePiecePosition -= queenEndGame[tableIndex];
    }
  }

  private void evalRooks() {
    for (int i = 0; i < rookSquares[nextToMove].size(); i++) {
      Square square = rookSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.ROOK);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility +=
        ROOK_MOBILITY_FACTOR * mobilityForPiece(PieceType.ROOK, square, rookDirections);

      // position
      final int tableIndex =
        nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += rookMidGame[tableIndex];
      this.endGamePiecePosition += rookEndGame[tableIndex];
    }
    for (int i = 0; i < rookSquares[opponent].size(); i++) {
      Square square = rookSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.ROOK);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -=
        ROOK_MOBILITY_FACTOR * mobilityForPiece(PieceType.ROOK, square, rookDirections);

      // position
      final int tableIndex =
        opponent == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= rookMidGame[tableIndex];
      this.endGamePiecePosition -= rookEndGame[tableIndex];
    }
  }

  private void evalBishops() {
    for (int i = 0; i < bishopSquares[nextToMove].size(); i++) {
      Square square = bishopSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.BISHOP);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility +=
        BISHOP_MOBILITY_FACTOR * mobilityForPiece(PieceType.BISHOP, square, bishopDirections);

      // position
      final int tableIndex =
        nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += bishopMidGame[tableIndex];
      this.endGamePiecePosition += bishopEndGame[tableIndex];

    }
    for (int i = 0; i < bishopSquares[opponent].size(); i++) {
      Square square = bishopSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.BISHOP);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -=
        BISHOP_MOBILITY_FACTOR * mobilityForPiece(PieceType.BISHOP, square, bishopDirections);

      // position
      final int tableIndex =
        opponent == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= bishopMidGame[tableIndex];
      this.endGamePiecePosition -= bishopEndGame[tableIndex];
    }
  }

  private void evalKnights() {
    for (int i = 0; i < knightSquares[nextToMove].size(); i++) {
      Square square = knightSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.KNIGHT);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility +=
        KNIGHTS_MOBILITY_FACTOR * mobilityForPiece(PieceType.KNIGHT, square, knightDirections);

      // position
      final int tableIndex =
        nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += knightMidGame[tableIndex];
      this.endGamePiecePosition += knightEndGame[tableIndex];

    }
    for (int i = 0; i < knightSquares[opponent].size(); i++) {
      Square square = knightSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PieceType.KNIGHT);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -=
        KNIGHTS_MOBILITY_FACTOR * mobilityForPiece(PieceType.KNIGHT, square, knightDirections);

      // position
      final int tableIndex =
        opponent == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= knightMidGame[tableIndex];
      this.endGamePiecePosition -= knightEndGame[tableIndex];
    }
  }

  private void evalPawns() {
    for (int i = 0; i < pawnSquares[nextToMove].size(); i++) {
      Square square = pawnSquares[nextToMove].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PAWN);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // position
      final int tableIndex =
        nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition += pawnsMidGame[tableIndex];
      this.endGamePiecePosition += pawnsEndGame[tableIndex];

    }
    for (int i = 0; i < pawnSquares[opponent].size(); i++) {
      Square square = pawnSquares[opponent].get(i);
      final int index = square.ordinal();
      assert (position.getPiece(square).getType() == PAWN);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // position
      final int tableIndex =
        opponent == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);
      this.midGamePiecePosition -= pawnsMidGame[tableIndex];
      this.endGamePiecePosition -= pawnsEndGame[tableIndex];
    }
  }

  /**
   * Iterates over all squares an does evaluations specific to the square
   */
  private void iterateOverSquares() {
    // not yet used
  }

  /**
   * Returns a value for the game phase between 0 and 24.
   * <p>
   * 24 is the standard opening position with all officer pieces present.<br>
   * 0 means no officer pieces present.
   * In rare cases were through pawn promotions more officers than the opening position
   * are present the value is at maximum 24.
   *
   * @param position
   * @return a value depending on officer midGameMaterial of both sides between 0 and 24
   */
  public static int getGamePhaseFactor(Position position) {

    // protect against null position
    if (position == null) {
      IllegalArgumentException e = new IllegalArgumentException();
      LOG.error("No position to evaluate. Set position before calling this", e);
      throw e;
    }

    // @formatter:off
    return Math.min(GAME_PHASE_MAX,
                    position.getKnightSquares()[WHITE].size() +
                    position.getKnightSquares()[BLACK].size() +
                    position.getBishopSquares()[WHITE].size() +
                    position.getBishopSquares()[BLACK].size() +
                    2 * position.getRookSquares()[WHITE].size() +
                    2 * position.getRookSquares()[BLACK].size() +
                    4 * position.getQueenSquares()[WHITE].size() +
                    4 * position.getQueenSquares()[BLACK].size());
    // @formatter:on
  }

  /**
   * @param position
   * @return material balance from the view of the active player
   */
  public int material(Position position) {
    // clear old values
    evaluate(position);
    return material;
  }

  /**
   * @return
   */
  public int position(Position position) {
    // piece position is done in the piece iteration
    evaluate(position);
    return piecePosition;
  }

  /**
   * @return number of pseudo legal moves for the next player
   */
  public int mobility(Position position) {
    // midGameMobility is done in the squares iteration
    evaluate(position);
    return mobility;
  }

  /**
   * @return number of pseudo legal moves for the next player
   */
  public int kingSafety(Position position) {
    // midGameMobility is done in the squares iteration
    evaluate(position);
    return kingSafety;
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
        final Piece target = position.getPiece(Square.getSquare(to));
        // free square - non capture
        if (target == NOPIECE) {
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

  public static int getPositionValue(Position position, int move) {

    final int nextToMove = position.getNextPlayer().ordinal();

    final int index = Move.getEnd(move).ordinal();
    final int tableIndex =
      nextToMove == WHITE ? getWhiteTableIndex(index) : getBlackTableIndex(index);

    final int midGame;
    final int endGame;

    switch (Move.getPiece(move).getType()) {
      case PAWN:
        midGame = pawnsMidGame[tableIndex];
        endGame = pawnsEndGame[tableIndex];
        break;
      case KNIGHT:
        midGame = knightMidGame[tableIndex];
        endGame = knightEndGame[tableIndex];
        break;
      case BISHOP:
        midGame = bishopMidGame[tableIndex];
        endGame = bishopEndGame[tableIndex];
        break;
      case ROOK:
        midGame = rookMidGame[tableIndex];
        endGame = rookEndGame[tableIndex];
        break;
      case QUEEN:
        midGame = queenMidGame[tableIndex];
        endGame = queenEndGame[tableIndex];
        break;
      case KING:
        midGame = kingMidGame[tableIndex];
        endGame = kingEndGame[tableIndex];
        break;
      default:
        midGame = 0;
        endGame = 0;
        break;
    }

    final int gamePhaseFactor = getGamePhaseFactor(position);

    return midGame * (gamePhaseFactor / GAME_PHASE_MAX) +
           endGame * (1 - gamePhaseFactor / GAME_PHASE_MAX);

  }
}
