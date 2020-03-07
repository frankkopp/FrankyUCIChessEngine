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

/**
 *
 */
package fko.FrankyEngine.Franky.openingbook;


import fko.FrankyEngine.Franky.Position;
import fko.FrankyEngine.util.HelperTools;
import org.junit.jupiter.api.Test;

import static fko.FrankyEngine.Franky.Move.NOMOVE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author fkopp
 *
 */
public class OpeningBookImplTest {

  @Test
  public void testPGNBook() {

    System.out.format("Testing Book...\n");

    long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    OpeningBook book = new OpeningBookImpl("/book/superbook.pgn", OpeningBookImpl.Mode.PGN);
    ((OpeningBookImpl) book)._config.FORCE_CREATE = true;
    book.initialize();

    Position currentBoard = new Position(OpeningBookImpl.STANDARD_BOARD_FEN);
    int bookMove;
    while ((bookMove = book.getBookMove(currentBoard.toFENString())) != NOMOVE) {
      //System.out.format("%s ==> %s%n",currentBoard.toFENString(),bookMove);
      currentBoard.doMove(bookMove);
    }
    assertNotEquals(OpeningBookImpl.STANDARD_BOARD_FEN, currentBoard.toFENString());
    System.out.format("Book OK%n%n");

    long memMid = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    book = null;

    System.gc();

    long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    System.out.println("Test End");
    System.out.format("Memory used at Start: %s MB %n", HelperTools.getMBytes(memStart));
    System.out.format("Memory used at Mid: %s MB %n", HelperTools.getMBytes(memMid));
    System.out.format("Memory used at End: %s MB%n%n", HelperTools.getMBytes(memEnd));

  }

  @Test
  public void testSimpleBook() {

    long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    OpeningBook book = new OpeningBookImpl("/book/book_smalltest.txt", OpeningBookImpl.Mode.SIMPLE);

    ((OpeningBookImpl) book)._config.FORCE_CREATE = true;

    book.initialize();

    System.out.format("Testing Book...");
    Position currentBoard = new Position(OpeningBookImpl.STANDARD_BOARD_FEN);
    int bookMove;
    while ((bookMove = book.getBookMove(currentBoard.toFENString())) != NOMOVE) {
      //System.out.format("%s ==> %s%n",currentBoard.toFENString(),bookMove);
      currentBoard.doMove(bookMove);
    }
    assertNotEquals(OpeningBookImpl.STANDARD_BOARD_FEN, currentBoard.toFENString());
    System.out.format("Book OK%n%n");

    long memMid = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    book = null;

    System.gc();

    long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    System.out.println("Test End");
    System.out.format("Memory used at Start: %s MB %n", HelperTools.getMBytes(memStart));
    System.out.format("Memory used at Mid: %s MB %n", HelperTools.getMBytes(memMid));
    System.out.format("Memory used at End: %s MB%n%n", HelperTools.getMBytes(memEnd));

  }

  @Test
  public void testSANBook() {

    long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    OpeningBook book = new OpeningBookImpl("/book/book_graham.txt", OpeningBookImpl.Mode.SAN);

    ((OpeningBookImpl) book)._config.FORCE_CREATE = true;

    book.initialize();

    System.out.format("Testing Book...");
    Position currentBoard = new Position(OpeningBookImpl.STANDARD_BOARD_FEN);
    int bookMove;
    while ((bookMove = book.getBookMove(currentBoard.toFENString())) != NOMOVE) {
      //System.out.format("%s ==> %s%n",currentBoard.toFENString(),bookMove);
      currentBoard.doMove(bookMove);
    }
    assertNotEquals(OpeningBookImpl.STANDARD_BOARD_FEN, currentBoard.toFENString());
    System.out.format("Book OK%n%n");

    long memMid = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    book = null;

    System.gc();

    long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    System.out.println("Test End");
    System.out.format("Memory used at Start: %s MB %n", HelperTools.getMBytes(memStart));
    System.out.format("Memory used at Mid: %s MB %n", HelperTools.getMBytes(memMid));
    System.out.format("Memory used at End: %s MB%n%n", HelperTools.getMBytes(memEnd));

  }

  @Test
  public void testSaveOpeningBooktoSERFile() throws Exception {
    final String testPath = "/book/unit_test_file.pgn";
    OpeningBookImpl book = new OpeningBookImpl(testPath, OpeningBookImpl.Mode.PGN);
    assertTrue(book.saveOpeningBooktoSERFile(testPath));
  }

  @Test
  public void testTryFromCache() throws Exception {
    final String testPath = "/book/unit_test_file.pgn";
    OpeningBookImpl book = new OpeningBookImpl(testPath, OpeningBookImpl.Mode.PGN);
    assertTrue(book.saveOpeningBooktoSERFile(testPath));
    assertTrue(book.tryFromCache(testPath));
  }

  /**
     *
     */
    @Test
    //@Disabled
    public void timingTest() {

        int runs = 0, runsPerRound = 1;
        long begin = System.nanoTime(), end;
        do {
            for (int i=0; i<runsPerRound; ++i) timedMethod();
            runs += runsPerRound;
        } while ((20*1000000000L) > System.nanoTime() - begin);
        end = System.nanoTime();

        final double rt = ((end-begin) / runs) * 0.000000001;
        System.out.println("Time for timedMethod() is " + rt + " seconds");
        System.out.format("Runs: %d in %f seconds", runs, ((end - begin) * 0.000000001));
    }

    void timedMethod() {
        //OpeningBook book = new OpeningBookImpl(null, FileSystems.getDefault().getPath("/book/book.txt"),Mode.SIMPLE);
        //OpeningBook book = new OpeningBookImpl(null, FileSystems.getDefault().getPath("/book/8moves_GM_LB.pgn"),Mode.PGN);

        OpeningBook book = new OpeningBookImpl("/book/Test_PGN/superbook.pgn", OpeningBookImpl.Mode.PGN);
        ((OpeningBookImpl) book)._config.FORCE_CREATE = true;
        book.initialize();
    }

}
