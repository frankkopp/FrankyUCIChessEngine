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
import org.openjdk.jol.info.ClassLayout;

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

  @Test
  @Disabled
  public void attackBitboards() {

    // White Pawns
    System.out.println("WHITE PAWNS");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(
        Bitboard.printBitString(Bitboard.pawnAttacks[Color.WHITE.ordinal()][square.index64]));
      System.out.println(
        Bitboard.toString(Bitboard.pawnAttacks[Color.WHITE.ordinal()][square.index64]));
      System.out.println();
    }
    // Knight
    System.out.println("BLACK PAWN");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(
        Bitboard.printBitString(Bitboard.pawnAttacks[Color.BLACK.ordinal()][square.index64]));
      System.out.println(
        Bitboard.toString(Bitboard.pawnAttacks[Color.BLACK.ordinal()][square.index64]));
      System.out.println();
    }

    // Knight
    System.out.println("KNIGHTS");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(Bitboard.printBitString(Bitboard.knightAttacks[square.index64]));
      System.out.println(Bitboard.toString(Bitboard.knightAttacks[square.index64]));
      System.out.println();
    }

    // Bishop
    System.out.println("BISHOPS\n");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(Bitboard.printBitString(Bitboard.bishopAttacks[square.index64]));
      System.out.println(Bitboard.toString(Bitboard.bishopAttacks[square.index64]));
      System.out.println();
    }

    // Bishop
    System.out.println("ROOKS\n");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(Bitboard.printBitString(Bitboard.rookAttacks[square.index64]));
      System.out.println(Bitboard.toString(Bitboard.rookAttacks[square.index64]));
      System.out.println();
    }

    // Queen
    System.out.println("QUEEN\n");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(Bitboard.printBitString(Bitboard.queenAttacks[square.index64]));
      System.out.println(Bitboard.toString(Bitboard.queenAttacks[square.index64]));
      System.out.println();
    }

    // King
    System.out.println("KING\n");
    for (Square square : Square.validSquares) {
      System.out.println(square);
      System.out.println(Bitboard.printBitString(Bitboard.kingAttacks[square.index64]));
      System.out.println(Bitboard.toString(Bitboard.kingAttacks[square.index64]));
      System.out.println();
    }
  }

  @Test
  @Disabled
  public void showSize() {
    //System.out.println(VM.current().details());
    System.out.println(ClassLayout.parseClass(Bitboard.class).toPrintable());
  }

}
