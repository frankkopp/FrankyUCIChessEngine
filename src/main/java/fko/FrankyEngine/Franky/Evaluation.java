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

import java.util.Arrays;

import static fko.FrankyEngine.Franky.Bitboard.*;
import static fko.FrankyEngine.Franky.EvaluationConfig.*;
import static fko.FrankyEngine.Franky.Piece.*;
import static fko.FrankyEngine.Franky.PieceType.*;
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
 * DONE: Board control
 */
public class Evaluation {

  private static final Logger LOG = LoggerFactory.getLogger(Evaluation.class);

  private static final boolean DEBUG = false;

  // Constants for evaluations
  // Our Transposition Table entry is bit encoded and because Java does not have
  // unsigned number primitives each TT entry value needs to be positive.
  // Therefore our max should be smaller than short/2 so we can shift the
  // negative values in the TT entry.
  public static final int INFINITE            = 15000; // TT uses shorts
  public static final int NOVALUE             = -INFINITE - 1; // TT uses shorts
  public static final int MIN                 = -10000;
  public static final int MAX                 = 10000;
  public static final int DRAW                = 0;
  public static final int CHECKMATE           = MAX;
  public static final int CHECKMATE_THRESHOLD = CHECKMATE - Byte.MAX_VALUE;

  // Convenience constants
  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  // Evaluation Results
  private int value   = 0;
  private int special = 0;

  private int material        = 0;
  private int midGameMaterial = 0;
  private int endGameMaterial = 0;

  private int piecePosition        = 0;
  private int midGamePiecePosition = 0;
  private int endGamePiecePosition = 0;

  private int mobility        = 0;
  private int midGameMobility = 0;
  private int endGameMobility = 0;

  private int kingSafety        = 0;
  private int midGameKingSafety = 0;
  private int endGameKingSafety = 0;

  private int pawnStructure        = 0;
  private int midGamePawnStructure = 0;
  private int endGamePawnStructure = 0;

  private int[][] controlAttacks = new int[2][64];
  private int[]   ownership      = new int[64];
  private long[]  attacksTo      = new long[64];
  private int     boardControl   = 0;

  private long[] attackedSquares = new long[2];

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

  // if we have alpha and beta we might cut off evaluation earlier
  private int alpha;
  private int beta;

  private void clearValues() {
    value = 0;
    special = 0;

    material = 0;
    midGameMaterial = 0;
    endGameMaterial = 0;

    piecePosition = 0;
    midGamePiecePosition = 0;
    endGamePiecePosition = 0;

    mobility = 0;
    midGameMobility = 0;
    endGameMobility = 0;

    kingSafety = 0;
    midGameKingSafety = 0;
    endGameKingSafety = 0;

    pawnStructure = 0;
    midGamePawnStructure = 0;
    endGamePawnStructure = 0;

    for (int i = 0; i < 64; i++) {
      controlAttacks[WHITE][i] = 0;
      controlAttacks[BLACK][i] = 0;
      ownership[i] = 0;
      attacksTo[i] = 0;
    }
    boardControl = 0;

    attackedSquares[WHITE] = 0L;
    attackedSquares[BLACK] = 0L;
  }

  /**
   * Creates an instance of the Evaluator
   */
  public Evaluation() {
    assert NOVALUE < Short.MAX_VALUE / 2;
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
    opponent = position.getNextPlayer().inverse().ordinal();
    pawnSquares = position.getPawnSquares();
    knightSquares = position.getKnightSquares();
    bishopSquares = position.getBishopSquares();
    rookSquares = position.getRookSquares();
    queenSquares = position.getQueenSquares();
    kingSquares = position.getKingSquares();

  }

  /**
   * Sets the position to evaluate and evaluates the position with given alpha and
   * beta. This might allow to short cut evaluation when values are well below or
   * above alpha or beta.
   *
   * Is equivalent to <code>setPosition(pos)</code> and <code>evaluate()</code>
   * <p>
   * Sets the reference to the position this evaluation operates on.
   * <p>
   * Attention: This does not create a deep copy. If the position changes after
   * setting the reference the change will be reflected here.
   *
   * @param position
   * @param alpha
   * @param beta
   * @return value of the position from active player's view.
   */
  public int evaluate(Position position, int alpha, int beta) {
    setPosition(position);
    this.alpha = alpha;
    this.beta = beta;
    final int evaluation = evaluate();
    if (DEBUG) printEvaluation();
    return evaluation;
  }

