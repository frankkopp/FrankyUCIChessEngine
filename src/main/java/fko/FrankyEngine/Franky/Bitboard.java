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

import fko.FrankyEngine.Franky.Square.*;
import fko.FrankyEngine.util.HelperTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fko.FrankyEngine.Franky.Square.*;

/**
 * Bitboard
 *
 * Much of this is inspired and taken from Beowulf (Colin Frayn)
 * All credit goes to him!
 */
public class Bitboard {

  private static final Logger LOG = LoggerFactory.getLogger(Bitboard.class);

  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  // proto attack bitboards - for the sliding pieces these are not attacks but rays
  /** pawn attacks for each color from square [color index][square.index64] */
  public static final long[][] pawnAttacks   = new long[2][64];
  /** knight attacks from square [square.index64] */
  public static final long[]   knightAttacks = new long[64];
  /** bishop attacks from square [square.index64] */
  public static final long[]   bishopAttacks = new long[64];
  /** rook attacks from square [square.index64] */
  public static final long[]   rookAttacks   = new long[64];
  /** queen attacks from square [square.index64] */
  public static final long[]   queenAttacks  = new long[64];
  /** king attacks from square [square.index64] */
  public static final long[]   kingAttacks   = new long[64];

  /** king ring (is identical to king attacks) */
  public static final long[] kingRing = kingAttacks;

  // rays for sliding pieces in each factor
  // NorthWest = 0 ...clockwise... West = 7
  /** factor used in <code>rays[dir][square]</code> */
  public static final int NORTHWEST = 0;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int NORTH     = 1;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int NORTHEAST = 2;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int EAST      = 3;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int SOUTHEAST = 4;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int SOUTH     = 5;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int SOUTHWEST = 6;
  /** factor used in <code>rays[dir][square]</code> */
  public static final int WEST      = 7;

  /** array of all directions used in <code>rays[dir][square]</code> */
  public static final int[] queenRays  = {0, 1, 2, 3, 4, 5, 6, 7};
  /** array of bishop directions used in <code>rays[dir][square]</code> */
  public static final int[] bishopRays = {0, 2, 4, 6};
  /** array of rook directions used in <code>rays[dir][square]</code> */
  public static final int[] rookRays   = {1, 3, 5, 7};

  /**
   * All sliding rays from a certain square.
   * When trying to determine closest piece (bit) use lowest bit for rays 0-3,
   * highest bit for 4-7
   */
  public static final long[][] rays = new long[8][64];

  /** pawn squares on file in front of each pawn and the neighbours one further
   * to the end */
  public static final long[][] passedPawnMask = new long[2][64];

  /** bitboards for all squares in between two squares */
  public static final long[][] intermediate = new long[64][64];

  /**
   * Bitboard can't be instantiated
   */
  private Bitboard() {
  }

  /**
   * Returns a string representing a bitboard in a 8 by 8 matrix
   *
   * @param bitboard
   * @return string representing a bitboard in a 8 by 8 matrix
   */
  public static String toString(long bitboard) {
    StringBuilder bitBoardLine = new StringBuilder();
    for (int i = 0; i < Long.numberOfLeadingZeros(bitboard); i++) bitBoardLine.append('0');
    bitBoardLine.append(Long.toBinaryString(bitboard));
    StringBuilder output = new StringBuilder();
    for (int r = 7; r >= 0; r--) {
      for (int f = 0; f <= 7; f++) {
        int idx = r * 8 + (7 - f);
        // output.append("(" + r + "," + f + ")");
        output.append(bitBoardLine.charAt(idx));
        output.append(" ");
      }
      output.append("\n");
    }
    return output.toString().trim();
  }

