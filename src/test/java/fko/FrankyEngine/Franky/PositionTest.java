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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** @author fkopp */
public class PositionTest {

  private static final Logger LOG = LoggerFactory.getLogger(PositionTest.class);

  private static final int ITERATIONS = 0;

  /** Test insufficient material */
  @Test
  public void testInsufficientMaterial() {

    String fen;
    Position position;

    // KK
    fen = "8/3k4/8/8/8/8/4K3/8 w - -";
    position = new Position(fen);
    assertTrue(position.checkInsufficientMaterial());

    // KQK
    fen = "8/3k4/8/8/8/8/4KQ2/8 w - -";
    position = new Position(fen);
    assertFalse(position.checkInsufficientMaterial());

    // KNK
    fen = "8/3k4/8/8/8/8/4KN2/8 w - -";
    position = new Position(fen);
    assertTrue(position.checkInsufficientMaterial());

    // KNNK
    fen = "8/3k4/8/8/8/8/4KNN1/8 w - -";
    position = new Position(fen);
    assertTrue(position.checkInsufficientMaterial());

    // KKN
    fen = "8/2nk4/8/8/8/8/4K3/8 w - -";
    position = new Position(fen);
    assertTrue(position.checkInsufficientMaterial());

    // KNNK
    fen = "8/1nnk4/8/8/8/8/4K3/8 w - -";
    position = new Position(fen);
    assertTrue(position.checkInsufficientMaterial());

    // KBKB - B same field color
    fen = "8/3k1b2/8/8/8/8/4K1B1/8 w - -";
    position = new Position(fen);
    assertTrue(position.checkInsufficientMaterial());

    // KBKB - B different field color
    fen = "8/3k2b1/8/8/8/8/4K1B1/8 w - -";
    position = new Position(fen);
    assertFalse(position.checkInsufficientMaterial());
  }

  @Test
  public void test3RepetitionsSimple() {
    Position position = new Position();

    position.makeMove(Move.fromSANNotation(position, "e4"));
    position.makeMove(Move.fromSANNotation(position, "e5"));
    System.out.println("Repetitions: " + position.countRepetitions());
    assertEquals(0, position.countRepetitions());

    // Simple repetition
    // !EnPassant - takes 3 loops to get to repetition
    for (int i = 0; i <= 2; i++) {
      position.makeMove(Move.fromSANNotation(position, "Nf3"));
      position.makeMove(Move.fromSANNotation(position, "Nc6"));
      position.makeMove(Move.fromSANNotation(position, "Ng1"));
      position.makeMove(Move.fromSANNotation(position, "Nb8"));
      System.out.println("Repetitions: " + position.countRepetitions());
    }

    System.out.println("3-Repetitions: " + position.countRepetitions());
    assertTrue(position.checkRepetitions(2));
  }

  @Test
  public void test3RepetitionsAdvanced() {
    Position position = new Position("6k1/p3q2p/1n1Q2pB/8/5P2/6P1/PP5P/3R2K1 b - -");

    position.makeMove(Move.fromSANNotation(position, "Qe3"));
    position.makeMove(Move.fromSANNotation(position, "Kg2"));
    System.out.println("Repetitions: " + position.countRepetitions());
    assertEquals(0, position.countRepetitions());

    // takes 2 loops to get to repetition
    for (int i = 0; i < 2; i++) {
      position.makeMove(Move.fromSANNotation(position, "Qe2"));
      position.makeMove(Move.fromSANNotation(position, "Kg1"));
      position.makeMove(Move.fromSANNotation(position, "Qe3"));
      position.makeMove(Move.fromSANNotation(position, "Kg2"));
      System.out.println("Repetitions: " + position.countRepetitions());
    }

    System.out.println("3-Repetitions: " + position.countRepetitions());
    assertTrue(position.checkRepetitions(2));
  }

  @Test
  public void test3RepetitionsCanceled() {
    Position position = new Position("6k1/p3q2p/1n1Q2pB/8/5P2/6P1/PP5P/3R2K1 b - -");

    position.makeMove(Move.fromSANNotation(position, "Qe3"));
    position.makeMove(Move.fromSANNotation(position, "Kg2"));
    System.out.println("Repetitions: " + position.countRepetitions());
    assertEquals(0, position.countRepetitions());

    // takes 2 loops to get to repetition
    for (int i = 0; i < 2; i++) {
      position.makeMove(Move.fromSANNotation(position, "Qe2"));
      position.makeMove(Move.fromSANNotation(position, "Kg1"));
      position.makeMove(Move.fromSANNotation(position, "Qe3"));
      position.makeMove(Move.fromSANNotation(position, "Kg2"));
      System.out.println("Repetitions: " + position.countRepetitions());
    }

    System.out.println("3-Repetitions: " + position.countRepetitions());
    assertTrue(position.checkRepetitions(2));

    position.makeMove(Move.fromSANNotation(position, "a5"));
    position.makeMove(Move.fromSANNotation(position, "Kf1"));
    position.makeMove(Move.fromSANNotation(position, "Qf3"));
    position.makeMove(Move.fromSANNotation(position, "Kg1"));
    position.makeMove(Move.fromSANNotation(position, "Qe3"));
    position.makeMove(Move.fromSANNotation(position, "Kg2"));
    System.out.println("Repetitions: " + position.countRepetitions());

    // takes 2 loops to get to repetition
    for (int i = 0; i < 2; i++) {
      position.makeMove(Move.fromSANNotation(position, "Qe2"));
      position.makeMove(Move.fromSANNotation(position, "Kg1"));
      position.makeMove(Move.fromSANNotation(position, "Qe3"));
      position.makeMove(Move.fromSANNotation(position, "Kg2"));
      System.out.println("Repetitions: " + position.countRepetitions());
    }

    System.out.println("3-Repetitions: " + position.countRepetitions());
    assertTrue(position.checkRepetitions(2));

  }