  /**
   * Sets the position to evaluate and evaluates the position.
   * Is equivalent to <code>setPosition(pos)</code> and <code>evaluate()</code>
   * <p>
   * Sets the reference to the position this evaluation operates on.
   * <p>
   * Attention: This does not create a deep copy. If the position changes after
   * setting the reference the change will be reflected here.
   *
   * @param position
   * @return value of the position from active player's view.
   */
  public int evaluate(Position position) {
    setPosition(position);
    this.alpha = Evaluation.MIN;
    this.beta = Evaluation.MAX;
    final int evaluation = evaluate();
    if (DEBUG) printEvaluation();
    return evaluation;
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

    final float phaseFactorMid = position.getGamePhaseFactor();
    final float phaseFactorEnd = 1f - phaseFactorMid;

    /*
     * Ideally evaluate in 3 Stages to avoid doing certain loops multiple times
     * - 1. Static > O(1)
     * - 2. all pieces > O(#pieces)
     * - 3. all Squares > O(#squares)
     */

    // Stage 1
    staticEvaluations();

    material = (int) (midGameMaterial * phaseFactorMid + endGameMaterial * phaseFactorEnd);

    // Stage 2
    iterateOverPieces();

    piecePosition = (int) (midGamePiecePosition * phaseFactorMid
      + endGamePiecePosition * phaseFactorEnd);

    // TODO: we can derive mobility from attacks - if we have more use for attack info
    //  then it is worth the extra effort
    mobility = (int) (midGameMobility * phaseFactorMid + endGameMobility * phaseFactorEnd);

    kingSafety = (int) (midGameKingSafety * phaseFactorMid + endGameKingSafety * phaseFactorEnd);

    pawnStructure = (int) (midGamePawnStructure * phaseFactorMid
      + endGamePawnStructure * phaseFactorEnd);

    // Stage 3
    iterateOverSquares();

    // ######################################
    // Sum up
    // @formatter:off
    value = material      * MATERIAL_WEIGHT +
            piecePosition * POSITION_WEIGHT +
            mobility      * MOBILITY_WEIGHT +
            kingSafety    * KING_SAFETY_WEIGHT +
            pawnStructure * PAWN_STRUCTURE_WEIGHT +
            boardControl  * BOARDCONTROL_WEIGHT +
            special;
    // @formatter:on
    // Sum up per game phase
    // ######################################

    // In very rare cases evaluation can be below or above the MIN or MAX.
    // Mostly in artificial cases with many queens - some test cases do this.
    // Therefore we limit the value to MIN+1 or MAX-1.
    if (value <= -Evaluation.CHECKMATE_THRESHOLD) value = -Evaluation.CHECKMATE_THRESHOLD + 1;
    else if (value >= Evaluation.CHECKMATE_THRESHOLD) value = Evaluation.CHECKMATE_THRESHOLD - 1;

    return value;
  }

  private void staticEvaluations() {

    // CHECK Bonus: Giving check or being in check has value as it forces evasion moves
    special += position.isAttacked(position.getNextPlayer(), kingSquares[opponent])
               ? CHECK_VALUE
               : 0;
    special -= position.isAttacked(position.getOpponent(), kingSquares[nextToMove])
               ? CHECK_VALUE
               : 0;

    // TEMPO Bonus for the side to move (helps with evaluation alternation -
    // less difference between side which makes aspiration search faster
    // (not empirically tested)
    special += TEMPO * position.getGamePhaseFactor();

    materialEvaluation();

  }

  /**
   * Iterates over all pieces for both colors and does all evaluations
   * for this piece.
   */
  private void iterateOverPieces() {

    evalPawns();
    evalKnights();
    evalBishops();
    evalRooks();
    evalQueens();
    evalKings();

    // for now they are always the same
    endGameMobility = midGameMobility;

  }

