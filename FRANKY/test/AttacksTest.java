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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttacksTest {

  private static final Logger LOG = LoggerFactory.getLogger(AttacksTest.class);

  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  private Attacks  attacks;
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
    System.out.printf("Has check: %s%n", attacks.hasCheck());
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck());
    // TODO additional asserts

    testFen = "4r3/1pn3k1/4pPb1/p1Pp3r/3P2NR/1P3B2/3K2P1/4R3 b - -";
    position = new Position(testFen);
    System.out.println(position);
    attacks.computeAttacks(position);
    System.out.printf("White Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[WHITE]));
    System.out.printf("Black Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[BLACK]));
    System.out.printf("Has check: %s%n", attacks.hasCheck());
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck());
    // TODO additional asserts

    testFen = "4r3/1pn3k1/4p1b1/p1Pp1P1r/3P2NR/1P3B2/3K2P1/4R3 w - -";
    position = new Position(testFen);
    System.out.println(position);
    attacks.computeAttacks(position);
    System.out.printf("White Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[WHITE]));
    System.out.printf("Black Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[BLACK]));
    System.out.printf("Has check: %s%n", attacks.hasCheck());
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck());
    // TODO additional asserts

  }

  @Test
  void attacksToTest() {
    position = new Position("rnbqkbnr/1pp1pppp/3p4/p7/Q1P5/8/PP1PPPPP/RNB1KBNR b KQkq -");
    System.out.println(position);

    boolean expected = position.hasCheck();
    System.out.println(expected);

    long attacks = Attacks.attacksTo(position, Square.e8, Color.WHITE);
    assertTrue(attacks != 0);

  }

  @Test
  void seeTest() {
    int seeScore, move;

    //     ---------------------------------
    // 8: |   |   | b | r | r |   | k |   |
    //    ---------------------------------
    // 7: |   | p | q |   | b |   | p |   |
    //    ---------------------------------
    // 6: | p |   | n | p |   | p |   | p |
    //    ---------------------------------
    // 5: | P |   | p |   | p |   |   | n |
    //    ---------------------------------
    // 4: |   | P | N | P | P | P |   |   |
    //    ---------------------------------
    // 3: |   |   | P |   | B | N | P |   |
    //    ---------------------------------
    // 2: |   |   |   |   | Q |   | B | P |
    //    ---------------------------------
    // 1: | R |   |   | R |   |   | K |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H

    // b4c5 - d6c5 - open rook line
    position = new Position("2brr1k1/1pq1b1p1/p1np1p1p/P1p1p2n/1PNPPP2/2P1BNP1/4Q1BP/R2R2K1 w - -");
    System.out.println(position);

    move = Move.fromUCINotation(position, "a1b1");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(0, seeScore);

    move = Move.fromUCINotation(position, "f4e5");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(0, seeScore);

    //     ---------------------------------
    // 8: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 7: |   |   | n |   | n |   |   |   |
    //    ---------------------------------
    // 6: |   |   | p | k | p |   |   |   |
    //    ---------------------------------
    // 5: |   | R |   | q |   |   | Q |   |
    //    ---------------------------------
    // 4: |   |   | P | K | P |   |   |   |
    //    ---------------------------------
    // 3: |   |   | N |   | N |   |   |   |
    //    ---------------------------------
    // 2: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 1: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H

    position = new Position("8/2n1n3/2pkp3/1R1q2Q1/2PKP3/2N1N3/8/8 w - -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "e4d5");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(900, seeScore);

    //    ---------------------------------
    // 8: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 7: |   |   | n |   | n |   |   |   |
    //    ---------------------------------
    // 6: |   |   | p | k | p |   |   |   |
    //    ---------------------------------
    // 5: |   | R |   | p |   |   | q |   |
    //    ---------------------------------
    // 4: |   |   | P | K | P |   |   |   |
    //    ---------------------------------
    // 3: |   |   | N |   | N |   |   |   |
    //    ---------------------------------
    // 2: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 1: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H

    position = new Position("8/2n1n3/2pkp3/1R1p2q1/2PKP3/2N1N3/8/8 w - -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "e4d5");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(0, seeScore);

    //    ---------------------------------
    // 8: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 7: |   | b | n |   | n |   |   |   |
    //    ---------------------------------
    // 6: |   |   | p | k | p |   |   |   |
    //    ---------------------------------
    // 5: |   | R |   | p |   |   | r | q |
    //    ---------------------------------
    // 4: |   |   | P | K | P |   |   |   |
    //    ---------------------------------
    // 3: |   | B | N |   | N |   |   |   |
    //    ---------------------------------
    // 2: | Q |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 1: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H
    // 1. exd5 cxd5 2. cxd5 exd5 3. Ncxd5 Ncxd5 4. Nxd5 Nxd5 5. Bxd5 Bxd5
    // 6. Rxd5+ Rxd5+ 7. Qxd5+ Qxd5+
    //
    // 0. Move: WHITE_PAWN on e4 captures BLACK_PAWN on d5: score=100
    // 1. Move: WHITE_PAWN on e4 captures BLACK_PAWN on d5: score=100 (risk=100)
    // 2. Move: BLACK_PAWN on c6 captures WHITE_PAWN on d5: score=0 (risk=100)
    // 3. Move: WHITE_PAWN on c4 captures BLACK_PAWN on d5: score=100 (risk=100)
    // 4. Move: BLACK_PAWN on e6 captures WHITE_PAWN on d5: score=0 (risk=100)
    // 5. Move: WHITE_KNIGHT on c3 captures BLACK_PAWN on d5: score=100 (risk=320)
    // 6. Move: BLACK_KNIGHT on c7 captures WHITE_KNIGHT on d5: score=220 (risk=320)
    // 7. Move: WHITE_KNIGHT on e3 captures BLACK_KNIGHT on d5: score=100 (risk=320)
    // 8. Move: BLACK_KNIGHT on e7 captures WHITE_KNIGHT on d5: score=220 (risk=320)
    // 9. Move: WHITE_BISHOP on b3 captures BLACK_KNIGHT on d5: score=100 (risk=330)
    // 10. Move: BLACK_BISHOP on b7 captures WHITE_BISHOP on d5: score=230 (risk=330)
    // 11. Move: WHITE_ROOK on b5 captures BLACK_BISHOP on d5: score=100 (risk=500)
    // 12. Move: BLACK_ROOK on g5 captures WHITE_ROOK on d5: score=400 (risk=500)
    // 13. Move: WHITE_QUEEN on a2 captures BLACK_ROOK on d5: score=100 (risk=900)
    // 14. Move: BLACK_QUEEN on h5 captures WHITE_QUEEN on d5: score=800 (risk=900)
    // Move: e4d5: gain=0
    // gain[] = [0, 0, 0, 0, -220, 220, -220, 220, -230, 230, -400, 400, -800, 800,
    //  100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    position = new Position("8/1bn1n3/2pkp3/1R1p2rq/2PKP3/1BN1N3/Q7/8 w - -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "e4d5");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(0, seeScore);

    // EN PASSANT TEST
    //    ---------------------------------
    // 8: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 7: |   |   |   | r |   | p | k |   |
    //    ---------------------------------
    // 6: | p |   | R |   |   | p |   |   |
    //    ---------------------------------
    // 5: |   | p |   |   |   |   |   | p |
    //    ---------------------------------
    // 4: | r |   |   | P | p |   |   |   |
    //    ---------------------------------
    // 3: | P | R |   |   |   |   | P |   |
    //    ---------------------------------
    // 2: |   |   | P |   | K | P |   | P |
    //    ---------------------------------
    // 1: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H

    // en passant does not reveal attacks until now - needss to be added or
    // forever ignored
    position = new Position("8/3r1pk1/p1R2p2/1p5p/r2Pp3/PR4P1/2P1KP1P/8 b - d3");
    System.out.println(position);
    move = Move.fromUCINotation(position, "e4d3");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(100, seeScore);

    // King protection
    //    ---------------------------------
    // 8: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 7: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 6: |   |   |   | p |   |   |   |   |
    //    ---------------------------------
    // 5: |   |   |   |   | r |   |   |   |
    //    ---------------------------------
    // 4: |   |   | R | K | P |   |   |   |
    //    ---------------------------------
    // 3: |   |   |   |   |   | k |   |   |
    //    ---------------------------------
    // 2: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 1: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H
    position = new Position("8/8/3p4/4r3/2RKP3/5k2/8/8 b - -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "e5e4");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(100, seeScore);

    // Bad SEE example - see produces bad capture although it is a winning move
    //    ---------------------------------
    // 8: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 7: | r | n |   |   |   |   |   |   |
    //    ---------------------------------
    // 6: | P |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 5: | K |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 4: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 3: |   |   |   |   |   |   |   |   |
    //    ---------------------------------
    // 2: |   |   |   |   |   |   | k |   |
    //    ---------------------------------
    // 1: |   | Q |   |   |   |   |   |   |
    //    ---------------------------------
    //      A   B   C   D   E   F   G   H
    position = new Position("8/rn6/P7/K7/8/8/6k1/1Q6 w - -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "b1b7");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(-80, seeScore);

    // promotion tests
    // TODO improve this - should count won promotion value
    position = new Position("rn6/P7/K7/8/8/6k1/1Q6/8 w - -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "b2b8");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(-80, seeScore);

    // bug fix
    position = new Position("r1b1kbQr/ppp1p3/2nq4/3p4/8/3B4/PPPP1PPP/RNBQK1NR b KQkq -");
    System.out.println(position);
    move = Move.fromUCINotation(position, "d6h2");
    seeScore = Attacks.see(position, move);
    System.out.printf("Move: %s: gain=%d%n", Move.toSimpleString(move), seeScore);
    assertEquals(-300, seeScore);

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
    int ITERATIONS = 10;
    int REPETITIONS = 10_000;

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
      attacks.computeAttacks(position);
    }
    for (int i = 0; i < moves.length; i++) position.undoMove();
  }

  @Test
  @Disabled
  public void testAbsoluteTiming() {

    int ROUNDS = 10;
    int DURATION = 300;
    int ITERATIONS;

    long start;

    System.out.println("Running Timing Test");

    position = new Position("2brr1k1/1pq1b1p1/p1np1p1p/P1p1p2n/1PNPPP2/2P1BNP1/4Q1BP/R2R2K1 w - -");
    final int move = Move.fromUCINotation(position, "f4e5");

    for (int j = 0; j < ROUNDS; j++) {
      System.gc();
      start = System.nanoTime();
      ITERATIONS = 0;
      do {
        ITERATIONS++;
        // ### TEST CODE
        //testCode();
        test3(position, move);
        // ### /TEST CODE
      } while ((System.nanoTime() - start) / 1e9 < DURATION);
      System.out.println(String.format("Timing: %,7d runs/s", ITERATIONS / DURATION));

    }
  }

  private void test3(Position position, int move) {
    Attacks.see(position, move);
  }

}