  /** Test Null Move */
  @Test
  public void testNullMove() {
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    Position position = new Position(fen);

    String f1 = position.toFENString();
    position.makeNullMove();
    String f1null = position.toFENString();
    position.undoNullMove();
    String f2 = position.toFENString();

    System.out.println(String.format("f1    : %s", f1));
    System.out.println(String.format("f1null: %s", f1null));
    System.out.println(String.format("f2    : %s", f2));

    assertEquals(f1, f2);
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq - 1 114", f1null);
  }

  /** Test Null Move */
  @Test
  public void testNullMoveEnPassant() {
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    Position position = new Position(fen);

    String f1 = position.toFENString();
    long zobrist1 = position.getZobristKey();
    position.makeNullMove();
    String f1null = position.toFENString();
    long zobristNull = position.getZobristKey();
    position.undoNullMove();
    String f2 = position.toFENString();
    long zobrist2 = position.getZobristKey();

    System.out.println(String.format("f1    : %-65s  zobrist1   : %d ", f1, zobrist1));
    System.out.println(String.format("f1null: %-65s  zobristNull: %d ", f1null, zobristNull));
    System.out.println(String.format("f2    : %-65s  zobrist2   : %d ", f2, zobrist2));

    assertEquals(f1, f2);
    assertEquals(zobrist1, zobrist2);
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq - 1 114", f1null);
  }

  /** Test Null Move MoveGeneration */
  @Test
  public void testNullMove_moveGen() {
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    Position position = new Position(fen);

    MoveGenerator omg = new MoveGenerator(position);
    MoveList moves = omg.getLegalMoves();
    assertEquals(81, moves.size());

    position.makeNullMove();

    omg.setPosition(position);
    moves = omg.getLegalMoves();
    assertEquals(26, moves.size());

    position.undoNullMove();

    omg.setPosition(position);
    moves = omg.getLegalMoves();
    assertEquals(81, moves.size());
  }

  @Test
  public void testMoveOnBoard() {
    Position position;

    String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";

    position = new Position(testFen);
    position.makeMove(Move.fromUCINotation(position, "c4a4"));
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/q3Pp2/6R1/p1p2PPP/1R4K1 w kq - 1 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // normal pawn move
    position.makeMove(Move.fromUCINotation(position, "b7b6"));
    assertEquals("r3k2r/2pn3p/1pq1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq - 0 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // normal capture
    position.makeMove(Move.fromUCINotation(position, "c4e4"));
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/4qp2/6R1/p1p2PPP/1R4K1 w kq - 0 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // pawn double
    position.makeMove(Move.fromUCINotation(position, "b7b5"));
    assertEquals("r3k2r/2pn3p/2q1q1n1/1p6/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq b6 0 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // castling
    position.makeMove(Move.fromUCINotation(position, "e8g8"));
    assertEquals("r4rk1/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w - - 1 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // promotion
    position.makeMove(Move.fromUCINotation(position, "a2a1q"));
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/2p2PPP/qR4K1 w kq - 0 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // promotion capture
    position.makeMove(Move.fromUCINotation(position, "a2b1r"));
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/2p2PPP/1r4K1 w kq - 0 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());

    // en passant
    position.makeMove(Move.fromUCINotation(position, "f4e3"));
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q5/4p1R1/p1p2PPP/1R4K1 w kq - 0 114",
                 position.toFENString());
    position.undoMove();
    assertEquals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113",
                 position.toFENString());
  }

  /** Some timings to find fastest code - so nfr test */
  @Test
  @Disabled
  public void testTimings() {

    Piece[] x88Board = new Piece[129];

    // fill array
    System.out.println("x88Board fill with value 1. Arrays.fill 2. for loop");
    Instant start = Instant.now();
    for (int i = 0; i < ITERATIONS; i++) Arrays.fill(x88Board, Piece.NOPIECE);
    Instant end = Instant.now();
    System.out.println(Duration.between(start, end));
    start = Instant.now();
    // clear board
    for (int i = 0; i < ITERATIONS; i++) {
      for (Square s : Square.getValueList()) {
        x88Board[s.ordinal()] = Piece.NOPIECE;
      }
    }
    end = Instant.now();
    System.out.println(Duration.between(start, end));

    // copy array
    System.out.println("Copy x88Board - 1. System.arraycopy 2. Arrays.copyof");
    x88Board = new Piece[128];
    Piece[] _x88Board2 = new Piece[128];
    start = Instant.now();
    // clear board
    for (int i = 0; i < ITERATIONS; i++) {
      System.arraycopy(x88Board, 0, _x88Board2, 0, x88Board.length);
    }
    end = Instant.now();
    System.out.println(Duration.between(start, end));
    start = Instant.now();
    // clear board
    for (int i = 0; i < ITERATIONS; i++) _x88Board2 = Arrays.copyOf(x88Board, x88Board.length);
    end = Instant.now();
    System.out.println(Duration.between(start, end));

    System.out.println("Position creation and Copy Contructor of Position");
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq e4 0 2";
    Position obp = null;
    start = Instant.now();
    for (int i = 0; i < ITERATIONS; i++) obp = new Position(fen);
    end = Instant.now();
    System.out.println(Duration.between(start, end));
    @SuppressWarnings("unused") Position obp_copy = null;
    start = Instant.now();
    for (int i = 0; i < ITERATIONS; i++) obp_copy = new Position(obp);
    end = Instant.now();
    System.out.println(Duration.between(start, end));

  }

