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

  public static long[][] pawnAttacks   = new long[2][64];
  public static long[]   knightAttacks = new long[64];
  public static long[]   bishopAttacks = new long[64];
  public static long[]   rookAttacks   = new long[64];
  public static long[]   queenAttacks  = new long[64];
  public static long[]   kingAttacks   = new long[64];

  static {

    // Pre compute all attack bitboards for all squares

    // white pawn attacks - ignore that pawns can'*t be on all squares
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        int[] directions = Square.pawnAttackDirections;
        for (int d : directions) {
          final int to = square.ordinal() + d * Color.WHITE.direction;
          if ((to & 0x88) != 0) continue;
          pawnAttacks[Color.WHITE.ordinal()][square.index64] |= Square.getSquare(to).bitBoard;
        }
      });
    // black pawn attacks - ignore that pawns can'*t be on all squares
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        int[] directions = Square.pawnAttackDirections;
        for (int d : directions) {
          final int to = square.ordinal() + d * Color.BLACK.direction;
          if ((to & 0x88) != 0) continue;
          pawnAttacks[Color.BLACK.ordinal()][square.index64] |= Square.getSquare(to).bitBoard;
        }
      });
    // knight attacks
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        int[] directions = Square.knightDirections;
        for (int d : directions) {
          final int to = square.ordinal() + d;
          if ((to & 0x88) != 0) continue;
          knightAttacks[square.index64] |= Square.getSquare(to).bitBoard;
        }
      });
    // bishop attacks
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        int[] directions = Square.bishopDirections;
        for (int d : directions) {
          int to = square.ordinal() + d;
          while ((to & 0x88) == 0) { // slide while valid square
            bishopAttacks[square.index64] |= Square.getSquare(to).bitBoard;
            to += d; // next sliding field in this direction
          }
        }
      });
    // rook attacks
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        int[] directions = Square.rookDirections;
        for (int d : directions) {
          int to = square.ordinal() + d;
          while ((to & 0x88) == 0) { // slide while valid square
            rookAttacks[square.index64] |= Square.getSquare(to).bitBoard;
            to += d; // next sliding field in this direction
          }
        }
      });
    // quuen attacks
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        queenAttacks[square.index64] = rookAttacks[square.index64] | bishopAttacks[square.index64];
      });
    // king attacks
    Square.validSquares
      //.parallelStream()
      .forEach(square -> {
        int[] directions = Square.kingDirections;
        for (int d : directions) {
          final int to = square.ordinal() + d;
          kingAttacks[square.index64] |= Square.getSquare(to).bitBoard;
        }
      });

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
        output.append(bitBoardLine.charAt(r * 8 + f));
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
