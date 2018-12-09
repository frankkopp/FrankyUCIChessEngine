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

package fko.javaUCIEngineFramework.Franky;

import fko.javaUCIEngineFramework.UCI.IUCIProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Attempt at a proper Unit Test for Evaluation
 */
public class TestEvaluation {
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
  }

  @Test
  void mobility() {
  }

  @Test
  void position() {
  }

  @Test
  public final void testEvaluate_startPosValueZero() {
    // standard position should be 0
    // change if next player gets a bonus
    position = new Position(fenStandard);
    int value = evaluation.evaluate(position);
    assertEquals(0, value, "Start Position should be 0");
  }

  @Test
  public void testEvaluate_mirroredPositionEqual() {
    // Mirrored position - should be equal
    String fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    position = new Position(fen);
    int value1 = evaluation.evaluate(position);
    fen = "8/8/8/8/k7/1p6/P7/N6K b - - 0 1"; // black
    position = new Position(fen);
    int value2 = evaluation.evaluate(position);
    assertEquals(value1, value2, "Mirrored Position should be equal");
  }

  @Test
  public final void testMaterial_OfStartPosition() {
    position = new Position(fenStandard);
    int value = evaluation.material();
    assertEquals(0, value);
  }

  @Test
  public final void testMaterial_OfDifferentPositions() {
    String fen = "k6n/7p/6P1/7K/8/8/8/8 w - - 0 1"; // white
    position = new Position(fen);
    evaluation.setPosition(position);
    int value = evaluation.material();
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
  public final void testMobility_StartPosition() {
    position = new Position(fenStandard);
    evaluation.setPosition(position);
    int value = evaluation.mobility();
    //System.out.println(value);
    assertEquals(0, value);
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
  public final void testMobility_otherPositions() {
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    position = new Position(fen);
    evaluation.setPosition(position);
    int value = evaluation.mobility();
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
  @Disabled
  public final void testNewEvals() {
    position = new Position();
    evaluation.setPosition(position);

    int value = evaluation.evaluate(position);
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
      while (true) {
        ITERATIONS++;
        // ### TEST CODE
        testCode();
        // ### /TEST CODE
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) {
          break;
        }
      }
      System.out.println(String.format("Timing: %,7d runs/s", ITERATIONS / DURATION));

    }
  }

  private void testCode() {
    evaluation.evaluate(position);
  }


}
