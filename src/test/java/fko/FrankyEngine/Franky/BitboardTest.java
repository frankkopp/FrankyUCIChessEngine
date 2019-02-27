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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static fko.FrankyEngine.Franky.Bitboard.*;
import static fko.FrankyEngine.Franky.Square.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unchecked")
class BitboardTest {

  private static final Logger LOG   = LoggerFactory.getLogger(BitboardTest.class);
  private static final int    WHITE = 0;
  private static final int    BLACK = 1;

  @Test
  void toStringTest() {
    // @formatter:off
    Square square;
    String actual;
    String expected;

    square = a1;
    actual = Bitboard.toString(square.bitboard());
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = a8;
    actual = Bitboard.toString(square.bitboard());
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = h1;
    actual = Bitboard.toString(square.bitboard());
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    square = h8;
    actual = Bitboard.toString(square.bitboard());
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = e4;
    actual = Bitboard.toString(square.bitboard());
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    // @formatter:on
  }

  @Test
  void printBitStringTest() {
    // @formatter:off
    Square square;
    String actual;
    String expected;

    square = b1;
    actual = Bitboard.printBitString(square.bitboard());
    LOG.debug("{}\n{}", square, actual);
    expected =  "00000010.00000000.00000000.00000000.00000000.00000000.00000000.00000000";
    assertEquals(expected, actual);

    actual = Bitboard.printBitString(square.getRank().bitBoard);
    LOG.debug("{}\n{}", square, actual);
    expected =  "11111111.00000000.00000000.00000000.00000000.00000000.00000000.00000000";
    assertEquals(expected, actual);
    // @formatter:on
  }

  @Test
  void bitBoardTest() {
    long bitboard = 0L;
    System.out.printf("Empty %d%n", bitboard);
    System.out.println(Bitboard.toString(bitboard));

    bitboard |= 255;
    System.out.printf("Long: %d%n", bitboard);
    System.out.println(Bitboard.toString(bitboard));

    bitboard = bitboard << 8;
    System.out.printf("Long: %d%n", bitboard);
    System.out.println(Bitboard.toString(bitboard));

    bitboard = bitboard >> 8;
    System.out.printf("Long: %d%n", bitboard);
    System.out.println(Bitboard.toString(bitboard));

    bitboard = bitboard << (8 * 7);
    System.out.printf("Long: %d%n", bitboard);
    System.out.println(Bitboard.toString(bitboard));

  }