  @Test
  public void testFromNotation() {
    Square os = Square.fromUCINotation("e2");
    assertEquals(Square.e2, os);
  }

  @Test
  public void testContructorFromFEN() {

    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";

    /*
       ---------------------------------
    8: |bR |   |   |   |bK |   |   |bR |
       ---------------------------------
    7: |   |bP |bP |bN |   |   |   |bP |
       ---------------------------------
    6: |   |   |bQ |   |bQ |   |bN |   |
       ---------------------------------
    5: |   |   |   |   |   |   |   |   |
       ---------------------------------
    4: |   |   |bQ |   |wP |bP |   |   |
       ---------------------------------
    3: |   |   |   |   |   |   |wR |   |
       ---------------------------------
    2: |bP |   |bP |   |   |wP |wP |wP |
       ---------------------------------
    1: |   |wR |   |   |   |   |wK |   |
       ---------------------------------
         A   B   C   D   E   F   G   H

    black, ep on e4, O-O & O-O-O for black
    */

    Position obp = new Position(fen);
    System.out.println(fen);
    System.out.println(obp.toFENString());
    assertEquals(fen, obp.toFENString());

    // Test invalid en passant square
    assertThrows(IllegalArgumentException.class, () -> {
      //do whatever you want to do here
      String fen2 = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq k9 0 113";
      Position obp2 = new Position(fen2);
    });
  }

  /** */
  @Test
  public void testCopyContructor() {
    String fen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq e3 0 2";
    Position position = new Position(fen);
    Position positionCopy = new Position(position);
    assertEquals(position, positionCopy);
    assertEquals(position.toFENString(), positionCopy.toFENString());
    assertEquals(position.getZobristKey(), positionCopy.getZobristKey());
  }

  /** Test Zobrist Key generation */
  @Test
  public void testZobrist() {

    String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    long initialZobrist = 0;
    long zobrist = 0;

    Position omegaBoard = new Position(testFen);

    int testMove =
      Move.createMove(MoveType.NORMAL, Square.b7, Square.b6, Piece.BLACK_PAWN, Piece.NOPIECE,
                      Piece.NOPIECE);

    System.out.println("Test if zobrist after move/undo are equal.");
    initialZobrist = omegaBoard.getZobristKey();
    System.out.println(initialZobrist);
    omegaBoard.makeMove(testMove);
    zobrist = omegaBoard.getZobristKey();
    System.out.println(zobrist);
    omegaBoard.undoMove();
    zobrist = omegaBoard.getZobristKey();
    System.out.println(zobrist);
    assertEquals(zobrist, initialZobrist);

    // test if zobrist key is identical if one board comes to the same position
    // as a newly created one
    String fenAfterMove = "r3k2r/2pn3p/1pq1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w kq - 0 114";
    omegaBoard.makeMove(testMove);
    Position omegaBoard2 = new Position(fenAfterMove);
    System.out.println(omegaBoard.getZobristKey() + " " + omegaBoard.toFENString());
    System.out.println(omegaBoard2.getZobristKey() + " " + omegaBoard2.toFENString());
    assertEquals(omegaBoard.toFENString(), omegaBoard2.toFENString());
    assertEquals(omegaBoard.getZobristKey(), omegaBoard2.getZobristKey());
    assertEquals(omegaBoard, omegaBoard2);
  }

  @Test
  public void testIsAttacked() {
    String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3";
    Position position = new Position(testFen);

    System.out.println(position);

    // pawns
    assertTrue(position.isAttacked(Color.WHITE, Square.g3));
    assertTrue(position.isAttacked(Color.WHITE, Square.e3));
    assertTrue(position.isAttacked(Color.BLACK, Square.b1));
    assertTrue(position.isAttacked(Color.BLACK, Square.e4));
    assertTrue(position.isAttacked(Color.BLACK, Square.e3));

    // knight
    assertTrue(position.isAttacked(Color.BLACK, Square.e5));
    assertTrue(position.isAttacked(Color.BLACK, Square.f4));
    assertFalse(position.isAttacked(Color.BLACK, Square.g1));

    // sliding
    assertTrue(position.isAttacked(Color.WHITE, Square.g6));
    assertTrue(position.isAttacked(Color.BLACK, Square.a5));

    testFen = "rnbqkbnr/1ppppppp/8/p7/Q1P5/8/PP1PPPPP/RNB1KBNR b KQkq - 1 2";
    position = new Position(testFen);

    // king
    System.out.println(position);
    assertTrue(position.isAttacked(Color.WHITE, Square.d1));

    System.out.println(position);
    assertFalse(position.isAttacked(Color.BLACK, Square.e1));

    // rook
    System.out.println(position);
    assertTrue(position.isAttacked(Color.BLACK, Square.a5));

    System.out.println(position);
    assertFalse(position.isAttacked(Color.BLACK, Square.a4));

    // queen
    System.out.println(position);
    assertFalse(position.isAttacked(Color.WHITE, Square.e8));

    System.out.println(position);
    assertTrue(position.isAttacked(Color.WHITE, Square.d7));

    System.out.println(position);
    assertFalse(position.isAttacked(Color.WHITE, Square.e8));

    // bug tests
    testFen = "r1bqk1nr/pppp1ppp/2nb4/1B2B3/3pP3/8/PPP2PPP/RN1QK1NR b KQkq -";
    position = new Position(testFen);
    System.out.println(position);
    assertFalse(position.isAttacked(Color.WHITE, Square.e8));
    assertFalse(position.isAttacked(Color.BLACK, Square.e1));

    testFen = "rnbqkbnr/ppp1pppp/8/1B6/3Pp3/8/PPP2PPP/RNBQK1NR b KQkq -";
    position = new Position(testFen);
    System.out.println(position);
    assertTrue(position.isAttacked(Color.WHITE, Square.e8));
    assertFalse(position.isAttacked(Color.BLACK, Square.e1));

  }

