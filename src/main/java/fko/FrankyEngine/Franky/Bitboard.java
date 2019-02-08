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

import static fko.FrankyEngine.Franky.Square.*;

/**
 * Bitboard
 */
public class Bitboard {

  private static final Logger LOG = LoggerFactory.getLogger(Bitboard.class);

  // upward diagonal bitboards @formatter:off
  public static final long a8UpDiag = a8.bitBoard;
  public static final long a7UpDiag = a7.bitBoard | b8.bitBoard;
  public static final long a6UpDiag = a6.bitBoard | b7.bitBoard | c8.bitBoard;
  public static final long a5UpDiag = a5.bitBoard | b6.bitBoard | c7.bitBoard | d8.bitBoard;
  public static final long a4UpDiag = a4.bitBoard | b5.bitBoard | c6.bitBoard | d7.bitBoard | e8.bitBoard;
  public static final long a3UpDiag = a3.bitBoard | b4.bitBoard | c5.bitBoard | d6.bitBoard | e7.bitBoard | f8.bitBoard;
  public static final long a2UpDiag = a2.bitBoard | b3.bitBoard | c4.bitBoard | d5.bitBoard | e6.bitBoard | f7.bitBoard | g8.bitBoard;
  public static final long a1UpDiag = a1.bitBoard | b2.bitBoard | c3.bitBoard | d4.bitBoard | e5.bitBoard | f6.bitBoard | g7.bitBoard | h8.bitBoard;
  public static final long b1UpDiag = b1.bitBoard | c2.bitBoard | d3.bitBoard | e4.bitBoard | f5.bitBoard | g6.bitBoard | h7.bitBoard;
  public static final long c1UpDiag = c1.bitBoard | d2.bitBoard | e3.bitBoard | f4.bitBoard | g5.bitBoard | h6.bitBoard;
  public static final long d1UpDiag = d1.bitBoard | e2.bitBoard | f3.bitBoard | g4.bitBoard | h5.bitBoard;
  public static final long e1UpDiag = e1.bitBoard | f2.bitBoard | g3.bitBoard | h4.bitBoard;
  public static final long f1UpDiag = f1.bitBoard | g2.bitBoard | h3.bitBoard;
  public static final long g1UpDiag = g1.bitBoard | h2.bitBoard;
  public static final long h1UpDiag = h1.bitBoard;

  // downward diagonal bitboards
  public static final long a1DownDiag = a1.bitBoard;
  public static final long a2DownDiag = a2.bitBoard | b1.bitBoard;
  public static final long a3DownDiag = a3.bitBoard | b2.bitBoard | c1.bitBoard;
  public static final long a4DownDiag = a4.bitBoard | b3.bitBoard | c2.bitBoard | d1.bitBoard;
  public static final long a5DownDiag = a5.bitBoard | b4.bitBoard | c3.bitBoard | d2.bitBoard | e1.bitBoard;
  public static final long a6DownDiag = a6.bitBoard | b5.bitBoard | c4.bitBoard | d3.bitBoard | e2.bitBoard | f1.bitBoard;
  public static final long a7DownDiag = a7.bitBoard | b6.bitBoard | c5.bitBoard | d4.bitBoard | e3.bitBoard | f2.bitBoard | g1.bitBoard;
  public static final long a8DownDiag = a8.bitBoard | b7.bitBoard | c6.bitBoard | d5.bitBoard | e4.bitBoard | f3.bitBoard | g2.bitBoard | h1.bitBoard;
  public static final long b8DownDiag = b8.bitBoard | c7.bitBoard | d6.bitBoard | e5.bitBoard | f4.bitBoard | g3.bitBoard | h2.bitBoard;
  public static final long c8DownDiag = c8.bitBoard | d7.bitBoard | e6.bitBoard | f5.bitBoard | g4.bitBoard | h3.bitBoard;
  public static final long d8DownDiag = d8.bitBoard | e7.bitBoard | f6.bitBoard | g5.bitBoard | h4.bitBoard;
  public static final long e8DownDiag = e8.bitBoard | f7.bitBoard | g6.bitBoard | h5.bitBoard;
  public static final long f8DownDiag = f8.bitBoard | g7.bitBoard | h6.bitBoard;
  public static final long g8DownDiag = g8.bitBoard | h7.bitBoard;
  public static final long h8DownDiag = h8.bitBoard;
  // @formatter:on

  static {

  }

  /**
   * Bitboard can't be instantiated
   */
  private Bitboard() {}

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
    return output.toString();
  }
}