  /**
   * Returns a string representing a bitboard as a binary long group in 8-bit
   * blocks.
   *
   * @param bitboard
   * @return string representing a bitboard as a binary long group in 8-bit blocks.
   */
  public static String printBitString(long bitboard) {
    StringBuilder bitBoardLine = new StringBuilder();
    for (int i = 0; i < Long.numberOfLeadingZeros(bitboard); i++) bitBoardLine.append('0');
    if (bitboard != 0) bitBoardLine.append(Long.toBinaryString(bitboard));
    return HelperTools.insertPeriodically(bitBoardLine.toString(), ".", 8);
  }

  // @formatter:off

  /* Rotated bitbord mappings. */
  private static int[] rotateR90 = new int[] {
    56,	48,	40,	32,	24,	16,	8	, 0,
    57,	49,	41,	33,	25,	17,	9	, 1,
    58,	50,	42,	34,	26,	18,	10,	2,
    59,	51,	43,	35,	27,	19,	11,	3,
    60,	52,	44,	36,	28,	20,	12,	4,
    61,	53,	45,	37,	29,	21,	13,	5,
    62,	54,	46,	38,	30,	22,	14,	6,
    63,	55,	47,	39,	31,	23,	15,	7
  };
  private static int[] indexR90 = new int[64];

  private static int[] rotateL90 = new int[] {
    7, 15,	23,	31,	39,	47,	55,	63,
    6, 14,	22,	30,	38,	46,	54,	62,
    5, 13,	21,	29,	37,	45,	53,	61,
    4, 12,	20,	28,	36,	44,	52,	60,
    3, 11,	19,	27,	35,	43,	51,	59,
    2, 10,	18,	26,	34,	42,	50,	58,
    1,  9,	17,	25,	33,	41,	49,	57,
    0,  8,	16,	24,	32,	40,	48,	56
  };
  private static int[] indexL90 = new int[64];

  private static int[] rotateR45 = new int[] {
     0,
     8,	 1,
    16,	 9,	 2,
    24,	17,	10,	 3,
    32,	25,	18,	11,	 4,
    40,	33,	26,	19,	12,	 5,
    48,	41,	34,	27,	20,	13,	 6,
    56,	49,	42,	35,	28,	21,	14,	7,
    57,	50,	43,	36,	29,	22,	15,
    58,	51,	44,	37,	30,	23,
    59,	52,	45,	38,	31,
    60,	53,	46,	39,
    61,	54,	47,
    62,	55,
    63
  };
  private static int[] indexR45 = new int[64];

  private static int[] rotateL45 = new int[] {
     7,
     6,	15,
     5,	14,	23,
     4,	13,	22,	31,
     3,	12,	21,	30,	39,
     2,	11,	20,	29,	38,	47,
     1,	10,	19,	28,	37,	46,	55,
     0,	 9,	18,	27,	36,	45,	54,	63,
     8,	17,	26,	35,	44,	53,	62,
    16,	25,	34,	43,	52,	61,
    24,	33,	42,	51,	60,
    32,	41,	50,	59,
    40,	49,	58,
    48,	55,
    56
  };
  private static int[] indexL45 = new int[64];
  // @formatter:on

  // Reverse index to quickly calculate the index of a square in the rotated board
  static {
    for (int i = 0; i < 64; i++) {
      indexR90[rotateR90[i]] = i;
      indexL90[rotateL90[i]] = i;
      indexR45[rotateR45[i]] = i;
      indexL45[rotateL45[i]] = i;
    }
  }

  /**
   * Generates a clockwise 90° rotated bitboard
   *
   * @param bitboard
   * @return clockwise 90° rotated bitboard
   */
  public static long rotateR90(long bitboard) {
    return rotate(bitboard, rotateR90);
  }

  /**
   * Generates a counter-clockwise 90° rotated bitboard
   *
   * @param bitboard
   * @return counter clockwise 90° rotated bitboard
   */
  public static long rotateL90(long bitboard) {
    return rotate(bitboard, rotateL90);
  }

  /**
   * Generates a clockwise 45° rotated bitboard
   *
   * @param bitboard
   * @return clockwise 45° rotated bitboard
   */
  public static long rotateR45(long bitboard) {
    return rotate(bitboard, rotateR45);
  }

