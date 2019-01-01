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

import java.util.Arrays;
import java.util.Random;

/**
 * This class represents the chess board and its position.<br>
 * It uses a x88 board, a stack for undo moves, zobrist keys for transposition tables, piece lists,
 * material counter.<br>
 * Can be created with any FEN notation and as a copy from another
 * Position.
 *
 * <p>x88 method
 *
 * <p>The 0x88 method takes advantage of the fact that a chessboard's 8x8 dimensions are an even
 * power of two (i.e. 8 squared). The board uses a one-dimensional array of size 16x8 = 128,
 * numbered 0 to 127 rather than an array of size 64. It is basically two boards next to each other,
 * the actual board on the left while the board on the right would contain illegal territory. The
 * binary layout for a legal board coordinate's rank and file within the array is 0rrr0fff (The r's
 * are the 3 bits used to represent the rank. The f's for the file). For example 0x71 (binary
 * 01110001) would represent the square b8 (in Algebraic notation). When generating moves from the
 * main board, one can check that a destination square is on the main board before consulting the
 * array simply by ANDing the square number with hexadecimal 0x88 (binary 10001000). A non-zero
 * result indicates that the square is off the main board. In addition, the difference between two
 * squares' coordinates uniquely determines whether those two squares are along the same row,
 * column, or diagonal (a common query used for determining check).
 *
 * <p>https://www.chessprogramming.org/0x88
 */
public class Position {

  /* Standard Board Setup as FEN */
  public static final String STANDARD_BOARD_FEN =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /* Size of 0x88 board */
  private static final int BOARDSIZE = 128;

  /* Max History */
  private static final int MAX_HISTORY = 255;

  /* random generator for use with zobrist hash keys */
  private static final Random random = new Random(0);

  /*
   * The zobrist key to use as a hash key in transposition tables
   * The zobrist key will be updated incrementally every time one of the the state variables change.
   */
  private long   zobristKey        = 0;
  private long[] zobristKeyHistory = new long[MAX_HISTORY];

  // history counter
  private int historyCounter = 0;

  // **********************************************************
  // Board State START ----------------------------------------
  // unique chess position
  //
  // 0x88 Board
  private              Piece[]  x88Board     = new Piece[BOARDSIZE];

  // hash for pieces - piece, board
  private static final long[][] pieceZobrist = new long[Piece.values.length][Square.values.length];

  // Castling rights
  private              boolean   castlingWK         = true;
  private              boolean[] castlingWK_History = new boolean[MAX_HISTORY];
  private static final long      castlingWK_Zobrist;
  private              boolean   castlingWQ         = true;
  private              boolean[] castlingWQ_History = new boolean[MAX_HISTORY];
  private static final long      castlingWQ_Zobrist;
  private              boolean   castlingBK         = true;
  private              boolean[] castlingBK_History = new boolean[MAX_HISTORY];
  private static final long      castlingBK_Zobrist;
  private              boolean   castlingBQ         = true;
  private              boolean[] castlingBQ_History = new boolean[MAX_HISTORY];
  private static final long      castlingBQ_Zobrist;

  // en passant field - if NOSQUARE then we do not have an en passant option
  private              Square   enPassantSquare         = Square.NOSQUARE;
  private              Square[] enPassantSquare_History = new Square[MAX_HISTORY];
  private static final long[]   enPassantSquare_Zobrist = new long[Square.values.length];

  // half move clock - number of half moves since last capture
  private int   halfMoveClock        = 0;
  private int[] halfMoveClockHistory = new int[MAX_HISTORY];
  // has no zobrist key

  // next player color
  private              Color nextPlayer = Color.WHITE;
  private static final long  nextPlayer_Zobrist;
  //
  // Board State END ------------------------------------------
  // **********************************************************

  // **********************************************************
  // Extended Board State ----------------------------------
  // not necessary for a unique position

  // we can recreate the board through the last move - no need for history of board itself
  private int[] moveHistory = new int[MAX_HISTORY];

  // half move number - the actual half move number to determine the full move number
  private int nextHalfMoveNumber = 1;

  // Lists for all pieces
  private final SquareList[] pawnSquares   = new SquareList[Color.values.length];
  private final SquareList[] knightSquares = new SquareList[Color.values.length];
  private final SquareList[] bishopSquares = new SquareList[Color.values.length];
  private final SquareList[] rookSquares   = new SquareList[Color.values.length];
  private final SquareList[] queenSquares  = new SquareList[Color.values.length];
  private final Square[]     kingSquares   = new Square[Color.values.length];

  // Material value will always be up to date
  private int[] material;

  // caches a hasCheck and hasMate Flag for the current position. Will be set after
  // a call to hasCheck() and reset to TBD every time a move is made or unmade.
  private Flag   hasCheck            = Flag.TBD;
  private Flag[] hasCheckFlagHistory = new Flag[MAX_HISTORY];
  private Flag   hasMate             = Flag.TBD;
  private Flag[] hasMateFlagHistory  = new Flag[MAX_HISTORY];

  private final MoveGenerator mateCheckMG = new MoveGenerator();

  // Flag for boolean states with undetermined state
  private enum Flag {
    TBD, TRUE, FALSE;
  }

  // **********************************************************
  // static initialization
  static {
    // all pieces on all squares
    for (Piece p : Piece.values) {
      for (Square s : Square.values) {
        pieceZobrist[p.ordinal()][s.ordinal()] = Math.abs(random.nextLong());
      }
    }
    // all castling combinations
    castlingWK_Zobrist = Math.abs(random.nextLong());
    castlingWQ_Zobrist = Math.abs(random.nextLong());
    castlingBK_Zobrist = Math.abs(random.nextLong());
    castlingBQ_Zobrist = Math.abs(random.nextLong());

    // all possible positions of the en passant square (easiest to use all fields and not just the
    // ones where en passant is indeed possible)
    for (Square s : Square.values) {
      enPassantSquare_Zobrist[s.ordinal()] = Math.abs(random.nextLong());
    }
    // set or unset this for the two color options
    nextPlayer_Zobrist = Math.abs(random.nextLong());
  }

