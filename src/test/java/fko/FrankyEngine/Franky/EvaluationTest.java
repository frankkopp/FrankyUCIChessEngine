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

import fko.UCI.IUCIProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Attempt at a proper Unit Test for Evaluation
 */
public class EvaluationTest {

  private static final Logger LOG = LoggerFactory.getLogger(EvaluationTest.class);

  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  private String fenStandard;

  private Position   position;
  private Evaluation evaluation;

  @BeforeEach
  public void setUp() {
    fenStandard = IUCIProtocolHandler.START_FEN;
    position = new Position(fenStandard);
    evaluation = new Evaluation();
  }

  @Test
  void evaluate() {
    // standard position should be 0
    // change if next player gets a bonus
    position = new Position(fenStandard);
    int value = evaluation.evaluate(position);
    assertEquals(10, value, "Start Position should be 10 from TEMPO");

    // Mirrored position - should be equal
    position = new Position("k6n/7p/6P1/7K/8/8/8/8 w - - 0 1");
    int value1 = evaluation.evaluate(position);
    LOG.info(evaluation.toString());
    position = new Position("8/8/8/8/k7/1p6/P7/N6K b - - 0 1");
    int value2 = evaluation.evaluate(position);
    LOG.info(evaluation.toString());
    assertEquals(value1, value2, "Mirrored Position should be equal");

  }

  @Test
  void material() {
    // Start position
    position = new Position(fenStandard);
    int value = evaluation.material(position);
    assertEquals(0, value);

    // other positions
    String fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    position = new Position(fen);
    value = evaluation.material(position);
    // System.out.println(value);
    assertEquals(-320, value);

    fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    position = new Position(fen);
    value = evaluation.material(position);
    // System.out.println(value);
    assertEquals(-320, value);
  }

  @Test
  void mobility() {
    // Start position
    position = new Position(fenStandard);
    int value = evaluation.mobility(position);
    //System.out.println(value);
    assertEquals(0, value);

    // other positions
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    position = new Position(fen);
    value = evaluation.mobility(position);
    //System.out.println(value);
    assertEquals(52, value);

    fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    position = new Position(fen);
    value = evaluation.mobility(position);
    //System.out.println(value);
    assertEquals(-4, value);

    fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    position = new Position(fen);
    value = evaluation.mobility(position);
    //System.out.println(value);
    assertEquals(-4, value);
  }

  @Test
  void position() {
    int value;
    // Start Position
    position = new Position();
    value = evaluation.position(this.position);
    assertEquals(0, value);

    this.position.makeMove(Move.fromUCINotation(this.position, "e2e4"));
    value = evaluation.position(this.position);
    assertEquals(-55, value);

    this.position.makeMove(Move.fromUCINotation(this.position, "e7e5"));
    value = evaluation.position(this.position);
    assertEquals(0, value);

    this.position.makeMove(Move.fromUCINotation(this.position, "g1f3"));
    value = evaluation.position(this.position);
    assertEquals(-50, value);

    // All White pieces no Black pieces but King
    this.position = new Position("4k3/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQ - 0 1");
    assertEquals(-117, evaluation.position(this.position));

    // All Black pieces no White pieces but King
    this.position = new Position("rnbqkbnr/pppppppp/8/8/8/8/8/4K3 w kq - 0 1");
    assertEquals(117, evaluation.position(this.position));

  }

  @Test
  public final void testNeutralPosition() {
    position = new Position("7k/7p/8/8/8/8/P7/K7 w - - 0 1");
    int mat = evaluation.material(position);
    int mob = evaluation.mobility(position);
    System.out.println("Material: " + mat);
    System.out.println("Mobility: " + mob);

    position = new Position("7k/7p/8/8/8/8/P7/K7 b - - 0 1");
    mat = evaluation.material(position);
    mob = evaluation.mobility(position);
    System.out.println("Material: " + mat);
    System.out.println("Mobility: " + mob);

    assertEquals(0, mat);
    assertEquals(0, mob);
  }

