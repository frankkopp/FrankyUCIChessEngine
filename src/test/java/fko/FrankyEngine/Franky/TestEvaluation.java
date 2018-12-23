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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Attempt at a proper Unit Test for Evaluation
 */
public class TestEvaluation {

  private static final Logger LOG = LoggerFactory.getLogger(TestEvaluation.class);

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
  void getGamePhaseFactor() {
    position = new Position();
    evaluation.setPosition(position);
    assertEquals(24, evaluation.getGamePhaseFactor());

    position = new Position("r6k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2KR4 w - - 0 1");
    evaluation.setPosition(position);
    assertEquals(11, evaluation.getGamePhaseFactor());

    position = new Position("k6n/7p/6P1/7K/8/8/8/8 w - - 0 1");
    evaluation.setPosition(position);
    assertEquals(1, evaluation.getGamePhaseFactor());
  }

  @Test
  void material() {
    // Start position
    position = new Position(fenStandard);
    evaluation.setPosition(position);
    int value = evaluation.material();
    assertEquals(0, value);

    // other positions
    String fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    position = new Position(fen);
    evaluation.setPosition(position);
    value = evaluation.material();
    // System.out.println(value);
    assertEquals(-320, value);

    fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    position = new Position(fen);
    evaluation.setPosition(position);
    value = evaluation.material();
    // System.out.println(value);
    assertEquals(-320, value);
  }

  @Test
  void mobility() {
    // Start position
    position = new Position(fenStandard);
    evaluation.setPosition(position);
    int value = evaluation.mobility();
    //System.out.println(value);
    assertEquals(0, value);

    // other positions
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    position = new Position(fen);
    evaluation.setPosition(position);
    value = evaluation.mobility();
    //System.out.println(value);
    assertEquals(52, value);

    fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    position = new Position(fen);
    evaluation.setPosition(position);
    value = evaluation.mobility();
    //System.out.println(value);
    assertEquals(-4, value);

    fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    position = new Position(fen);
    evaluation.setPosition(position);
    value = evaluation.mobility();
    //System.out.println(value);
    assertEquals(-4, value);
  }

  @Test
  void position() {
    // Start Position
    position = new Position();
    evaluation.setPosition(position);
    evaluation.evaluate();
    assertEquals(0,evaluation.position());

    // All White pieces no Black pieces but King
    position = new Position("4k3/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQ - 0 1");
    evaluation.setPosition(position);
    evaluation.evaluate();
    assertEquals(-35,evaluation.position());

    // All Black pieces no White pieces but King
    position = new Position("rnbqkbnr/pppppppp/8/8/8/8/8/4K3 w kq - 0 1");
    evaluation.setPosition(position);
    evaluation.evaluate();
    assertEquals(35,evaluation.position());

  }

  @Test
  public final void testNeutralPosition() {
    position = new Position("7k/7p/8/8/8/8/P7/K7 w - - 0 1");
    evaluation.setPosition(position);
    int mat = evaluation.material();
    int mob = evaluation.mobility();
    System.out.println("Material: " + mat);
    System.out.println("Mobility: " + mob);

    position = new Position("7k/7p/8/8/8/8/P7/K7 b - - 0 1");
    evaluation.setPosition(position);
    mat = evaluation.material();
    mob = evaluation.mobility();
    System.out.println("Material: " + mat);
    System.out.println("Mobility: " + mob);

    assertEquals(0, mat);
    assertEquals(0, mob);
  }

  @Test
  public final void testCheckPosition() {
    // no in check
    position = new Position("r6k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2KR4 w - - 0 1");
    assertEquals(221, evaluation.evaluate(position));
    LOG.info(evaluation.toString());

    // white gives check to black
    position = new Position("r2R3k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2K5 b - - 0 1");
    assertEquals(-304, evaluation.evaluate(position));
    LOG.info(evaluation.toString());

    // black gives check to white
    position = new Position("r6k/6R1/p4p1p/2p2P1P/1p1qPN2/6P1/1PPK4/3R4 w - - 0 2");
    assertEquals(189, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
  }

  @Test
  public final void testSinglePieces() {

    // king values
    position = new Position("8/4k3/8/8/8/8/8/4K3 w - -");
    assertEquals(-30, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
    position = new Position("8/4k3/8/8/8/8/8/4K3 b - -");
    assertEquals(30, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
    position = new Position("k7/8/8/8/4K3/8/8/8 w - -");
    assertEquals(90, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
    position = new Position("k7/8/8/8/4K3/8/8/8 b - -");
    assertEquals(-90, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
    position = new Position("K7/8/8/8/4k3/8/8/8 w - -");
    assertEquals(-90, evaluation.evaluate(position));
    LOG.info(evaluation.toString());
    position = new Position("K7/8/8/8/4k3/8/8/8 b - -");
    assertEquals(90, evaluation.evaluate(position));
    LOG.info(evaluation.toString());

  }

  @Test
  public final void testKingSafety() {
    position = new Position("rnbq1rk1/pppp1ppp/5n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 w - -");
    evaluation.setPosition(position);
    assertEquals(0, evaluation.kingSafety());
    LOG.info(evaluation.toString());

    position = new Position("2kr1bnr/pppq1ppp/2np4/4p3/2B1P1b1/2NP1N2/PPP2PPP/R1BQ1RK1 w - -");
    evaluation.setPosition(position);
    assertEquals(10, evaluation.kingSafety());
    LOG.info(evaluation.toString());

    position = new Position("rnbq1rk1/pppp1ppp/5n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 b - -");
    evaluation.setPosition(position);
    assertEquals(0, evaluation.kingSafety());
    LOG.info(evaluation.toString());

    position = new Position("2kr1bnr/pppq1ppp/2np4/4p3/2B1P1b1/2NP1N2/PPP2PPP/R1BQ1RK1 b - -");
    evaluation.setPosition(position);
    assertEquals(-10, evaluation.kingSafety());
    LOG.info(evaluation.toString());
  }

  @Test
  @Disabled
  public final void testNewEvals() {
    position = new Position("rnbqkbnr/8/pppppppp/8/8/PPPPPPPP/8/RNBQKBNR w KQkq - 0 1");
    evaluation.setPosition(position);
    evaluation.evaluate();
  }

  @Test
  @Disabled
  public void testTiming() {

    int ROUNDS = 5;
    int DURATION = 2;

    int ITERATIONS = 0;

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
        testCode();
        // ### /TEST CODE
      } while (Duration.between(start, Instant.now()).getSeconds() < DURATION);
      System.out.println(String.format("Timing: %,7d runs/s", ITERATIONS / DURATION));

    }
  }

  private void testCode() {
    evaluation.evaluate(position);
  }


}