  /**
   * Iterates over all squares an does evaluations specific to the square
   */
  private void iterateOverSquares() {
    for (int i = 0; i < 64; i++) {
      // Board control
      if (controlAttacks[WHITE][i] == 0 && controlAttacks[BLACK][i] == 0) ownership[i] = 0; // none
      else if (controlAttacks[WHITE][i] == 0 && controlAttacks[BLACK][i] > 0)
        ownership[i] = -1; // black
      else if (controlAttacks[WHITE][i] > 0 && controlAttacks[BLACK][i] == 0)
        ownership[i] = 1; // white
      else ownership[i] = evaluateOwnership(position, i); // extra evaluation of ownership
      boardControl += position.getNextPlayer().factor * Integer.compare(ownership[i], 0);
    }
  }

  private void materialEvaluation() {

    // midGameMaterial is incrementally counted in Position
    midGameMaterial = position.getNextPlayer().factor * (position.getMaterial(Color.WHITE)
      - position.getMaterial(Color.BLACK));

    // bonus/malus for bishop pair
    if (bishopSquares[nextToMove].size() >= 2) midGameMaterial += BISHOP_PAIR;
    if (bishopSquares[opponent].size() >= 2) midGameMaterial -= BISHOP_PAIR;

    // bonus/malus for knight pair
    if (knightSquares[nextToMove].size() >= 2) midGameMaterial += KNIGHT_PAIR;
    if (knightSquares[opponent].size() >= 2) midGameMaterial -= KNIGHT_PAIR;

    // bonus/malus for rook pair
    if (rookSquares[nextToMove].size() >= 2) midGameMaterial += ROOK_PAIR;
    if (rookSquares[opponent].size() >= 2) midGameMaterial -= ROOK_PAIR;

    // for now they are always the same
    // TODO: e.g. should reflect that in endgames certain combinations are
    //  draws or mostly draws
    endGameMaterial = midGameMaterial;
  }
  private void evalPawns() {
    for (int i = 0, size = pawnSquares[nextToMove].size(); i < size; i++) {
      final Square square = pawnSquares[nextToMove].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PAWN);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // position
      final int tableIndex = nextToMove == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition += pawnsMidGame[tableIndex];
      this.endGamePiecePosition += pawnsEndGame[tableIndex];

      // penalty for doubled pawn
      final long pawnsOnFile =
        Bitboard.rays[nextToMove == WHITE ? Bitboard.NORTH : Bitboard.SOUTH][bbIdx]
          & position.getPiecesBitboards(nextToMove, PAWN);
      int count = Long.bitCount(pawnsOnFile);
      this.midGamePawnStructure += count * DOUBLED_PAWN_PENALTY;
      this.endGamePawnStructure += count * DOUBLED_PAWN_PENALTY;

      // bonus for passed pawn (no opponent pawn on file and neighbour files
      if ((Bitboard.passedPawnMask[nextToMove][bbIdx]
        & position.getPiecesBitboards(opponent, PAWN)) == 0
      ) {
        this.midGamePawnStructure += PASSED_PAWN;
        this.endGamePawnStructure += PASSED_PAWN;
      }

      // count all attacks to squares
      buildAttackTo(square, this.nextToMove, Square.pawnAttackDirections);
    }

    for (int i = 0, size = pawnSquares[opponent].size(); i < size; i++) {
      final Square square = pawnSquares[opponent].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PAWN);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // position
      final int tableIndex = opponent == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition -= pawnsMidGame[tableIndex];
      this.endGamePiecePosition -= pawnsEndGame[tableIndex];

      // penalty for doubled pawn
      final long pawnsOnFile =
        Bitboard.rays[opponent == WHITE ? Bitboard.NORTH : Bitboard.SOUTH][bbIdx]
          & position.getPiecesBitboards(opponent, PAWN);
      int count = Long.bitCount(pawnsOnFile);
      this.midGamePawnStructure -= count * DOUBLED_PAWN_PENALTY;
      this.endGamePawnStructure -= count * DOUBLED_PAWN_PENALTY;

