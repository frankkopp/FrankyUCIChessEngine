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

/**
 * Predefined values for Evaluation of positions.
 */
class EvaluationConfig {

  static final int TEMPO       = 10;
  static final int CHECK_VALUE = 30;

  static final int BISHOP_PAIR = 30;
  static final int KNIGHT_PAIR = 10;
  static final int ROOK_PAIR   = 15;

  static final int KNIGHTS_MOBILITY_FACTOR = 2;
  static final int BISHOP_MOBILITY_FACTOR  = 2;
  static final int ROOK_MOBILITY_FACTOR    = 2;
  static final int QUEEN_MOBILITY_FACTOR   = 1;

  static final float MATERIAL_WEIGHT       = 1;
  static final float POSITION_WEIGHT       = 0.5f;
  static final float MOBILITY_WEIGHT       = 2;
  static final float KING_SAFETY_WEIGHT    = 1;
  static final float PAWN_STRUCTURE_WEIGHT = 1;
  static final float BOARDCONTROL_WEIGHT   = 5;

  static final int KING_SAFETY_PAWNSHIELD   = 10;
  static final int KING_RING_ATTACK_PENALTY = -10;
  static final int CORNERED_ROOK_PENALTY    = -50;

  static final int DOUBLED_PAWN_PENALTY = -10;
  static final int PASSED_PAWN          = 30;

  public static int CONTEMPT_FACTOR = 2; // will be multiplied with game phase factor

  // @formatter:off
  // PAWN Tables
  // use the index64 for white and 63-index64 for black

  static int[] pawnsMidGame  = new int[] {
     0,  0,  0,  0,  0,  0,  0,  0,
     0,  0,  0,  0,  0,  0,  0,  0,
     0,  5,  5,  5,  5,  5,  5,  0,
     5,  5, 10, 30, 30, 10,  5,  5,
     0,  0,  0, 25, 25,  0,  0,  0,
     5, -5,-10,  0,  0,-10, -5,  5,
     5, 10, 10,-30,-30, 10, 10,  5,
     0,  0,  0,  0,  0,  0,  0,  0
  };
  static int[] pawnsEndGame  = new int[] {
     0,  0,  0,  0,  0,  0,  0,  0,
    50, 50, 50, 50, 50, 50, 50, 50,
    10, 20, 20, 30, 30, 20, 20, 10,
    10, 10, 20, 30, 30, 20, 10, 10,
    10, 10, 20, 30, 30, 20, 10, 10,
     5, 10, 10, 10, 10, 10, 10,  5,
     5, 10, 10, 10, 10, 10, 10,  5,
     0,  0,  0,  0,  0,  0,  0,  0
  };
  // KNIGHT Tables
    static int[] knightMidGame = new int[] {
    -50,-40,-30,-30,-30,-30,-40,-50,
    -40,-20,  0,  0,  0,  0,-20,-40,
    -30,  0, 10, 15, 15, 10,  0,-30,
    -30,  5, 15, 20, 20, 15,  5,-30,
    -30,  0, 15, 20, 20, 15,  0,-30,
    -30,  5, 10, 15, 15, 10,  5,-30,
    -40,-20,  0,  5,  5,  0,-20,-40,
    -50,-40,-20,-30,-30,-20,-40,-50,
  };
  static int[] knightEndGame = new int[] {
    -50,-40,-30,-30,-30,-30,-40,-50,
    -40,-20,  0,  0,  0,  0,-20,-40,
    -30,  0, 10, 15, 15, 10,  0,-30,
    -30,  0, 15, 20, 20, 15,  0,-30,
    -30,  0, 15, 20, 20, 15,  0,-30,
    -30,  0, 10, 15, 15, 10,  0,-30,
    -40,-20,  0,  0,  0,  0,-20,-40,
    -50,-40,-20,-30,-30,-20,-40,-50,
  };
  // BISHOP Tables
    static int[] bishopMidGame = new int[] {
    -20,-10,-10,-10,-10,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5, 10, 10,  5,  0,-10,
    -10,  5,  5, 10, 10,  5,  5,-10,
    -10,  0, 10, 10, 10, 10,  0,-10,
    -10, 10, 10, 10, 10, 10, 10,-10,
    -10,  5,  0,  0,  0,  0,  5,-10,
    -20,-10,-40,-10,-10,-40,-10,-20,
  };
  static int[] bishopEndGame = new int[] {
    -20,-10,-10,-10,-10,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
    -10,  0,  5, 10, 10,  5,  0,-10,
    -10,  0,  5, 10, 10,  5,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -20,-10,-10,-10,-10,-10,-10,-20,
  };
  // ROOK Tables
    static int[] rookMidGame   = new int[] {
      5,  5,  5,  5,  5,  5,  5,  5,
     10, 10, 10, 10, 10, 10, 10, 10,
      0,  0,  0,  0,  0,  0,  0,  0,
      0,  0,  0,  0,  0,  0,  0,  0,
      0,  0,  0,  0,  0,  0,  0,  0,
      0,  0,  0,  0,  0,  0,  0,  0,
      0,  0,  0,  0,  0,  0,  0,  0,
    -15,-10, 15, 15, 15, 15,-10,-15,
  };
  static int[] rookEndGame   = new int[] {
    5,  5,  5,  5,  5,  5,  5,  5,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,
  };
  // Queen Tables
    static int[] queenMidGame  = new int[] {
    -20,-10,-10, -5, -5,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  0,  0,  0,  0,  0,-10,
     -5,  0,  2,  2,  2,  2,  0, -5,
     -5,  0,  5,  5,  5,  5,  0, -5,
    -10,  0,  5,  5,  5,  5,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
    -20,-10,-10, -5, -5,-10,-10,-20
  };
  static int[] queenEndGame  = new int[] {
    -20,-10,-10, -5, -5,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
     -5,  0,  5,  5,  5,  5,  0, -5,
     -5,  0,  5,  5,  5,  5,  0, -5,
    -10,  0,  5,  5,  5,  5,  0,-10,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -20,-10,-10, -5, -5,-10,-10,-20
  };
  // King Tables
    static int[] kingMidGame   = new int[] {
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -20,-30,-30,-40,-40,-30,-30,-20,
    -10,-20,-20,-30,-30,-30,-20,-10,
      0,  0,-20,-20,-20,-20,  0,  0,
     20, 50,  0,-20,-20,   0, 50, 20
  };
  static int[] kingEndGame   = new int[] {
    -50,-30,-30,-20,-20,-30,-30,-50,
    -30,-20,-10,  0,  0,-10,-20,-30,
    -30,-10, 20, 30, 30, 20,-10,-30,
    -30,-10, 30, 40, 40, 30,-10,-30,
    -30,-10, 30, 40, 40, 30,-10,-30,
    -30,-10, 20, 30, 30, 20,-10,-30,
    -30,-30,  0,  0,  0,  0,-30,-30,
    -50,-30,-30,-30,-30,-30,-30,-50
  };
  // @formatter:on

}
