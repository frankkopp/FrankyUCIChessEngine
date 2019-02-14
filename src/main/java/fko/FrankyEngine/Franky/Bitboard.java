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

/**
 * Bitboard
 */
public class Bitboard {

  private static final Logger LOG = LoggerFactory.getLogger(Bitboard.class);

  // upward diagonal bitboards @formatter:off
  public static final long a8UpDiag = 0b00000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long a7UpDiag = 0b00000010_00000001_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long a6UpDiag = 0b00000100_00000010_00000001_00000000_00000000_00000000_00000000_00000000L;
  public static final long a5UpDiag = 0b00001000_00000100_00000010_00000001_00000000_00000000_00000000_00000000L;
  public static final long a4UpDiag = 0b00010000_00001000_00000100_00000010_00000001_00000000_00000000_00000000L;
  public static final long a3UpDiag = 0b00100000_00010000_00001000_00000100_00000010_00000001_00000000_00000000L;
  public static final long a2UpDiag = 0b01000000_00100000_00010000_00001000_00000100_00000010_00000001_00000000L;
  public static final long a1UpDiag = 0b10000000_01000000_00100000_00010000_00001000_00000100_00000010_00000001L;
  public static final long b1UpDiag = 0b00000000_10000000_01000000_00100000_00010000_00001000_00000100_00000010L;
  public static final long c1UpDiag = 0b00000000_00000000_10000000_01000000_00100000_00010000_00001000_00000100L;
  public static final long d1UpDiag = 0b00000000_00000000_00000000_10000000_01000000_00100000_00010000_00001000L;
  public static final long e1UpDiag = 0b00000000_00000000_00000000_00000000_10000000_01000000_00100000_00010000L;
  public static final long f1UpDiag = 0b00000000_00000000_00000000_00000000_00000000_10000000_01000000_00100000L;
  public static final long g1UpDiag = 0b00000000_00000000_00000000_00000000_00000000_00000000_10000000_01000000L;
  public static final long h1UpDiag = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_10000000L;

  // downward diagonal bitboards
  public static final long a1DownDiag = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L;
  public static final long a2DownDiag = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000010L;
  public static final long a3DownDiag = 0b00000000_00000000_00000000_00000000_00000000_00000001_00000010_00000100L;
  public static final long a4DownDiag = 0b00000000_00000000_00000000_00000000_00000001_00000010_00000100_00001000L;
  public static final long a5DownDiag = 0b00000000_00000000_00000000_00000001_00000010_00000100_00001000_00010000L;
  public static final long a6DownDiag = 0b00000000_00000000_00000001_00000010_00000100_00001000_00010000_00100000L;
  public static final long a7DownDiag = 0b00000000_00000001_00000010_00000100_00001000_00010000_00100000_01000000L;
  public static final long a8DownDiag = 0b00000001_00000010_00000100_00001000_00010000_00100000_01000000_10000000L;
  public static final long b8DownDiag = 0b00000010_00000100_00001000_00010000_00100000_01000000_10000000_00000000L;
  public static final long c8DownDiag = 0b00000100_00001000_00010000_00100000_01000000_10000000_00000000_00000000L;
  public static final long d8DownDiag = 0b00001000_00010000_00100000_01000000_10000000_00000000_00000000_00000000L;
  public static final long e8DownDiag = 0b00010000_00100000_01000000_10000000_00000000_00000000_00000000_00000000L;
  public static final long f8DownDiag = 0b00100000_01000000_10000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long g8DownDiag = 0b01000000_10000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long h8DownDiag = 0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  // @formatter:on

  // proto attack bitboards - for the sliding pieces these are not attacks but rays
  public static final long[][] pawnAttacks   = new long[2][64];
  public static final long[]   knightAttacks = new long[64];
  public static final long[]   bishopAttacks = new long[64];
  public static final long[]   rookAttacks   = new long[64];
  public static final long[]   queenAttacks  = new long[64];
  public static final long[]   kingAttacks   = new long[64];

