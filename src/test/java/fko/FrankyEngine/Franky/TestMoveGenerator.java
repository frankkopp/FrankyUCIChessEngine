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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 */
public class TestMoveGenerator {

  @Test
  public void testFromStandardBoard() {

    {
      // Standard position has 20 moves to make
      Position position = new Position();
      MoveGenerator moveGenerator = new MoveGenerator(position);
      MoveList moves = moveGenerator.getLegalMoves();
      System.out.println(moves);
      assertEquals(20, moves.size());
    }

    {
      // 83 moves to make
      String testFen = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3";
      Position position = new Position(testFen);
      MoveGenerator moveGenerator = new MoveGenerator(position);
      MoveList moves = moveGenerator.getLegalMoves();
      System.out.println(moves);
      assertEquals(83, moves.size());
    }

    {
      // 218 moves to make
      String testFen = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1";
      Position position = new Position(testFen);
      MoveGenerator moveGenerator = new MoveGenerator(position);
      MoveList moves = moveGenerator.getLegalMoves();
      System.out.println(moves);
      assertEquals(218, moves.size());
    }
  }

  /**
   * Perft Test https://chessprogramming.wikispaces.com/Perft+Results
   */
  @Test
  public void testStandardPerft() {

    System.out.println("Standard PERFT Test");
    System.out.println("==============================");

    // @formatter:off
    long[][] results = {
          //N  Nodes      Captures EP     Checks  Mates
          { 0, 1,         0,       0,     0,      0},
          { 1, 20,        0,       0,     0,      0},
          { 2, 400,       0,       0,     0,      0},
          { 3, 8902,      34,      0,     12,     0},
          { 4, 197281,    1576,    0,     469,    8},
          { 5, 4865609,   82719,   258,   27351,  347},
          { 6, 119060324, 2812008, 5248,  809099, 10828},
    };
    // @formatter:on

    int maxDepth = 5;

    PERFT perftTest = new PERFT();

    for (int i = 1; i <= maxDepth; i++) {
      perftTest.testPerft(i);

      assertEquals(perftTest.get_nodes(), results[i][1]);
      assertEquals(perftTest.get_captureCounter(), results[i][2]);
      assertEquals(perftTest.get_enpassantCounter(), results[i][3]);
      assertEquals(perftTest.get_checkCounter(), results[i][4]);
      assertEquals(perftTest.get_checkMateCounter(), results[i][5]);
    }
    System.out.println("==============================");
  }

  @Test
  public void testMaxMovesPosition() {
    String testFen = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - -"; // 218 moves to make
    Position board = new Position(testFen);
    MoveGenerator moveGenerator = new MoveGenerator(board);
    MoveList legal_moves = moveGenerator.getLegalMoves().clone();
    MoveList pseudo_moves = moveGenerator.getPseudoLegalMoves().clone();
    assertEquals(218, legal_moves.size());
    assertEquals(218, pseudo_moves.size());
  }

  @Test
  public void testPosition() {

    String testFen = "1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - -";
    Position board = new Position(testFen);

    MoveGenerator moveGenerator = new MoveGenerator(board);
    MoveList pseudo_moves = moveGenerator.getPseudoLegalMoves().clone();
    MoveList legal_moves = moveGenerator.getLegalMoves().clone();
    MoveList qsearch_moves = moveGenerator.getPseudoLegalQSearchMoves().clone();

    assertEquals(49, pseudo_moves.size());
    assertEquals(48, legal_moves.size());
    assertEquals(1, qsearch_moves.size());

    for (int plMove : pseudo_moves) {
      boolean found = false;
      System.out.print("Move: " + Move.toSimpleString(plMove) + " ");
      for (int lMove : legal_moves) {
        if (plMove == lMove) {
          System.out.print(Move.toSimpleString(lMove) + " ");
          found = true;
        }
      }
      if (!found) {
        System.out.print("---- ");
      }
      found = false;
      for (int qMove : qsearch_moves) {
        if (plMove == qMove) {
          System.out.print(Move.toSimpleString(qMove) + " ");
          found = true;
        }
      }
      if (!found) {
        System.out.print("----");
      }
      System.out.println();
    }
  }