  @Test
  void givesCheckTest() {
    Position position;
    int move;

    // DIRECT CHECKS

    // Pawns
    position = new Position("4r3/1pn3k1/4p1b1/p1Pp1P1r/3P2NR/1P3B2/3K2P1/4R3 w - -");
    move = Move.fromUCINotation(position, "f5f6");
    assertTrue(position.givesCheck(move));

    position = new Position("5k2/4pp2/1N2n1p1/r3P2p/P5PP/2rR1K2/P7/3R4 b - -");
    move = Move.fromUCINotation(position, "h5g4");
    assertTrue(position.givesCheck(move));

    // Knights
    position = new Position("5k2/4pp2/1N2n1p1/r3P2p/P5PP/2rR1K2/P7/3R4 w - -");
    move = Move.fromUCINotation(position, "b6d7");
    assertTrue(position.givesCheck(move));

    position = new Position("5k2/4pp2/1N2n1p1/r3P2p/P5PP/2rR1K2/P7/3R4 b - -");
    move = Move.fromUCINotation(position, "e6d4");
    assertTrue(position.givesCheck(move));

    // Rooks
    position = new Position("5k2/4pp2/1N2n1pp/r3P3/P5PP/2rR4/P3K3/3R4 w - -");
    move = Move.fromUCINotation(position, "d3d8");
    assertTrue(position.givesCheck(move));

    position = new Position("5k2/4pp2/1N2n1pp/r3P3/P5PP/2rR4/P3K3/3R4 b - -");
    move = Move.fromUCINotation(position, "c3c2");
    assertTrue(position.givesCheck(move));

    // blocked opponent piece - no check
    position = new Position("5k2/4pp2/1N2n1pp/r3P3/P5PP/2rR4/P2RK3/8 b - -");
    move = Move.fromUCINotation(position, "c3c2");
    assertFalse(position.givesCheck(move));
    // blocked own piece - no check
    position = new Position("5k2/4pp2/1N2n1pp/r3P3/P5PP/2rR4/P2nK3/3R4 b - -");
    move = Move.fromUCINotation(position, "c3c2");
    assertFalse(position.givesCheck(move));

    // Bishop
    position = new Position("6k1/3q2b1/p1rrnpp1/P3p3/2B1P3/1p1R3Q/1P4PP/1B1R3K w - -");
    move = Move.fromUCINotation(position, "c4e6");
    assertTrue(position.givesCheck(move));

    // Queen
    position = new Position("5k2/4pp2/1N2n1pp/r3P3/P5PP/2qR4/P3K3/3R4 b - -");
    move = Move.fromUCINotation(position, "c3c2");
    assertTrue(position.givesCheck(move));

    position = new Position("6k1/3q2b1/p1rrnpp1/P3p3/2B1P3/1p1R3Q/1P4PP/1B1R3K w - -");
    move = Move.fromUCINotation(position, "h3e6");
    assertTrue(position.givesCheck(move));

    position = new Position("6k1/p3q2p/1n1Q2pB/8/5P2/6P1/PP5P/3R2K1 b - -");
    move = Move.fromUCINotation(position, "e7e3");
    assertTrue(position.givesCheck(move));

    // no check
    position = new Position("6k1/p3q2p/1n1Q2pB/8/5P2/6P1/PP5P/3R2K1 b - -");
    move = Move.fromUCINotation(position, "e7e4");
    assertFalse(position.givesCheck(move));

    // promotion
    position = new Position("1k3r2/1p1bP3/2p2p1Q/Ppb5/4Rp1P/2q2N1P/5PB1/6K1 w - -");
    move = Move.fromUCINotation(position, "e7f8q");
    assertTrue(position.givesCheck(move));

    position = new Position("1r3r2/1p1bP2k/2p2n2/p1Pp4/P2N1PpP/1R2p3/1P2P1BP/3R2K1 w - -");
    move = Move.fromUCINotation(position, "e7f8n");
    assertTrue(position.givesCheck(move));

    // Castling checks
    position = new Position("r4k1r/8/8/8/8/8/8/R3K2R w KQ -");
    move = Move.fromUCINotation(position, "e1g1");
    assertTrue(position.givesCheck(move));

    position = new Position("r2k3r/8/8/8/8/8/8/R3K2R w KQ -");
    move = Move.fromUCINotation(position, "e1c1");
    assertTrue(position.givesCheck(move));

    position = new Position("r3k2r/8/8/8/8/8/8/R4K1R b kq -");
    move = Move.fromUCINotation(position, "e8g8");
    assertTrue(position.givesCheck(move));

    position = new Position("r3k2r/8/8/8/8/8/8/R2K3R b kq -");
    move = Move.fromUCINotation(position, "e8c8");
    assertTrue(position.givesCheck(move));

    position = new Position("r6r/8/8/8/8/8/8/2k1K2R w K -");
    move = Move.fromUCINotation(position, "e1g1");
    assertTrue(position.givesCheck(move));

    // en passant checks
    position = new Position("8/3r1pk1/p1R2p2/1p5p/r2Pp3/PRP3P1/4KP1P/8 b - d3");
    move = Move.fromUCINotation(position, "e4d3");
    assertTrue(position.givesCheck(move));

    // REVEALED CHECKS
    position = new Position("6k1/8/3P1bp1/2BNp3/8/1Q3P1q/7r/1K2R3 w - -");
    move = Move.fromUCINotation(position, "d5e7");
    assertTrue(position.givesCheck(move));

    position = new Position("6k1/8/3P1bp1/2BNp3/8/1Q3P1q/7r/1K2R3 w - -");
    move = Move.fromUCINotation(position, "d5c7");
    assertTrue(position.givesCheck(move));

    position = new Position("6k1/8/3P1bp1/2BNp3/8/1B3P1q/7r/1K2R3 w - -");
    move = Move.fromUCINotation(position, "d5c7");
    assertTrue(position.givesCheck(move));

    position = new Position("6k1/8/3P1bp1/2BNp3/8/1Q3P1q/7r/1K2R3 w - -");
    move = Move.fromUCINotation(position, "d5e7");
    assertTrue(position.givesCheck(move));

    position = new Position("1Q1N2k1/8/3P1bp1/2B1p3/8/5P1q/7r/1K2R3 w - -");
    move = Move.fromUCINotation(position, "d8e6");
    assertTrue(position.givesCheck(move));

    position = new Position("1R1N2k1/8/3P1bp1/2B1p3/8/5P1q/7r/1K2R3 w - -");
    move = Move.fromUCINotation(position, "d8e6");
    assertTrue(position.givesCheck(move));

    // revealed by en passant capture
    position = new Position("8/b2r1pk1/p1R2p2/1p5p/r2Pp3/PRP3P1/5K1P/8 b - d3");
    move = Move.fromUCINotation(position, "e4d3");
    assertTrue(position.givesCheck(move));

    // test where we had bugs
    position = new Position("2r1r3/pb1n1kpn/1p1qp3/6p1/2PP4/8/P2Q1PPP/3R1RK1 w - -");
    move = Move.fromUCINotation(position, "f2f4");
    assertFalse(position.givesCheck(move));
    position = new Position("2r1r1k1/pb3pp1/1p1qpn2/4n1p1/2PP4/6KP/P2Q1PP1/3RR3 b - -");
    move = Move.fromUCINotation(position, "e5d3");
    assertTrue(position.givesCheck(move));
    position = new Position("R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q1NNQQ2/1p6/qk3KB1 b - -");
    move = Move.fromUCINotation(position, "b1c2");
    assertTrue(position.givesCheck(move));
    position = new Position("8/8/8/8/8/5K2/R7/7k w - -");
    move = Move.fromUCINotation(position, "a2h2");
    assertTrue(position.givesCheck(move));
    position = new Position("r1bqkb1r/ppp1pppp/2n2n2/1B1P4/8/8/PPPP1PPP/RNBQK1NR w KQkq -");
    move = Move.fromUCINotation(position, "d5c6");
    assertFalse(position.givesCheck(move));

  }

