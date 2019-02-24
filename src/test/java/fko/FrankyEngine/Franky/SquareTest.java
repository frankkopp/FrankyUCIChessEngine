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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

import static fko.FrankyEngine.Franky.Bitboard.*;
import static fko.FrankyEngine.Franky.Square.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 *
 */
public class SquareTest {

  private static final Logger LOG = LoggerFactory.getLogger(SquareTest.class);

  @Test
  @Disabled
  void listFields() throws IllegalAccessException {
    for (Field f : Square.class.getFields()) {
      f.setAccessible(true);
      System.out.println(f.getName());
      if (f.isEnumConstant()) {
        final Square square = (Square) f.get(null);
        System.out.println(Bitboard.toString(square.bitboard()));
        System.out.println(printBitString(square.bitboard()));
        System.out.printf("upDiag %d%n", square.getUpDiag());
        System.out.printf("downDiag %d%n", square.getDownDiag());
      }
      System.out.println();
    }
  }

  @Test
  public void testSquares() {
    // Square addressing
    assertEquals(getSquare(0), a1);
    assertEquals(getSquare(119), h8);
    assertEquals(getSquare(8), NOSQUARE);
    assertEquals(getSquare(-1), NOSQUARE);
    assertEquals(getSquare(128), NOSQUARE);
    assertTrue(h8.isValidSquare());
    assertFalse(i8.isValidSquare());
    assertFalse(NOSQUARE.isValidSquare());

    // addressing with file and rank
    assertEquals(getSquare(1, 1), a1);
    assertEquals(getSquare(8, 8), h8);
    assertEquals(getSquare(1, 9), NOSQUARE);
    assertEquals(getSquare(0, 8), NOSQUARE);
    assertEquals(getSquare(9, 9), NOSQUARE);

    // getFile
    assertEquals(a1.getFile(), File.a);
    assertEquals(h8.getFile(), File.h);
    assertEquals(j1.getFile(), File.NOFILE);
    assertEquals(getSquare(0).getFile(), File.a);
    assertEquals(getSquare(8).getFile(), File.NOFILE);
    assertEquals(getSquare(128).getFile(), File.NOFILE);

    // getRank
    assertEquals(a1.getRank(), Rank.r1);
    assertEquals(h8.getRank(), Rank.r8);
    assertEquals(j1.getRank(), Rank.NORANK);
    assertEquals(getSquare(0).getRank(), Rank.r1);
    assertEquals(getSquare(8).getRank(), Rank.NORANK);
    assertEquals(getSquare(128).getRank(), Rank.NORANK);

    // base rows
    Square square = a2;
    assertTrue(square.isWhitePawnBaseRow());
    assertFalse(square.isBlackPawnBaseRow());
    assertTrue(square.isPawnBaseRow(Color.WHITE));
    assertFalse(square.isPawnBaseRow(Color.BLACK));
    square = e7;
    assertFalse(square.isWhitePawnBaseRow());
    assertTrue(square.isBlackPawnBaseRow());
    assertFalse(square.isPawnBaseRow(Color.WHITE));
    assertTrue(square.isPawnBaseRow(Color.BLACK));

    // iteration
    int counter = 0;
    for (Square sq : values) {
      if (!sq.isValidSquare()) continue;
      counter++;
    }
    assertEquals(64, counter);

    // access through getValueList()
    List<Square> list = getValueList();
    assertEquals(64, list.size());
    assertEquals(list.get(0), a1);
    assertEquals(list.get(63), h8);

    // check order by creating string
    StringBuilder sb = new StringBuilder();
    list.forEach(sb::append);
    assertEquals(
      "a1b1c1d1e1f1g1h1a2b2c2d2e2f2g2h2a3b3c3d3e3f3g3h3a4b4c4d4e4f4g4h4a5b5c5d5e5f5g5h5a6b6c6d6e6f6g6h6a7b7c7d7e7f7g7h7a8b8c8d8e8f8g8h8",
      sb.toString());

    counter = 0;
    for (File f : File.values()) {
      if (f == File.NOFILE) continue;
      counter++;
    }
    assertEquals(8, counter);

    counter = 0;
    for (Rank r : Rank.values()) {
      if (r == Rank.NORANK) continue;
      counter++;
    }
    assertEquals(8, counter);

  }

  @Test
  public void testDirections() {
    Square e4 = Square.e4;
    assertSame(e4.getNorth(), e5);
    assertSame(e4.getSouth(), e3);
    assertSame(e4.getEast(), f4);
    assertSame(e4.getWest(), d4);
  }

  @Test
  public void validSquaresTest() {
    assertTrue(Square.a1.isValidSquare());
    assertTrue(Square.h1.isValidSquare());
    assertTrue(Square.a8.isValidSquare());
    assertTrue(Square.h8.isValidSquare());
    assertFalse(Square.i1.isValidSquare());
    assertFalse(Square.i8.isValidSquare());
    assertFalse(Square.o1.isValidSquare());
    assertFalse(Square.o8.isValidSquare());
  }

