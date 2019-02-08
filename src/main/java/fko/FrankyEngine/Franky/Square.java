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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static fko.FrankyEngine.Franky.Bitboard.*;

/**
 * This enumeration class represents all squares on a chess board.
 * It uses a numbering for a x88 board so that a1=0 and a2=16
 * It has several convenience methods for calculation in relation
 * to other squares.
 * <p>
 * As enumeration is type safe and also very fast this is preferred over
 * static final int.
 */
public enum Square {

  // @formatter:off
  /*
   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15 */
  a1, b1, c1, d1, e1, f1, g1, h1, i1, j1, k1, l1, m1, n1, o1, p1, // 0-15
  a2, b2, c2, d2, e2, f2, g2, h2, i2, j2, k2, l2, m2, n2, o2, p2, // 16-31
  a3, b3, c3, d3, e3, f3, g3, h3, i3, j3, k3, l3, m3, n3, o3, p3, // 32-47
  a4, b4, c4, d4, e4, f4, g4, h4, i4, j4, k4, l4, m4, n4, o4, p4, // 48-63
  a5, b5, c5, d5, e5, f5, g5, h5, i5, j5, k5, l5, m5, n5, o5, p5, // 64-79
  a6, b6, c6, d6, e6, f6, g6, h6, i6, j6, k6, l6, m6, n6, o6, p6, // 80-95
  a7, b7, c7, d7, e7, f7, g7, h7, i7, j7, k7, l7, m7, n7, o7, p7, // 96-111
  a8, b8, c8, d8, e8, f8, g8, h8, i8, j8, k8, l8, m8, n8, o8, p8, // 112-127
  NOSQUARE;

  // pre-filled list with all squares
  public static final Square[] values;

  /**
   * pre-computed if square is valid
   */
  public final boolean validSquare;

  /**
   * pre-computed index for a 64bit index a1=0, h8=63
   */
  public final int index64;

  /**
   * pre-computed bitBoard representation
   */
  public final long bitBoard;

  private long upDiag;
  private long downDiag;

  /**
   * pre-filled list with all valid squares
   */
  public static final List<Square> validSquares;
  // Move deltas north, south, east, west and combinations
  static final int N = 16;
  static final int E = 1;
  static final int S = -16;
  static final int W = -1;
  static final int NE = N + E;
  static final int SE = S + E;
  static final int SW = S + W;

  static final int NW = N + W;
  static final int[] pawnDirections = {N, NW, NE};
  static final int[] pawnAttackDirections = {NW, NE};
  static final int[] knightDirections = {
      N + N + E,
      N + E + E,
      S + E + E,
      S + S + E,
      S + S + W,
      S + W + W,
      N + W + W,
      N + N + W
  };
  static final int[] bishopDirections = {
      NE, SE, SW, NW
  };
  static final int[] rookDirections = {
      N, E, S, W
  };
  static final int[] queenDirections = {
      N, NE, E, SE,
      S, SW, W, NW
  };
  static final int[] kingDirections = {
      N, NE, E, SE,
      S, SW, W, NW
  };