  @Test
  public final void testCheckPosition() {
    // no in check
    position = new Position("r6k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2KR4 w - - 0 1");
    assertEquals(191, evaluation.evaluate(position));
    LOG.info(evaluation.toString());

    // white gives check to black
    position = new Position("r2R3k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2K5 b - - 0 1");
    assertEquals(-234, evaluation.evaluate(position));
    LOG.info(evaluation.toString());

    // black gives check to white
    position = new Position("r6k/6R1/p4p1p/2p2P1P/1p1qPN2/6P1/1PPK4/3R4 w - - 0 2");
    assertEquals(135, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
  }

  @Test
  public final void testSinglePieces() {

    // king values
    position = new Position("8/4k3/8/8/8/8/8/4K3 w - -");
    assertEquals(0, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
    position = new Position("8/4k3/8/8/8/8/8/4K3 b - -");
    assertEquals(0, evaluation.evaluate(position));

  }

  @Test
  public final void testKingSafety() {
    position = new Position("rnbq1rk1/pppp1ppp/5n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 w - -");
    assertEquals(0, evaluation.kingSafety(position));
    LOG.info(evaluation.toString());

    position = new Position("2kr1bnr/pppq1ppp/2np4/4p3/2B1P1b1/2NP1N2/PPP2PPP/R1BQ1RK1 w - -");
    assertEquals(10, evaluation.kingSafety(position));
    LOG.info(evaluation.toString());

    position = new Position("rnbq1rk1/pppp1ppp/5n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 b - -");
    assertEquals(0, evaluation.kingSafety(position));
    LOG.info(evaluation.toString());

    position = new Position("2kr1bnr/pppq1ppp/2np4/4p3/2B1P1b1/2NP1N2/PPP2PPP/R1BQ1RK1 b - -");
    assertEquals(-10, evaluation.kingSafety(position));
    LOG.info(evaluation.toString());
  }

  @Test
  public void testPositionValue() {
    position = new Position();
    int move;

    move = Move.fromSANNotation(position, "e4");
    assertEquals(25, Evaluation.getPositionValue(position, move));

    move = Move.fromSANNotation(position, "c3");
    assertEquals(-10, Evaluation.getPositionValue(position, move));

    move = Move.fromSANNotation(position, "Na3");
    assertEquals(-30, Evaluation.getPositionValue(position, move));

    position = new Position("r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq -");

    move = Move.fromSANNotation(position, "a6");
    assertEquals(5, Evaluation.getPositionValue(position, move));

    move = Move.fromSANNotation(position, "Qh4");
    assertEquals(-5, Evaluation.getPositionValue(position, move));

    move = Move.fromSANNotation(position, "Ke7");
    assertEquals(-20, Evaluation.getPositionValue(position, move));

  }

  @Test
  void pawnStructure() {
    // doubled pawn
    position = new Position("r2qkbnr/1pp2ppp/p1np4/4p3/B3P3/5P1P/PPPP1P2/RNBQ1RK1 b kq -");
    evaluation.evaluate(position);
    evaluation.printEvaluation();
    assertEquals(10, evaluation.pawnStructure(position));

    // passed pawn
    position = new Position("2r1r1k1/5npp/3q4/1QpP1p2/1p6/4PP2/1B2R1PP/2R3K1 w - -");
    evaluation.evaluate(position);
    evaluation.printEvaluation();
    assertEquals(-30, evaluation.pawnStructure(position));

    position = new Position("1qr1r1k1/5pp1/1p2p2p/1Qbn3b/2R5/3P1NPP/3NPPB1/1R4K1 w - -");
    evaluation.evaluate(position);
    evaluation.printEvaluation();
    assertEquals(-30, evaluation.pawnStructure(position));
  }

  @Test
  @Disabled
  public final void testNewEvals() {
    position = new Position("r1bq1rk1/pp1p1ppp/2n2b2/2p1p3/4P3/2NP1NP1/PPP1KPBP/R2Q3R b - - 1 1");
    evaluation.evaluate(position);
  }

  @Test
  @Disabled
  public void testAbsoluteTiming() {

    int ROUNDS = 10;
    int DURATION = 30;
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
        test3(position);
        // ### /TEST CODE
      } while (Duration.between(start, Instant.now()).getSeconds() < DURATION);
      System.out.println(String.format("Timing: %,7d runs/s", ITERATIONS / DURATION));

    }
  }

