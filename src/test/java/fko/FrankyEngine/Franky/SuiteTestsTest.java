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

import org.apache.commons.text.WordUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SuiteTestsTest
 * <p>
 * https://www.chessprogramming.org/Test-Positions#Test_Suites
 * http://www.bergbomconsulting.se/chess/testsuites.html
 * <p>
 * https://www.chessprogramming.org/Extended_Position_Description
 */
public class SuiteTestsTest {

  private static final Logger LOG = LoggerFactory.getLogger(SuiteTestsTest.class);

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
    /*
    Franky 0.10 - 10sec
    Successful:   9 (75 Prozent)
    Failed:       2 (16 Prozent)
    Franky 0.11 - 10sec
    Successful:  10 (83 Prozent)
    Failed:       1 (8 Prozent)
     */
    testSuite = new TestSuite("./testsets/franky_tests.epd");
    testSuite.setSearchTime(10000);
    testSuite.startTests();
  }

  @Test
  void startMateSuite() {
    /*
    Franky 0.10 - 10sec
    Successful:  11 (55 Prozent)
    Failed:       9 (45 Prozent)
    Franky 0.11 - 10sec
    Successful:   9 (45 Prozent)
    Failed:      11 (55 Prozent)
     */
    testSuite = new TestSuite("./testsets/mate_test_suite.epd");
    testSuite.setSearchTime(10000);
    testSuite.startTests();
  }

  @Test
  void startNullMOveZuzwangSuite() {
    /*
    Franky-0.11 - 10sec
    Successful:   4 (80 Prozent)
    Failed:       1 (20 Prozent)
    */
    testSuite = new TestSuite("./testsets/nullMoveZufZwangTest.epd");
    testSuite.setSearchTime(10000);
    testSuite.startTests();
  }

  @Test
  @Disabled
  void startECM98Suite() {
    /*
    Frank 0.10 - 10sec
    Successful: 375 (48 Prozent)
    Failed:     394 (51 Prozent)
    Franky-0.11 - 10sec
    Successful: 376 (48 Prozent)
    Failed:     393 (51 Prozent)
    */
    testSuite = new TestSuite("./testsets/ecm98.epd");
    testSuite.setSearchTime(10000);
    testSuite.startTests();
  }

  @Test
  @Disabled
  void startWACSuite() {
    /*
    0.11 BASE (PV earlier)
    Successful: 103 (34 Prozent)
    Failed:     197 (65 Prozent)
    0.11 BASE (PV later)
    Successful: 104 (34 Prozent)
    Failed:     196 (65 Prozent)
    */
    testSuite = new TestSuite("./testsets/wac.epd");
    testSuite.setSearchTime(5000);
    testSuite.startTests();
  }

  @Test
  @Disabled
  void startDefaultTestSuite() {
    /*
    Franky-0.10 - 10sec
    Successful: 172 (49 Prozent)
    Failed:     174 (50 Prozent)
    Franky-0.11 - 10sec
    Successful: 157 (45 Prozent)
    Failed:     189 (54 Prozent)
    */
    // ./testsets/crafty_test.epd
    testSuite.setSearchTime(5000);
    testSuite.startTests();
  }

  @Test
  @Disabled
  void startWACSuiteFeatureRun() {
    testSuite = new TestSuite("./testsets/wac.epd");
    final Configuration config = new Configuration();

    config.USE_BOOK = false;

    // Successful: 105 (35 Prozent)
    // Failed:     195 (65 Prozent)
    config.USE_ALPHABETA_PRUNING = true;
    config.USE_PVS = true;
    config.USE_PVS_ORDERING = true;
    config.USE_KILLER_MOVES = true;


    config.USE_ASPIRATION_WINDOW = false;
    config.USE_MTDf = false;

    config.USE_TRANSPOSITION_TABLE = false;
    config.USE_TT_ROOT = false;

    config.USE_MDP = false;
    config.USE_MPP = false;

    config.USE_RFP = false;
    config.USE_NMP = false;
    config.USE_RAZOR_PRUNING = false;

    config.USE_EXTENSIONS = false;

    config.USE_LIMITED_RAZORING = false;
    config.USE_EXTENDED_FUTILITY_PRUNING = false;
    config.USE_FUTILITY_PRUNING = false;
    config.USE_LMP = false;
    config.USE_LMR = false;

    config.USE_QUIESCENCE = false;

    final int time = 5000;

    System.out.printf("WAC Test: %,d ms %nCONFG: %s %n%n", time,
                      WordUtils.wrap(config.toString(), 80));
    testSuite.setConfig(config);
    testSuite.setSearchTime(time);
    testSuite.startTests();
  }

  @Test
  void startOneTest() {

    // bm test time limited
    assertTrue(
      testSuite.startOneTest("r7/2r1kpp1/1p6/pB1Pp1P1/Pbp1P3/2N2b1P/1PPK1P2/R6R b - - bm Bh1;",
                             5000, 0));

    // bm test depth limited
    assertTrue(
      testSuite.startOneTest("r7/2r1kpp1/1p6/pB1Pp1P1/Pbp1P3/2N2b1P/1PPK1P2/R6R b - - bm Bh1;", 0,
                             8));

    // bm test time & depth limited
    assertTrue(
      testSuite.startOneTest("r7/2r1kpp1/1p6/pB1Pp1P1/Pbp1P3/2N2b1P/1PPK1P2/R6R b - - bm Bh1;",
                             5000, 8));

  }

  @Test
  void startOneMateTest() {

    // dm test depth limited
    assertTrue(testSuite.startOneTest("8/8/8/8/8/3K4/R7/5k2 w - - dm 4;", 0, 12));

    // dm test time limited
    assertTrue(testSuite.startOneTest("8/8/8/8/8/3K4/R7/5k2 w - - dm 4;", 10000, 0));

    // dm test depth limited
    assertTrue(testSuite.startOneTest("8/8/8/8/8/3K4/R7/5k2 w - - dm 4;", 10000, 16));
  }

  @Test
  @Disabled
  public void manualTest() {

    // difficult for LMR because of Queen sacrifice
    assertTrue(
      testSuite.startOneTest("6K1/n1P2N1p/6pr/b1pp3b/n2Bp1k1/1R2R1Pp/3p1P2/2qN1B2 w - - dm 3;",
                             15000, 0));

  }

}
