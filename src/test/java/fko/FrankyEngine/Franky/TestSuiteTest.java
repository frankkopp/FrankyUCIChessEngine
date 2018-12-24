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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TestSuiteTest
 * <p>
 * https://www.chessprogramming.org/Test-Positions#Test_Suites
 * http://www.bergbomconsulting.se/chess/testsuites.html
 * <p>
 * TODO: Implement test set testing
 * TODO: Implement reading test sets from file - Extended Position Description (EPD)
 * https://www.chessprogramming.org/Extended_Position_Description
 */
public class TestSuiteTest {

  private static final Logger LOG = LoggerFactory.getLogger(TestSuiteTest.class);

  TestSuite testSuite;

  @BeforeEach
  void setUp() {
    testSuite = new TestSuite();
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void startFrankySuite() {
    testSuite = new TestSuite("./testsets/franky_tests.epd");
    testSuite.startTests(5000);
  }

  @Test
  @Disabled
  void startTestSuite() {
    testSuite.startTests(5000);
  }

  @Test
  void startOneTest() {
    //    testSuite.startOneTest("r1q1r1k1/1p3pp1/2p1bn1p/p3N3/2P2P2/P1Q1R3/1PB3PP/4R1K1 w - - bm
    //    h3; " +
    //                           "id \"Crafty Test Pos.23\"; c0 \"GK/DB Philadelphia 1996, Game
    //                           4, move" +
    //                           " 21W (h3)\";", 5000);
    //testSuite.startOneTest("1k1r3r/pp3pp1/1np1p1p1/4q3/1P1P4/5N2/P1P1RPPP/4R1K1 w - - bm Nxe5;
    // ", 5000);
    testSuite.startOneTest("r1bqkb1r/ppp1p1pp/1nnpp3/8/2PP4/5N2/PP3PPP/RNBQKB1R w KQkq - bm Ng5",
                           5000);
  }

  @Test
  void startOneMateTest() {
    assertTrue(testSuite.startOneTest("8/8/8/8/8/3K4/R7/5k2 w - - dm 4;", 5000));
    assertTrue(testSuite.startOneTest("8/8/8/8/4K3/8/R7/4k3 w - - dm 5;", 5000));
  }

}
