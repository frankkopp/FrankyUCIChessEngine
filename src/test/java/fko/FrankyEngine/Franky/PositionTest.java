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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** @author fkopp */
public class PositionTest {

  private static final int ITERATIONS = 999;

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

  /** */
  @Test
  public void testMoveOnBoard() {
    //        GameBoard gameBoard = new GameBoardImpl();
    //        Position omegaBoard = new Position(gameBoard);
    //
    //        String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    //
    //        // normal
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        GameMove gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"c4-a4");
    //        int move = Move.convertFromGameMove(gameMove);
    //        GameMove convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/q3Pp2/6R1/p1p2PPP/1R4K1 w
    // kq - 1 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // normal pawn move
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"b7-b6");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/2pn3p/1pq1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // normal capture
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"c4-e4");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/4qp2/6R1/p1p2PPP/1R4K1 w
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // pawn double
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"b7-b5");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/2pn3p/2q1q1n1/1p6/2q1Pp2/6R1/p1p2PPP/1R4K1
    // w kq b6 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // castling
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"e8-g8");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r4rk1/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 w
    // - - 1 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // promotion
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"a2-a1Q");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/2p2PPP/qR4K1 w
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // promotion capture
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"a2-b1R");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/2p2PPP/1r4K1 w
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // en passant
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"f4-e3");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q5/4p1R1/p1p2PPP/1R4K1 w
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b
    // kq e3 0 113"));
    //
    //        // multiple moves
    //        // normal
    //        gameBoard = new GameBoardImpl(testFen);
    //        omegaBoard = new Position(gameBoard);
    //        // en passant
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"f4-e3");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //        gameBoard.makeMove(convertedMove);
    //        System.out.println(omegaBoard);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q5/4p1R1/p1p2PPP/1R4K1 w
    // kq - 0 114"));
    //        // pawn capture
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"f2-e3");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //        gameBoard.makeMove(convertedMove);
    //        System.out.println(omegaBoard);
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q5/4P1R1/p1p3PP/1R4K1 b
    // kq - 0 114"));
    //        // castling
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"e8-g8");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //        gameBoard.makeMove(convertedMove);
    //        System.out.println(omegaBoard);
    //
    // assertTrue(omegaBoard.toFENString().equals("r4rk1/1ppn3p/2q1q1n1/8/2q5/4P1R1/p1p3PP/1R4K1 w -
    // - 1 115"));
    //        // pawn double
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"h2-h4");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //        gameBoard.makeMove(convertedMove);
    //        System.out.println(omegaBoard);
    //
    // assertTrue(omegaBoard.toFENString().equals("r4rk1/1ppn3p/2q1q1n1/8/2q4P/4P1R1/p1p3P1/1R4K1 b
    // - h3 0 115"));
    //        // pawn promotion
    //        gameMove = NotationHelper.createNewMoveFromSimpleNotation(gameBoard,"a2-b1R");
    //        move = Move.convertFromGameMove(gameMove);
    //        convertedMove = Move.convertToGameMove(move);
    //        assert(gameMove.equals(convertedMove));
    //        omegaBoard.makeMove(move);
    //        gameBoard.makeMove(convertedMove);
    //        System.out.println(omegaBoard);
    //
    // assertTrue(omegaBoard.toFENString().equals("r4rk1/1ppn3p/2q1q1n1/8/2q4P/4P1R1/2p3P1/1r4K1 w -
    // - 0 116"));
    //        // now test undo
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r4rk1/1ppn3p/2q1q1n1/8/2q4P/4P1R1/p1p3P1/1R4K1 b
    // - h3 0 115"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r4rk1/1ppn3p/2q1q1n1/8/2q5/4P1R1/p1p3PP/1R4K1 w -
    // - 1 115"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q5/4P1R1/p1p3PP/1R4K1 b
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //
    // assertTrue(omegaBoard.toFENString().equals("r3k2r/1ppn3p/2q1q1n1/8/2q5/4p1R1/p1p2PPP/1R4K1 w
    // kq - 0 114"));
    //        omegaBoard.undoMove();
    //        assertTrue(omegaBoard.toFENString().equals(testFen));
    //        System.out.println(omegaBoard);

  }

  /** Some timings to find fastest code - so nfr test */
  @Test
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
    String testFen = "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2PPP/1R4K1 b kq e3 0 113";
    Position omegaBoard = new Position(testFen);

    System.out.println(omegaBoard);

    // pawns
    assertTrue(omegaBoard.isAttacked(Color.WHITE, Square.g3));
    assertTrue(omegaBoard.isAttacked(Color.WHITE, Square.e3));
    assertTrue(omegaBoard.isAttacked(Color.BLACK, Square.b1));
    assertTrue(omegaBoard.isAttacked(Color.BLACK, Square.e4));
    assertTrue(omegaBoard.isAttacked(Color.BLACK, Square.e3));

    // sliding
    assertTrue(omegaBoard.isAttacked(Color.WHITE, Square.g6));
    assertTrue(omegaBoard.isAttacked(Color.BLACK, Square.a5));

    // king
    testFen = "rnbqkbnr/1ppppppp/8/p7/Q1P5/8/PP1PPPPP/RNB1KBNR b KQkq - 1 2";
    omegaBoard = new Position(testFen);
    System.out.println(omegaBoard);
    assertFalse(omegaBoard.isAttacked(Color.WHITE, Square.e8));
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

  /** Tests the timing */
  @Test
  @Disabled
  public void testIsAttackedTiming() {

    int ITERATIONS = 0;
    int DURATION = 2;

    Position board = null;

    int i = 0;
    String[] fens = getFENs();
    while (fens[i] != null) {
      String testFen = fens[i];
      board = new Position(testFen);

      boolean test = false;
      Instant start = Instant.now();
      do {
        ITERATIONS++;
        test = board.isAttacked(Color.WHITE, Square.d4);
      } while (Duration.between(start, Instant.now()).getSeconds() != DURATION);

      //            System.out.println(board);
      //            System.out.println(moves);
      System.out.println(
        String.format("%,d runs/s for %s (%b)", ITERATIONS / DURATION, fens[i], test));
      i++;
      ITERATIONS = 0;
    }
  }

  String[] getFENs() {

    int i = 0;
    String[] fen = new String[200];
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

    ArrayList<String> result = new ArrayList<>();

    int ROUNDS = 5;
    int ITERATIONS = 10;
    int REPETITIONS = 2_000_000;

    final Position position = new Position("8/b2r1pk1/p1R2p2/1p5p/r2Pp3/PRP3P1/5K1P/8 b - d3");
    final int move = Move.fromUCINotation(position, "e4d3");

    for (int round = 0; round < ROUNDS; round++) {
      long start = 0, end = 0, sum = 0;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round);
      System.gc();

      int i = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test1(position, move);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;

      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          test2(position, move);
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 1 avg: %,.3f sec for %,d repetitions", round, avg1,
                               REPETITIONS));
      result.add(String.format("Round %d Test 2 avg: %,.3f sec for %,d repetitions", round, avg2,
                               REPETITIONS));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }

  }

  private void test1(final Position position, int move) {
    // slow version
    position.makeMove(move);
    if (position.isAttacked(position.getOpponent(),
                            position.getKingSquares()[position.getNextPlayer().ordinal()])) {
      position.undoMove();
      return;
    }
    // undo move
    position.undoMove();
  }

  private void test2(final Position position, int move) {
    position.givesCheck(move);
  }
}