  @Test
  void getGamePhaseFactor() {
    Position position = new Position();
    assertEquals(24, position.getGamePhaseValue());

    position = new Position("r6k/6R1/p4p1p/2p2P1P/1pq1PN2/6P1/1PP5/2KR4 w - - 0 1");
    assertEquals(11, position.getGamePhaseValue());

    position = new Position("k6n/7p/6P1/7K/8/8/8/8 w - - 0 1");
    assertEquals(1, position.getGamePhaseValue());

    // too many officers
    position = new Position("R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1");
    assertEquals(24, position.getGamePhaseValue());
  }

  @Test
  void bitBoardStandardPosition() {
    Position pos = new Position();
    for (Color c : Color.values) {
      for (PieceType pt : PieceType.values) {
        final long bitboard = pos.getPiecesBitboards(c, pt);
        //        System.out.printf("%s %s %d %n", c.name(), pt.name(), bitboard);
        //        System.out.println(Bitboard.toString(bitboard));
        if (c == Color.WHITE && pt == PieceType.PAWN) assertEquals(71776119061217280L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.KNIGHT)
          assertEquals(4755801206503243776L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.BISHOP)
          assertEquals(2594073385365405696L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.ROOK)
          assertEquals(-9151314442816847872L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.QUEEN)
          assertEquals(576460752303423488L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.KING)
          assertEquals(1152921504606846976L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.PAWN) assertEquals(65280L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.KNIGHT) assertEquals(66L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.BISHOP) assertEquals(36L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.ROOK) assertEquals(129L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.QUEEN) assertEquals(8L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.KING) assertEquals(16L, bitboard);
      }
    }
  }