  /**
   * Tests mate position
   */
  @Test
  public void testMatePosition() {

    String testFen = "rnb1kbnr/pppp1ppp/4p3/8/5P1q/N7/PPPPP1PP/R1BQKBNR w KQkq - 2 3";

    Position board = new Position(testFen);
    MoveGenerator moveGenerator = new MoveGenerator(board);

    boolean hasLegalMoves = moveGenerator.hasLegalMove();
    MoveList moves = moveGenerator.getLegalMoves();
    boolean hasMate = board.hasCheckMate();

    assertFalse(hasMate);
    assertFalse(moves.empty());
    assertTrue(hasLegalMoves);

  }

  /**
   * Tests the generated moves from board setup with killer moves sorting
   */
  @Test
  public void testKiller() {

    String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/pbp2PPP/1R4K1 w kq - 0 113";
    Position board = new Position(testFen);

    int killer1 = 67178790; // 67178790 (NORMAL Rg3-c3)
    int killer2 = 67178534; // 67178534 (NORMAL Rg3-a3)
    int killer3 = 67130005; // already at first position

    MoveGenerator moveGenerator = new MoveGenerator(board);
    moveGenerator.setKillerMoves(new int[]{killer1, killer2, killer3});
    MoveList moves = moveGenerator.getLegalMoves();

    int lastCapture = 0;
    for (int i = 0; i < moves.size(); i++) {
      // skip the captures
      if (!Move.getTarget(moves.get(i)).equals(Piece.NOPIECE)) {
        lastCapture = i;
        continue;
      }
      // now we should have our two killers
      assertTrue(i == lastCapture + 1
                 && moves.get(i) == killer1
                 && moves.get(i + 1) == killer2);
      break;
    }
    System.out.println(moves);
    System.out.println(moveGenerator.hasLegalMove());

  }

  /**
   * Tests the generated moves from a standard board setup
   */
  @Test
  public void testOnDemand() {

    // 83 moves to make
    final String fen = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3";
    Position position = new Position(fen);

    MoveGenerator moveGenerator = new MoveGenerator(position);

//    { // test all moves
//
//      MoveList moves = moveGenerator.getPseudoLegalMoves().clone();
//      assertEquals(85, moves.size());
//
//      int j = 0; int move;
//
//      moveGenerator.setPosition(position);
//      while ((move = moveGenerator.getNextPseudoLegalMove(false)) != Move.NOMOVE) {
//        System.out.printf("%3d. %-20s %-20s %n",j, Move.toString(move), Move.toString(moves.get(j)));
//        j++;
//      }
//      System.out.println("OnDemand: " + j + " Bulk: " + moves.size());
//      System.out.println();
//      assertEquals(moves.size(), j);
//    }

    { // test all moves with killers sorted before non-captures
      int j = 0;
      int move;
      moveGenerator.setPosition(position);
      moveGenerator.setKillerMoves(new int[]{67320564, 67318516});
      moveGenerator.setPVMove(67248483);

      while ((move = moveGenerator.getNextPseudoLegalMove(false)) != Move.NOMOVE) {
        System.out.println((j++) + ". " + Move.toString(move));
      }
      MoveList moves = moveGenerator.getPseudoLegalMoves();
      System.out.println("OnDemand: " + j + " Bulk: " + moves.size());
      System.out.println();
      assertEquals(moves.size(), j);
    }

  }

  /**
   * Tests the generated moves from a standard board setup
   */
  @Test
  public void testOnDemandFromFenBoard() {

    MoveGenerator moveGenerator = new MoveGenerator();
    Position position;

    int i = 0;
    String[] fens = getFENs();
    while (fens[i] != null) {
      String testFen = fens[i];
      position = new Position(testFen);

      int j = 0;
      moveGenerator.setPosition(position);
      int move;
      while ((move = moveGenerator.getNextPseudoLegalMove(false)) != Move.NOMOVE) {
        System.out.println((j++) + ". " + Move.toString(move));
      }

      MoveList moves = moveGenerator.getPseudoLegalMoves().clone();

      System.out.println("OnDemand: " + j + " Bulk: " + moves.size());
      System.out.println();

      assertEquals(moves.size(), j);

      i++;
    }

  }

