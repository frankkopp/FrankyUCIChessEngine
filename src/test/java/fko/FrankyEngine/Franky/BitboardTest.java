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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class BitboardTest {

  @Test
  void listFields() throws IllegalAccessException {
    for (Field f : Bitboard.class.getFields()) {
      f.setAccessible(true);
      long bitboard = (long) f.get(null);
      System.out.printf("%s %s %n%s%n", f.getName(), bitboard, Bitboard.toString(bitboard));
    }
  }

  @Test
  void toStringTest() {

    //    System.out.printf("Square a8: %d%n%s%n", Square.a8.bitBoard,
    //                      Bitboard.toString(Square.a8.bitBoard));
    ////    assertEquals(
    ////      "1 0 0 0 0 0 0 0 \n" + "0 0 0 0 0 0 0 0 \n" + "0 0 0 0 0 0 0 0 \n" + "0 0 0 0 0 0 0 0 \n"
    ////      + "0 0 0 0 0 0 0 0 \n" + "0 0 0 0 0 0 0 0 \n" + "0 0 0 0 0 0 0 0 \n" + "0 0 0 0 0 0 0 0 \n",
    ////      Bitboard.toString(Square.a8.bitBoard));
    //
    //    System.out.printf("Square a1: %d%n%s%n", Square.a1.bitBoard,
    //                      Bitboard.toString(Square.a1.bitBoard));
    //    System.out.printf("Square e4: %d%n%s%n", Square.e4.bitBoard,
    //                      Bitboard.toString(Square.e4.bitBoard));
    //    System.out.printf("Square h1: %d%n%s%n", Square.h1.bitBoard,
    //                      Bitboard.toString(Square.h1.bitBoard));
    //    System.out.printf("Square h8: %d%n%s%n", Square.h8.bitBoard,
    //                      Bitboard.toString(Square.h8.bitBoard));
    //
    //    System.out.printf("Diagonal a8 up %d%n%s%n", a8UpDiag, Bitboard.toString(a8UpDiag));
    //    System.out.printf("Diagonal a4 up %d%n%s%n", a4UpDiag, Bitboard.toString(a4UpDiag));
    //    System.out.printf("Diagonal a1 up %d%n%s%n", a1UpDiag, Bitboard.toString(a1UpDiag));
    //    System.out.printf("Diagonal c1 up %d%n%s%n", c1UpDiag, Bitboard.toString(c1UpDiag));
    //    System.out.printf("Diagonal h1 up %d%n%s%n", h1UpDiag, Bitboard.toString(h1UpDiag));
    //    System.out.printf("Diagonal a1 up %d%n%s%n", a1UpDiag, Bitboard.toString(a1UpDiag));
    //
    //    System.out.printf("Diagonal a8 down %d%n%s%n", a8DownDiag, Bitboard.toString(a8DownDiag));

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

    bitboard = bitboard << (8*7);
    System.out.printf("Long: %d%n", bitboard);
    System.out.println(Bitboard.toString(bitboard));

  }

}