  /**
   * Generates a counter clockwise 45° rotated bitboard
   *
   * @param bitboard
   * @return counter clockwise 45° rotated bitboard
   */
  public static long rotateL45(long bitboard) {
    return rotate(bitboard, rotateL45);
  }

  /**
   * Iterate over all squares and copy the set bits into the new board.
   *
   * @param bitboard
   * @param rotMap
   * @return rotated bitboard
   */
  private static long rotate(long bitboard, int[] rotMap) {
    long rotated = 0L;
    for (int i = 0; i < 64; i++) {
      if ((bitboard & index64Map[rotMap[i]].bitboard()) != 0)
        rotated |= index64Map[i].bitboard();
    }
    return rotated;
  }

  /**
   * Returns the clockwise 90° rotated index of a bitboard entry
   *
   * @param index64
   * @return clockwise 90° rotated index
   */
  public static int rotateIndexR90(int index64) {
    return indexR90[index64];
  }

  /**
   * Returns the counter-clockwise 90° rotated index of a bitboard entry
   *
   * @param index64
   * @return counter clockwise 90° rotated index
   */
  public static int rotateIndexL90(int index64) {
    return indexL90[index64];
  }

  /**
   * Generates a clockwise 45° rotated index
   *
   * @param index64
   * @return clockwise 45° rotated index
   */
  public static int rotateIndexR45(int index64) {
    return indexR45[index64];
  }

  /**
   * Generates a counter clockwise 45° rotated index
   *
   * @param index64
   * @return counter clockwise 45° rotated index
   */
  public static int rotateIndexL45(int index64) {
    return indexL45[index64];
  }

  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length.
   *
   * @param square
   * @return
   */
  public static int getShiftUp(Square square) { return shiftsDiagUp[square.bbIndex()]; }
  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length.
   *
   * @param idx
   * @return
   */
  public static int getShiftUp(int idx) { return shiftsDiagUp[idx]; }
  private static final int[] shiftsDiagUp = new int[]{ // @formatter:off
     0,  1,  3,  6, 10, 15, 21, 28,
     1,  3,  6, 10, 15, 21, 28, 36,
     3,  6, 10, 15, 21, 28, 36, 43,
     6, 10, 15, 21, 28, 36, 43, 49,
    10, 15, 21, 28, 36, 43, 49, 54,
    15, 21, 28, 36, 43, 49, 54, 58,
    21, 28, 36, 43, 49, 54, 58, 61,
    28, 36, 43, 49, 54, 58, 61, 63
  }; // @formatter:on

  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length.
   *
   * @param square
   * @return
   */
  public static int getShiftDown(Square square) { return shiftsDiagDown[square.bbIndex()]; }
  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length.
   *
   * @param idx
   * @return
   */
  public static int getShiftDown(int idx) { return shiftsDiagDown[idx]; }
  private static final int[] shiftsDiagDown = new int[]{ // @formatter:off
    28, 21, 15, 10,  6,  3,  1,  0,
    36, 28, 21, 15, 10,  6,  3,  1,
    43, 36, 28, 21, 15, 10,  6,  3,
    49, 43, 36, 28, 21, 15, 10,  6,
    54, 49, 43, 36, 28, 21, 15, 10,
    58, 54, 49, 43, 36, 28, 21, 15,
    61, 58, 54, 49, 43, 36, 28, 21,
    63, 61, 58, 54, 49, 43, 36, 28
  }; // @formatter:on

  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length. To mask these bits we need the exact length.
   *
   * @param idx
   * @return
   */
  public static long getLengthMaskUp(int idx) { return (1L << lengthDiagUp[idx]) - 1; }
  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length. To mask these bits we need the exact length.
   *
   * @param square
   * @return
   */
  public static long getLengthMaskUp(Square square) {
    return (1L << lengthDiagUp[square.bbIndex()]) - 1;
  }
  static final int[] lengthDiagUp = new int[]{ // @formatter:off
    1, 2, 3, 4, 5, 6, 7, 8,
    2, 3, 4, 5, 6, 7, 8, 7,
    3, 4, 5, 6, 7, 8, 7, 6,
    4, 5, 6, 7, 8, 7, 6, 5,
    5, 6, 7, 8, 7, 6, 5, 4,
    6, 7, 8, 7, 6, 5, 4, 3,
    7, 8, 7, 6, 5, 4, 3, 2,
    8, 7, 6, 5, 4, 3, 2, 1
  }; // @formatter:on

  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length. To mask these bits we need the exact length.
   *
   * @param square
   * @return
   */
  public static long getLengthMaskDown(Square square) {
    return (1L << lengthDiagDown[square.bbIndex()]) - 1;
  }
  /**
   * To generate a bitboard where all diagonal squares are next to each other the
   * bitboard needs to be rotated. To get the diagonals squares into the lower
   * 8-bits it needs to be shifted by a certain amount depending on the diagonal's
   * length. To mask these bits we need the exact length.
   *
   * @param idx
   * @return
   */
  public static long getLengthMaskDown(int idx) { return (1L << lengthDiagDown[idx]) - 1; }
  /* These simply store the length of the diagonal in the required sense */
  static final int[] lengthDiagDown = new int[]{ // @formatter:off
    8, 7, 6, 5, 4, 3, 2, 1,
    7, 8, 7, 6, 5, 4, 3, 2,
    6, 7, 8, 7, 6, 5, 4, 3,
    5, 6, 7, 8, 7, 6, 5, 4,
    4, 5, 6, 7, 8, 7, 6, 5,
    3, 4, 5, 6, 7, 8, 7, 6,
    2, 3, 4, 5, 6, 7, 8, 7,
    1, 2, 3, 4, 5, 6, 7, 8
  }; // @formatter:on