  // Constructors START -----------------------------------------
  /**
   * Creates a standard board position and initializes it with standard chess setup.
   */
  public Position() {
    this(STANDARD_BOARD_FEN);
  }

  /**
   * Creates a standard board position and initializes it with a fen position
   *
   * @param fen
   */
  public Position(String fen) {
    initializeLists();
    initBoard(fen);
  }

  /**
   * Copy constructor - creates a copy of the given Position
   *
   * @param op
   */
  public Position(Position op) {
    if (op == null) throw new NullPointerException("Parameter op may not be null");

    // x88 board
    System.arraycopy(op.getX88Board(), 0, this.getX88Board(), 0, op.getX88Board().length);

    // game state
    this.nextHalfMoveNumber = op.nextHalfMoveNumber;
    this.nextPlayer = op.getNextPlayer();
    this.zobristKey = op.getZobristKey();

    this.castlingWK = op.isCastlingWK();
    this.castlingWQ = op.isCastlingWQ();
    this.castlingBK = op.isCastlingBK();
    this.castlingBQ = op.isCastlingBQ();
    this.enPassantSquare = op.getEnPassantSquare();
    this.halfMoveClock = op.halfMoveClock;

    this.hasCheck = op.hasCheck;
    this.hasMate = op.hasMate;

    // history
    this.historyCounter = op.historyCounter;
    System.arraycopy(op.zobristKeyHistory, 0, zobristKeyHistory, 0, zobristKeyHistory.length);

    System.arraycopy(op.castlingWK_History, 0, castlingWK_History, 0, castlingWK_History.length);
    System.arraycopy(op.castlingWQ_History, 0, castlingWQ_History, 0, castlingWQ_History.length);
    System.arraycopy(op.castlingBK_History, 0, castlingBK_History, 0, castlingBK_History.length);
    System.arraycopy(op.castlingBQ_History, 0, castlingBQ_History, 0, castlingBQ_History.length);
    System.arraycopy(op.enPassantSquare_History, 0, enPassantSquare_History, 0,
                     enPassantSquare_History.length);
    System.arraycopy(op.halfMoveClockHistory, 0, halfMoveClockHistory, 0,
                     halfMoveClockHistory.length);

    System.arraycopy(op.hasCheckFlagHistory, 0, hasCheckFlagHistory, 0, hasCheckFlagHistory.length);
    System.arraycopy(op.hasMateFlagHistory, 0, hasMateFlagHistory, 0, hasMateFlagHistory.length);

    // move history
    System.arraycopy(op.moveHistory, 0, moveHistory, 0, op.moveHistory.length);

    // initializeLists();
    // copy piece lists
    for (int i = 0; i <= 1; i++) { // foreach color
      this.getPawnSquares()[i] = op.getPawnSquares()[i].clone();
      this.getKnightSquares()[i] = op.getKnightSquares()[i].clone();
      this.getBishopSquares()[i] = op.getBishopSquares()[i].clone();
      this.getRookSquares()[i] = op.getRookSquares()[i].clone();
      this.getQueenSquares()[i] = op.getQueenSquares()[i].clone();
      this.getKingSquares()[i] = op.getKingSquares()[i];
    }
    material = new int[2];
    this.material[0] = op.material[0];
    this.material[1] = op.material[1];
  }

  /**
   * Initialize the lists for the pieces and the material counter
   */
  private void initializeLists() {
    for (int i = 0; i <= 1; i++) { // foreach color
      getPawnSquares()[i] = new SquareList();
      getKnightSquares()[i] = new SquareList();
      getBishopSquares()[i] = new SquareList();
      getRookSquares()[i] = new SquareList();
      getQueenSquares()[i] = new SquareList();
      getKingSquares()[i] = Square.NOSQUARE;
    }
    material = new int[2];
  }

  /**
   * Commits a move to the board. Due to performance there is no check if this move is legal on the
   * current board. Legal check needs to be done beforehand. Usually the move will be generated by
   * our MoveGenerator and therefore the move will be assumed legal anyway.
   *
   * @param move the move
   */
  public void makeMove(int move) {
    assert (move != Move.NOMOVE);

    Square fromSquare = Move.getStart(move);
    assert fromSquare.isValidSquare();
    Square toSquare = Move.getEnd(move);
    assert toSquare.isValidSquare();
    Piece piece = Move.getPiece(move);
    assert piece != Piece.NOPIECE;
    Piece target = Move.getTarget(move);
    Piece promotion = Move.getPromotion(move);

    // Save state for undoMove
    moveHistory[historyCounter] = move;
    castlingWK_History[historyCounter] = isCastlingWK();
    castlingWQ_History[historyCounter] = isCastlingWQ();
    castlingBK_History[historyCounter] = isCastlingBK();
    castlingBQ_History[historyCounter] = isCastlingBQ();
    enPassantSquare_History[historyCounter] = getEnPassantSquare();
    halfMoveClockHistory[historyCounter] = halfMoveClock;
    zobristKeyHistory[historyCounter] = getZobristKey();
    hasCheckFlagHistory[historyCounter] = hasCheck;
    hasMateFlagHistory[historyCounter] = hasMate;
    historyCounter++;

    // reset check and mate flag
    hasCheck = Flag.TBD;
    hasMate = Flag.TBD;

    // make move
    switch (Move.getMoveType(move)) {
      case NORMAL:
        invalidateCastlingRights(fromSquare, toSquare);
        if (target != Piece.NOPIECE) {
          removePiece(toSquare, target);
          halfMoveClock = 0; // reset half move clock because of capture
        } else if (piece.getType() == PieceType.PAWN) {
          halfMoveClock = 0; // reset half move clock because of pawn move
        } else {
          halfMoveClock++;
        }
        movePiece(fromSquare, toSquare, piece);
        clearEnPassant();
        break;
      case PAWNDOUBLE:
        assert fromSquare.isPawnBaseRow(piece.getColor());
        assert !piece.getColor().isNone();
        movePiece(fromSquare, toSquare, piece);
        clearEnPassant();
        // set new en passant target field - always one "behind" the toSquare
        enPassantSquare = piece.getColor().isWhite() ? toSquare.getSouth() : toSquare.getNorth();
        zobristKey =
          getZobristKey() ^ enPassantSquare_Zobrist[getEnPassantSquare().ordinal()]; // in
        halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case ENPASSANT:
        assert target != Piece.NOPIECE;
        assert target.getType() == PieceType.PAWN;
        assert !target.getColor().isNone();
        Square targetSquare =
          target.getColor().isWhite() ? toSquare.getNorth() : toSquare.getSouth();
        removePiece(targetSquare, target);
        movePiece(fromSquare, toSquare, piece);
        clearEnPassant();
        halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case CASTLING:
        makeCastlingMove(fromSquare, toSquare, piece);
        clearEnPassant();
        halfMoveClock++;
        break;
      case PROMOTION:
        if (target != Piece.NOPIECE) removePiece(toSquare, target);
        invalidateCastlingRights(fromSquare, toSquare);
        removePiece(fromSquare, piece);
        putPiece(toSquare, promotion);
        clearEnPassant();
        halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case NOMOVETYPE:
      default:
        throw new IllegalArgumentException();
    }

    // update halfMoveNumber
    nextHalfMoveNumber++;

    // change color (active player)
    nextPlayer = getNextPlayer().getInverseColor();
    zobristKey = getZobristKey() ^ nextPlayer_Zobrist;
  }