  @Test
  void bitBoardPosition() {
    Position pos = new Position();
    pos.makeMove(Move.fromUCINotation(pos, "e2e4"));
    pos.makeMove(Move.fromUCINotation(pos, "d7d5"));
    pos.makeMove(Move.fromUCINotation(pos, "e4d5"));
    pos.makeMove(Move.fromUCINotation(pos, "d8d5"));
    pos.makeMove(Move.fromUCINotation(pos, "b1c3"));
    pos.makeMove(Move.fromUCINotation(pos, "d5e5"));
    pos.makeMove(Move.fromUCINotation(pos, "f1e2"));
    pos.makeMove(Move.fromUCINotation(pos, "e8d8"));
    pos.makeMove(Move.fromUCINotation(pos, "g1f3"));
    pos.makeMove(Move.fromUCINotation(pos, "e5g5"));
    pos.makeMove(Move.fromUCINotation(pos, "e1g1"));
    pos.makeMove(Move.fromUCINotation(pos, "b8c6"));

    for (Color c : Color.values) {
      for (PieceType pt : PieceType.values) {
        final long bitboard = pos.getPiecesBitboards(c, pt);
        //        System.out.printf("%s %s %d %n", c.name(), pt.name(), bitboard);
        //        System.out.println(Bitboard.toString(bitboard));
        if (c == Color.WHITE && pt == PieceType.PAWN) assertEquals(67272519433846784L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.KNIGHT)
          assertEquals(39582418599936L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.BISHOP)
          assertEquals(292733975779082240L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.ROOK)
          assertEquals(2377900603251621888L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.QUEEN)
          assertEquals(576460752303423488L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.KING)
          assertEquals(4611686018427387904L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.PAWN) assertEquals(63232L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.KNIGHT) assertEquals(262208L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.BISHOP) assertEquals(36L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.ROOK) assertEquals(129L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.QUEEN) assertEquals(1073741824L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.KING) assertEquals(8L, bitboard);
      }
    }

    Position posCopy = new Position(pos);
    for (Color c : Color.values) {
      for (PieceType pt : PieceType.values) {
        final long bitboard = posCopy.getPiecesBitboards(c, pt);
        //        System.out.printf("%s %s %d %n", c.name(), pt.name(), bitboard);
        //        System.out.println(Bitboard.toString(bitboard));
        if (c == Color.WHITE && pt == PieceType.PAWN) assertEquals(67272519433846784L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.KNIGHT)
          assertEquals(39582418599936L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.BISHOP)
          assertEquals(292733975779082240L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.ROOK)
          assertEquals(2377900603251621888L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.QUEEN)
          assertEquals(576460752303423488L, bitboard);
        else if (c == Color.WHITE && pt == PieceType.KING)
          assertEquals(4611686018427387904L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.PAWN) assertEquals(63232L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.KNIGHT) assertEquals(262208L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.BISHOP) assertEquals(36L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.ROOK) assertEquals(129L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.QUEEN) assertEquals(1073741824L, bitboard);
        else if (c == Color.BLACK && pt == PieceType.KING) assertEquals(8L, bitboard);
      }
    }
  }

  @Test
  void bitBoardCombinations() {
    Position pos = new Position();
    final long white = pos.getOccupiedBitboards(Color.WHITE);
    final long black = pos.getOccupiedBitboards(Color.BLACK);
    //    System.out.printf("%s %d%n%s%n", Color.WHITE, white, Bitboard.toString(white));
    //    System.out.printf("%s %d%n%S%n", Color.BLACK, black, Bitboard.toString(black));
    assertEquals(-281474976710656L, white);
    assertEquals(65535L, black);
    long occupiedSquares = (white | black);
    //    System.out.println("All occupoied squares: "+occupiedSquares);
    //    System.out.println(Bitboard.toString(occupiedSquares));
    assertEquals(-281474976645121L, occupiedSquares);
    long emptySquares = ~occupiedSquares;
    //    System.out.println("All empty squares: "+emptySquares);
    //    System.out.println(Bitboard.toString(emptySquares));
    assertEquals(281474976645120L, emptySquares);
  }

  @Test
  void bitBoardCombinations2() {
    Position pos = new Position();
    pos.makeMove(Move.fromUCINotation(pos, "e2e4"));
    pos.makeMove(Move.fromUCINotation(pos, "d7d5"));
    pos.makeMove(Move.fromUCINotation(pos, "e4d5"));
    pos.makeMove(Move.fromUCINotation(pos, "d8d5"));
    pos.makeMove(Move.fromUCINotation(pos, "b1c3"));
    pos.makeMove(Move.fromUCINotation(pos, "d5e5"));
    pos.makeMove(Move.fromUCINotation(pos, "f1e2"));
    pos.makeMove(Move.fromUCINotation(pos, "e8d8"));
    pos.makeMove(Move.fromUCINotation(pos, "g1f3"));
    pos.makeMove(Move.fromUCINotation(pos, "e5g5"));
    pos.makeMove(Move.fromUCINotation(pos, "e1g1"));
    pos.makeMove(Move.fromUCINotation(pos, "b8c6"));
    final long white = pos.getOccupiedBitboards(Color.WHITE);
    final long black = pos.getOccupiedBitboards(Color.BLACK);
    System.out.printf("%s %d%n%s%n", Color.WHITE, white, Bitboard.toString(white));
    System.out.printf("%s %d%n%S%n", Color.BLACK, black, Bitboard.toString(black));
    long occupiedSquares = (white | black);
    System.out.println("All occupoied squares: " + occupiedSquares);
    System.out.println(Bitboard.toString(occupiedSquares));
    long emptySquares = ~occupiedSquares;
    System.out.println("All empty squares: " + emptySquares);
    System.out.println(Bitboard.toString(emptySquares));
  }

  @Test
  void bitBoardsCalculations() {
    String testFen = "rnbqkbnr/1ppppppp/8/p7/2P1Q3/8/PP1PPPPP/RNB1KBNR b KQkq - 1 2";
    Position position = new Position(testFen);
    System.out.println(position);
    assertFalse(position.isAttacked(Color.WHITE, Square.e8));

    System.out.println(Bitboard.toString(
      (position.getPiecesBitboards(Color.WHITE, PieceType.ROOK) | position.getPiecesBitboards(
        Color.WHITE, PieceType.QUEEN)) & (Square.e8.getFile().bitBoard
        | Square.e8.getRank().bitBoard)));

    System.out.println(
      ((position.getPiecesBitboards(Color.WHITE, PieceType.ROOK) | position.getPiecesBitboards(
        Color.WHITE, PieceType.QUEEN)) & (Square.e8.getFile().bitBoard
        | Square.e8.getRank().bitBoard)) > 0);
  }