  /**
   * Bitboards for all possible horizontal moves on the rank of the square with
   * the rank content (blocking pieces) determined from the given position.
   */
  public static long getSlidingMovesRank(Square square, Position position) {
    return getSlidingMovesRank(square, position.getAllOccupiedBitboard());
  }

  /**
   * Bitboard for all possible horizontal moves on the rank of the square with
   * the rank content (blocking pieces) determined from the given pieces bitboard.
   */
  public static long getSlidingMovesRank(Square square, long content) {
    // content = the pieces currently on the board and maybe blocking the moves
    // no rotation necessary for ranks - their squares are already in a row
    // shift to the first byte (to the right in Java)
    final Rank rank = square.getRank();
    final long pieceBitmap = content >>> ((7 - rank.ordinal()) * 8);
    // retrieve all possible moves for this square with the current content
    // and mask with the first row to erase any other pieces
    return movesRank[square.bbIndex()][(int) pieceBitmap & 255];
  }
  static final long[][] movesRank = new long[64][256];

  /**
   * Bitboard for all possible horizontal moves on the file of the square with
   * the file content (blocking pieces) determined from the given position.
   */
  public static long getSlidingMovesFile(Square square, Position position) {
    return getSlidingMoves(square, position.getAllOccupiedBitboardR90());
  }

  /**
   * Bitboard for all possible horizontal moves on the rank of the square with
   * the rank content (blocking pieces) determined from the given pieces bitboard.
   */
  public static long getSlidingMovesFile(Square square, long content) {
    // content = the pieces currently on the board and maybe blocking the moves
    // rotate the content of the board to get all file squares in a row
    final long rotated = rotateR90(content);
    return getSlidingMoves(square, rotated);
  }

  private static long getSlidingMoves(Square square, long rotated) {
    // shift to the first byte (to the right in Java)
    final File file = square.getFile();
    final long pieceBitmap = (int) ((rotated >>> (((file.ordinal()) * 8))));
    // retrieve all possible moves for this square with the current content
    // and mask with the first row to erase any other pieces not erased by shift
    return movesFile[square.bbIndex()][(int) pieceBitmap & 255];
  }
  static final long[][] movesFile = new long[64][256];