  static {
    values = Square.values();
    validSquares = Collections.unmodifiableList(
      Arrays.stream(values()).filter(Square::isValidSquare).collect(Collectors.toList()));

    // precompute diagonals for squares
    for (Square sq : values()) {
      if ((sq.bitBoard & a8UpDiag) > 0) sq.upDiag = a8UpDiag;
      else if ((sq.bitBoard & a7UpDiag) > 0) sq.upDiag = a7UpDiag;
      else if ((sq.bitBoard & a6UpDiag) > 0) sq.upDiag = a6UpDiag;
      else if ((sq.bitBoard & a5UpDiag) > 0) sq.upDiag = a5UpDiag;
      else if ((sq.bitBoard & a4UpDiag) > 0) sq.upDiag = a4UpDiag;
      else if ((sq.bitBoard & a3UpDiag) > 0) sq.upDiag = a3UpDiag;
      else if ((sq.bitBoard & a2UpDiag) > 0) sq.upDiag = a2UpDiag;
      else if ((sq.bitBoard & a1UpDiag) > 0) sq.upDiag = a1UpDiag;
      else if ((sq.bitBoard & b1UpDiag) > 0) sq.upDiag = b1UpDiag;
      else if ((sq.bitBoard & c1UpDiag) > 0) sq.upDiag = c1UpDiag;
      else if ((sq.bitBoard & d1UpDiag) > 0) sq.upDiag = d1UpDiag;
      else if ((sq.bitBoard & e1UpDiag) > 0) sq.upDiag = e1UpDiag;
      else if ((sq.bitBoard & f1UpDiag) > 0) sq.upDiag = f1UpDiag;
      else if ((sq.bitBoard & g1UpDiag) > 0) sq.upDiag = g1UpDiag;
      else if ((sq.bitBoard & h1UpDiag) > 0) sq.upDiag = h1UpDiag;

      if ((sq.bitBoard & a1DownDiag) > 0) sq.downDiag = a1DownDiag;
      else if ((sq.bitBoard & a2DownDiag) > 0) sq.downDiag = a2DownDiag;
      else if ((sq.bitBoard & a3DownDiag) > 0) sq.downDiag = a3DownDiag;
      else if ((sq.bitBoard & a4DownDiag) > 0) sq.downDiag = a4DownDiag;
      else if ((sq.bitBoard & a5DownDiag) > 0) sq.downDiag = a5DownDiag;
      else if ((sq.bitBoard & a6DownDiag) > 0) sq.downDiag = a6DownDiag;
      else if ((sq.bitBoard & a7DownDiag) > 0) sq.downDiag = a7DownDiag;
      else if ((sq.bitBoard & a8DownDiag) > 0) sq.downDiag = a8DownDiag;
      else if ((sq.bitBoard & b8DownDiag) > 0) sq.downDiag = b8DownDiag;
      else if ((sq.bitBoard & c8DownDiag) > 0) sq.downDiag = c8DownDiag;
      else if ((sq.bitBoard & d8DownDiag) > 0) sq.downDiag = d8DownDiag;
      else if ((sq.bitBoard & e8DownDiag) > 0) sq.downDiag = e8DownDiag;
      else if ((sq.bitBoard & f8DownDiag) > 0) sq.downDiag = f8DownDiag;
      else if ((sq.bitBoard & g8DownDiag) > 0) sq.downDiag = g8DownDiag;
      else if ((sq.bitBoard & h8DownDiag) > 0) sq.downDiag = h8DownDiag;
    }

  } // @formatter:on

  /**
   * Contructor
   * Computes all relevant precompeted indexes and bitboards
   */
  Square() {
    if ((this.ordinal() & 0x88) == 0) {
      validSquare = true;
      index64 = this.ordinal() / 16 * 8 + this.ordinal() % 16;
      // set bit for bitboard
      bitBoard = 1L << index64;
    }
    else {
      validSquare = false;
      index64 = -1;
      bitBoard = 0L;
    }
  }

  /**
   * @param index
   * @return the Square for the given index of a 0x88 board - returns NOSQUARE if not a valid
   * index
   */
  public static Square getSquare(int index) {
    if ((index & 0x88) != 0) return NOSQUARE;
    return Square.values[index];
  }

  /**
   * Returns the square from the given notation and checks if
   * it is a valid square.
   *
   * @param s
   * @return The Square of the notation or NOSQAURE if the notation was invalid
   */
  public static Square fromUCINotation(final String s) {
    final Square square;
    try {
      square = Square.valueOf(s);
    } catch (IllegalArgumentException e) {
      return NOSQUARE;
    }
    return square.isValidSquare() ? square : NOSQUARE;
  }

  /**
   * @return true if Square is a valid chess square
   */
  public boolean isValidSquare() {
    return validSquare;
  }

  /**
   * Returns the Square for the given file and rank
   * @param file
   * @param rank
   * @return the Square for the given file and rank
   */
  public static Square getSquare(int file, int rank) {
    if (file < 1 || file > 8 || rank < 1 || rank > 8) return Square.NOSQUARE;
    // index starts with 0 while file and rank start with 1 - decrease
    final int index = (rank - 1) * 16 + (file - 1);
    return Square.values[index];
  }

  /**
   * Returns the square north of this square. as seen from the white side.
   *
   * @return square north
   */
  public Square getNorth() {
    int index = this.ordinal() + N;
    if ((index & 0x88) != 0) return NOSQUARE;
    return Square.values[index];
  }

  /**
   * Returns the square north of this square. as seen from the white side.
   *
   * @return square north
   */
  public Square getSouth() {
    int index = this.ordinal() + S;
    if ((index & 0x88) != 0) return NOSQUARE;
    return Square.values[index];
  }

