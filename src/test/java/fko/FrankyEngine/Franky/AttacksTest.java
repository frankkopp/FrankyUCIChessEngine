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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttacksTest {

  private static final Logger LOG = LoggerFactory.getLogger(AttacksTest.class);

  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  private Attacks attacks;
  private Position position;

  @BeforeEach
  void setUp() {
    attacks = new Attacks();
  }

  @Test
  void computeAttacks() {
    String testFen;

    position = new Position();
    System.out.println(position);
    attacks.computeAttacks(position);
    System.out.printf("White Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[WHITE]));
    System.out.printf("Black Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[BLACK]));
    System.out.printf("Has check: %s", attacks.hasCheck);
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck);
    // TODO additional asserts

    testFen = "4r3/1pn3k1/4pPb1/p1Pp3r/3P2NR/1P3B2/3K2P1/4R3 b - -";
    position = new Position(testFen);
    System.out.println(position);
    attacks.computeAttacks(position);
    System.out.printf("White Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[WHITE]));
    System.out.printf("Black Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[BLACK]));
    System.out.printf("Has check: %s", attacks.hasCheck);
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck);
    // TODO additional asserts

    testFen = "4r3/1pn3k1/4p1b1/p1Pp1P1r/3P2NR/1P3B2/3K2P1/4R3 w - -";
    position = new Position(testFen);
    System.out.println(position);
    attacks.computeAttacks(position);
    System.out.printf("White Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[WHITE]));
    System.out.printf("Black Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[BLACK]));
    System.out.printf("Has check: %s", attacks.hasCheck);
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck);
    // TODO additional asserts

  }

  @Test
  @Disabled
  public void testTiming() {

    attacks = new Attacks();

    Position pos = new Position();
    final int e2e4 = Move.fromUCINotation(pos, "e2e4");
    pos.makeMove(e2e4);
    final int d7d5 = Move.fromUCINotation(pos, "d7d5");
    pos.makeMove(d7d5);
    final int e4d5 = Move.fromUCINotation(pos, "e4d5");
    pos.makeMove(e4d5);
    final int d8d5 = Move.fromUCINotation(pos, "d8d5");
    pos.makeMove(d8d5);
    final int b1c3 = Move.fromUCINotation(pos, "b1c3");
    pos.makeMove(b1c3);
    final int d5e5 = Move.fromUCINotation(pos, "d5e5");
    pos.makeMove(d5e5);
    final int f1e2 = Move.fromUCINotation(pos, "f1e2");
    pos.makeMove(f1e2);
    final int e8d8 = Move.fromUCINotation(pos, "e8d8");
    pos.makeMove(e8d8);
    final int g1f3 = Move.fromUCINotation(pos, "g1f3");
    pos.makeMove(g1f3);
    final int e5g5 = Move.fromUCINotation(pos, "e5g5");
    pos.makeMove(e5g5);
    final int e1g1 = Move.fromUCINotation(pos, "e1g1");
    pos.makeMove(e1g1);
    final int b8c6 = Move.fromUCINotation(pos, "b8c6");
    int[] moves = new int[]{e2e4, d7d5, e4d5, d8d5, b1c3, d5e5, f1e2, e8d8, g1f3, e5g5, e1g1, b8c6};

    ArrayList<String> result = new ArrayList<>();

    int ROUNDS = 5;
    int ITERATIONS = 20;
    int REPETITIONS = 50_000;

    for (int round = 1; round <= ROUNDS; round++) {
      long start, end, sum, i;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);

      System.gc();
      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1(new Position(), moves);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;
      result.add(String.format("Round %d Test 1 avg: %,.3f sec for %,d repetitions", round, avg1,
                               REPETITIONS));

      System.gc();
      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test2(new Position(), moves);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 2 avg: %,.3f sec for %,d repetitions", round, avg2,
                               REPETITIONS));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }
  }

  private void test1(final Position position, int[] moves) {
    for (int i = 0; i < moves.length; i++) {
      position.makeMove(moves[i]);
      attacks.computeAttacks(position);
    }
    for (int i = 0; i < moves.length; i++) position.undoMove();
  }

  private void test2(final Position position, int[] moves) {
    for (int i = 0; i < moves.length; i++) {
      position.makeMove(moves[i]);
      attacks.computeAttacks2(position);
    }
    for (int i = 0; i < moves.length; i++) position.undoMove();
  }

  @Test
  @Disabled
  public void testAbsoluteTiming() {

    int ROUNDS = 10;
    int DURATION = 3;
    int ITERATIONS;

    Instant start;

    System.out.println("Running Timing Test");

    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    position = new Position(fen);

    for (int j = 0; j < ROUNDS; j++) {
      System.gc();
      start = Instant.now();
      ITERATIONS = 0;
      do {
        ITERATIONS++;
        // ### TEST CODE
        //testCode();
        test3(position);
        // ### /TEST CODE
      } while (Duration.between(start, Instant.now()).getSeconds() < DURATION);
      System.out.println(String.format("Timing: %,7d runs/s", ITERATIONS / DURATION));

    }
  }

  private void test3(Position position) {
    attacks = new Attacks();
    attacks.computeAttacks(position);
  }

}
