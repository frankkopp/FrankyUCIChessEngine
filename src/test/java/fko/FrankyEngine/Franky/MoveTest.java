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

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank
 *
 */
public class MoveTest {


    @Test
    public void testValidMove() {
        int move = 100;
        assertFalse(Move.isValid(move));
        move = Move.createMove(MoveType.NORMAL, Square.e2, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertTrue(Move.isValid(move));
        move = Move.createMove(MoveType.NOMOVETYPE, Square.e2, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertFalse(Move.isValid(move));
        move = Move.createMove(MoveType.NOMOVETYPE, Square.e2, Square.d3, Piece.NOPIECE, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertFalse(Move.isValid(move));
        move = Move.createMove(MoveType.NOMOVETYPE, Square.j1, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        assertFalse(Move.isValid(move));
    }

    @Test
    public void testCreateMove() {
        int move = Move.createMove(MoveType.NORMAL, Square.e2, Square.d3, Piece.WHITE_PAWN, Piece.BLACK_KNIGHT, Piece.NOPIECE);
        System.out.println(Move.toString(move));
        move = Move.createMove(MoveType.PROMOTION, Square.e2, Square.f1, Piece.BLACK_PAWN, Piece.WHITE_ROOK, Piece.BLACK_QUEEN);
        System.out.println(Move.toString(move));
        move = Move.createMove(MoveType.CASTLING, Square.e1, Square.g1, Piece.WHITE_KING, Piece.NOPIECE, Piece.NOPIECE);
        System.out.println(Move.toString(move));
    }

  static class Zug {
    int v = -99;
  }

  @Test
  @Disabled
  public void showSize() {
    //System.out.println(VM.current().details());
//    System.out.println(ClassLayout.parseClass(Zug.class).toPrintable());
  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();
    Random r = new Random();

    int ROUNDS = 5;
    int ITERATIONS = 20;
    int REPETITIONS = 10_000;

    for (int round = 0; round < ROUNDS; round++) {
      long start = 0, end = 0, sum = 0;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);
      System.gc();

      int i = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;

      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test2();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 1 avg: %,.3f sec", round, avg1));
      result.add(String.format("Round %d Test 2 avg: %,.3f sec", round, avg2));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }

  }

  private void test1() {
    Zug[] z = new Zug[10000];
    IntStream.range(0, 10000).forEach(i -> { z[i] = new Zug(); });
  }

  private void test2() {
    int[] z = new int[10000];
    IntStream.range(0, 10000).forEach(i -> { z[i] = 5; });

  }

}
