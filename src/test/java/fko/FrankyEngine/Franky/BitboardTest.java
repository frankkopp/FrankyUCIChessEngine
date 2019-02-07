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

import static org.junit.jupiter.api.Assertions.*;

class BitboardTest {

  @Test
  void toStringTest() {
    System.out.printf("Square a8: %d%n%s%n", Square.a8.bitBoard, Bitboard.toString(Square.a8.bitBoard));
    System.out.printf("Square a1: %d%n%s%n", Square.a1.bitBoard, Bitboard.toString(Square.a1.bitBoard));
    System.out.printf("Square e4: %d%n%s%n", Square.e4.bitBoard, Bitboard.toString(Square.e4.bitBoard));
    System.out.printf("Square h1: %d%n%s%n", Square.h1.bitBoard, Bitboard.toString(Square.h1.bitBoard));
    System.out.printf("Square h8: %d%n%s%n", Square.h8.bitBoard, Bitboard.toString(Square.h8.bitBoard));
    System.out.printf("Diagonal a1 up %d%n%s%n", Square.a1UpDiag, Bitboard.toString(Square.a1UpDiag));
    System.out.printf("Diagonal a8 down %d%n%s%n", Square.a8DownDiag, Bitboard.toString(Square.a8DownDiag));
  }
}