  @Test
  public void testMoveSortingCaptures() {

    MoveGenerator moveGenerator = new MoveGenerator();
    Position board;

    int i = 0;
    String[] fens = getFENs();
    while (fens[i] != null) {
      String testFen = fens[i++];
      System.out.println(testFen);
      board = new Position(testFen);

      moveGenerator.setPosition(board);
      MoveList moves = moveGenerator.getPseudoLegalMoves();

      moves.stream()
           .filter((move) -> Move.getTarget(move) != Piece.NOPIECE)
           .forEach((m) -> {
             System.out.print(Move.toString(m));
             System.out.print(" " + (Move.getPiece(m).getType().getValue() -
                                     Move.getTarget(m).getType().getValue()));
             System.out.println();
           });
      System.out.println();
    }
  }

  @Test
  public void testSingleMoveSorting() {


    Position position = new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3");
    MoveGenerator moveGenerator = new MoveGenerator(position);

    int killer1 = 67320564;
    int killer2 = 67318516;
    int pvMove = 67248483;

    // OD list
    MoveList movesOD = new MoveList();
    moveGenerator.setPosition(position);
    moveGenerator.setKillerMoves(new int[]{killer1, killer2});
    moveGenerator.setPVMove(pvMove);
    int m;
    while ((m = moveGenerator.getNextPseudoLegalMove(false)) != Move.NOMOVE) {
      movesOD.add(m);
    }

    // direct list
    moveGenerator.setPosition(position);
    moveGenerator.setKillerMoves(new int[]{killer1, killer2});
    moveGenerator.setPVMove(pvMove);
    MoveList moves = moveGenerator.getPseudoLegalMoves().clone();

    assertEquals(moves.size(), movesOD.size());

    assertEquals(pvMove, movesOD.getFirst());

    int lastCapture = 0;
    // we start with 1 as 0 could be noncapturing pvMove
    for (int i = 1; i < movesOD.size(); i++) {
      // skip the captures
      if (!Move.getTarget(movesOD.get(i)).equals(Piece.NOPIECE)) {
        lastCapture = i;
        continue;
      }
      // now we should have our two killers
      assertTrue(i == lastCapture + 1
                 && movesOD.get(i) == killer1
                 && movesOD.get(i + 1) == killer2
                 && movesOD.get(i + 2) == 247578640 // queen promotion
                );
      break;
    }

    for (int move : movesOD) {
      System.out.printf("%-20s %d (%d) %n",
                        Move.toString(move),
                        move,
                        Evaluation.getPositionValue(position, move));
    }

  }

  /**
   * Tests the timing
   */
  @Test
  @Disabled
  public void testTimingOnDemand() {

    int ITERATIONS = 0;
    int DURATION = 1;

    MoveGenerator moveGenerator = new MoveGenerator();
    Position board = null;
    Instant start = null;

    int i = 0;
    String[] fens = getFENs();
    while (fens[i] != null) {
      String testFen = fens[i];
      board = new Position(testFen);

      // Pseudo Legal Moves
      MoveList moves;
      start = Instant.now();
      while (true) {
        ITERATIONS++;
        moveGenerator.setPosition(board);
        moves = moveGenerator.getPseudoLegalMoves();
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) {
          break;
        }
      }
      //System.out.println(moves);
      System.out.println(
        String.format("   PseudoLegal: %,10d runs/s for %s (%,d)", ITERATIONS / DURATION, fens[i],
                      moves.size()));

      // Legal Moves On Demand
      ITERATIONS = 0;
      int moveCounter = 0;
      start = Instant.now();
      while (true) {
        ITERATIONS++;
        moveCounter = 0;
        moveGenerator.setPosition(board);
        int move = moveGenerator.getNextPseudoLegalMove(false);
        while (move != Move.NOMOVE) {
          moveCounter++;
          move = moveGenerator.getNextPseudoLegalMove(false);
        }
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) {
          break;
        }
      }
      System.out.println(
        String.format("OD PseudoLegal: %,10d runs/s for %s (%,d)", ITERATIONS / DURATION, fens[i],
                      moveCounter));