  /**
   * Returns the square north of this square. as seen from the white side.
   *
   * @return square north
   */
  public Square getEast() {
    int index = this.ordinal() + E;
    if ((index & 0x88) != 0) return NOSQUARE;
    return Square.values[index];
  }

  /**
   * Returns the square north of this square. as seen from the white side.
   *
   * @return square north
   */
  public Square getWest() {
    int index = this.ordinal() + W;
    if ((index & 0x88) != 0) return NOSQUARE;
    return Square.values[index];
  }

  /**
   * @return Square.File for this Square
   */
  public File getFile() {
    if (!this.validSquare) return File.NOFILE;
    return File.values[this.ordinal() % 16];
  }

  /**
   * @return Square.Rank for this Square
   */
  public Rank getRank() {
    if (!this.validSquare) return Rank.NORANK;
    return Rank.values[this.ordinal() >>> 4];
  }

  /**
   * Returns a list of all valid squares in the correct order. [0]=a1, [63]=h8
   */
  public static List<Square> getValueList() {
    return validSquares;
  }

  /**
   * pre-computed on which upwards diagonal this square is
   */
  public long getUpDiag() {
    return upDiag;
  }

  /**
   * pre-computed on which downwards diagonal this square is
   */
  public long getDownDiag() {
    return downDiag;
  }

  /**
   * This enum represents all files of a chess board. If used in a loop via values() omit NOFILE.
   */
  public enum File {
    a, b, c, d, e, f, g, h, NOFILE;

    // pre-filled list with all squares
    public static final File[] values;
    public final long bitBoard;

    static {
      values = File.values();
    }

    File() {
      final long a = 0b0000000100000001000000010000000100000001000000010000000100000001L;
      if (ordinal() < 8) bitBoard = a << ordinal();
      else bitBoard = 0;
    }

    /**
     * returns the file index number from 1..8
     *
     * @return
     */
    public int get() {
      return this.ordinal() + 1;
    }

    /**
     * returns the enum File for a given file number
     *
     * @param file
     * @return
     */
    public static File get(int file) {
      return File.values[file - 1];
    }

    @Override
    public String toString() {
      if (this == NOFILE) {
        return "-";
      }
      return this.name();
    }}

  /**
   * This enum represents all ranks of a chess board If used in a loop via values() omit NORANK.
   */
  public enum Rank {
    r1, r2, r3, r4, r5, r6, r7, r8, NORANK;

    // pre-filled list with all squares
    public static final Rank[] values;
    public final long bitBoard;

    static {
      values = Rank.values();
    }

    Rank() {
      final long a = 0b11111111L;
      if (ordinal() < 8) this.bitBoard = a << 8*ordinal();
      else bitBoard = 0;
    }

    /**
     * returns the rank index number from 1..8
     *
     * @return
     */
    public int get() {
      return this.ordinal() + 1;
    }

    /**
     * returns the enum Rank for a given rank number
     *
     * @param rank
     * @return
     */
    public static Rank get(int rank) {
      return Rank.values[rank - 1];
    }

    @Override
    public String toString() {
      if (this == NORANK) {
        return "-";
      }
      return "" + (this.ordinal() + 1);
    }}

  public static final EnumSet<Square> WHITE_PAWNBASE_ROW  =
    EnumSet.of(a2, b2, c2, d2, e2, f2, g2, h2);
  public static final EnumSet<Square> BLACK_PAWNBASE_ROW  =
    EnumSet.of(a7, b7, c7, d7, e7, f7, g7, h7);
  public static final EnumSet<Square> WHITE_PROMOTION_ROW =
    EnumSet.of(a8, b8, c8, d8, e8, f8, g8, h8);
  public static final EnumSet<Square> BLACK_PROMOTION_ROW =
    EnumSet.of(a1, b1, c1, d1, e1, f1, g1, h1);

  public boolean isWhitePawnBaseRow() {
    return WHITE_PAWNBASE_ROW.contains(this);
  }

  public boolean isBlackPawnBaseRow() {
    return BLACK_PAWNBASE_ROW.contains(this);
  }

  public boolean isPawnBaseRow(Color c) {
    switch (c) {
      case WHITE:
        return isWhitePawnBaseRow();
      case BLACK:
        return isBlackPawnBaseRow();
      default:
        throw new RuntimeException("Invalid Color");
    }
  }

}