  // rays for sliding pieces in each direction
  // NorthWest = 0 ...clockwise... West = 7
  public static final int      NORTHWEST  = 0;
  public static final int      NORTH      = 1;
  public static final int      NORTHEAST  = 2;
  public static final int      EAST       = 3;
  public static final int      SOUTHEAST  = 4;
  public static final int      SOUTH      = 5;
  public static final int      SOUTHWEST  = 6;
  public static final int      WEST       = 7;
  public static final int[]    queenRays  = {0, 1, 2, 3, 4, 5, 6, 7};
  public static final int[]    bishopRays = {0, 2, 4, 6};
  public static final int[]    rookRays   = {1, 3, 5, 7};
  /**
   * All queen rays from a certain square.
   * When determining closest piece (bit) use lowest bit for rays 0-3,
   * highest bit for 4-7
   */
  public static final long[][] rays       = new long[8][64];

  /** king ring (is identical to king attacks) */
  public static final long[] kingRing = kingAttacks;

  /** pawn squares on file in front of each pawn */
  public static final long[][] pawnFrontLines = new long[2][64];
  // TODO add passed pawn

  /** all direction from sq1 to sq2 along queen movements */
  public static final int[][] direction = new int[64][64];

  /** bitboards for all squares in between two squares */
  public static final long[][] intermediate = new long[64][64];

  static {

    // Pre compute all attack bitboards for all squares
    //.parallelStream() // does not work in static initializer deadlock (Java Issue)

    // white pawn attacks - ignore that pawns can'*t be on all squares
    Square.validSquares.forEach(square -> {
      int[] directions = Square.pawnAttackDirections;
      for (int d : directions) {
        final int to = square.ordinal() + d * Color.WHITE.direction;
        if ((to & 0x88) != 0) continue;
        pawnAttacks[Color.WHITE.ordinal()][square.index64] |= Square.getSquare(to).bitBoard;
      }
    });
    // black pawn attacks - ignore that pawns can'*t be on all squares
    Square.validSquares.forEach(square -> {
      int[] directions = Square.pawnAttackDirections;
      for (int d : directions) {
        final int to = square.ordinal() + d * Color.BLACK.direction;
        if ((to & 0x88) != 0) continue;
        pawnAttacks[Color.BLACK.ordinal()][square.index64] |= Square.getSquare(to).bitBoard;
      }
    });
    // knight attacks
    Square.validSquares.forEach(square -> {
      int[] directions = Square.knightDirections;
      for (int d : directions) {
        final int to = square.ordinal() + d;
        if ((to & 0x88) != 0) continue;
        knightAttacks[square.index64] |= Square.getSquare(to).bitBoard;
      }
    });
    // bishop attacks
    Square.validSquares.forEach(square -> {
      int[] directions = Square.bishopDirections;
      for (int d : directions) {
        int to = square.ordinal() + d;
        while ((to & 0x88) == 0) { // slide while valid square
          final long toBitboard = Square.getSquare(to).bitBoard;
          final int sqIdx = square.index64;
          bishopAttacks[sqIdx] |= toBitboard;
          switch (d) {
            case Square.NW:
              rays[NORTHWEST][sqIdx] |= toBitboard;
              break;
            case Square.N:
              rays[NORTH][sqIdx] |= toBitboard;
              break;
            case Square.NE:
              rays[NORTHEAST][sqIdx] |= toBitboard;
              break;
            case Square.E:
              rays[EAST][sqIdx] |= toBitboard;
              break;
            case Square.SE:
              rays[SOUTHEAST][sqIdx] |= toBitboard;
              break;
            case Square.S:
              rays[SOUTH][sqIdx] |= toBitboard;
              break;
            case Square.SW:
              rays[SOUTHWEST][sqIdx] |= toBitboard;
              break;
            case Square.W:
              rays[WEST][sqIdx] |= toBitboard;
              break;
          }
          to += d; // next sliding field in this direction
        }
      }
    });
    // rook attacks
    Square.validSquares.forEach(square -> {
      int[] directions = Square.rookDirections;
      for (int d : directions) {
        int to = square.ordinal() + d;
        while ((to & 0x88) == 0) { // slide while valid square
          final long toBitboard = Square.getSquare(to).bitBoard;
          final int sqIdx = square.index64;
          rookAttacks[square.index64] |= Square.getSquare(to).bitBoard;
          switch (d) {
            case Square.NW:
              rays[NORTHWEST][sqIdx] |= toBitboard;
              break;
            case Square.N:
              rays[NORTH][sqIdx] |= toBitboard;
              break;
            case Square.NE:
              rays[NORTHEAST][sqIdx] |= toBitboard;
              break;
            case Square.E:
              rays[EAST][sqIdx] |= toBitboard;
              break;
            case Square.SE:
              rays[SOUTHEAST][sqIdx] |= toBitboard;
              break;
            case Square.S:
              rays[SOUTH][sqIdx] |= toBitboard;
              break;
            case Square.SW:
              rays[SOUTHWEST][sqIdx] |= toBitboard;
              break;
            case Square.W:
              rays[WEST][sqIdx] |= toBitboard;
              break;
          }
          to += d; // next sliding field in this direction
        }
      }
    });
    // quuen attacks
    Square.validSquares.forEach(square -> {
      queenAttacks[square.index64] = rookAttacks[square.index64] | bishopAttacks[square.index64];
    });
    // king attacks
    Square.validSquares.forEach(square -> {
      int[] directions = Square.kingDirections;
      for (int d : directions) {
        final int to = square.ordinal() + d;
        if ((to & 0x88) != 0) continue;
        kingAttacks[square.index64] |= Square.getSquare(to).bitBoard;
      }
    });

    // Pawn front line - all squares north of the square for white, south for black
    // white pawn - ignore that pawns can'*t be on all squares
    // TODO precompute passed pawn masks
    Square.validSquares.forEach(square -> {
      int to = square.ordinal() + Square.N;
      while ((to & 0x88) == 0) {
        pawnFrontLines[Color.WHITE.ordinal()][square.index64] |= Square.getSquare(to).bitBoard;
        to += Square.N;
      }
    });
    // black pawn - ignore that pawns can'*t be on all squares
    Square.validSquares.forEach(square -> {
      int to = square.ordinal() + Square.S;
      while ((to & 0x88) == 0) {
        pawnFrontLines[Color.BLACK.ordinal()][square.index64] |= Square.getSquare(to).bitBoard;
        to += Square.S;
      }
    });

    // directions and intermediate
    for (Square from : Square.validSquares) {
      for (Square to : Square.validSquares) {
        boolean found = false;
        int[] directions = Square.queenDirections;
        for (int d : directions) {
          int t = from.ordinal() + d;
          while ((t & 0x88) == 0) { // slide while valid square
            Square square = Square.getSquare(t);
            if (square == to) {
              direction[from.index64][to.index64] = d;
              found = true;
              break;
            }
            else {
              intermediate[from.index64][to.index64] |= square.bitBoard;
            }
            t += d; // next sliding field in this direction
          }
          if (found) break;
          intermediate[from.index64][to.index64] = 0L;
        }
      }
    }

  }