  /**
   * Takes back the last move from the board
   */
  public void undoMove() {
    // Get state for undoMove
    historyCounter--;

    int move = moveHistory[historyCounter];

    // reset move history
    moveHistory[historyCounter] = Move.NOMOVE;

    // undo piece move / restore board
    Square fromSquare = Move.getStart(move);
    assert fromSquare.isValidSquare();
    Square toSquare = Move.getEnd(move);
    assert toSquare.isValidSquare();
    Piece piece = Move.getPiece(move);
    assert piece != Piece.NOPIECE;
    Piece target = Move.getTarget(move);
    Piece promotion = Move.getPromotion(move);

    switch (Move.getMoveType(move)) {
      case NORMAL:
        movePiece(toSquare, fromSquare, piece);
        if (target != Piece.NOPIECE) {
          putPiece(toSquare, target);
        }
        break;
      case PAWNDOUBLE:
        movePiece(toSquare, fromSquare, piece);
        break;
      case ENPASSANT:
        Square targetSquare =
          target.getColor().isWhite() ? toSquare.getNorth() : toSquare.getSouth();
        movePiece(toSquare, fromSquare, piece);
        putPiece(targetSquare, target);
        break;
      case CASTLING:
        undoCastlingMove(fromSquare, toSquare, piece);
        break;
      case PROMOTION:
        removePiece(toSquare, promotion);
        putPiece(fromSquare, piece);
        if (target != Piece.NOPIECE) putPiece(toSquare, target);
        break;
      case NOMOVETYPE:
      default:
        throw new IllegalArgumentException();
    }

    // restore castling rights
    castlingWK = castlingWK_History[historyCounter];
    castlingWQ = castlingWQ_History[historyCounter];
    castlingBK = castlingBK_History[historyCounter];
    castlingBQ = castlingBQ_History[historyCounter];

    // restore en passant square
    enPassantSquare = enPassantSquare_History[historyCounter];

    // restore halfMoveClock
    halfMoveClock = halfMoveClockHistory[historyCounter];

    // decrease _halfMoveNumber
    nextHalfMoveNumber--;

    // change back color
    nextPlayer = getNextPlayer().getInverseColor();

    // zobristKey - just overwrite - should be the same as before the move
    zobristKey = zobristKeyHistory[historyCounter];

    // get the check and mate flag from history
    hasCheck = hasCheckFlagHistory[historyCounter];
    hasMate = hasMateFlagHistory[historyCounter];
  }

  private void clearEnPassant() {
    if (getEnPassantSquare() != Square.NOSQUARE) {
      zobristKey = getZobristKey() ^ enPassantSquare_Zobrist[getEnPassantSquare().ordinal()]; // out
      enPassantSquare = Square.NOSQUARE;
    }
  }

  /**
   * @param fromSquare
   * @param toSquare
   */
  private void invalidateCastlingRights(Square fromSquare, Square toSquare) {
    // check for castling rights invalidation
    // no else here - combination of these can occur! BIG BUG before :)
    if (fromSquare == Square.e1 || toSquare == Square.e1) {
      // only take out zobrist if the castling was true before.
      if (isCastlingWK()) zobristKey = getZobristKey() ^ castlingWK_Zobrist;
      castlingWK = false;
      if (isCastlingWQ()) zobristKey = getZobristKey() ^ castlingWQ_Zobrist;
      castlingWQ = false;
    }
    if (fromSquare == Square.e8 || toSquare == Square.e8) {
      // only take out zobrist if the castling was true before.
      if (isCastlingBK()) zobristKey = getZobristKey() ^ castlingBK_Zobrist;
      castlingBK = false;
      if (isCastlingBQ()) zobristKey = getZobristKey() ^ castlingBQ_Zobrist;
      castlingBQ = false;
    }
    if (fromSquare == Square.a1 || toSquare == Square.a1) {
      if (isCastlingWQ()) zobristKey = getZobristKey() ^ castlingWQ_Zobrist;
      castlingWQ = false;
    }
    if (fromSquare == Square.h1 || toSquare == Square.h1) {
      if (isCastlingWK()) zobristKey = getZobristKey() ^ castlingWK_Zobrist;
      castlingWK = false;
    }
    if (fromSquare == Square.a8 || toSquare == Square.a8) {
      if (isCastlingBQ()) zobristKey = getZobristKey() ^ castlingBQ_Zobrist;
      castlingBQ = false;
    }
    if (fromSquare == Square.h8 || toSquare == Square.h8) {
      if (isCastlingBK()) zobristKey = getZobristKey() ^ castlingBK_Zobrist;
      castlingBK = false;
    }
  }