      // bonus for passed pawn (no opponent pawn on file and neighbour file
      if ((Bitboard.passedPawnMask[opponent][bbIdx]
        & position.getPiecesBitboards(nextToMove, PAWN)) == 0
      ) {
        this.midGamePawnStructure -= PASSED_PAWN;
        this.endGamePawnStructure -= PASSED_PAWN;
      }

      // count all attacks to squares
      buildAttackTo(square, this.opponent, Square.pawnAttackDirections);
    }
  }

  private void evalKnights() {
    for (int i = 0, size = knightSquares[nextToMove].size(); i < size; i++) {
      final Square square = knightSquares[nextToMove].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.KNIGHT);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += KNIGHTS_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, nextToMove, PieceType.KNIGHT, knightDirections);

      // position
      final int tableIndex = nextToMove == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition += knightMidGame[tableIndex];
      this.endGamePiecePosition += knightEndGame[tableIndex];

    }
    for (int i = 0, size = knightSquares[opponent].size(); i < size; i++) {
      final Square square = knightSquares[opponent].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.KNIGHT);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= KNIGHTS_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, opponent, PieceType.KNIGHT, knightDirections);

      // position
      final int tableIndex = opponent == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition -= knightMidGame[tableIndex];
      this.endGamePiecePosition -= knightEndGame[tableIndex];
    }
  }

  private void evalBishops() {
    for (int i = 0, size = bishopSquares[nextToMove].size(); i < size; i++) {
      final Square square = bishopSquares[nextToMove].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.BISHOP);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += BISHOP_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, nextToMove, PieceType.BISHOP, bishopDirections);

      // position
      final int tableIndex = nextToMove == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition += bishopMidGame[tableIndex];
      this.endGamePiecePosition += bishopEndGame[tableIndex];

    }
    for (int i = 0, size = bishopSquares[opponent].size(); i < size; i++) {
      final Square square = bishopSquares[opponent].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.BISHOP);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= BISHOP_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, opponent, PieceType.BISHOP, bishopDirections);

      // position
      final int tableIndex = opponent == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition -= bishopMidGame[tableIndex];
      this.endGamePiecePosition -= bishopEndGame[tableIndex];
    }
  }

  private void evalRooks() {
    for (int i = 0, size = rookSquares[nextToMove].size(); i < size; i++) {
      final Square square = rookSquares[nextToMove].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.ROOK);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += ROOK_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, nextToMove, PieceType.ROOK, rookDirections);

      // position
      final int tableIndex = nextToMove == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition += rookMidGame[tableIndex];
      this.endGamePiecePosition += rookEndGame[tableIndex];
    }
    for (int i = 0, size = rookSquares[opponent].size(); i < size; i++) {
      final Square square = rookSquares[opponent].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.ROOK);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= ROOK_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, opponent, PieceType.ROOK, rookDirections);

      // position
      final int tableIndex = opponent == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition -= rookMidGame[tableIndex];
      this.endGamePiecePosition -= rookEndGame[tableIndex];
    }
  }

  private void evalQueens() {
    for (int i = 0, size = queenSquares[nextToMove].size(); i < size; i++) {
      final Square square = queenSquares[nextToMove].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.QUEEN);
      assert (position.getPiece(square).getColor().ordinal() == nextToMove);

      // midGameMobility
      midGameMobility += QUEEN_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, nextToMove, PieceType.QUEEN, queenDirections);

      // position
      final int tableIndex = nextToMove == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition += queenMidGame[tableIndex];
      this.endGamePiecePosition += queenEndGame[tableIndex];
    }
    for (int i = 0, size = queenSquares[opponent].size(); i < size; i++) {
      final Square square = queenSquares[opponent].get(i);
      final int bbIdx = square.bbIndex();

      assert (position.getPiece(square).getType() == PieceType.QUEEN);
      assert (position.getPiece(square).getColor().ordinal() == opponent);

      // midGameMobility
      midGameMobility -= QUEEN_MOBILITY_FACTOR *
        mobilityForPiece(this.position, square, opponent, PieceType.QUEEN, queenDirections);

      // position
      final int tableIndex = opponent == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition -= queenMidGame[tableIndex];
      this.endGamePiecePosition -= queenEndGame[tableIndex];
    }
  }

  private void evalKings() {
    // count all attacks to squares
    // we do this first then we have all attacks and can use them in the king
    // evaluation
    buildAttackTo(kingSquares[nextToMove], nextToMove, kingDirections);
    buildAttackTo(kingSquares[opponent], opponent, kingDirections);

    { // ME
      final Square nextToMoveKingSquare = kingSquares[nextToMove];
      final int bbIdx = nextToMoveKingSquare.bbIndex();

      assert (position.getPiece(nextToMoveKingSquare).getType() == PieceType.KING);
      assert (position.getPiece(nextToMoveKingSquare).getColor().ordinal() == nextToMove);

      // position
      final int tableIndex = nextToMove == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition += kingMidGame[tableIndex];
      this.endGamePiecePosition += kingEndGame[tableIndex];

      // king castle safety - skip in endgame
      if (position.getGamePhaseFactor() >= 0.5) {
        kingCastleSafety(nextToMove);
      }
      // TODO: distance to opp king in endgame

      // king ring attacks
      if ((kingRing[bbIdx] & attackedSquares[opponent]) != 0) {
        midGameKingSafety += KING_RING_ATTACK_PENALTY;
        endGameKingSafety += KING_RING_ATTACK_PENALTY;
      }
    }

    { // OPPONENT
      final Square opponentKingSquare = kingSquares[opponent];
      final int bbIdx = opponentKingSquare.bbIndex();

      assert (position.getPiece(opponentKingSquare).getType() == PieceType.KING);
      assert (position.getPiece(opponentKingSquare).getColor().ordinal() == opponent);

      // position
      final int tableIndex = opponent == WHITE ? bbIdx : 63 - bbIdx;
      this.midGamePiecePosition -= kingMidGame[tableIndex];
      this.endGamePiecePosition -= kingEndGame[tableIndex];

      // king castle safety - skip in endgame
      if (position.getGamePhaseFactor() >= 0.5) {
        kingCastleSafety(opponent);
      }
      // TODO: distance to opp king in endgame

      if ((kingRing[bbIdx] & attackedSquares[nextToMove]) != 0) {
        midGameKingSafety -= KING_RING_ATTACK_PENALTY;
        endGameKingSafety -= KING_RING_ATTACK_PENALTY;
      }
    }
  }

  private void kingCastleSafety(int color) {

    int midGamePiecePositionTemp = 0;
    int midGameKingSafetyTemp = 0;

    // king castle safety WHITE
    if (color == WHITE && kingSquares[color].getRank() == r1) {

      // king side castle
      if (kingSquares[color].getFile().get() > e.get()) {
        // rook in the corner penalty
        if (position.getPiece(h1) == WHITE_ROOK || position.getPiece(g1) == WHITE_ROOK) {
          midGamePiecePositionTemp += CORNERED_ROOK_PENALTY;
        }
        // pawns in front
        if (position.getPiece(f2) == WHITE_PAWN) {
          midGameKingSafetyTemp += 2 * KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(g2) == WHITE_PAWN || position.getPiece(g3) == WHITE_PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(h2) == WHITE_PAWN || position.getPiece(h3) == WHITE_PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
      }
      // queen side castle
      else if (kingSquares[color].getFile().get() < d.get()) {
        // queen side castle is weaker as king is more exposed
        midGameKingSafetyTemp += -KING_SAFETY_PAWNSHIELD;
        // rook in the corner penalty
        if (position.getPiece(a1) == WHITE_ROOK || position.getPiece(b1) == WHITE_ROOK) {
          midGamePiecePositionTemp += EvaluationConfig.CORNERED_ROOK_PENALTY;
        }
        // extra bonus for queen side castle and king on b or a file
        if (kingSquares[color].getFile().get() < c.get()) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
        // pawns in front
        if (position.getPiece(c2).getType() == PAWN) {
          midGameKingSafetyTemp += 2 * KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(b2).getType() == PAWN
          || position.getPiece(b3).getType() == PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(a2).getType() == PAWN
          || position.getPiece(a3).getType() == PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
      }
    }
    // king castle safety BLACK
    else if (color == BLACK && kingSquares[color].getRank() == r8) {
      // king side castle
      if (kingSquares[color].getFile().get() > e.get()) {
        // rook in the corner penalty
        if (position.getPiece(h8) == BLACK_ROOK || position.getPiece(g8) == BLACK_ROOK) {
          midGamePiecePositionTemp += CORNERED_ROOK_PENALTY;
        }
        // pawns in front
        if (position.getPiece(f7) == BLACK_PAWN) {
          midGameKingSafetyTemp += 2 * KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(g7) == BLACK_PAWN || position.getPiece(g6) == BLACK_PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(h7) == BLACK_PAWN || position.getPiece(h6) == BLACK_PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
      }
      // queen side castle
      else if (kingSquares[color].getFile().get() < d.get()) {
        // queen side castle is weaker as king is more exposed
        midGameKingSafetyTemp += -KING_SAFETY_PAWNSHIELD;
        // rook in the corner penalty
        if (position.getPiece(a8) == BLACK_ROOK || position.getPiece(b8) == BLACK_ROOK) {
          midGamePiecePositionTemp += EvaluationConfig.CORNERED_ROOK_PENALTY;
        }
        // extra bonus for queen side castle and king on b or a file
        if (kingSquares[color].getFile().get() < c.get()) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
        // pawns in front
        if (position.getPiece(c7) == BLACK_PAWN) {
          midGameKingSafetyTemp += 2 * KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(b7) == BLACK_PAWN || position.getPiece(b6) == BLACK_PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
        if (position.getPiece(a7) == BLACK_PAWN || position.getPiece(a6) == BLACK_PAWN) {
          midGameKingSafetyTemp += KING_SAFETY_PAWNSHIELD;
        }
      }
    }
    final int factor = (color == WHITE) ? Color.WHITE.factor : Color.BLACK.factor;
    midGamePiecePosition += midGamePiecePositionTemp * factor * position.getNextPlayer().factor;
    midGameKingSafety += midGameKingSafetyTemp * factor * position.getNextPlayer().factor;
  }

  /**
   * For pieces counting towards mobility: Counts how many field this piece can
   * move to (including defending an own piece).
   * Also increases controlAttacks counter for all visited squares.
   *
   * @param position
   * @param square
   * @param colorIdx
   * @param type
   * @param pieceDirections
   */
  int mobilityForPiece(Position position, Square square, int colorIdx, PieceType type,
                       int[] pieceDirections) {

    int numberOfMoves = 0;
    for (int i = 0, pieceDirectionsLength = pieceDirections.length;
         i < pieceDirectionsLength;
         i++
    ) {
      final int d = pieceDirections[i];
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        // We also count capturing and defending a piece a mobility
        numberOfMoves++;

        final Square squareTo = getSquare(to);
        // bitboard of all attacked squares by color
        attackedSquares[colorIdx] |= squareTo.bitboard();
        // build an index to count all attacks to a square by color
        controlAttacks[colorIdx][squareTo.bbIndex()]++;
        // get a bitboard of all attacks to this square
        attacksTo[squareTo.bbIndex()] |= square.bitboard();

        // break if you hit another piece
        if (position.getPiece(Square.getSquare(to)) != NOPIECE) break;
        // next sliding field in this factor
        if (type.isSliding()) to += d;
        else break;
      }
    }
    return numberOfMoves;
  }

  /**
   * Increates controlAttacks per square.
   * @param square
   * @param colorIdx
   * @param attackDirections
   */
  private void buildAttackTo(Square square, int colorIdx, int[] attackDirections) {
    int dir = (colorIdx == WHITE) ? Color.WHITE.factor : Color.BLACK.factor;
    for (int j = 0, pieceDirectionsLength = attackDirections.length;
         j < pieceDirectionsLength;
         j++
    ) {
      final int to = square.ordinal() + attackDirections[j] * dir;
      // build an index to count all attacks to a square by color
      if ((to & 0x88) == 0) {
        final Square squareTo = getSquare(to);
        // bitboard of all attacked squares by color
        attackedSquares[colorIdx] |= squareTo.bitboard();
        // build an index to count all attacks to a square by color
        controlAttacks[colorIdx][squareTo.bbIndex()]++;
        // get a bitboard of all attacks to this square
        attacksTo[squareTo.bbIndex()] |= square.bitboard();
      }
    }
  }
  /**
   * Evaluate ownership of aquare be determining which side has the most low
   * value piece attacks.
   * Credit to Beowulf.
   * @param position
   * @param i
   * @return positive is ownership is for white, negative if ownership if for black
   */
  private int evaluateOwnership(Position position, int i) {
    // get the attacks from both colors
    long attacks = Attacks.attacksTo(position, index64Map[i], Color.WHITE)
      | Attacks.attacksTo(position, index64Map[i], Color.BLACK);

    // Test all attackers from the least valuable upwards
    // Ownership is determined by the most less valuable pieces attacking the square

    // Pawns
    if ((attacks & (position.getPiecesBitboards(Color.WHITE, PAWN)
      | position.getPiecesBitboards(Color.BLACK, PAWN))) != 0) {
      return Long.bitCount(attacks & position.getPiecesBitboards(Color.WHITE, PAWN))
        - Long.bitCount(attacks & position.getPiecesBitboards(Color.BLACK, PAWN));
    }

    // Bishop and Knights
    final long kbW = position.getPiecesBitboards(Color.WHITE, KNIGHT)
      | position.getPiecesBitboards(Color.WHITE, BISHOP);
    final long kbB = position.getPiecesBitboards(Color.BLACK, KNIGHT)
      | position.getPiecesBitboards(Color.BLACK, BISHOP);

    if ((attacks & (kbW | kbB)) != 0) {
      return Long.bitCount(attacks & kbW) - Long.bitCount(attacks & kbB);
    }

    // Rooks
    if ((attacks & (position.getPiecesBitboards(Color.WHITE, ROOK)
      | position.getPiecesBitboards(Color.BLACK, ROOK))) != 0) {
      return Long.bitCount(attacks & position.getPiecesBitboards(Color.WHITE, ROOK))
        - Long.bitCount(attacks & position.getPiecesBitboards(Color.BLACK, ROOK));
    }

    // Queens
    if ((attacks & (position.getPiecesBitboards(Color.WHITE, QUEEN)
      | position.getPiecesBitboards(Color.BLACK, QUEEN))) != 0) {
      return Long.bitCount(attacks & position.getPiecesBitboards(Color.WHITE, QUEEN))
        - Long.bitCount(attacks & position.getPiecesBitboards(Color.BLACK, QUEEN));
    }

    return 0;
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
   * @return number of pseudo legal moves for the next player
   */
  public int pawnStructure(Position position) {
    // midGameMobility is done in the squares iteration
    evaluate(position);
    return pawnStructure;
  }

  public static int getPositionValue(Position position, int move) {

    final int nextToMove = position.getNextPlayer().ordinal();

    final int index = Move.getEnd(move).ordinal();
    final int tableIndex = nextToMove == WHITE
                           ? getWhiteTableIndex(index)
                           : getBlackTableIndex(index);

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

    return (int) (midGame * position.getGamePhaseFactor() + endGame * (1f
      - position.getGamePhaseFactor()));
  }

  @Override
  public String toString() {
    return "Evaluation{" +
      "value=" + value +
      ", special=" + special +
      ", material=" + material +
      ", midGameMaterial=" + midGameMaterial +
      ", endGameMaterial=" + endGameMaterial +
      ", piecePosition=" + piecePosition +
      ", midGamePiecePosition=" + midGamePiecePosition +
      ", endGamePiecePosition=" + endGamePiecePosition +
      ", mobility=" + mobility +
      ", midGameMobility=" + midGameMobility +
      ", endGameMobility=" + endGameMobility +
      ", kingSafety=" + kingSafety +
      ", midGameKingSafety=" + midGameKingSafety +
      ", endGameKingSafety=" + endGameKingSafety +
      ", pawnStructure=" + pawnStructure +
      ", midGamePawnStructure=" + midGamePawnStructure +
      ", endGamePawnStructure=" + endGamePawnStructure +
      ", controlAttacks=" + Arrays.toString(controlAttacks) +
      ", ownership=" + Arrays.toString(ownership) +
      ", boardControl=" + boardControl +
      ", attackedSquares=" + Arrays.toString(attackedSquares) +
      ", attacksTo=" + Arrays.toString(attacksTo) +
      ", position=" + position +
      ", nextToMove=" + nextToMove +
      ", opponent=" + opponent +
      ", pawnSquares=" + Arrays.toString(pawnSquares) +
      ", knightSquares=" + Arrays.toString(knightSquares) +
      ", bishopSquares=" + Arrays.toString(bishopSquares) +
      ", rookSquares=" + Arrays.toString(rookSquares) +
      ", queenSquares=" + Arrays.toString(queenSquares) +
      ", kingSquares=" + Arrays.toString(kingSquares) +
      ", alpha=" + alpha +
      ", beta=" + beta +
      '}';
  }

  public void printEvaluation() {
    // @formatter:off
    LOG.debug("==========================================================================================");
    LOG.debug(String.format("Evaluation: Last move was %s", Move.toString(position.getLastMove())));
    LOG.debug(String.format("%n%s", position.toBoardString()));
    LOG.debug(String.format("Position has check? %s", position.hasCheck()));
    LOG.debug(String.format("Next Move: %s", position.getNextPlayer().toString()));
    LOG.debug(String.format("Gamephase:          %5d (%,.2f)", position.getGamePhaseValue(), position.getGamePhaseFactor()));
    LOG.debug("-----------------------------------------------");
    LOG.debug(String.format("Material:        %2d %5d (%5d, %5d)", MATERIAL_WEIGHT, material, midGameMaterial, endGameMaterial));
    LOG.debug(String.format("Piece Position   %2d %5d (%5d, %5d)", POSITION_WEIGHT, piecePosition, midGamePiecePosition, endGamePiecePosition));
    LOG.debug(String.format("Mobility:        %2d %5d (%5d, %5d)", MOBILITY_WEIGHT, mobility, midGameMobility, endGameMobility));
    LOG.debug(String.format("King Safety:     %2d %5d (%5d, %5d)", KING_SAFETY_WEIGHT, kingSafety, midGameKingSafety, endGameKingSafety));
    LOG.debug(String.format("Pawn Structure:  %2d %5d (%5d, %5d)", PAWN_STRUCTURE_WEIGHT, pawnStructure, midGamePawnStructure, endGamePawnStructure));
    LOG.debug(String.format("Board Control:   %2d %5d", BOARDCONTROL_WEIGHT, boardControl));
    LOG.debug(String.format("Special:            %5d ", special));
    LOG.debug("-----------------------------------------------");
    LOG.debug(String.format("Evaluation          %5d ", value));
    // @formatter:on

    //    for (int i = 0; i < 64; i++) {
    //      Square square = index64Map[i];
    //      System.out.println(square);
    //      System.out.println(Bitboard.toString(attacksTo[i]));
    //    }

  }

  /**
   * @param type
   * @param square
   * @param pieceDirections
   * @param position
   */
  static int mobilityForPiece2(PieceType type, Square square, int[] pieceDirections,
                               Position position) {

    final int sIdx = square.bbIndex();
    final long allOccupiedBitboard = position.getAllOccupiedBitboard();

    int m = 0;
    switch (type) {
      case PAWN:
        break;
      case KING:
        break;
      case NOTYPE:
        break;
      case KNIGHT:
        m += Long.bitCount(knightAttacks[sIdx]);
        break;
      case ROOK:
        m += Long.bitCount(getSlidingMovesRank(square, allOccupiedBitboard)
                             | getSlidingMovesFile(square, allOccupiedBitboard));
        break;
      case BISHOP:
        m += Long.bitCount(getSlidingMovesDiagUp(square, allOccupiedBitboard)
                             | getSlidingMovesDiagDown(square, allOccupiedBitboard));
        break;
      case QUEEN:
        m += Long.bitCount(getSlidingMovesRank(square, allOccupiedBitboard)
                             | getSlidingMovesFile(square, allOccupiedBitboard)
                             | getSlidingMovesDiagUp(square, allOccupiedBitboard)
                             | getSlidingMovesDiagDown(square, allOccupiedBitboard));
        break;
      default:
        break;
    }

    return m;
  }
}