  /**
   * Bitboard can't be instantiated
   */
  private Bitboard() {
  }

  /**
   * Returns a string representing a bitboard in a 8 by 8 matrix
   * @param bitboard
   * @return
   */
  public static String toString(long bitboard) {
    StringBuilder bitBoardLine = new StringBuilder();
    for (int i = 0; i < Long.numberOfLeadingZeros(bitboard); i++) bitBoardLine.append('0');
    bitBoardLine.append(Long.toBinaryString(bitboard));
    StringBuilder output = new StringBuilder();
    for (int r = 0; r <= 7; r++) {
      for (int f = 7; f >= 0; f--) {
        int idx = r * 8 + f;
        // output.append("(" + r + "," + f + ")");
        output.append(bitBoardLine.charAt(idx));
        output.append(" ");
      }
      output.append(System.lineSeparator());
    }
    return output.toString().trim();
  }

  /**
   * Returns a string representing a bitboard as a binary long
   * @param bitboard
   * @return
   */
  public static String printBitString(long bitboard) {
    StringBuilder bitBoardLine = new StringBuilder();
    for (int i = 0; i < Long.numberOfLeadingZeros(bitboard); i++) bitBoardLine.append('0');
    String binaryString = Long.toBinaryString(bitboard);
    if (binaryString.equals("0")) binaryString = "";
    return bitBoardLine.append(binaryString).toString();
  }

}
