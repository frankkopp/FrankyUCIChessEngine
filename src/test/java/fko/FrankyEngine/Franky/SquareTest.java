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
import org.junit.jupiter.api.Test;

import java.util.List;

import static fko.FrankyEngine.Franky.Bitboard.a6UpDiag;
import static fko.FrankyEngine.Franky.Square.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 *
 */
public class SquareTest {

  /**
   * Tests basic Square operations
   */
  @Test
  public void test() {
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
  public void index64Test() {
    assertEquals(0, a1.index64);
    assertEquals(7, h1.index64);
    assertEquals(8, a2.index64);
    assertEquals(56, a8.index64);
    assertEquals(63, h8.index64);
    assertEquals(-1, i1.index64);
    assertEquals(-1, p8.index64);
  }

  @Test
  public void bitBoardTest() {
    for (Square sq : validSquares) {
      System.out.printf("%s: %s  on up diagonal: %s down diagonal: %s%n", sq,
                        getBitboardString(sq.bitBoard), getBitboardString(sq.getUpDiag()),
                        getBitboardString(sq.getDownDiag()));
    }
    System.out.println();

    System.out.printf("%s: %s %n", a6UpDiag, getBitboardString(a6UpDiag));
    System.out.println();

    System.out.printf("Is %s on %s: %s %n", a7, a6.name() + "upDiag",
                      a7.getUpDiag() == a6UpDiag ? "TRUE" : "FALSE");
    System.out.printf("Is %s on %s: %s %n", b7, a6.name() + "upDiag",
                      b7.getUpDiag() == a6UpDiag ? "TRUE" : "FALSE");

    System.out.printf("Is %s on %s: %s %n", a7, a6.name() + "downDiag",
                      a7.getDownDiag() == a6UpDiag ? "TRUE" : "FALSE");
    System.out.printf("Is %s on %s: %s %n", b7, a6.name() + "downDiag",
                      b7.getDownDiag() == a6UpDiag ? "TRUE" : "FALSE");

  }

  @Test
  void fileBitBoard() {
    System.out.println(File.a.bitBoard);
    System.out.println(Bitboard.toString(File.a.bitBoard));
    assertEquals(72340172838076673L, File.a.bitBoard);
    System.out.println(File.e.bitBoard);
    System.out.println(Bitboard.toString(File.e.bitBoard));
    assertEquals(1157442765409226768L, File.e.bitBoard);
    System.out.println(File.h.bitBoard);
    System.out.println(Bitboard.toString(File.h.bitBoard));
    assertEquals(-9187201950435737472L, File.h.bitBoard);
    System.out.println(File.NOFILE.bitBoard);
    System.out.println(Bitboard.toString(File.NOFILE.bitBoard));
    assertEquals(0, File.NOFILE.bitBoard);

  }

  @Test
  void rankBitBoard() {
    System.out.println(Rank.r1.bitBoard);
    System.out.println(Bitboard.toString(Rank.r1.bitBoard));
    assertEquals(255L, Rank.r1.bitBoard);
    System.out.println(Rank.r4.bitBoard);
    System.out.println(Bitboard.toString(Rank.r4.bitBoard));
    assertEquals(4278190080L, Rank.r4.bitBoard);
    System.out.println(Rank.r8.bitBoard);
    System.out.println(Bitboard.toString(Rank.r8.bitBoard));
    assertEquals(-72057594037927936L, Rank.r8.bitBoard);
    System.out.println(Rank.NORANK.bitBoard);
    System.out.println(Bitboard.toString(Rank.NORANK.bitBoard));
    assertEquals(0, Rank.NORANK.bitBoard);
  }


  String getBitboardString(long bitboard) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < Long.numberOfLeadingZeros((long) bitboard); i++) {
      stringBuilder.append('0');
    }
    stringBuilder.append(Long.toBinaryString(bitboard));
    return stringBuilder.toString();
  }


}