  // @formatter:off
  @Test
  void pawnMoves() {
    Square square;
    String actual;
    String expected;

    square = a2;
    actual = Bitboard.toString(pawnAttacks[WHITE][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = h7;
    actual = Bitboard.toString(pawnAttacks[BLACK][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = e7;
    actual = Bitboard.toString(pawnMoves[BLACK][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d2;
    actual = Bitboard.toString(pawnMoves[WHITE][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
  }

  @Test
  void knightAttacks() {
    Square square;
    String actual;
    String expected;

    square = d4;
    actual = Bitboard.toString(knightAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected = "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 0 1 0 0 0 \n"
              + "0 1 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 1 0 0 \n"
              + "0 0 1 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = a1;
    actual = Bitboard.toString(knightAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = g8;
    actual = Bitboard.toString(knightAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 1 0 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = e2;
    actual = Bitboard.toString(knightAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 1 0 0 \n"
              + "0 0 1 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 1 0";
    assertEquals(expected, actual);

  }

  @Test
  void kingAttacks() {
    Square square;
    String actual;
    String expected;

    square = d4;
    actual = Bitboard.toString(kingAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected = "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 0 1 0 1 0 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = a1;
    actual = Bitboard.toString(kingAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 1 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = g8;
    actual = Bitboard.toString(kingAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 1 0 1 \n"
              + "0 0 0 0 0 1 1 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = e2;
    actual = Bitboard.toString(kingAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 0 1 0 1 0 0 \n"
              + "0 0 0 1 1 1 0 0";
    assertEquals(expected, actual);

    square = h4;
    actual = Bitboard.toString(kingAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 1 1 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 1 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
  }

  @Test
  void sliderPseudoAttacks() {
    Square square;
    String actual;
    String expected;

    square = d4;
    actual = Bitboard.toString(bishopPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 1 \n"
              + "1 0 0 0 0 0 1 0 \n"
              + "0 1 0 0 0 1 0 0 \n"
              + "0 0 1 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 0 1 0 0 0 \n"
              + "0 1 0 0 0 1 0 0 \n"
              + "1 0 0 0 0 0 1 0";
    assertEquals(expected, actual);

    square = d4;
    actual = Bitboard.toString(rookPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "1 1 1 0 1 1 1 1 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0";
    assertEquals(expected, actual);

    square = d4;
    actual = Bitboard.toString(queenPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 1 0 0 0 1 \n"
              + "1 0 0 1 0 0 1 0 \n"
              + "0 1 0 1 0 1 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "1 1 1 0 1 1 1 1 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 1 0 1 0 1 0 0 \n"
              + "1 0 0 1 0 0 1 0";
    assertEquals(expected, actual);

    square = a1;
    actual = Bitboard.toString(bishopPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = a1;
    actual = Bitboard.toString(rookPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "0 1 1 1 1 1 1 1";
    assertEquals(expected, actual);

    square = a1;
    actual = Bitboard.toString(queenPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 0 0 0 0 0 0 1 \n"
              + "1 0 0 0 0 0 1 0 \n"
              + "1 0 0 0 0 1 0 0 \n"
              + "1 0 0 0 1 0 0 0 \n"
              + "1 0 0 1 0 0 0 0 \n"
              + "1 0 1 0 0 0 0 0 \n"
              + "1 1 0 0 0 0 0 0 \n"
              + "0 1 1 1 1 1 1 1";
    assertEquals(expected, actual);

    square = h4;
    actual = Bitboard.toString(bishopPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 1 0 0 0";
    assertEquals(expected, actual);

    square = h4;
    actual = Bitboard.toString(rookPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    square = h4;
    actual = Bitboard.toString(queenPseudoAttacks[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 1 0 0 0 1 \n"
              + "0 0 0 0 1 0 0 1 \n"
              + "0 0 0 0 0 1 0 1 \n"
              + "0 0 0 0 0 0 1 1 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "0 0 0 0 0 0 1 1 \n"
              + "0 0 0 0 0 1 0 1 \n"
              + "0 0 0 0 1 0 0 1";
    assertEquals(expected, actual);
  }

  @Test
  void fileMasksTest() {
    Square square;
    String actual;
    String expected;

    square = d4;
    actual = Bitboard.toString(filesWestMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d4;
    actual = Bitboard.toString(filesEastMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 1 1 1 1";
    assertEquals(expected, actual);

    square = d4;
    actual = Bitboard.toString(fileWestMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d4;
    actual = Bitboard.toString(fileEastMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0";
    assertEquals(expected, actual);

    square = a4;
    actual = Bitboard.toString(filesWestMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = a4;
    actual = Bitboard.toString(filesEastMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1 \n"
              + "0 1 1 1 1 1 1 1";
    assertEquals(expected, actual);

    square = h4;
    actual = Bitboard.toString(filesWestMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0 \n"
              + "1 1 1 1 1 1 1 0";
    assertEquals(expected, actual);

    square = h4;
    actual = Bitboard.toString(filesEastMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
  }

  @Test
  void rankMasksTest() {
    Square square;
    String actual;
    String expected;

    square = d4;
    actual = Bitboard.toString(ranksNorthMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d4;
    actual = Bitboard.toString(ranksSouthMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1";
    assertEquals(expected, actual);

    square = d8;
    actual = Bitboard.toString(ranksNorthMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d8;
    actual = Bitboard.toString(ranksSouthMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1";
    assertEquals(expected, actual);

    square = d1;
    actual = Bitboard.toString(ranksNorthMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d1;
    actual = Bitboard.toString(ranksSouthMask[square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
  }

  @Test
  void rays() {

    Square square;
    String actual;
    String expected;
    int direction;

    square = d4;
    direction = NORTHWEST;
    actual = Bitboard.toString(rays[direction][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d4;
    direction = EAST;
    actual = Bitboard.toString(rays[direction][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d4;
    direction = SOUTHEAST;
    actual = Bitboard.toString(rays[direction][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 1 0";
    assertEquals(expected, actual);

    square = d4;
    direction = WEST;
    actual = Bitboard.toString(rays[direction][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 1 1 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

  //    for (Square square : validSquares) {
  //      System.out.println("Square: " + square);
  //      for (int d = 0; d < 8; d++) {
  //        System.out.println("Direction: " + d);
  //        System.out.println(Bitboard.toString(Bitboard.rays[d][square.ordinal()]));
  //      }
  //    }
  }

  @Test
  public void passedPawnBitboards() {
    Square square;
    String actual;
    String expected;

    square = e4;
    actual = Bitboard.toString(passedPawnMask[WHITE][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 1 1 1 0 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = d5;
    actual = Bitboard.toString(passedPawnMask[BLACK][square.ordinal()]);
    LOG.debug("{}\n{}", square, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 0 1 1 1 0 0 0";
    assertEquals(expected, actual);

//    // White Pawns
//    System.out.println("WHITE PAWNS");
//    for (Square sq : validSquares) {
//      System.out.println(sq);
//      System.out.println(
//        printBitString(Bitboard.passedPawnMask[Color.WHITE.ordinal()][sq.ordinal()]));
//      System.out.println(
//        Bitboard.toString(Bitboard.passedPawnMask[Color.WHITE.ordinal()][sq.ordinal()]));
//      System.out.println();
//    }
//    // Black Pawns
//    System.out.println("BLACK PAWN");
//    for (Square sq : validSquares) {
//      System.out.println(sq);
//      System.out.println(
//        printBitString(Bitboard.passedPawnMask[Color.BLACK.ordinal()][sq.ordinal()]));
//      System.out.println(
//        Bitboard.toString(Bitboard.passedPawnMask[Color.BLACK.ordinal()][sq.ordinal()]));
//      System.out.println();
//    }
  }

  @Test
  void squareColors() {
    String actual;
    String expected;

    actual = Bitboard.toString(whiteSquares);
    LOG.debug("{}\n{}", "white squares", actual);
    expected =  "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1";
    assertEquals(expected, actual);

    actual = Bitboard.toString(blackSquares);
    LOG.debug("{}\n{}", "black squares", actual);
    expected =  "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 1 \n"
              + "1 0 1 0 1 0 1 0";
    assertEquals(expected, actual);
  }

  @Test
  public void intermediates() {

    Square from, to;
    String actual;
    String expected;

    from = b3;
    to = f7;
    actual = Bitboard.toString(intermediate[from.ordinal()][to.ordinal()]);
    LOG.debug("{}\n{}", from, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    from = b7;
    to = h1;
    actual = Bitboard.toString(intermediate[from.ordinal()][to.ordinal()]);
    LOG.debug("{}\n{}", from, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    //    for (Square from : validSquares) {
    //      for (Square to : validSquares) {
    //        System.out.printf("From: %s To: %s%n%s%n%n", from, to, Bitboard.toString(
    //          Bitboard.intermediate[from.ordinal()][to.ordinal()]));
    //      }
    //    }
  }

  @Test
  public void distances() {

    Square from, to;
    int actual;

    from = b3;
    to = f7;
    actual = distance[from.ordinal()][to.ordinal()];
    LOG.debug("{} to {}: {}", from, to, actual);
    assertEquals(4, actual);

    from = b7;
    to = e1;
    actual = distance[from.ordinal()][to.ordinal()];
    LOG.debug("{} to {}: {}", from, to, actual);
    assertEquals(6, actual);

    from = a1;
    to = h8;
    actual = distance[from.ordinal()][to.ordinal()];
    LOG.debug("{} to {}: {}", from, to, actual);
    assertEquals(7, actual);

    //    for (Square from : validSquares) {
    //      for (Square to : validSquares) {
    //        System.out.printf("From: %s To: %s: %d%n", from, to, distance[from.ordinal()][to.ordinal()]);
    //      }
    //    }
  }

  @Test
  public void centerDistances() {

    Square from;
    int actual;

    from = b3;
    actual = centerDistance[from.ordinal()];
    LOG.debug("{}: {}", from, actual);
    assertEquals(2, actual);

    from = b7;
    actual = centerDistance[from.ordinal()];
    LOG.debug("{}: {}", from, actual);
    assertEquals(2, actual);

    from = a1;
    actual = centerDistance[from.ordinal()];
    LOG.debug("{}: {}", from, actual);
    assertEquals(3, actual);

    //        for (Square square : validSquares) {
    //          System.out.printf("From: %s: %d%n", square, centerDistance[square.ordinal()]);
    //        }
  }

  @Test
  public void testKingRing() {
    for (int i = 0; i < 64; i++) {
      assertEquals(kingAttacks[i], kingRing[i]);
    }
  }

  // @formatter:on

  @Test
  @Disabled
  public void attackBitboards() {

    // White Pawns
    System.out.println("WHITE PAWNS");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(
        printBitString(Bitboard.pawnAttacks[Color.WHITE.ordinal()][square.ordinal()]));
      System.out.println(
        Bitboard.toString(Bitboard.pawnAttacks[Color.WHITE.ordinal()][square.ordinal()]));
      System.out.println();
    }
    // Black Pawns
    System.out.println("BLACK PAWN");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(
        printBitString(Bitboard.pawnAttacks[Color.BLACK.ordinal()][square.ordinal()]));
      System.out.println(
        Bitboard.toString(Bitboard.pawnAttacks[Color.BLACK.ordinal()][square.ordinal()]));
      System.out.println();
    }

    // Knight
    System.out.println("KNIGHTS");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(printBitString(Bitboard.knightAttacks[square.ordinal()]));
      System.out.println(Bitboard.toString(Bitboard.knightAttacks[square.ordinal()]));
      System.out.println();
    }

    // Bishop
    System.out.println("BISHOPS\n");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(printBitString(Bitboard.bishopPseudoAttacks[square.ordinal()]));
      System.out.println(Bitboard.toString(Bitboard.bishopPseudoAttacks[square.ordinal()]));
      System.out.println();
    }

    // Bishop
    System.out.println("ROOKS\n");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(printBitString(Bitboard.rookPseudoAttacks[square.ordinal()]));
      System.out.println(Bitboard.toString(Bitboard.rookPseudoAttacks[square.ordinal()]));
      System.out.println();
    }

    // Queen
    System.out.println("QUEEN\n");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(printBitString(Bitboard.queenPseudoAttacks[square.ordinal()]));
      System.out.println(Bitboard.toString(Bitboard.queenPseudoAttacks[square.ordinal()]));
      System.out.println();
    }

    // King
    System.out.println("KING\n");
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(printBitString(Bitboard.kingAttacks[square.ordinal()]));
      System.out.println(Bitboard.toString(Bitboard.kingAttacks[square.ordinal()]));
      System.out.println();
    }
  }



  @Test
  @Disabled
  void bitboardRotations() {

    System.out.println("File ================================================");
    System.out.println("=====================================================");
    System.out.println(Bitboard.toString(File.d.bitBoard));
    System.out.println(printBitString(File.d.bitBoard));
    System.out.println("R90");
    System.out.println(Bitboard.toString(rotateR90(File.d.bitBoard)));
    System.out.println(printBitString(rotateR90(File.d.bitBoard)));
    System.out.println(printBitString(rotateR90(File.d.bitBoard) >>> ((File.d.ordinal() + 1) * 8)));
    System.out.println(
      printBitString((rotateR90(File.d.bitBoard) >>> ((File.d.ordinal() + 1) * 8)) & 255));
    System.out.println("L90");
    System.out.println(Bitboard.toString(rotateL90(File.d.bitBoard)));
    System.out.println(printBitString(rotateL90(File.d.bitBoard)));
    System.out.println("R45");
    System.out.println(Bitboard.toString(rotateR45(File.d.bitBoard)));
    System.out.println(printBitString(rotateR45(File.d.bitBoard)));
    System.out.println("L45");
    System.out.println(Bitboard.toString(rotateL45(File.d.bitBoard)));
    System.out.println(printBitString(rotateL45(a1UpDiag)));
    System.out.println();

    System.out.println("Rank ================================================");
    System.out.println("=====================================================");
    System.out.println(Bitboard.toString(Rank.r4.bitBoard));
    System.out.println(printBitString(Rank.r4.bitBoard));
    System.out.println(printBitString(Rank.r4.bitBoard >>> (Rank.r4.ordinal()) * 8));
    System.out.println(printBitString((Rank.r4.bitBoard >>> (Rank.r4.ordinal()) * 8) & 255));
    System.out.println("R90");
    System.out.println(Bitboard.toString(rotateR90(Rank.r4.bitBoard)));
    System.out.println(printBitString(rotateR90(Rank.r4.bitBoard)));
    System.out.println("L90");
    System.out.println(Bitboard.toString(rotateL90(Rank.r4.bitBoard)));
    System.out.println(printBitString(rotateL90(Rank.r4.bitBoard)));
    System.out.println("R45");
    System.out.println(Bitboard.toString(rotateR45(Rank.r4.bitBoard)));
    System.out.println(printBitString(rotateR45(Rank.r4.bitBoard)));
    System.out.println("L45");
    System.out.println(Bitboard.toString(rotateL45(Rank.r4.bitBoard)));
    System.out.println(printBitString(rotateL45(a1UpDiag)));
    System.out.println();

    System.out.println("diag ==============================================");
    System.out.println("=====================================================");
    Square square1 = e5;
    long diag = square1.getUpDiag();
    System.out.println(Bitboard.toString(diag));
    System.out.println(printBitString(diag));
    System.out.println("R90");
    System.out.println(Bitboard.toString(rotateR90(diag)));
    System.out.println(printBitString(rotateR90(diag)));
    System.out.println("L90");
    System.out.println(Bitboard.toString(rotateL90(diag)));
    System.out.println(printBitString(rotateL90(diag)));
    System.out.println("R45");
    long rotateR45 = rotateR45(diag);
    System.out.println(Bitboard.toString(rotateR45));
    System.out.println("Rot:  " + printBitString(rotateR45));
    int index64 = square1.ordinal();
    int shift = getShiftUp(index64);
    long shiftBitboard = rotateR45 >>> shift;
    System.out.println("Shif: " + printBitString(shiftBitboard));
    long mask = getLengthMaskUp(index64);
    System.out.println("Mask: " + printBitString(mask));
    System.out.println("MBit: " + printBitString(shiftBitboard & mask));
    System.out.println("L45");
    System.out.println(Bitboard.toString(rotateL45(diag)));
    System.out.println(printBitString(rotateL45(diag)));
    System.out.println();

    System.out.println("downDiag ============================================");
    System.out.println("=====================================================");
    square1 = a2;
    diag = square1.getDownDiag();
    System.out.println(Bitboard.toString(diag));
    System.out.println(printBitString(diag));
    System.out.println("R90");
    System.out.println(Bitboard.toString(rotateR90(diag)));
    System.out.println(printBitString(rotateR90(diag)));
    System.out.println("L90");
    System.out.println(Bitboard.toString(rotateL90(diag)));
    System.out.println(printBitString(rotateL90(diag)));
    System.out.println("R45");
    long rotateL45 = rotateL45(diag);
    System.out.println(Bitboard.toString(rotateL45));
    System.out.println("L45");
    System.out.println(Bitboard.toString(rotateL45));
    System.out.println("Rot:  " + printBitString(rotateL45));
    index64 = square1.ordinal();
    shift = getShiftDown(index64);
    shiftBitboard = rotateL45 >>> shift;
    System.out.println("Shif: " + printBitString(shiftBitboard));
    mask = getLengthMaskDown(index64);
    System.out.println("Mask: " + printBitString(mask));
    System.out.println("MBit: " + printBitString(shiftBitboard & mask));
    System.out.println();
  }

  @Test
  void rotateR90Test() {
    // @formatter:off
    long bitboard;
    String expected, actual, rotation;

    Function f = b -> rotateR90((long) b);
    rotation = "R90";

    bitboard = File.d.bitBoard;
    actual = rotation(bitboard, rotation, f);
    expected = "0 0 0 0 0 0 0 0 \n" +
               "0 0 0 0 0 0 0 0 \n" +
               "0 0 0 0 0 0 0 0 \n" +
               "1 1 1 1 1 1 1 1 \n" +
               "0 0 0 0 0 0 0 0 \n" +
               "0 0 0 0 0 0 0 0 \n" +
               "0 0 0 0 0 0 0 0 \n" +
               "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Rank.r4.bitBoard;
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.e5.getUpDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "1 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    bitboard = Square.e5.getDownDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0";
    assertEquals(expected, actual);

//    bitboard = new Position("PPPPPPPP/1PPPPPPP/2PPPPPP/3PPPPP/4PPPP/5PPP/6PP/7P b - -").getAllOccupiedBitboard();
//    actual = rotation(bitboard, rotation, f);
//    expected =  "0 0 0 0 0 0 0 1 \n"
//              + "0 0 0 0 0 0 1 1 \n"
//              + "0 0 0 0 0 1 1 1 \n"
//              + "0 0 0 0 1 1 1 1 \n"
//              + "0 0 0 1 1 1 1 1 \n"
//              + "0 0 1 1 1 1 1 1 \n"
//              + "0 1 1 1 1 1 1 1 \n"
//              + "1 1 1 1 1 1 1 1";
//    assertEquals(expected, actual);
//
//    bitboard = ~new Position("PPPPPPPP/1PPPPPPP/2PPPPPP/3PPPPP/4PPPP/5PPP/6PP/7P b - -").getAllOccupiedBitboard();
//    actual = rotation(bitboard, rotation, f);
//    expected =  "1 1 1 1 1 1 1 0 \n"
//              + "1 1 1 1 1 1 0 0 \n"
//              + "1 1 1 1 1 0 0 0 \n"
//              + "1 1 1 1 0 0 0 0 \n"
//              + "1 1 1 0 0 0 0 0 \n"
//              + "1 1 0 0 0 0 0 0 \n"
//              + "1 0 0 0 0 0 0 0 \n"
//              + "0 0 0 0 0 0 0 0";
//    assertEquals(expected, actual);
  // @formatter:on
  }

  @Test
  void rotateL90Test() {
    // @formatter:off
    long bitboard;
    String expected, actual, rotation;

    Function f = b -> rotateL90((long) b);
    rotation = "L90";

    bitboard = File.d.bitBoard;
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 1 1 1 1 1 1 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Rank.r4.bitBoard;
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.e5.getUpDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "1 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    bitboard = Square.e5.getDownDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

//    bitboard = new Position("PPPPPPPP/1PPPPPPP/2PPPPPP/3PPPPP/4PPPP/5PPP/6PP/7P b - -").getAllOccupiedBitboard();
//    actual = rotation(bitboard, rotation, f);
//    expected =  "1 1 1 1 1 1 1 1 \n"
//              + "1 1 1 1 1 1 1 0 \n"
//              + "1 1 1 1 1 1 0 0 \n"
//              + "1 1 1 1 1 0 0 0 \n"
//              + "1 1 1 1 0 0 0 0 \n"
//              + "1 1 1 0 0 0 0 0 \n"
//              + "1 1 0 0 0 0 0 0 \n"
//              + "1 0 0 0 0 0 0 0";
//    assertEquals(expected, actual);
//
//    bitboard = ~new Position("PPPPPPPP/1PPPPPPP/2PPPPPP/3PPPPP/4PPPP/5PPP/6PP/7P b - -").getAllOccupiedBitboard();
//    actual = rotation(bitboard, rotation, f);
//    expected =  "0 0 0 0 0 0 0 0 \n"
//              + "0 0 0 0 0 0 0 1 \n"
//              + "0 0 0 0 0 0 1 1 \n"
//              + "0 0 0 0 0 1 1 1 \n"
//              + "0 0 0 0 1 1 1 1 \n"
//              + "0 0 0 1 1 1 1 1 \n"
//              + "0 0 1 1 1 1 1 1 \n"
//              + "0 1 1 1 1 1 1 1";
//    assertEquals(expected, actual);
//     // @formatter:on
  }

  @Test
  void rotateR45Test() {
    // @formatter:off
    long bitboard, rotated;
    String expected, actual, rotation;

    Function f = b -> rotateR45((long) b);
    rotation = "R45";

    bitboard = Square.a1.getUpDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "1 1 1 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.a5.getUpDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.a8.getUpDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "1 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.f3.getUpDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 1 1 1 1 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard =  e2.bitboard() | g4.bitboard();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    // @formatter:on
  }

  @Test
  void rotateL45Test() {
    // @formatter:off
    long bitboard;
    String expected, actual, rotation;

    Function f = b -> rotateL45((long) b);
    rotation = "R45";

    bitboard = Square.a8.getDownDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 1 1 1 \n"
              + "1 1 1 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.a1.getDownDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    bitboard = Square.a5.getDownDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 1 1 1 1 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.h5.getDownDiag();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard =  g5.bitboard() | e7.bitboard();
    actual = rotation(bitboard, rotation, f);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 1 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    // @formatter:on
  }

  private String rotation(long bitboard, String rotation, Function f) {
    long rotated;
    String actual;
    LOG.debug("Origin:\n{}", Bitboard.toString(bitboard));
    rotated = (long) f.apply(bitboard);
    actual = Bitboard.toString(rotated);
    LOG.debug("Rotated {}:\n{}", rotation, actual);
    LOG.debug("Origin:  {}", Bitboard.printBitString(bitboard));
    LOG.debug("Rotated: {}", Bitboard.printBitString(rotated));
    return actual;
  }

  @Test
  void rotateIndexR90() { // @formatter:off
    Square square, newSquare;
    String actual, expected;

    square = a1;
    expected =  "1 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexR90(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = h1;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexR90(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = g7;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexR90(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);
  } // @formatter:on

  @Test
  void rotateIndexL90() { // @formatter:off
    Square square, newSquare;
    String actual, expected;

    square = a1;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexL90(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = h1;
    expected =  "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexL90(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = g7;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexL90(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);
  } // @formatter:on

  @Test
  void rotateIndexR45() { // @formatter:off
    Square square, newSquare;
    String actual, expected;

    square = a1;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexR45(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = h1;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexR45(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = g7;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexR45(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);
  } // @formatter:on

  @Test
  void rotateIndexL45() { // @formatter:off
    Square square, newSquare;
    String actual, expected;

    square = a1;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexL45(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = h1;
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexL45(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);

    square = g7;
    expected =  "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    LOG.debug("{}", String.format("%s:%n%s%n", square, Bitboard.toString(square.bitboard())));
    newSquare = Square.values[Bitboard.rotateIndexL45(square.ordinal())];
    LOG.debug("{}", String.format("Rotated: %n%s%n", Bitboard.toString(newSquare.bitboard())));
    actual = Bitboard.toString(newSquare.bitboard());
    assertEquals(expected, actual);
  } // @formatter:on

  @Test
  @Disabled
  void horizontalSlidingAttackBitboards() {
    for (int i = 0b0000_0000; i <= 0b1111_1111; i++) {
      System.out.printf("Blocker : %s%n", printBitString(i));
      System.out.printf(
        "=================================================================================%n");

      for (Square square : validSquares) {

        final int rank = square.getRank().ordinal();
        long rankMask = square.getRank().bitBoard;
        System.out.printf("RankMask: %s%n", printBitString(rankMask));

        long blocker = i;
        blocker = blocker << ((7 - rank) * 8);
        blocker = rankMask & blocker;
        System.out.printf("Blocker : %s%n", printBitString(blocker));

        long shifted = blocker >>> ((7 - rank) * 8);
        System.out.printf("Blocker>: %s%n", printBitString(shifted));

        final int pieceBitmap = (int) ((shifted & 255));
        System.out.printf("Masked  : %s%n", printBitString(pieceBitmap));

        System.out.printf("Square  : %s%n", square);
        System.out.printf("Square  : %s%n", printBitString((square.bitboard())));

        long bitboard = movesRank[square.ordinal()][pieceBitmap];
        System.out.printf("Moves   : %s%n", printBitString(bitboard));
        System.out.printf("Moves   : %n%s%n", Bitboard.toString(bitboard));
        System.out.println();
      }
    }
  }

  @Test
  void horizontalSlidingAttackBitboardsTests() {
    // @formatter:off
    long blocker, moves;
    String expected, actual;
    Square square;

    square = b1;
    blocker = e1.bitboard();
    moves = getSlidingMovesRank(square, blocker);
    actual = Bitboard.toString(moves);
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "1 0 1 1 1 0 0 0";
    assertEquals(expected, actual);
    assertEquals(2089670227099910144L, moves);

    square = e5;
    blocker = b5.bitboard();
    moves = getSlidingMovesRank(square, blocker);
    actual = Bitboard.toString(moves);
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 1 1 0 1 1 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    assertEquals(3992977408L, moves);
   // @formatter:on
  }

  @Test
  @Disabled
  void verticalSlidingAttackBitboards() {
    for (int i = 0b0000_0000; i <= 0b1111_1111; i++) {
      System.out.printf("Blocker : %s%n", printBitString(i));
      System.out.printf(
        "=================================================================================%n");
      for (Square square : validSquares) {
        long fileMask = square.getFile().bitBoard;
        System.out.printf("FileMask: %s%n", printBitString(fileMask));
        System.out.printf("Square  : %s%n", square);
        System.out.printf("Square  : %s%n", printBitString((square.bitboard())));
        long bitboard = movesFile[square.ordinal()][i];
        System.out.printf("Moves   : %s%n", printBitString(bitboard));
        System.out.printf("Moves   : %n%s%n", Bitboard.toString(bitboard));
        System.out.println();
      }
    }
  }

  @Test
  void verticalSlidingAttackBitboardsTests() {
    // @formatter:off
    long blocker, moves;
    String expected, actual;
    Square square;

    square = b1;
    blocker = b5.bitboard();
    moves = getSlidingMovesFile(square, blocker);
    actual = Bitboard.toString(moves);
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    assertEquals(565157600165888L, moves);

    square = e5;
    blocker = e3.bitboard();
    moves = getSlidingMovesFile(square, blocker);
    actual = Bitboard.toString(moves);
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    assertEquals(17660906573840L, moves);

    square = e5;
    blocker = e3.bitboard() | e1.bitboard();
    moves = getSlidingMovesFile(square, blocker);
    actual = Bitboard.toString(moves);
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    assertEquals(17660906573840L, moves);
   // @formatter:on
  }

  @Test
  @Disabled
  void diagUpSlidingAttackBitboards() {
    for (int i = 0b0000_0000; i <= 0b1111_1111; i++) {
      for (Square square : validSquares) {
        String bitString = printBitString(i);
        System.out.printf("Square %s: Length: %d movesUpDiag[ %2d ][ %8s ] = %s (%d)%n", square,
                          lengthDiagUp[square.ordinal()], square.ordinal(),
                          bitString.substring(bitString.length() - 8),
                          Bitboard.printBitString(movesUpDiag[square.ordinal()][i]),
                          movesUpDiag[square.ordinal()][i]);

        long bitboard = movesUpDiag[square.ordinal()][i];
        System.out.printf("%s%n", Bitboard.toString(bitboard));
        System.out.println();
      }
    }
  }

  @Test
  void diagUpSlidingAttackBitmapTest() {
    // @formatter:off
    String actual, expected;
    Square square;
    long blockers, bitboard;

    // the square we are on
    square = Square.a1;
    // the pieces currently on the board and maybe blocking the moves
    blockers = h8.bitboard() | g7.bitboard() | f6.bitboard() | e5.bitboard()
                    | b7.bitboard() | g2.bitboard();
    // expected output
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 1 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0 \n"
                + "0 1 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";

    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("Content: %s", Bitboard.printBitString(blockers)));
    LOG.debug("{}",String.format("%n%s", Bitboard.toString(blockers)));

    // retrieve all possible moves for this square with the current content
    bitboard = getSlidingMovesDiagUp(square, blockers);

    actual = Bitboard.toString(bitboard);
    LOG.debug("{}",String.format("%n%s", actual));
    assertEquals(expected, actual);
    System.out.println();

     // the square we are on
    square = Square.a4;
    // the pieces currently on the board and maybe blocking the moves
    blockers = c6.bitboard() | d7.bitboard() | f6.bitboard() | e5.bitboard()
                    | b7.bitboard() | g2.bitboard();
    // expected output
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0 \n"
                + "0 1 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";

    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("Content: %s", Bitboard.printBitString(blockers)));
    LOG.debug("{}",String.format("%n%s", Bitboard.toString(blockers)));

    // retrieve all possible moves for this square with the current content
    bitboard = getSlidingMovesDiagUp(square, blockers);

    actual = Bitboard.toString(bitboard);
    LOG.debug("{}",String.format("%n%s", actual));
    assertEquals(expected, actual);
    System.out.println();

    // the square we are on
    square = Square.f3;
    // the pieces currently on the board and maybe blocking the moves
    blockers = e2.bitboard() | g4.bitboard();
                    // | f6.getBitBoard() | e5.getBitBoard()
                    // | b7.getBitBoard() | g2.getBitBoard();
    // expected output
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 1 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";

    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("Content: %s", Bitboard.printBitString(blockers)));
    LOG.debug("{}",String.format("%n%s", Bitboard.toString(blockers)));

    // retrieve all possible moves for this square with the current content
    bitboard = getSlidingMovesDiagUp(square, blockers);

    actual = Bitboard.toString(bitboard);
    LOG.debug("{}",String.format("%n%s", actual));
    assertEquals(expected, actual);
    System.out.println();
    // @formatter:on
  }

  @Test
  void diagDownSlidingAttackBitmapTest() {
    // @formatter:off
    String actual, expected;
    Square square;
    long blockers, bitboard;

    // the square we are on
    square = Square.a8;
    // the pieces currently on the board and maybe blocking the moves
    blockers = h1.bitboard() | g2.bitboard() | f3.bitboard() | e4.bitboard()
                    | b2.bitboard() | g7.bitboard();
    // expected output
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 1 0 0 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0 \n"
                + "0 0 0 1 0 0 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";

    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("Content: %s", Bitboard.printBitString(blockers)));
    LOG.debug("{}",String.format("%n%s", Bitboard.toString(blockers)));

    // retrieve all possible moves for this square with the current content
    bitboard = getSlidingMovesDiagDown(square, blockers);

    actual = Bitboard.toString(bitboard);
    LOG.debug("{}",String.format("%n%s", actual));
    assertEquals(expected, actual);
    System.out.println();

    // the square we are on
    square = Square.d8;
    // the pieces currently on the board and maybe blocking the moves
    blockers = f6.bitboard() | g5.bitboard() | f3.bitboard() | e4.bitboard()
                    | b2.bitboard() | g7.bitboard();
    // expected output
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 0 0 1 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";

    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("Content: %s", Bitboard.printBitString(blockers)));
    LOG.debug("{}",String.format("%n%s", Bitboard.toString(blockers)));

    // retrieve all possible moves for this square with the current content
    bitboard = getSlidingMovesDiagDown(square, blockers);

    actual = Bitboard.toString(bitboard);
    LOG.debug("{}",String.format("%n%s", actual));
    assertEquals(expected, actual);
    System.out.println();

    // the square we are on
    square = Square.a4;
    // the pieces currently on the board and maybe blocking the moves
    blockers = c2.bitboard() | g5.bitboard() | f3.bitboard() | e4.bitboard()
                    | b2.bitboard() | g7.bitboard();
    // expected output
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 1 0 0 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";

    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("UpDiag : %s", Bitboard.printBitString(square.getUpDiag())));
    LOG.debug("{}",String.format("Content: %s", Bitboard.printBitString(blockers)));
    LOG.debug("{}",String.format("%n%s", Bitboard.toString(blockers)));

    // retrieve all possible moves for this square with the current content
    bitboard = getSlidingMovesDiagDown(square, blockers);

    actual = Bitboard.toString(bitboard);
    LOG.debug("{}",String.format("%n%s", actual));
    assertEquals(expected, actual);
    System.out.println();
    // @formatter:on
  }

  //  @Test
  //  void slidingAttackBitboardTest() {
  //    Position position = new Position("4r3/1pn3k1/4p1b1/p1Pp1P1r/3P2NR/1P3B2/3K2P1/4R3 w - -");
  //    System.out.println(position);
  //    Square square;
  //    long slidingMoves;
  //
  //    // horizontal on rank
  //    square = e1;
  //    slidingMoves = getSlidingMovesRank(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Horizontal Rank Attacks for %s%n", square);
  //    System.out.printf("Moves Rank %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(-1224979098644774912L, slidingMoves);
  //
  //    square = e8;
  //    slidingMoves = getSlidingMovesRank(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Horizontal Rank Attacks for %s%n", square);
  //    System.out.printf("Moves Rank %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(239L, slidingMoves);
  //
  //    square = h4;
  //    slidingMoves = getSlidingMovesRank(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Horizontal Rank Attacks for %s%n", square);
  //    System.out.printf("Moves Rank %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(274877906944L, slidingMoves);
  //
  //    square = h5;
  //    slidingMoves = getSlidingMovesRank(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Horizontal Rank Attacks for %s%n", square);
  //    System.out.printf("Moves Rank %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(1610612736L, slidingMoves);
  //
  //    // vertical on file
  //    square = e1;
  //    slidingMoves = getSlidingMovesFile(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Vertical File Attacks for %s%n", square);
  //    System.out.printf("Moves File %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(4521260802375680L, slidingMoves);
  //
  //    square = e8;
  //    slidingMoves = getSlidingMovesFile(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Vertical File Attacks for %s%n", square);
  //    System.out.printf("Moves File %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(1052672L, slidingMoves);
  //
  //    square = h4;
  //    slidingMoves = getSlidingMovesFile(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Vertical File Attacks for %s%n", square);
  //    System.out.printf("Moves File %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(-9187202500199972864L, slidingMoves);
  //
  //    square = h5;
  //    slidingMoves = getSlidingMovesFile(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Vertical File Attacks for %s%n", square);
  //    System.out.printf("Moves File %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(549764235392L, slidingMoves);
  //
  //    // diagonal upwards
  //    square = f3;
  //    System.out.printf("Diag Up Attacks for %s%n", square);
  //    slidingMoves = getSlidingMovesDiagUp(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Moves Diag Up %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(580964626808700928L, slidingMoves);
  //
  //    square = g6;
  //    slidingMoves = getSlidingMovesDiagUp(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Diag Up Attacks for %s%n", square);
  //    System.out.printf("Moves Diag Up %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(536903680L, slidingMoves);
  //
  //    square = h4;
  //    slidingMoves = getSlidingMovesDiagUp(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Diag Up Attacks for %s%n", square);
  //    System.out.printf("Moves Diag Up %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(1161999072605765632L, slidingMoves);
  //
  //    square = h5;
  //    slidingMoves = getSlidingMovesDiagUp(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Diag Up Attacks for %s%n", square);
  //    System.out.printf("Moves Diag Up %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(274877906944L, slidingMoves);
  //
  //    position = new Position("1r2kb1r/2Rn4/p4p2/4pN1p/4N1p1/6B1/P4PPP/3R2K1 b k -");
  //
  //    // diagonal downwards
  //    square = f8;
  //    System.out.printf("Diag Down Attacks for %s%n", square);
  //    slidingMoves = getSlidingMovesDiagDown(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Moves Diag Down %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(8404992L, slidingMoves);
  //
  //    square = g3;
  //    System.out.printf("Diag Down Attacks for %s%n", square);
  //    slidingMoves = getSlidingMovesDiagDown(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Moves Diag Down %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(36028934726352896L, slidingMoves);
  //
  //    square = f5;
  //    System.out.printf("Diag Down  Attacks for %s%n", square);
  //    slidingMoves = getSlidingMovesDiagDown(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Moves Diag Down %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(274878957568L, slidingMoves);
  //
  //    square = h4;
  //    System.out.printf("Diag Down Attacks for %s%n", square);
  //    slidingMoves = getSlidingMovesDiagDown(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Moves Diag Down %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(1075838976L, slidingMoves);
  //
  //    square = e4;
  //    System.out.printf("Diag Down Attacks for %s%n", square);
  //    slidingMoves = getSlidingMovesDiagDown(square, position.getAllOccupiedBitboard());
  //    System.out.printf("Moves Diag Down %s: %n%s%n", square, Bitboard.toString(slidingMoves));
  //    System.out.println();
  //    assertEquals(18049583016051201l, slidingMoves);
  //
  //  }

  @Test
  void shiftBitboardTest() {
    // @formatter:off
    long bitboard, shifted;
    String expected, actual;
    Square square;
    int shift;


    bitboard = e4.bitboard();
    shift = SE;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = e4.bitboard();
    shift = S;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = e4.bitboard();
    shift = SW;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = e4.bitboard();
    shift = W;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = e4.bitboard();
    shift = NW;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);


    bitboard = e4.bitboard();
    shift = N;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);


    bitboard = e4.bitboard();
    shift = NE;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = e4.bitboard();
    shift = E;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = a4.bitboard();
    shift = NW;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = a4.bitboard();
    shift = NE;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = h4.bitboard();
    shift = NW;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = h4.bitboard();
    shift = NE;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = h8.bitboard();
    shift = NE;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = h8.bitboard();
    shift = SW;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = e1.bitboard();
    shift = S;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = whiteSquares;
    shift = W;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 1 0 1 0 1 0 0 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 0 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 0 \n"
              + "1 0 1 0 1 0 1 0 \n"
              + "0 1 0 1 0 1 0 0 \n"
              + "1 0 1 0 1 0 1 0";
    assertEquals(expected, actual);

    bitboard = a1UpDiag;
    shift = E;
    LOG.debug("Bitboard:\n{}", Bitboard.toString(bitboard));
    actual = Bitboard.toString(Bitboard.shiftBitboard(shift, bitboard));
    LOG.debug("Shifted by {}\n{}", shift, actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 1 0 \n"
              + "0 0 0 0 0 1 0 0 \n"
              + "0 0 0 0 1 0 0 0 \n"
              + "0 0 0 1 0 0 0 0 \n"
              + "0 0 1 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0";
    assertEquals(expected, actual);


  }
}