  @Test
  public void index64Test() {
    assertEquals(56, a1.bbIndex());
    assertEquals(63, h1.bbIndex());
    assertEquals(48, a2.bbIndex());
    assertEquals(0, a8.bbIndex());
    assertEquals(7, h8.bbIndex());
    assertEquals(-1, i1.bbIndex());
    assertEquals(-1, p8.bbIndex());
  }

  @Test
  public void index64MapTest() {
    assertEquals(a1, Square.index64Map[56]);
    assertEquals(a8, Square.index64Map[0]);
    assertEquals(h1, Square.index64Map[63]);
    assertEquals(h8, Square.index64Map[7]);
  }

  @Test
  public void fileRankTest() {
    assertEquals(File.c, Square.c3.getFile());
    assertEquals(2, Square.c3.getFile().ordinal());
    assertEquals(File.g, Square.g8.getFile());
    assertEquals(6, Square.g8.getFile().ordinal());

    assertEquals(Rank.r3, Square.c3.getRank());
    assertEquals(2, Square.c3.getRank().ordinal());
    assertEquals(Rank.r8, Square.g8.getRank());
    assertEquals(7, Square.g8.getRank().ordinal());

    assertTrue(Square.a2.isWhitePawnBaseRow());
    assertTrue(Square.e2.isWhitePawnBaseRow());
    assertTrue(Square.h2.isWhitePawnBaseRow());
    assertFalse(Square.b3.isWhitePawnBaseRow());
    assertFalse(Square.a2.isBlackPawnBaseRow());
    assertTrue(Square.a7.isBlackPawnBaseRow());
    assertTrue(Square.e7.isBlackPawnBaseRow());
    assertFalse(Square.h6.isWhitePawnBaseRow());
  }

  @Test
  public void fileRankBitboardsTest() {
    Square square;
    String actual;
    String expected;

    square = b1;
    actual = Bitboard.printBitString(square.getRank().bitBoard);
    LOG.debug("{}\n{}", square, actual);
    expected = "11111111.00000000.00000000.00000000.00000000.00000000.00000000.00000000";
    assertEquals(expected, actual);
    assertEquals(-72057594037927936L, square.getRank().bitBoard);

    square = c8;
    actual = Bitboard.printBitString(square.getRank().bitBoard);
    LOG.debug("{}\n{}", square, actual);
    expected = "00000000.00000000.00000000.00000000.00000000.00000000.00000000.11111111";
    assertEquals(expected, actual);

    square = a1;
    actual = Bitboard.printBitString(square.getFile().bitBoard);
    LOG.debug("{}\n{}", square, actual);
    expected = "00000001.00000001.00000001.00000001.00000001.00000001.00000001.00000001";
    assertEquals(expected, actual);

    square = b1;
    actual = Bitboard.printBitString(square.getFile().bitBoard);
    LOG.debug("{}\n{}", square, actual);
    expected = "00000010.00000010.00000010.00000010.00000010.00000010.00000010.00000010";
    assertEquals(expected, actual);

    square = f7;
    actual = Bitboard.printBitString(square.getFile().bitBoard);
    LOG.debug("{}\n{}", square, actual);
    expected = "00100000.00100000.00100000.00100000.00100000.00100000.00100000.00100000";
    assertEquals(expected, actual);
  }

  @Test
  void getFirstSquareTest() {
    assertEquals(a1, Square.getFirstSquare(a1.bitboard()));
    assertEquals(a8, Square.getFirstSquare(a8.bitboard()));
    assertEquals(h1, Square.getFirstSquare(h1.bitboard()));
    assertEquals(h8, Square.getFirstSquare(h8.bitboard()));

    assertEquals(h8, Square.getFirstSquare(h8.bitboard() | h1.bitboard()));
    assertEquals(a8, Square.getFirstSquare(h8.bitboard() | a8.bitboard()));
    assertEquals(g1, Square.getFirstSquare(g1.bitboard() | h1.bitboard()));
    assertEquals(e4, Square.getFirstSquare(e4.bitboard() | e3.bitboard()));
  }

  @Test
  void loopThroughPieces() {
    long bitboard = new Position().getAllOccupiedBitboard();
    Square square;
    int counter = 0;
    while ((square = Square.getFirstSquare(bitboard)) != NOSQUARE) {
      LOG.debug("{}", square);
      counter++;
      bitboard ^= Square.getFirstSquare(bitboard).bitboard();
    }
    assertEquals(32, counter);
  }

  /**
   * Tests basic Square operations
   */
  @Test
  @Disabled
  public void listSquareBitboards() {
    for (Square square : validSquares) {
      System.out.println(square);
      System.out.println(Bitboard.toString(square.bitboard()));
      System.out.println(printBitString(square.bitboard()));
    }
  }

