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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttacksTest {

  private static final Logger LOG = LoggerFactory.getLogger(AttacksTest.class);

  private static final int WHITE = Color.WHITE.ordinal();
  private static final int BLACK = Color.BLACK.ordinal();

  private Attacks attacks;

  @BeforeEach
  void setUp() {
    attacks = new Attacks();
  }

  @Test
  void computeAttacks() {
    String testFen;
    Position position;

    position = new Position();
    System.out.println(position);
    attacks.computeAttacks(position);
    System.out.printf("White Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[WHITE]));
    System.out.printf("Black Attacks: %n%s%n", Bitboard.toString(attacks.allAttacks[BLACK]));
    System.out.printf("Has check: %s", attacks.hasCheck);
    System.out.printf("Mobility White: %d Mobility Black: %d%n", attacks.mobility[WHITE],
                      attacks.mobility[BLACK]);
    assertEquals(position.hasCheck(), attacks.hasCheck);

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
  }

}