      // Legal Moves
      ITERATIONS = 0;
      moves = new MoveList();
      start = Instant.now();
      while (true) {
        ITERATIONS++;
        moveGenerator.setPosition(board);
        moves = moveGenerator.getLegalMoves();
        if (Duration.between(start, Instant.now()).getSeconds() >= DURATION) {
          break;
        }
        ;
      }
      System.out.println(
        String.format("         Legal: %,10d runs/s for %s (%,d)", ITERATIONS / DURATION, fens[i],
                      moves.size()));

      i++;
    }
  }

  String[] getFENs() {

    int i = 0;
    String[] fen = new String[200];
    fen[i++] = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113";
    fen[i++] = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1"; // 218 moves to make
    fen[i++] = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/6R1/pbp2PPP/1R4K1 b kq e3 0 113";
    fen[i++] = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/6R1/pbp2PPP/1R4K1 w kq - 0 113";
    fen[i++] = "8/1P6/6k1/8/8/8/p1K5/8 w - - 0 1";
    fen[i++] = "1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - - 0 1";
    fen[i++] = "4rk2/p5p1/1p2P2N/7R/nP5P/5PQ1/b6K/q7 w - - 0 1";
    fen[i++] = "4k2r/1q1p1pp1/p3p3/1pb1P3/2r3P1/P1N1P2p/1PP1Q2P/2R1R1K1 b k - 0 1";
    fen[i++] = "r2r1n2/pp2bk2/2p1p2p/3q4/3PN1QP/2P3R1/P4PP1/5RK1 w - - 0 1";
    fen[i++] = "1kr4r/ppp2bq1/4n3/4P1pp/1NP2p2/2PP2PP/5Q1K/4R2R w - - 0 1";
    fen[i++] = "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - 0 1";
    //        fen[i++]="8/5k2/8/8/2N2N2/2B5/2K5/8 w - - 0 1";
    //        fen[i++]="8/8/6k1/8/8/8/P1K5/8 w - - 0 1";
    //        fen[i++]="8/5k2/8/8/8/8/1BK5/1B6 w - - 0 1";
    //        fen[i++]="5r1k/4Qpq1/4p3/1p1p2P1/2p2P2/1p2P3/1K1P4/B7 w - - 0 1";
    //        fen[i++]="1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - -";
    //        fen[i++]="3r1k2/4npp1/1ppr3p/p6P/P2PPPP1/1NR5/5K2/2R5 w - -";
    //        fen[i++]="2q1rr1k/3bbnnp/p2p1pp1/2pPp3/PpP1P1P1/1P2BNNP/2BQ1PRK/7R b - -";
    //        fen[i++]="rnbqkb1r/p3pppp/1p6/2ppP3/3N4/2P5/PPP1QPPP/R1B1KB1R w KQkq -";
    //        fen[i++]="r1b2rk1/2q1b1pp/p2ppn2/1p6/3QP3/1BN1B3/PPP3PP/R4RK1 w - -";
    //        fen[i++]="2r3k1/pppR1pp1/4p3/4P1P1/5P2/1P4K1/P1P5/8 w - -";
    //        fen[i++]="1nk1r1r1/pp2n1pp/4p3/q2pPp1N/b1pP1P2/B1P2R2/2P1B1PP/R2Q2K1 w - -";
    //        fen[i++]="4b3/p3kp2/6p1/3pP2p/2pP1P2/4K1P1/P3N2P/8 w - -";
    //        fen[i++]="2kr1bnr/pbpq4/2n1pp2/3p3p/3P1P1B/2N2N1Q/PPP3PP/2KR1B1R w - -";
    //        fen[i++]="3rr1k1/pp3pp1/1qn2np1/8/3p4/PP1R1P2/2P1NQPP/R1B3K1 b - -";
    //        fen[i++]="2r1nrk1/p2q1ppp/bp1p4/n1pPp3/P1P1P3/2PBB1N1/4QPPP/R4RK1 w - -";
    //        fen[i++]="r3r1k1/ppqb1ppp/8/4p1NQ/8/2P5/PP3PPP/R3R1K1 b - -";
    //        fen[i++]="r2q1rk1/4bppp/p2p4/2pP4/3pP3/3Q4/PP1B1PPP/R3R1K1 w - -";
    //        fen[i++]="rnb2r1k/pp2p2p/2pp2p1/q2P1p2/8/1Pb2NP1/PB2PPBP/R2Q1RK1 w - -";
    //        fen[i++]="2r3k1/1p2q1pp/2b1pr2/p1pp4/6Q1/1P1PP1R1/P1PN2PP/5RK1 w - -";
    //        fen[i++]="r1bqkb1r/4npp1/p1p4p/1p1pP1B1/8/1B6/PPPN1PPP/R2Q1RK1 w kq -";
    //        fen[i++]="r2q1rk1/1ppnbppp/p2p1nb1/3Pp3/2P1P1P1/2N2N1P/PPB1QP2/R1B2RK1 b - -";
    //        fen[i++]="r1bq1rk1/pp2ppbp/2np2p1/2n5/P3PP2/N1P2N2/1PB3PP/R1B1QRK1 b - -";
    //        fen[i++]="3rr3/2pq2pk/p2p1pnp/8/2QBPP2/1P6/P5PP/4RRK1 b - -";
    //        fen[i++]="r4k2/pb2bp1r/1p1qp2p/3pNp2/3P1P2/2N3P1/PPP1Q2P/2KRR3 w - -";
    //        fen[i++]="3rn2k/ppb2rpp/2ppqp2/5N2/2P1P3/1P5Q/PB3PPP/3RR1K1 w - -";
    //        fen[i++]="2r2rk1/1bqnbpp1/1p1ppn1p/pP6/N1P1P3/P2B1N1P/1B2QPP1/R2R2K1 b - -";
    //        fen[i++]="r1bqk2r/pp2bppp/2p5/3pP3/P2Q1P2/2N1B3/1PP3PP/R4RK1 b kq -";
    //        fen[i++]="r2qnrnk/p2b2b1/1p1p2pp/2pPpp2/1PP1P3/PRNBB3/3QNPPP/5RK1 w - -";
    //        fen[i++]="r3qb1k/1b4p1/p2pr2p/3n4/Pnp1N1N1/6RP/1B3PP1/1B1QR1K1 w - - 0 1";
    //        // repeat the first
    //        fen[i++]="r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/6R1/pbp2PPP/1R4K1 b kq e3 0 113";
    return fen;

  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    MoveList m1 = new MoveList();
    MoveList m2 = new MoveList();
    final Position position =
      new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");

    int ROUNDS = 5;
    int ITERATIONS = 25;
    int REPETITIONS = 20000;

    for (int round=0; round<ROUNDS; round++) {
      long start=0, end=0, sum=0;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);
      System.gc();

      int i = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          m1 = test1(position);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float)sum/ITERATIONS) / 1e9f;

      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          m2 = test2(position);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float)sum/ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 1 avg: %,.3f sec", round, avg1));
      result.add(String.format("Round %d Test 2 avg: %,.3f sec", round, avg2));
    }

    System.out.println();
    System.out.printf("%-20s %-20s %n", "Test 1", "Test 2");
    for (int i = 0; i < m1.size(); i++) {
      System.out.printf("%-20s %-20s %n", Move.toString(m1.get(i)), Move.toString(m2.get(i)));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }

  }

  private MoveList test1(final Position position) {

    final MoveGenerator mG = new MoveGenerator(position);
    mG.SORT_MOVES = false;
    mG.SORT_CAPTURING_MOVES = true;

    final int killer1 = 67320564;
    final int killer2 = 67318516;
    mG.setKillerMoves(new int[]{killer1, killer2});

    return mG.getPseudoLegalMoves();

  }

  private MoveList test2(final Position position) {

    final MoveGenerator mG = new MoveGenerator(position);
    mG.SORT_MOVES = true;
    mG.SORT_CAPTURING_MOVES = true;

    final int killer1 = 67320564;
    final int killer2 = 67318516;
    mG.setKillerMoves(new int[]{killer1, killer2});

    return mG.getPseudoLegalMoves();
  }

  @Test
  @Disabled
  public void showSize() {
    System.out.println(VM.current().details());
    System.out.println(ClassLayout.parseClass(MoveGenerator.class).toPrintable());
  }

}