  /**
   * Bitboard for all possible diagonal up moves of the square with
   * the content (blocking pieces) determined from the given pieces position.
   */
  public static long getSlidingMovesDiagUp(Square square, Position position) {
    return getSlidingMovesDiag(square, position.getAllOccupiedBitboardR45(), getShiftUp(square),
                               getLengthMaskUp(square), movesUpDiag);
  }
  /**
   * Bitboard for all possible diagonal up moves of the square with
   *    * the content (blocking pieces) determined from the given pieces bitboard.
   */
  public static long getSlidingMovesDiagUp(Square square, long content) {
    // content = the pieces currently on the board and maybe blocking the moves
    // rotate the content of the board to get all diagonals in a row
    final long rotated = rotateR45(content);
    //    System.out.printf("Rotated: %s%n", Bitboard.printBitString(rotated));
    return getSlidingMovesDiag(square, rotated, getShiftUp(square), getLengthMaskUp(square),
                               movesUpDiag);
  }
  static final long[][] movesUpDiag = new long[64][256];

  /**
   * Bitboard for all possible diagonal down moves on the rank of the square with
   * the rank content (blocking pieces) determined from the given pieces position.
   */
  public static long getSlidingMovesDiagDown(Square square, Position position) {
    return getSlidingMovesDiag(square, position.getAllOccupiedBitboardL45(), getShiftDown(square),
                               getLengthMaskDown(square), movesDownDiag);
  }

  /**
   * Bitboard for all possible horizontal moves on the rank of the square with
   * the rank content (blocking pieces) determined from the given pieces bitboard.
   */
  public static long getSlidingMovesDiagDown(Square square, long content) {
    // content = the pieces currently on the board and maybe blocking the moves
    // rotate the content of the board to get all diagonals in a row
    final long rotated = rotateL45(content);
    //    System.out.printf("Rotated: %s%n", Bitboard.printBitString(rotated));
    // shift the correct row to the first byte (to the right in Java)
    return getSlidingMovesDiag(square, rotated, getShiftDown(square), getLengthMaskDown(square),
                               movesDownDiag);
  }

  private static long getSlidingMovesDiag(Square square, long rotated, int shift, long lengthMask,
                                          long[][] moves) {
    // shift the correct row to the first byte (to the right in Java)
    final long shifted = rotated >>> shift;
    // mask the content with the length of the diagonal to erase any other pieces
    // which have not been erased by the shift
    final long contentReMasked = shifted & lengthMask;
    // retrieve all possible moves for this square with the current content
    return moves[square.bbIndex()][(int) contentReMasked];
  }
  static final long[][] movesDownDiag = new long[64][256];