  /**
   * @param fromSquare
   * @param toSquare
   * @param piece
   */
  private void makeCastlingMove(Square fromSquare, Square toSquare, Piece piece) {
    assert piece.getType() == PieceType.KING;

    Piece rook;
    Square rookFromSquare;
    Square rookToSquare;

    switch (toSquare) {
      case g1: // white kingside
        assert (isCastlingWK());
        rook = Piece.WHITE_ROOK;
        rookFromSquare = Square.h1;
        assert (getX88Board()[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
          : "rook to castle not there";
        rookToSquare = Square.f1;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case c1: // white queenside
        assert (isCastlingWQ());
        rook = Piece.WHITE_ROOK;
        rookFromSquare = Square.a1;
        assert (getX88Board()[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
          : "rook to castle not there";
        rookToSquare = Square.d1;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case g8: // black kingside
        assert (isCastlingBK());
        rook = Piece.BLACK_ROOK;
        rookFromSquare = Square.h8;
        assert (getX88Board()[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
          : "rook to castle not there";
        rookToSquare = Square.f8;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case c8: // black queenside
        assert (isCastlingBQ());
        rook = Piece.BLACK_ROOK;
        rookFromSquare = Square.a8;
        assert (getX88Board()[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
          : "rook to castle not there";
        rookToSquare = Square.d8;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      default:
        throw new IllegalArgumentException("Castling to wrong square " + toSquare.toString());
    }
    // King
    movePiece(fromSquare, toSquare, piece);
    // Rook
    movePiece(rookFromSquare, rookToSquare, rook);
  }

  /**
   * @param fromSquare
   * @param toSquare
   * @param piece
   */
  private void undoCastlingMove(Square fromSquare, Square toSquare, Piece piece) {
    Piece rook;
    Square rookFromSquare;
    Square rookToSquare;

    switch (toSquare) {
      case g1: // white kingside
        rook = Piece.WHITE_ROOK;
        rookFromSquare = Square.h1;
        rookToSquare = Square.f1;
        break;
      case c1: // white queenside
        rook = Piece.WHITE_ROOK;
        rookFromSquare = Square.a1;
        rookToSquare = Square.d1;
        break;
      case g8: // black kingside
        rook = Piece.BLACK_ROOK;
        rookFromSquare = Square.h8;
        rookToSquare = Square.f8;
        break;
      case c8: // black queenside
        rook = Piece.BLACK_ROOK;
        rookFromSquare = Square.a8;
        rookToSquare = Square.d8;
        break;
      default:
        throw new IllegalArgumentException("Castling to wrong square " + toSquare.toString());
    }

    // ignore Zobrist Key as it will be restored in undoMove!!

    // King
    movePiece(toSquare, fromSquare, piece);
    // Rook
    movePiece(rookToSquare, rookFromSquare, rook);
  }

  /**
   * Makes a null move. Essentially switches sides within same position.
   */
  public void makeNullMove() {

    // Save state for undoMove
    castlingWK_History[historyCounter] = isCastlingWK();
    castlingWQ_History[historyCounter] = isCastlingWQ();
    castlingBK_History[historyCounter] = isCastlingBK();
    castlingBQ_History[historyCounter] = isCastlingBQ();
    enPassantSquare_History[historyCounter] = getEnPassantSquare();
    halfMoveClockHistory[historyCounter] = halfMoveClock;
    zobristKeyHistory[historyCounter] = getZobristKey();
    hasCheckFlagHistory[historyCounter] = hasCheck;
    hasMateFlagHistory[historyCounter] = hasMate;
    historyCounter++;

    // reset check and mate flag
    hasCheck = Flag.TBD;
    hasMate = Flag.TBD;

    // clear en passant
    clearEnPassant();

    // increase half move clock
    halfMoveClock++;

    // increase halfMoveNumber
    nextHalfMoveNumber++;

    // change color (active player)
    nextPlayer = getNextPlayer().getInverseColor();
    zobristKey = getZobristKey() ^ nextPlayer_Zobrist;
  }

  /**
   * Undo a null move. Essentially switches back sides within same position.
   */
  public void undoNullMove() {
    // Get state for undoMove
    historyCounter--;

    // restore castling rights
    castlingWK = castlingWK_History[historyCounter];
    castlingWQ = castlingWQ_History[historyCounter];
    castlingBK = castlingBK_History[historyCounter];
    castlingBQ = castlingBQ_History[historyCounter];

    // restore en passant square
    enPassantSquare = enPassantSquare_History[historyCounter];

    // restore halfMoveClock
    halfMoveClock = halfMoveClockHistory[historyCounter];

    // decrease _halfMoveNumber
    nextHalfMoveNumber--;

    // change back color
    nextPlayer = getNextPlayer().getInverseColor();

    // zobristKey - just overwrite - should be the same as before the move
    zobristKey = zobristKeyHistory[historyCounter];

    // get the check and mate flag from history
    hasCheck = hasCheckFlagHistory[historyCounter];
    hasMate = hasMateFlagHistory[historyCounter];
  }

  /**
   * @param fromSquare
   * @param toSquare
   * @param piece
   */
  private void movePiece(Square fromSquare, Square toSquare, Piece piece) {
    assert fromSquare.isValidSquare();
    assert toSquare.isValidSquare();
    assert piece != Piece.NOPIECE;
    // assert
    assert (getX88Board()[fromSquare.ordinal()] == piece) // check if moved piece is indeed there
      : "piece to move not there";
    assert (getX88Board()[toSquare.ordinal()] == Piece.NOPIECE) // // should be empty
      : "to square should be empty";
    // due to performance we do not call remove and put
    // no need to update counters when moving
    // remove
    getX88Board()[fromSquare.ordinal()] = Piece.NOPIECE;
    zobristKey = getZobristKey() ^ pieceZobrist[piece.ordinal()][fromSquare.ordinal()]; // out
    // update piece lists
    final int color = piece.getColor().ordinal();
    removeFromPieceLists(fromSquare, piece, color);
    // put
    getX88Board()[toSquare.ordinal()] = piece;
    zobristKey = getZobristKey() ^ pieceZobrist[piece.ordinal()][toSquare.ordinal()]; // in
    // update piece lists
    addToPieceLists(toSquare, piece, color);
  }

  /**
   * @param square
   * @param piece
   */
  private void putPiece(Square square, Piece piece) {
    assert square.isValidSquare();
    assert piece != Piece.NOPIECE;
    assert getX88Board()[square.ordinal()] == Piece.NOPIECE; // should be empty
    // put
    getX88Board()[square.ordinal()] = piece;
    zobristKey = getZobristKey() ^ pieceZobrist[piece.ordinal()][square.ordinal()]; // in
    // update piece lists
    final int color = piece.getColor().ordinal();
    addToPieceLists(square, piece, color);
    // update material
    material[color] += piece.getType().getValue();
  }

  /**
   * @return the removed piece
   */
  private Piece removePiece(Square square, Piece piece) {
    assert square.isValidSquare();
    assert piece != Piece.NOPIECE;
    assert getX88Board()[square.ordinal()] == piece // check if removed piece is indeed there
      : "piece to be removed not there";
    // remove
    Piece old = getX88Board()[square.ordinal()];
    getX88Board()[square.ordinal()] = Piece.NOPIECE;
    zobristKey = getZobristKey() ^ pieceZobrist[piece.ordinal()][square.ordinal()]; // out
    // update piece lists
    final int color = piece.getColor().ordinal();
    removeFromPieceLists(square, piece, color);
    // update material
    material[color] -= piece.getType().getValue();
    // return the remove piece
    return old;
  }

  /**
   * Retrieve piece on given square.
   *
   * @param square
   * @return returns the piece or <code>NOPIECE</code> of the given square
   */
  public Piece getPiece(final Square square) {
    return x88Board[square.ordinal()];
  }

  /**
   * @param toSquare
   * @param piece
   * @param color
   */
  private void addToPieceLists(Square toSquare, Piece piece, final int color) {
    switch (piece.getType()) {
      case PAWN:
        getPawnSquares()[color].add(toSquare);
        break;
      case KNIGHT:
        getKnightSquares()[color].add(toSquare);
        break;
      case BISHOP:
        getBishopSquares()[color].add(toSquare);
        break;
      case ROOK:
        getRookSquares()[color].add(toSquare);
        break;
      case QUEEN:
        getQueenSquares()[color].add(toSquare);
        break;
      case KING:
        getKingSquares()[color] = toSquare;
        break;
      default:
        break;
    }
  }

  /**
   * @param fromSquare
   * @param piece
   * @param color
   */
  private void removeFromPieceLists(Square fromSquare, Piece piece, final int color) {
    switch (piece.getType()) {
      case PAWN:
        getPawnSquares()[color].remove(fromSquare);
        break;
      case KNIGHT:
        getKnightSquares()[color].remove(fromSquare);
        break;
      case BISHOP:
        getBishopSquares()[color].remove(fromSquare);
        break;
      case ROOK:
        getRookSquares()[color].remove(fromSquare);
        break;
      case QUEEN:
        getQueenSquares()[color].remove(fromSquare);
        break;
      case KING:
        getKingSquares()[color] = Square.NOSQUARE;
        break;
      default:
        break;
    }
  }

  /**
   * This checks if a certain square is currently under attack by the player of the given color. It
   * does not matter who has the next move on this position. It also is not checking if the actual
   * attack can be done as a legal move. E.g. a pinned piece could not actually make a capture on
   * the square.
   *
   * @param attackerColor
   * @param square
   * @return true if under attack
   */
  public boolean isAttacked(Color attackerColor, Square square) {
    assert (square != Square.NOSQUARE);
    assert (!attackerColor.isNone());

    final int squareIndex = square.ordinal();
    final boolean isWhite = attackerColor.isWhite();

    /*
     * Checks are ordered for likelihood to return from this as fast as possible
     */

    // check pawns
    // reverse direction to look for pawns which could attack
    final int pawnDir = isWhite ? -1 : 1;
    final Piece attackerPawn = isWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
    for (int d : Square.pawnAttackDirections) {
      final int i = squareIndex + d * pawnDir;
      if ((i & 0x88) == 0 && getX88Board()[i] == attackerPawn) return true;
    }

    final int attackerColorIndex = attackerColor.ordinal();

    // check sliding horizontal (rook + queen) if there are any
    if (!(getRookSquares()[attackerColorIndex].isEmpty() &&
          getQueenSquares()[attackerColorIndex].isEmpty())) {
      for (int d : Square.rookDirections) {
        int i = squareIndex + d;
        while ((i & 0x88) == 0) { // slide while valid square
          if (getX88Board()[i] != Piece.NOPIECE) { // not empty
            if (getX88Board()[i].getColor() == attackerColor // attacker piece
                && (getX88Board()[i].getType() == PieceType.ROOK ||
                    getX88Board()[i].getType() == PieceType.QUEEN)) {
              return true;
            }
            break; // not an attacker color or attacker piece blocking the way.
          }
          i += d; // next sliding field in this direction
        }
      }
    }

    // check sliding diagonal (bishop + queen) if there are any
    if (!(getBishopSquares()[attackerColorIndex].isEmpty() &&
          getQueenSquares()[attackerColorIndex].isEmpty())) {
      for (int d : Square.bishopDirections) {
        int i = squareIndex + d;
        while ((i & 0x88) == 0) { // slide while valid square
          if (getX88Board()[i] != Piece.NOPIECE) { // not empty
            if (getX88Board()[i].getColor() == attackerColor // attacker piece
                && (getX88Board()[i].getType() == PieceType.BISHOP ||
                    getX88Board()[i].getType() == PieceType.QUEEN)) {
              return true;
            }
            break; // not an attacker color or attacker piece blocking the way.
          }
          i += d; // next sliding field in this direction
        }
      }
    }

    // check knights if there are any
    if (!(getKnightSquares()[attackerColorIndex].isEmpty())) {
      for (int d : Square.knightDirections) {
        int i = squareIndex + d;
        if ((i & 0x88) == 0) { // valid square
          if (getX88Board()[i] != Piece.NOPIECE // not empty
              && getX88Board()[i].getColor() == attackerColor // attacker piece
              && (getX88Board()[i].getType() == PieceType.KNIGHT)) {
            return true;
          }
        }
      }
    }

    // check king
    for (int d : Square.kingDirections) {
      int i = squareIndex + d;
      if ((i & 0x88) == 0) { // valid square
        if (getX88Board()[i] != Piece.NOPIECE // not empty
            && getX88Board()[i].getColor() == attackerColor // attacker piece
            && (getX88Board()[i].getType() == PieceType.KING)) {
          return true;
        }
      }
    }

    // check en passant
    if (this.getEnPassantSquare() != Square.NOSQUARE) {
      if (isWhite // white is attacker
          && getX88Board()[getEnPassantSquare().getSouth().ordinal()] == Piece.BLACK_PAWN
          // black is target
          && this.getEnPassantSquare().getSouth() ==
             square) { // this is indeed the en passant attacked square
        // left
        int i = squareIndex + Square.W;
        if ((i & 0x88) == 0 && getX88Board()[i] == Piece.WHITE_PAWN) return true;
        // right
        i = squareIndex + Square.E;
        if ((i & 0x88) == 0 && getX88Board()[i] == Piece.WHITE_PAWN) return true;
      } else if (!isWhite // black is attacker (assume not noColor)
                 && getX88Board()[getEnPassantSquare().getNorth().ordinal()] == Piece.WHITE_PAWN
                 // white is target
                 && this.getEnPassantSquare().getNorth() ==
                    square) { // this is indeed the en passant attacked square
        // attack from left
        int i = squareIndex + Square.W;
        if ((i & 0x88) == 0 && getX88Board()[i] == Piece.BLACK_PAWN) return true;
        // attack from right
        i = squareIndex + Square.E;
        if ((i & 0x88) == 0 && getX88Board()[i] == Piece.BLACK_PAWN) return true;
      }
    }

    return false;
  }

  /**
   * @return true if current position has check for next player
   */
  public boolean hasCheck() {
    if (hasCheck != Flag.TBD) return hasCheck == Flag.TRUE;
    boolean check =
      isAttacked(getNextPlayer().getInverseColor(), getKingSquares()[getNextPlayer().ordinal()]);
    hasCheck = check ? Flag.TRUE : Flag.FALSE;
    return check;
  }

  /**
   * Tests for mate on this position. If true the next player has lost. Expensive test as all legal
   * moves have to be generated.
   *
   * @return true if current position is mate for next player
   */
  public boolean hasCheckMate() {
    if (!hasCheck()) return false;
    if (hasMate != Flag.TBD) return hasMate == Flag.TRUE;
    if (!mateCheckMG.hasLegalMove(this)) {
      hasMate = Flag.TRUE;
      return true;
    }
    hasMate = Flag.FALSE;
    return false;
  }

  /**
   * The fifty-move rule if during the previous 50 moves no pawn has been moved and no capture has
   * been made, either player may claim a draw.
   *
   * @return true if during the previous 50 moves no pawn has been moved and no capture has been
   * made
   */
  public boolean check50Moves() {
    return this.halfMoveClock >= 100;
  }

  /**
   * Threefold repetition of a position this most commonly occurs when neither side is able to avoid
   * repeating moves without incurring a disadvantage. The three occurrences of the position need
   * not occur on consecutive moves for a claim to be valid. FIDE rules make no mention of perpetual
   * check; this is merely a specific type of draw by threefold repetition.
   *
   * @return true if this position has been played three times
   */
  public boolean check3Repetitions() {

    /*
    [0]     3185849660387886977 << 1st
    [1]     447745478729458041
    [2]     3230145143131659788
    [3]     491763876012767476
    [4]     3185849660387886977 << 2nd
    [5]     447745478729458041
    [6]     3230145143131659788
    [7]     491763876012767476  <<< history
    [8]     3185849660387886977 <<< 3rd REPETITION from current zobrist
     */

    if (historyCounter < 8) return false;
    int counter = 0;
    int i = historyCounter - 4;
    while (i >= 0) {
      if (getZobristKey() == zobristKeyHistory[i]) counter++;
      if (counter >= 2) return true;
      i -= 4;
    }
    return false;
  }

  /**
   * FIDE Draws - Evaluation might define some more draw values.
   *
   * @return true if neither side can win
   */
  public boolean checkInsufficientMaterial() {

    /*
     * both sides have a bare king
     * one side has a king and a minor piece against a bare king
     * one side has two knights against the bare king
     * both sides have a king and a bishop, the bishops being the same color
     */
    if (getPawnSquares()[Color.WHITE.ordinal()].size() == 0 &&
        getPawnSquares()[Color.BLACK.ordinal()].size() == 0 &&
        getRookSquares()[Color.WHITE.ordinal()].size() == 0 &&
        getRookSquares()[Color.BLACK.ordinal()].size() == 0 &&
        getQueenSquares()[Color.WHITE.ordinal()].size() == 0 &&
        getQueenSquares()[Color.BLACK.ordinal()].size() == 0) {

      // white king bare KK*
      if (getKnightSquares()[Color.WHITE.ordinal()].size() == 0 &&
          getBishopSquares()[Color.WHITE.ordinal()].size() == 0) {

        // both kings bare KK, KKN, KKNN
        if (getKnightSquares()[Color.BLACK.ordinal()].size() <= 2 &&
            getBishopSquares()[Color.BLACK.ordinal()].size() == 0) {
          return true;
        }

        // KKB
        if (getKnightSquares()[Color.BLACK.ordinal()].size() == 0 &&
            getBishopSquares()[Color.BLACK.ordinal()].size() == 1) {
          return true;
        }

      }
      // only black king bare K*K
      else if (getKnightSquares()[Color.BLACK.ordinal()].size() == 0 &&
               getBishopSquares()[Color.BLACK.ordinal()].size() == 0) {

        // both kings bare KK, KNK, KNNK
        if (getKnightSquares()[Color.WHITE.ordinal()].size() <= 2 &&
            getBishopSquares()[Color.WHITE.ordinal()].size() == 0) {
          return true;
        }

        // KBK
        if (getKnightSquares()[Color.BLACK.ordinal()].size() == 0 &&
            getBishopSquares()[Color.BLACK.ordinal()].size() == 1) {
          return true;
        }
      }

      // KBKB - B same field color
      else if (getKnightSquares()[Color.BLACK.ordinal()].size() == 0 &&
               getBishopSquares()[Color.BLACK.ordinal()].size() == 1 &&
               getKnightSquares()[Color.WHITE.ordinal()].size() == 0 &&
               getBishopSquares()[Color.WHITE.ordinal()].size() == 1) {

        /*
         * Bishops are on the same field color if the sum of the
         * rank and file of the fields are on both even or both odd :
         * (file + rank) % 2 == 0 = black field
         * (file + rank) % 2 == 1 = white field
         */
        final Square whiteBishop = getBishopSquares()[Color.WHITE.ordinal()].get(0);
        int file_w = whiteBishop.getFile().get();
        int rank_w = whiteBishop.getRank().get();
        final Square blackBishop = getBishopSquares()[Color.BLACK.ordinal()].get(0);
        int file_b = blackBishop.getFile().get();
        int rank_b = blackBishop.getRank().get();
        if (((file_w + rank_w) % 2) == ((file_b + rank_b) % 2)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return the zobristKey
   */
  public long getZobristKey() {
    return this.zobristKey;
  }

  /**
   * @param c Color
   * @return the material value
   */
  public int getMaterial(Color c) {
    return this.material[c.ordinal()];
  }

  /**
   * @return color of next player
   */
  public Color getNextPlayer() {
    return nextPlayer;
  }

  /**
   * @return color of opponent player
   */
  public Color getOpponent() {
    return nextPlayer.getInverseColor();
  }

  /**
   * Returns the last move. Returns Move.NOMOVE if there is no last move.
   *
   * @return int representing a move
   */
  public int getLastMove() {
    if (historyCounter == 0) return Move.NOMOVE;
    return moveHistory[historyCounter - 1];
  }

  Piece[] getX88Board() {
    return x88Board;
  }

  boolean isCastlingWK() {
    return castlingWK;
  }

  boolean isCastlingWQ() {
    return castlingWQ;
  }

  boolean isCastlingBK() {
    return castlingBK;
  }

  boolean isCastlingBQ() {
    return castlingBQ;
  }

  SquareList[] getPawnSquares() {
    return pawnSquares;
  }

  SquareList[] getKnightSquares() {
    return knightSquares;
  }

  SquareList[] getBishopSquares() {
    return bishopSquares;
  }

  SquareList[] getRookSquares() {
    return rookSquares;
  }

  SquareList[] getQueenSquares() {
    return queenSquares;
  }

  Square[] getKingSquares() {
    return kingSquares;
  }

  Square getEnPassantSquare() {
    return enPassantSquare;
  }

  /**
   * @param fen
   */
  private void initBoard(String fen) {
    // clear board
    Arrays.fill(getX88Board(), Piece.NOPIECE);
    // setup board according to given FEN
    setupFromFEN(fen);
  }

  /**
   * Setup board according to given FEN
   * @param fen
   */
  private void setupFromFEN(String fen) {
    assert getZobristKey() == 0;

    if (fen.isEmpty()) throw new IllegalArgumentException("FEN Syntax not valid - empty string");

    String[] parts = fen.trim().split(" ");
    if (parts.length < 1) {
      throw new IllegalArgumentException(
        "FEN Syntax not valid - need at least two parts separated with space");
    }

    int i = 0;
    int rank = 8;
    int file = 1;
    String s = null;

    // pieces on squares
    for (i = 0; i < parts[0].length(); i++) {
      s = parts[0].substring(i, i + 1);
      if (s.matches("[pnbrqkPNBRQK]")) {
        if (s.toLowerCase().equals(s)) { // black
          switch (s) {
            case "p":
              putPiece(Square.getSquare(file, rank), Piece.BLACK_PAWN);
              break;
            case "n":
              putPiece(Square.getSquare(file, rank), Piece.BLACK_KNIGHT);
              break;
            case "b":
              putPiece(Square.getSquare(file, rank), Piece.BLACK_BISHOP);
              break;
            case "r":
              putPiece(Square.getSquare(file, rank), Piece.BLACK_ROOK);
              break;
            case "q":
              putPiece(Square.getSquare(file, rank), Piece.BLACK_QUEEN);
              break;
            case "k":
              putPiece(Square.getSquare(file, rank), Piece.BLACK_KING);
              break;
            default:
              throw new IllegalArgumentException("FEN Syntax not valid - expected a-hA-H");
          }
        } else if (s.toUpperCase().equals(s)) { // white
          switch (s) {
            case "P":
              putPiece(Square.getSquare(file, rank), Piece.WHITE_PAWN);
              break;
            case "N":
              putPiece(Square.getSquare(file, rank), Piece.WHITE_KNIGHT);
              break;
            case "B":
              putPiece(Square.getSquare(file, rank), Piece.WHITE_BISHOP);
              break;
            case "R":
              putPiece(Square.getSquare(file, rank), Piece.WHITE_ROOK);
              break;
            case "Q":
              putPiece(Square.getSquare(file, rank), Piece.WHITE_QUEEN);
              break;
            case "K":
              putPiece(Square.getSquare(file, rank), Piece.WHITE_KING);
              break;
            default:
              throw new IllegalArgumentException("FEN Syntax not valid - expected a-hA-H");
          }
        } else {
          throw new IllegalArgumentException("FEN Syntax not valid - expected a-hA-H");
        }
        file++;
      } else if (s.matches("[1-8]")) {
        int e = Integer.parseInt(s);
        file += e;
      } else if (s.equals("/")) {
        rank--;
        file = 1;
      } else {
        throw new IllegalArgumentException("FEN Syntax not valid - expected (1-9a-hA-H/)");
      }

      if (file > 9) {
        throw new IllegalArgumentException("FEN Syntax not valid - expected (1-9a-hA-H/)");
      }
    }

    // next player
    nextPlayer = null;
    if (parts.length >= 2) {
      s = parts[1];
      if (s.equals("w")) {
        nextPlayer = Color.WHITE;
      } else if (s.equals("b")) {
        nextPlayer = Color.BLACK;
        zobristKey =
          getZobristKey() ^ nextPlayer_Zobrist; // only when black to have the right in/out rhythm
      } else {
        throw new IllegalArgumentException("FEN Syntax not valid - expected w or b");
      }
    } else { // default "w"
      nextPlayer = Color.WHITE;
    }

    // castling
    // reset all castling first
    castlingWK = castlingWQ = castlingBK = castlingBQ = false;
    if (parts.length >= 3) { // default "-"
      for (i = 0; i < parts[2].length(); i++) {
        s = parts[2].substring(i, i + 1);
        switch (s) {
          case "K":
            castlingWK = true;
            zobristKey = getZobristKey() ^ castlingWK_Zobrist;
            break;
          case "Q":
            castlingWQ = true;
            zobristKey = getZobristKey() ^ castlingWQ_Zobrist;
            break;
          case "k":
            castlingBK = true;
            zobristKey = getZobristKey() ^ castlingBK_Zobrist;
            break;
          case "q":
            castlingBQ = true;
            zobristKey = getZobristKey() ^ castlingBQ_Zobrist;
            break;
          case "-":
          default:
        }
      }
    }

    // en passant - which filed and if null no en passant option
    if (parts.length >= 4) { // default "-"
      s = parts[3];
      if (!s.equals("-")) {
        enPassantSquare = Square.fromUCINotation(s);
        if (getEnPassantSquare().equals(Square.NOSQUARE)) {
          throw new IllegalArgumentException(
            "FEN Syntax not valid - expected valid en passant square");
        }
      }
    }
    // set en passant if not NOSQUARE
    if (getEnPassantSquare() != Square.NOSQUARE) {
      zobristKey = getZobristKey() ^ enPassantSquare_Zobrist[getEnPassantSquare().ordinal()]; // in
    }

    // half move clock
    if (parts.length >= 5) { // default "0"
      s = parts[4];
      halfMoveClock = Integer.parseInt(s);
    } else {
      halfMoveClock = 0;
    }

    // full move number - mapping to half move number
    if (parts.length >= 6) { // default "1"
      s = parts[5];
      nextHalfMoveNumber = (2 * Integer.parseInt(s));
      if (nextHalfMoveNumber == 0) nextHalfMoveNumber = 2;
    } else {
      nextHalfMoveNumber = 2;
    }
    if (getNextPlayer().isWhite()) nextHalfMoveNumber--;

    // double check correct numbering
    assert ((getNextPlayer().isWhite() && nextHalfMoveNumber % 2 == 1) ||
            getNextPlayer().isBlack() && nextHalfMoveNumber % 2 == 0);
  }

  @Override
  public String toString() {
    return toBoardString();
  }

  /**
   * Returns a String representation the chess position of this OmegaBoardPoistion as a FEN String-
   *
   * @return FEN String of this position
   */
  public String toFENString() {

    String fen = "";

    for (int rank = 8; rank >= 1; rank--) {
      int emptySquares = 0;
      for (int file = 1; file <= 8; file++) {

        Piece piece = getX88Board()[Square.getSquare(file, rank).ordinal()];

        if (piece == Piece.NOPIECE) {
          emptySquares++;
        } else {
          if (emptySquares > 0) {
            fen += emptySquares;
            emptySquares = 0;
          }
          if (piece.getColor().isWhite()) {
            fen += piece.toString();
          } else {
            fen += piece.toString();
          }
        }
      }
      if (emptySquares > 0) {
        fen += emptySquares;
      }
      if (rank > 1) {
        fen += '/';
      }
    }
    fen += ' ';

    // Color
    fen += this.getNextPlayer().toChar();
    fen += ' ';

    // Castling
    boolean castlingAvailable = false;
    if (isCastlingWK()) {
      castlingAvailable = true;
      fen += "K";
    }
    if (isCastlingWQ()) {
      castlingAvailable = true;
      fen += "Q";
    }
    if (isCastlingBK()) {
      castlingAvailable = true;
      fen += "k";
    }
    if (isCastlingBQ()) {
      castlingAvailable = true;
      fen += "q";
    }
    if (!castlingAvailable) {
      fen += '-';
    }
    fen += ' ';

    // En passant
    if (this.getEnPassantSquare() != Square.NOSQUARE) {
      fen += getEnPassantSquare().toString();
    } else {
      fen += '-';
    }
    fen += ' ';

    // Half move clock
    fen += this.halfMoveClock;
    fen += ' ';

    // Full move number
    fen += (nextHalfMoveNumber + 1) / 2;

    return fen;
  }

  /**
   * Returns a visual board string for use in a console. Adds FEN String at the end.
   *
   * @return String of visual board for use in console
   */
  public String toBoardString() {

    StringBuilder boardString = new StringBuilder();

    // backwards as highest row is on top
    for (int rank = 8; rank >= 1; rank--) {

      // upper border
      boardString.append("    ---------------------------------\n");

      // rank number
      boardString.append(' ').append(Integer.toString(rank)).append(": |");

      // fields
      for (int file = 1; file <= 8; file++) {
        Piece p = getX88Board()[Square.getSquare(file, rank).ordinal()];
        if (p == Piece.NOPIECE) {
          boardString.append("   |");
        } else {
          boardString.append(" ").append(p.toString()).append(" |");
        }
      }
      boardString.append("\n");
    }

    // lower border
    boardString.append("    ---------------------------------\n");

    // file letters
    boardString.append("     "); // 4 * space
    for (int file = 1; file <= 8; file++) {
      boardString.append(' ').append(Square.File.get(file).toString().toUpperCase()).append("  ");
    }
    boardString.append("\n\n");

    boardString.append(toFENString());

    return boardString.toString();
  }

  /**
   * @see Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (this.getZobristKey() ^ (this.getZobristKey() >>> 32));
    return result;
  }

  /**
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Position)) {
      return false;
    }
    Position other = (Position) obj;
    if (this.getZobristKey() != other.getZobristKey()) {
      return false;
    }
    return true;
  }
}
