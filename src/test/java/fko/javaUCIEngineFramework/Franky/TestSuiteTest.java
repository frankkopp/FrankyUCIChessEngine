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

import fko.javaUCIEngineFramework.UCI.IUCIEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * TestSuiteTest
 *
 * https://www.chessprogramming.org/Test-Positions#Test_Suites
 * http://www.bergbomconsulting.se/chess/testsuites.html
 *
 * TODO: Implement test set testing
 * TODO: Implement reading test sets from file - Extended Position Description (EPD)
 * https://www.chessprogramming.org/Extended_Position_Description
 *
 */
public class TestSuiteTest {

  private static final Logger LOG = LoggerFactory.getLogger(TestSuiteTest.class);

  private IUCIEngine engine;
  private Search     search;

  @BeforeEach
  void setUp() {
    engine = new FrankyEngine();
    search = new Search(engine, new Configuration());
  }

  @AfterEach
  void tearDown()  {
  }

  /**
   * 2rqk2r/pb1nbp1p/4p1p1/1B1n4/Np1N4/7Q/PP3PPP/R1B1R1K1 w k - bm Rxe6;
   * r1bq1rk1/3nbppp/p2pp3/6PQ/1p1BP2P/2NB4/PPP2P2/2KR3R w - - bm Bxg7;
   * 2kr4/ppq2pp1/2b1pn2/2P4r/2P5/3BQN1P/P4PP1/R4RK1 b - - bm Ng4!;
   * r1bqr1k1/pp1n1ppp/5b2/4N1B1/3p3P/8/PPPQ1PP1/2K1RB1R w - - bm Nxf7;
   * 3r4/2r5/p3nkp1/1p3p2/1P1pbP2/P2B3R/2PRN1P1/6K1 b - - bm Rc3!!;
   * 6k1/p3b1np/6pr/6P1/1B2p2Q/K7/7P/8 w - - am Qxh6??;
   * 3b4/p3P1q1/P1n2pr1/4p3/2B1n1Pk/1P1R4/P1p3KN/1N6 w - - bm Rh3+ M15;
   * 7r/8/pB1p1R2/4k2q/1p6/1Pr5/P5Q1/6K1 w - - bm Bd4+ M15;
   * 3r1r1k/1b4pp/ppn1p3/4Pp1R/Pn5P/3P4/4QP2/1qB1NKR1 w - - bm Rxh7+ M18;
   * 1k2r2r/pbb2p2/2qn2p1/8/PP6/2P2N2/1Q2NPB1/R4RK1 b - - bm Qxf3;
   * r6k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2KR4 w - - bm b3!;
   */
  @Test
  @Disabled
  public void manualTestSuite() {
    String testFen = "1k2r2r/pbb2p2/2qn2p1/8/PP6/2P2N2/1Q2NPB1/R4RK1 b - -";

    BoardPosition boardPosition = new BoardPosition(testFen);

    System.out.println(boardPosition.toBoardString());

    SearchMode searchMode = new SearchMode(0, 0, 0,
                                           0, 0, 0,
                                           0, 0, 0,
                                           null, false,
                                           true, false);

    search.startSearch(boardPosition, searchMode);

    // test search
    waitWhileSearching();

  }

  private void waitWhileSearching() {
    while (search.isSearching()) {
      try {
        Thread.sleep(1000);
        LOG.debug(search.getSearchCounter().toString());
      } catch (InterruptedException ignored) {
      }
    }
  }

}