  static {

    // Pre compute all attack bitboards for all squares
    // As this is done only once speed is not an issue / readability and correctness
    // have priority

    // .parallelStream() // does not work in static initializer deadlock (Java Issue)

    // white pawn attacks - ignore that pawns can'*t be on all squares
    validSquares.forEach(square -> {
      for (int d : pawnAttackDirections) {
        final int to = square.ordinal() + d * Color.WHITE.factor;
        if ((to & 0x88) != 0) continue;
        pawnAttacks[WHITE][square.bbIndex()] |= getSquare(to).bitboard();
      }
    });
    // black pawn attacks - ignore that pawns can'*t be on all squares
    validSquares.forEach(square -> {
      for (int d : pawnAttackDirections) {
        final int to = square.ordinal() + d * Color.BLACK.factor;
        if ((to & 0x88) != 0) continue;
        pawnAttacks[BLACK][square.bbIndex()] |= getSquare(to).bitboard();
      }
    });
    // knight attacks
    validSquares.forEach(square -> {
      for (int d : knightDirections) {
        final int to = square.ordinal() + d;
        if ((to & 0x88) != 0) continue;
        knightAttacks[square.bbIndex()] |= getSquare(to).bitboard();
      }
    });
    // bishop attacks
    validSquares.forEach(square -> {
      for (int d : bishopDirections) {
        int to = square.ordinal() + d;
        while ((to & 0x88) == 0) { // slide while valid square
          final long toBitboard = getSquare(to).bitboard();
          final int sqIdx = square.bbIndex();
          bishopAttacks[sqIdx] |= toBitboard;
          switch (d) {
            case NW:
              rays[NORTHWEST][sqIdx] |= toBitboard;
              break;
            case N:
              rays[NORTH][sqIdx] |= toBitboard;
              break;
            case NE:
              rays[NORTHEAST][sqIdx] |= toBitboard;
              break;
            case E:
              rays[EAST][sqIdx] |= toBitboard;
              break;
            case SE:
              rays[SOUTHEAST][sqIdx] |= toBitboard;
              break;
            case S:
              rays[SOUTH][sqIdx] |= toBitboard;
              break;
            case SW:
              rays[SOUTHWEST][sqIdx] |= toBitboard;
              break;
            case W:
              rays[WEST][sqIdx] |= toBitboard;
              break;
          }
          to += d; // next sliding field in this factor
        }
      }
    });
    // rook attacks
    validSquares.forEach(square -> {
      for (int d : rookDirections) {
        int to = square.ordinal() + d;
        while ((to & 0x88) == 0) { // slide while valid square
          final long toBitboard = getSquare(to).bitboard();
          final int sqIdx = square.bbIndex();
          rookAttacks[square.bbIndex()] |= getSquare(to).bitboard();
          switch (d) {
            case NW:
              rays[NORTHWEST][sqIdx] |= toBitboard;
              break;
            case N:
              rays[NORTH][sqIdx] |= toBitboard;
              break;
            case NE:
              rays[NORTHEAST][sqIdx] |= toBitboard;
              break;
            case E:
              rays[EAST][sqIdx] |= toBitboard;
              break;
            case SE:
              rays[SOUTHEAST][sqIdx] |= toBitboard;
              break;
            case S:
              rays[SOUTH][sqIdx] |= toBitboard;
              break;
            case SW:
              rays[SOUTHWEST][sqIdx] |= toBitboard;
              break;
            case W:
              rays[WEST][sqIdx] |= toBitboard;
              break;
          }
          to += d; // next sliding field in this factor
        }
      }
    });
    // queen attacks
    validSquares.forEach(square -> {
      queenAttacks[square.bbIndex()] = rookAttacks[square.bbIndex()]
        | bishopAttacks[square.bbIndex()];
    });
    // king attacks
    validSquares.forEach(square -> {
      for (int d : kingDirections) {
        final int to = square.ordinal() + d;
        if ((to & 0x88) != 0) continue;
        kingAttacks[square.bbIndex()] |= getSquare(to).bitboard();
      }
    });

    // Pawn front line - all squares north of the square for white, south for black
    // white pawn - ignore that pawns can'*t be on all squares
    validSquares.forEach(square -> {
      int squareIdx = square.bbIndex();
      passedPawnMask[WHITE][square.bbIndex()] |= rays[NORTH][squareIdx];
      final int file = square.getFile().ordinal();
      final int rank = square.getRank().ordinal();
      if (file > 0 && rank < 7)
        passedPawnMask[WHITE][square.bbIndex()]
          |= rays[NORTH][square.getWest().getNorth().bbIndex()];
      if (file < 7 && rank < 7)
        passedPawnMask[WHITE][square.bbIndex()]
          |= rays[NORTH][square.getEast().getNorth().bbIndex()];
    });
    // black pawn - ignore that pawns can'*t be on all squares
    validSquares.forEach(square -> {
      int squareIdx = square.bbIndex();
      passedPawnMask[BLACK][square.bbIndex()] |= rays[SOUTH][squareIdx];
      final int file = square.getFile().ordinal();
      final int rank = square.getRank().ordinal();
      if (file > 0 && rank > 0)
        passedPawnMask[BLACK][square.bbIndex()]
          |= rays[SOUTH][square.getWest().getSouth().bbIndex()];
      if (file < 7 && rank > 0)
        passedPawnMask[BLACK][square.bbIndex()]
          |= rays[SOUTH][square.getEast().getSouth().bbIndex()];
    });

    // intermediate
    for (Square from : validSquares) {
      for (Square to : validSquares) {
        boolean found = false;
        for (int d : queenDirections) {
          int t = from.ordinal() + d;
          while ((t & 0x88) == 0) { // slide while valid square
            Square square = getSquare(t);
            if (square == to) {
              found = true;
              break;
            }
            else {
              intermediate[from.bbIndex()][to.bbIndex()] |= square.bitboard();
            }
            t += d; // next sliding field in this factor
          }
          if (found) break;
          intermediate[from.bbIndex()][to.bbIndex()] = 0L;
        }
      }
    }

    // sliding attacks - horizontal
    // Shamefully copied from Beowulf :)
    for (int file = 0; file < 8; file++) {
      for (int j = 0; j < 256; j++) {
        long mask = 0L;
        for (int x = file - 1; x >= 0; x--) {
          mask += (1L << x);
          if ((j & (1 << x)) != 0) break;
        }
        for (int x = file + 1; x < 8; x++) {
          mask += (1L << x);
          if ((j & (1 << x)) != 0) break;
        }
        for (int rankno = 0; rankno < 8; rankno++) {
          movesRank[(rankno * 8) + file][j] = mask << (rankno * 8);
        }
      }
    }

    // sliding attacks - vertical sliders
    // Shamefully copied from Beowulf :)
    for (int rank = 0; rank < 8; rank++) {
      for (int j = 0; j < 256; j++) {
        long mask = 0;
        for (int x = 6 - rank; x >= 0; x--) {
          mask += (1L << (8 * (7 - x)));
          if ((j & (1 << x)) != 0) break;
        }
        for (int x = 8 - rank; x < 8; x++) {
          mask += (1L << (8 * (7 - x)));
          if ((j & (1 << x)) != 0) break;
        }
        for (int file = 0; file < 8; file++) {
          movesFile[(rank * 8) + file][j] = mask << file;
        }
      }
    }

    // sliding attacks - up diag sliders
    // Shamefully copied from Beowulf :)
    for (int i = 0; i < 64; i++) {
      long unit = 1L;
      int file = i & 7;
      int rank = i >>> 3;

      /* Get the far left hand square on this diagonal */
      int diagstart = 7 * (Math.min(file, 7 - rank)) + i;
      int dsfile = diagstart & 7;
      int dl = lengthDiagUp[i];

      /* Loop through all possible occupations of this diagonal line */
      for (int j = 0; j < (1 << dl); j++) {
        long mask = 0, mask2 = 0;

        /* Calculate possible target squares */
        for (int x = (file - dsfile) - 1; x >= 0; x--) {
          mask += (unit << x);
          if ((j & (1 << x)) != 0) break;
        }

        for (int x = (file - dsfile) + 1; x < dl; x++) {
          mask += (unit << x);
          if ((j & (1 << x)) != 0) break;
        }

        /* Rotate the target line back onto the required diagonal */
        for (int x = 0; x < dl; x++) mask2 += (((mask >>> x) & 1) << (diagstart - (7 * x)));

        movesUpDiag[i][j] = mask2;

        //        String bitString = printBitString(j);
        //        Square square = index64Map[i];
        //        System.out.printf(">Square %s: Length: %d movesUpDiag[ %2d ][ %8s ] = %s (%d)%n",
        //                          square,
        //                          lengthDiagUp[i],
        //                          square.getIndex64(),
        //                          bitString.substring(bitString.length() - 8),
        //                          Bitboard.printBitString(movesUpDiag[i][j]),
        //                          movesUpDiag[square.getIndex64()][j]);
      }
      //System.out.println();
    }

    // sliding attacks - down diag sliders
    // Shamefully copied from Beowulf :)
    for (int i = 0; i < 64; i++) {
      long unit = 1L;
      int file = i & 7;
      int rank = i >>> 3;

      /* Get the far left hand square on this diagonal */
      int diagstart = i - 9 * (Math.min(file, rank));
      int dsfile = diagstart & 7;
      int dl = lengthDiagDown[i];

      /* Loop through all possible occupations of this diagonal line */
      for (int j = 0; j < (1 << dl); j++) {
        long mask = 0, mask2 = 0;

        /* Calculate possible target squares */
        for (int x = (file - dsfile) - 1; x >= 0; x--) {
          mask += (unit << x);
          if ((j & (1 << x)) != 0) break;
        }

        for (int x = (file - dsfile) + 1; x < dl; x++) {
          mask += (unit << x);
          if ((j & (1 << x)) != 0) break;
        }
        /* Rotate target squares back */
        for (int x = 0; x < dl; x++) mask2 += (((mask >> x) & 1) << (diagstart + (9 * x)));
        movesDownDiag[i][j] = mask2;

        //        long output = movesDownDiag[i][j];
        //        String bitString = printBitString(j);
        //        Square square = index64Map[i];
        //        System.out.printf(">Square %s: Length: %d movesUpDiag[ %2d ][ %8s ] = %s (%d)
        //        %n", square,
        //                          dl, square.getIndex64(), bitString.substring(bitString.length
        //                          () - 8),
        //                          Bitboard.printBitString(output), output);
      }
      //      System.out.println();
    }

    // double check with asserts on the empty board - should identical to the
    // generated Attack lists of the sliding pieces (which we could replace now
    // with the lists but we keep them for now)
    validSquares.forEach(square -> {
      int i = square.bbIndex();
      long rookLike = movesRank[i][0] | movesFile[i][0];
      long bishopLike = movesUpDiag[i][0] | movesDownDiag[i][0];
      long queenLike = rookLike | bishopLike;
      assert rookLike == rookAttacks[i];
      assert bishopLike == bishopAttacks[i];
      assert queenLike == queenAttacks[i];
    });

  }