  private void testCode() {
    evaluation.evaluate(position);
  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    int ROUNDS = 5;
    int ITERATIONS = 10;
    int REPETITIONS = 1_000_000;

    final Position position = new Position(
      "1qr1r1k1/5pp1/1p2p2p/1Qbn3b/2R5/3P1NPP/3NPPB1/1R4K1 w - -");

    for (int round = 1; round <= ROUNDS; round++) {
      long start, end, sum, i;

      System.out.printf("Running round %d of Timing%n", round);

      System.gc();
      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1(position);
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
          test2(position);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 2 avg: %,.3f sec for %,d repetitions", round, avg2,
                               REPETITIONS));

      System.gc();
      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test3(position);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg3 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 3 avg: %,.3f sec for %,d repetitions", round, avg3,
                               REPETITIONS));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }
  }

  Attacks attacks = new Attacks();

  private void test1(final Position position) {
    int m = 0;
    List<Square> validSquares = Square.validSquares;
    for (int i = 0, validSquaresSize = validSquares.size(); i < validSquaresSize; i++) {
      Square sq = validSquares.get(i);
      Piece pc = position.getPiece(sq);
      if (pc.getColor().isBlack()) continue;
      switch (pc.getType()) {
        case NOTYPE:
          break;
        case PAWN:
          break;
        case KNIGHT:
          m += Evaluation.mobilityForPiece(PieceType.KNIGHT, sq, Square.knightDirections, position);
          break;
        case BISHOP:
          m += Evaluation.mobilityForPiece(PieceType.BISHOP, sq, Square.bishopDirections, position);
          break;
        case ROOK:
          m += Evaluation.mobilityForPiece(PieceType.ROOK, sq, Square.rookDirections, position);
          break;
        case QUEEN:
          m += Evaluation.mobilityForPiece(PieceType.QUEEN, sq, Square.queenDirections, position);
          break;
        case KING:
          break;
        default:
          break;
      }
    }
    //    System.out.println("Mob1=" + m);
  }

  private void test2(final Position position) {
    int m = 0;
    List<Square> validSquares = Square.validSquares;
    for (int i = 0, validSquaresSize = validSquares.size(); i < validSquaresSize; i++) {
      Square sq = validSquares.get(i);
      Piece pc = position.getPiece(sq);
      if (pc.getColor().isBlack()) continue;
      switch (pc.getType()) {
        case NOTYPE:
          break;
        case PAWN:
          break;
        case KNIGHT:
          m += Evaluation.mobilityForPiece2(PieceType.KNIGHT, sq, Square.knightDirections,
                                            position);
          break;
        case BISHOP:
          m += Evaluation.mobilityForPiece2(PieceType.BISHOP, sq, Square.bishopDirections,
                                            position);
          break;
        case ROOK:
          m += Evaluation.mobilityForPiece2(PieceType.ROOK, sq, Square.rookDirections, position);
          break;
        case QUEEN:
          m += Evaluation.mobilityForPiece2(PieceType.QUEEN, sq, Square.queenDirections, position);
          break;
        case KING:
          break;
        default:
          break;
      }
    }
  }

  private void test3(final Position position) {
    int m;
    attacks.computeAttacks(position);
    m = attacks.mobility[WHITE];
    //    System.out.println("Mob3=" + m);
  }

}