  @Test
  void squareBitboardTest() {
    // @formatter:off
    long bitboard;
    String expected, actual;
    Square square;

    square = Square.b1;
    bitboard = square.bitboard();
    actual = Bitboard.toString(bitboard);
    LOG.debug("Square {} Index64 {}", square, square.bbIndex());
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 1 0 0 0 0 0 0";
    assertEquals(expected, actual);
    assertEquals(144115188075855872L, bitboard);

    square = Square.d8;
    bitboard = square.bitboard();
    actual = Bitboard.toString(bitboard);
    LOG.debug("Square {} Index64 {}", square, square.bbIndex());
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 1 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    square = Square.h5;
    bitboard = square.bitboard();
    actual = Bitboard.toString(bitboard);
    LOG.debug("Square {} Index64 {}", square, square.bbIndex());
    LOG.debug("\n{}", actual);
    expected =  "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 1 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0 \n"
              + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);
    // @formatter:on

  }

  @Test
  void diagonalBitboards() {
    // @formatter:off
    long bitboard;
    String expected, actual;

    // UP
    bitboard = Square.a8UpDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =   "1 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0 \n"
               + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = Square.a1UpDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "0 0 0 0 0 0 0 1 \n"
                + "0 0 0 0 0 0 1 0 \n"
                + "0 0 0 0 0 1 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 1 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0 \n"
                + "0 1 0 0 0 0 0 0 \n"
                + "1 0 0 0 0 0 0 0";
    assertEquals(expected, actual);


    bitboard = Square.c1UpDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 1 \n"
                + "0 0 0 0 0 0 1 0 \n"
                + "0 0 0 0 0 1 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 1 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0";
    assertEquals(expected, actual);


    bitboard = Square.h1UpDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    // DOWN
    bitboard = a8DownDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "1 0 0 0 0 0 0 0 \n"
                + "0 1 0 0 0 0 0 0 \n"
                + "0 0 1 0 0 0 0 0 \n"
                + "0 0 0 1 0 0 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 0 0 1 0 0 \n"
                + "0 0 0 0 0 0 1 0 \n"
                + "0 0 0 0 0 0 0 1";
    assertEquals(expected, actual);

    bitboard = a1DownDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "1 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = c8DownDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "0 0 1 0 0 0 0 0 \n"
                + "0 0 0 1 0 0 0 0 \n"
                + "0 0 0 0 1 0 0 0 \n"
                + "0 0 0 0 0 1 0 0 \n"
                + "0 0 0 0 0 0 1 0 \n"
                + "0 0 0 0 0 0 0 1 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    bitboard = h8DownDiag;
    actual = Bitboard.toString(bitboard);
    LOG.debug("\n{}", actual);
    expected =    "0 0 0 0 0 0 0 1 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0 \n"
                + "0 0 0 0 0 0 0 0";
    assertEquals(expected, actual);

    // @formatter:on
  }

  @Test
  public void diagonalBitboardTest() {
    for (Square sq : validSquares) {
      System.out.printf("%s:%n%s%non up diagonal:%n%s%ndown diagonal:%n%s%n", sq,
                        Bitboard.toString(sq.bitboard()), Bitboard.toString(sq.getUpDiag()),
                        Bitboard.toString(sq.getDownDiag()));
    }
    System.out.println();

    System.out.printf("%s: %s %n", a6UpDiag, Bitboard.toString(a6UpDiag));
    System.out.println();

    assertEquals(a1.getDownDiag(), a1DownDiag);
    assertEquals(a2.getDownDiag(), a2DownDiag);
    assertEquals(b1.getDownDiag(), a2DownDiag);
    assertEquals(b7.getDownDiag(), a8DownDiag);
    assertEquals(c4.getDownDiag(), a6DownDiag);
    assertEquals(e4.getDownDiag(), a8DownDiag);
    assertEquals(e8.getDownDiag(), e8DownDiag);
    assertEquals(g7.getDownDiag(), f8DownDiag);

    assertEquals(a1.getUpDiag(), a1UpDiag);
    assertEquals(a2.getUpDiag(), a2UpDiag);
    assertEquals(b1.getUpDiag(), b1UpDiag);
    assertEquals(b7.getUpDiag(), a6UpDiag);
    assertEquals(c4.getUpDiag(), a2UpDiag);
    assertEquals(e4.getUpDiag(), b1UpDiag);
    assertEquals(e8.getUpDiag(), a4UpDiag);
    assertEquals(g7.getUpDiag(), a1UpDiag);

    System.out.printf("Is %s on %s: %s %n", a7, a6.name() + "upDiag",
                      a7.getUpDiag() == a6UpDiag ? "TRUE" : "FALSE");
    assertNotEquals(a7.getUpDiag(), a6UpDiag);
    System.out.printf("Is %s on %s: %s %n", b7, a6.name() + "upDiag",
                      b7.getUpDiag() == a6UpDiag ? "TRUE" : "FALSE");
    assertEquals(b7.getUpDiag(), a6UpDiag);
    System.out.printf("Is %s on %s: %s %n", a7, a6.name() + "downDiag",
                      a7.getDownDiag() == a6UpDiag ? "TRUE" : "FALSE");
    assertNotEquals(a7.getDownDiag(), a6DownDiag);
    System.out.printf("Is %s on %s: %s %n", b7, a6.name() + "downDiag",
                      b7.getDownDiag() == a6UpDiag ? "TRUE" : "FALSE");
    assertNotEquals(b7.getDownDiag(), a6DownDiag);

  }

}