  /**
   * Finds the right most (in Java the Least Significant Bit from the right) set bit
   * in a bitboard and returns the according index.
   * Can be used to loop through all set squares in a bitboard in conjunction
   * with removeFirstSquare()
   *
   * @param bitboard
   * @return the first Square index of the given Bitboard from a8-h8-h1
   */
  public static int getLSB(long bitboard) {
    return Long.numberOfTrailingZeros(bitboard);
  }

  /**
   * Finds the left most (in Java the Most Significant Bit from the left) set bit
   * in a bitboard and returns the according index.
   * Can be used to loop through all set squares in a bitboard in conjunction
   * with removeFirstSquare()
   * Needs to be subtracted from 63 to get the correct bit index in a bitboard
   *
   * @param bitboard
   * @return the first Square index of the given Bitboard from a8-h8-h1
   */
  public static int getMSB(long bitboard) {
    return Long.numberOfLeadingZeros(bitboard);
  }

  /**
   * Finds the most significant bit in a bitboard and removes it.
   * Can be used to loop through all set squares in a bitboard in conjunction
   * with getFirstSquare()
   *
   * @param bitboard
   * @return the bitboard without the removed square
   */
  public static long removeMSB(long bitboard) {
    return bitboard & (bitboard - 1);
  }

  /**
   * Finds the bit in a bitboard and removes it.
   * Can be used to loop through all set squares in a bitboard in conjunction
   * with getFirstSquare()
   *
   * @param bitboard
   * @param bitIdx LSB to MSB 0...63
   * @return the bitboard without the removed square
   */
  public static long removeBit(long bitboard, int bitIdx) {
    final long bit = 1L << bitIdx;
    assert (bitboard & bit) != 0 : "Bit to be removed not set.";
    return bitboard ^ bit;
  }
}