  @Test
  void rotatedBitboardsTest() { // @formatter:off

    Position position;
    String actual, expected;

    position = new Position();
    actual = Bitboard.toString(position.getAllOccupiedBitboardR90());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardL90());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardR45());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 1 0 1 1 0 0 \n"
              + "1 1 0 0 0 1 1 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 1 1 1 1 0 0 \n"
              + "0 0 1 1 1 1 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 1 1 0 0 0 1 1 \n"
              + "0 0 1 1 0 1 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardL45());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 1 1 1 0 1 1 \n"
              + "0 0 1 1 0 0 0 1 \n"
              + "1 0 0 0 0 1 1 0 \n"
              + "0 0 0 0 1 1 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 1 1 0 0 0 0 1 \n"
              + "1 0 0 0 1 1 0 0 \n"
              + "1 1 0 1 1 1 1 1";
    assertEquals(expected, actual);

    position.makeMove(Move.fromUCINotation(position, "e2e4"));
    actual = Bitboard.toString(position.getAllOccupiedBitboardR90());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 0 0 1 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardL90());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 1 0 0 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardR45());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 1 0 1 1 0 0 \n"
              + "1 1 0 0 0 1 1 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 1 1 1 1 0 0 \n"
              + "0 0 1 1 1 1 0 1 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 1 0 0 0 0 1 1 \n"
              + "0 0 1 1 0 1 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardL45());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 1 1 1 0 1 1 \n"
              + "0 0 1 1 0 0 0 1 \n"
              + "1 0 0 0 0 1 1 0 \n"
              + "0 0 0 0 1 1 0 0 \n"
              + "1 0 1 1 1 0 0 0 \n"
              + "0 1 1 0 0 0 0 0 \n"
              + "1 0 0 0 1 1 0 0 \n"
              + "1 1 0 1 1 1 1 1";
    assertEquals(expected, actual);

    position.undoMove();
    actual = Bitboard.toString(position.getAllOccupiedBitboardR90());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardL90());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1 \n"
              + "1 1 0 0 0 0 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardR45());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 1 0 1 1 0 0 \n"
              + "1 1 0 0 0 1 1 0 \n"
              + "0 0 0 1 1 1 0 0 \n"
              + "0 0 1 1 1 1 0 0 \n"
              + "0 0 1 1 1 1 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 1 1 0 0 0 1 1 \n"
              + "0 0 1 1 0 1 1 1";
    assertEquals(expected, actual);
        actual = Bitboard.toString(position.getAllOccupiedBitboardL45());
    LOG.debug("{}", String.format("%n%s%n", actual));
    expected =  "1 1 1 1 1 0 1 1 \n"
              + "0 0 1 1 0 0 0 1 \n"
              + "1 0 0 0 0 1 1 0 \n"
              + "0 0 0 0 1 1 0 0 \n"
              + "0 0 1 1 1 0 0 0 \n"
              + "0 1 1 0 0 0 0 1 \n"
              + "1 0 0 0 1 1 0 0 \n"
              + "1 1 0 1 1 1 1 1";
    assertEquals(expected, actual);
  } // @formatter:on

  @Test
  @Disabled
  void bitBoardsAttacksDev() {

    System.out.println(Bitboard.printBitString(Square.a1.bitboard()));
    System.out.println(Bitboard.printBitString(Square.h8.bitboard()));

    String testFen = "rnbqkb2/1ppppp1r/6p1/p7/1PP1Q1np/8/P1NPPPPP/1RB1KBNR b Kq -";
    Position position = new Position(testFen);
    System.out.println(position);

    long allPieces = position.getAllOccupiedBitboard();
    long whitePieces = position.getOccupiedBitboards(Color.WHITE);
    long blackPieces = position.getOccupiedBitboards(Color.BLACK);

    Square queenSquare = Square.e4;
    int queenSquareIdx = queenSquare.bbIndex();

    long queenRays = Bitboard.queenAttacks[queenSquareIdx];
    System.out.println("All Queen rays");
    System.out.println(Bitboard.toString(queenRays));
    System.out.println();

    //    System.out.println("All queen rays without white pieces");
    //    long queenNoOwn = ~whitePieces & queenRays;
    //    System.out.println(Bitboard.toString(queenNoOwn));
    //    System.out.println();

    System.out.println("All queen rays attacks");
    long queenAttacks = 0L;
    for (int d : Bitboard.queenRays) {
      System.out.println("===================================================");
      long ray = Bitboard.rays[d][queenSquareIdx];

      System.out.println("Ray: " + d);
      System.out.println(Bitboard.toString(ray));
      System.out.println();

      long rayHits = (ray & allPieces);
      System.out.println("Ray hits pieces");
      System.out.println(Bitboard.toString(rayHits));
      System.out.println(Bitboard.printBitString(rayHits));
      System.out.println();

      if (rayHits == 0) continue;

      //      queenAttacks ^= rayHits;

      // diag_attacks=plus7[F5];
      //	blockers=diag_attacks & occupied_squares;
      //	blocking_square=FirstOne(blockers);
      //	diag_attacks^=plus7[blocking_square];

      long firstRayHit;
      int hitIdx;
      if (d <= 3) firstRayHit = Long.lowestOneBit(rayHits);
      else firstRayHit = Long.highestOneBit(rayHits);
      // DEBUG
      Square hitSquare = Square.getFirstSquare(firstRayHit);

      System.out.println("Ray hits first piece");
      System.out.println(Bitboard.toString(firstRayHit));
      System.out.println(Bitboard.printBitString(firstRayHit));
      System.out.println();

      long blockerRay = Bitboard.rays[d][hitSquare.bbIndex()];
      long rayAttacks = ray ^ blockerRay;
      System.out.println("Ray attacks from " + queenSquare + " blocked on " + hitSquare);
      System.out.println(Bitboard.toString(rayAttacks));
      System.out.println();

      queenAttacks |= rayAttacks;

    }
    System.out.println("Queen Attacks:");
    System.out.println(Bitboard.toString(queenAttacks));
    System.out.println();

    long queenMoves = queenAttacks & ~whitePieces;
    System.out.println("Queen Moves:");
    System.out.println(Bitboard.toString(queenMoves));
    System.out.println();

  }

  @Test
  void bitBoardsAttacks() {

    String testFen = "rnbqkb2/1ppppp1r/6p1/p7/1PP1Q1np/8/P1NPPPPP/1RB1KBNR b Kq -";
    Position position = new Position(testFen);

    Color myColor = Color.BLACK;
    Square square = Square.h7;
    int[] rays = Bitboard.rookRays;

    long myPieces = position.getOccupiedBitboards(myColor);
    long oppPieces = position.getOccupiedBitboards(myColor.inverse());
    long attacks = getSlidingAttacks(position, square, rays);

    System.out.println(position);
    System.out.println();
    System.out.println("Attacks:");
    System.out.println(Bitboard.toString(attacks));
    System.out.println();

    long moves = attacks & ~myPieces;
    System.out.println("Moves:");
    System.out.println(Bitboard.toString(moves));
    System.out.println();

    long captures = moves & oppPieces;
    System.out.println("Captures:");
    System.out.println(Bitboard.toString(captures));
    System.out.println();

    long nonCaptures = moves & ~oppPieces;
    System.out.println("Non Captures:");
    System.out.println(Bitboard.toString(nonCaptures));
    System.out.println();

  }

  private long getSlidingAttacks(Position position, Square square, int[] rays) {
    long attacks = 0L;
    int sIdx = square.bbIndex();
    for (int d : rays) {
      long rayHits = (Bitboard.rays[d][sIdx] & position.getAllOccupiedBitboard());
      if (rayHits == 0) attacks |= Bitboard.rays[d][sIdx];
      else {
        long hitSquare;
        if (d <= 3) hitSquare = Long.numberOfTrailingZeros(Long.lowestOneBit(rayHits));
        else hitSquare = Long.numberOfTrailingZeros(Long.highestOneBit(rayHits));
        // TODO TEST & DEBUG
        attacks |=
          Bitboard.rays[d][sIdx] ^ Bitboard.rays[d][Square.getFirstSquare(hitSquare).bbIndex()];
      }
    }
    return attacks;
  }

  /** Tests the timing */
  @Test
  @Disabled
  public void testIsAttackedTiming() {

    int ITERATIONS = 0;
    int DURATION = 20;

    Position board;

    int i = 0;
    String[] fens = getFENs();
    while (fens[i] != null) {
      String testFen = fens[i];
      board = new Position(testFen);

      boolean test;
      Instant start = Instant.now();
      do {
        ITERATIONS++;
        test = board.isAttacked(Color.WHITE, Square.d4);
      } while (Duration.between(start, Instant.now()).getSeconds() != DURATION);

      System.out.println(
        String.format("%,d runs/s for %s (%b)", ITERATIONS / DURATION, fens[i], test));
      i++;
      ITERATIONS = 0;
    }
  }

  String[] getFENs() {

    int i = 0;
    String[] fen = new String[200];
    fen[i++] = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3";
    fen[i++] = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1"; // 218 moves to make
    fen[i++] = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/6R1/pbp2PPP/1R4K1 b kq e3";
    fen[i++] = "r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/6R1/pbp2PPP/1R4K1 w kq -";
    fen[i++] = "8/1P6/6k1/8/8/8/p1K5/8 w - -";
    fen[i++] = "1r3rk1/1pnnq1bR/p1pp2B1/P2P1p2/1PP1pP2/2B3P1/5PK1/2Q4R w - -";
    fen[i++] = "4rk2/p5p1/1p2P2N/7R/nP5P/5PQ1/b6K/q7 w - -";
    fen[i++] = "4k2r/1q1p1pp1/p3p3/1pb1P3/2r3P1/P1N1P2p/1PP1Q2P/2R1R1K1 b k -";
    fen[i++] = "r2r1n2/pp2bk2/2p1p2p/3q4/3PN1QP/2P3R1/P4PP1/5RK1 w - -";
    fen[i++] = "1kr4r/ppp2bq1/4n3/4P1pp/1NP2p2/2PP2PP/5Q1K/4R2R w - -";
    fen[i++] = "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - -";
    fen[i++] = "8/5k2/8/8/2N2N2/2B5/2K5/8 w - -";
    fen[i++] = "8/8/6k1/8/8/8/P1K5/8 w - -";
    fen[i++] = "8/5k2/8/8/8/8/1BK5/1B6 w - -";
    fen[i++] = "8/8/8/8/8/8/8/8 w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/8 w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/7B w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/6B1 w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/7R w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/6R1 w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/7Q w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/6Q1 w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/7N w - - 0 1";
    fen[i++] = "8/8/8/8/8/8/8/6N1 w - - 0 1";

    return fen;
  }

  @Test
  @Disabled
  public void testTiming() {

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
    pos = new Position();

    ArrayList<String> result = new ArrayList<>();

    int ROUNDS = 5;
    int ITERATIONS = 20;
    int REPETITIONS = 1_000_000;

    for (int round = 1; round <= ROUNDS; round++) {
      long start, end, sum, i;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);

      System.gc();
      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1(pos, moves);
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
          test2(pos, moves);
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
    for (int i = 0; i < moves.length; i++) position.makeMove(moves[i]);
    for (int i = 0; i < moves.length; i++) position.undoMove();
  }

  private void test2(final Position position, int[] moves) {
    for (int i = 0; i < moves.length; i++) position.makeMove(moves[i]);
    for (int i = 0; i < moves.length; i++) position.undoMove();
  }

}
