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

import java.util.Arrays;
import java.util.Random;

/**
 * This class represents the Omega board and its position.<br>
 * It uses a x88 board, a stack for undo moves, zobrist keys for transposition tables, piece lists,
 * material counter.<br>
 * Can be created with any FEN notation and also from GameBoards or as a copy from another
 * BoardPosition.
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
public class BoardPosition {

  public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /* Size of 0x88 board */
  private static final int BOARDSIZE = 128;

  /* Max History */
  private static final int MAX_HISTORY = 255;

  /* Standard Board Setup as FEN */
  private static final String STANDARD_BOARD_FEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /* random generator for use with zobrist hash keys */
  private static final Random random = new Random(0);

  /*
   * The zobrist key to use as a hash key in transposition tables
   * The zobrist key will be updated incrementally every time one of the the state variables change.
   */
  long _zobristKey = 0;
  long[] _zobristKey_History = new long[MAX_HISTORY];

  // history counter
  int _historyCounter = 0;

  // **********************************************************
  // Board State START ----------------------------------------
  // unique chess position
  //
  // 0x88 Board
  Piece[] _x88Board = new Piece[BOARDSIZE];
  // hash for pieces - piece, board
  static final long[][] _piece_Zobrist =
      new long[Piece.values.length][Square.values.length];

  // Castling rights
  boolean _castlingWK = true;
  boolean[] _castlingWK_history = new boolean[MAX_HISTORY];
  static final long _castlingWK_Zobrist;
  boolean _castlingWQ = true;
  boolean[] _castlingWQ_history = new boolean[MAX_HISTORY];
  static final long _castlingWQ_Zobrist;
  boolean _castlingBK = true;
  boolean[] _castlingBK_history = new boolean[MAX_HISTORY];
  static final long _castlingBK_Zobrist;
  boolean _castlingBQ = true;
  boolean[] _castlingBQ_history = new boolean[MAX_HISTORY];
  static final long _castlingBQ_Zobrist;

  // en passant field - if NOSQUARE then we do not have an en passant option
  Square   _enPassantSquare         = Square.NOSQUARE;
  Square[] _enPassantSquare_History = new Square[MAX_HISTORY];
  // hash for castling rights
  static final long[] _enPassantSquare_Zobrist = new long[Square.values.length];

  // half move clock - number of half moves since last capture
  int _halfMoveClock = 0;
  int[] _halfMoveClock_History = new int[MAX_HISTORY];
  // has no zobrist key

  // next player color
  Color _nextPlayer = Color.WHITE;
  // hash for next player
  static final long _nextPlayer_Zobrist;
  //
  // Board State END ------------------------------------------
  // **********************************************************

  // **********************************************************
  // Extended Board State ----------------------------------
  // not necessary for a unique position

  // we can recreate the board through the last move - no need for history of board itself
  int[] _moveHistory = new int[MAX_HISTORY];

  // half move number - the actual half move number to determine the full move number
  int _nextHalfMoveNumber = 1;

  /** Lists for all pieces */
  final SquareList[] _pawnSquares = new SquareList[Color.values.length];

  final SquareList[] _knightSquares = new SquareList[Color.values.length];
  final SquareList[] _bishopSquares = new SquareList[Color.values.length];
  final SquareList[] _rookSquares   = new SquareList[Color.values.length];
  final SquareList[] _queenSquares  = new SquareList[Color.values.length];
  final Square[]     _kingSquares   = new Square[Color.values.length];

  // Material value will always be up to date
  int[] _material;

  // caches a hasCheck and hasMate Flag for the current position. Will be set after
  // a call to hasCheck() and reset to TBD every time a move is made or unmade.
  private Flag _hasCheck = Flag.TBD;
  Flag[] _hasCheckFlag_History = new Flag[MAX_HISTORY];
  private Flag _hasMate = Flag.TBD;
  Flag[] _hasMateFlag_History = new Flag[MAX_HISTORY];

  private final MoveGenerator _mateCheckMG = new MoveGenerator();

  private enum Flag {
    TBD,
    TRUE,
    FALSE
  }

  // **********************************************************
  // static initialization
  static {
    // all pieces on all squares
    for (Piece p : Piece.values) {
      for (Square s : Square.values) {
        _piece_Zobrist[p.ordinal()][s.ordinal()] = Math.abs(random.nextLong());
      }
    }
    // all castling combinations
    _castlingWK_Zobrist = Math.abs(random.nextLong());
    _castlingWQ_Zobrist = Math.abs(random.nextLong());
    _castlingBK_Zobrist = Math.abs(random.nextLong());
    _castlingBQ_Zobrist = Math.abs(random.nextLong());

    // all possible positions of the en passant square (easiest to use all fields and not just the
    // ones where en passant is indeed possible)
    for (Square s : Square.values) {
      _enPassantSquare_Zobrist[s.ordinal()] = Math.abs(random.nextLong());
    }
    // set or unset this for the two color options
    _nextPlayer_Zobrist = Math.abs(random.nextLong());
  }

  // Constructors START -----------------------------------------

  /** Creates a standard Chessly board and initializes it with standard chess setup. */
  public BoardPosition() {
    this(STANDARD_BOARD_FEN);
  }

  /**
   * Creates a standard Chessly board and initializes it with a fen position
   *
   * @param fen
   */
  public BoardPosition(String fen) {
    initializeLists();
    initBoard(fen);
  }

  /**
   * Copy constructor - creates a copy of the given BoardPosition
   *
   * @param op
   */
  public BoardPosition(BoardPosition op) {
    if (op == null) throw new NullPointerException("Parameter op may not be null");

    // x88 board
    System.arraycopy(op._x88Board, 0, this._x88Board, 0, op._x88Board.length);

    // game state
    this._nextHalfMoveNumber = op._nextHalfMoveNumber;
    this._nextPlayer = op._nextPlayer;
    this._zobristKey = op._zobristKey;

    this._castlingWK = op._castlingWK;
    this._castlingWQ = op._castlingWQ;
    this._castlingBK = op._castlingBK;
    this._castlingBQ = op._castlingBQ;
    this._enPassantSquare = op._enPassantSquare;
    this._halfMoveClock = op._halfMoveClock;

    this._hasCheck = op._hasCheck;
    this._hasMate = op._hasMate;

    // history
    this._historyCounter = op._historyCounter;
    System.arraycopy(op._zobristKey_History, 0, _zobristKey_History, 0, _zobristKey_History.length);

    System.arraycopy(op._castlingWK_history, 0, _castlingWK_history, 0, _castlingWK_history.length);
    System.arraycopy(op._castlingWQ_history, 0, _castlingWQ_history, 0, _castlingWQ_history.length);
    System.arraycopy(op._castlingBK_history, 0, _castlingBK_history, 0, _castlingBK_history.length);
    System.arraycopy(op._castlingBQ_history, 0, _castlingBQ_history, 0, _castlingBQ_history.length);
    System.arraycopy(
        op._enPassantSquare_History,
        0,
        _enPassantSquare_History,
        0,
        _enPassantSquare_History.length);
    System.arraycopy(
        op._halfMoveClock_History, 0, _halfMoveClock_History, 0, _halfMoveClock_History.length);

    System.arraycopy(
        op._hasCheckFlag_History, 0, _hasCheckFlag_History, 0, _hasCheckFlag_History.length);
    System.arraycopy(
        op._hasMateFlag_History, 0, _hasMateFlag_History, 0, _hasMateFlag_History.length);

    // move history
    System.arraycopy(op._moveHistory, 0, _moveHistory, 0, op._moveHistory.length);

    // initializeLists();
    // copy piece lists
    for (int i = 0; i <= 1; i++) { // foreach color
      this._pawnSquares[i] = op._pawnSquares[i].clone();
      this._knightSquares[i] = op._knightSquares[i].clone();
      this._bishopSquares[i] = op._bishopSquares[i].clone();
      this._rookSquares[i] = op._rookSquares[i].clone();
      this._queenSquares[i] = op._queenSquares[i].clone();
      this._kingSquares[i] = op._kingSquares[i];
    }
    _material = new int[2];
    this._material[0] = op._material[0];
    this._material[1] = op._material[1];
  }

  /**
   * Copy constructor from GameBoard - creates a equivalent BoardPosition from the give
   * GameBoard
   *
   * @param oldBoard
   */
  // public BoardPosition(GameBoard oldBoard) {
  //        this(oldBoard.toFENString());
  //    }

  /** Initialize the lists for the pieces and the material counter */
  private void initializeLists() {
    for (int i = 0; i <= 1; i++) { // foreach color
      _pawnSquares[i] = new SquareList();
      _knightSquares[i] = new SquareList();
      _bishopSquares[i] = new SquareList();
      _rookSquares[i] = new SquareList();
      _queenSquares[i] = new SquareList();
      _kingSquares[i] = Square.NOSQUARE;
    }
    _material = new int[2];
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
    _moveHistory[_historyCounter] = move;

    _castlingWK_history[_historyCounter] = _castlingWK;
    _castlingWQ_history[_historyCounter] = _castlingWQ;
    _castlingBK_history[_historyCounter] = _castlingBK;
    _castlingBQ_history[_historyCounter] = _castlingBQ;
    _enPassantSquare_History[_historyCounter] = _enPassantSquare;
    _halfMoveClock_History[_historyCounter] = _halfMoveClock;
    _zobristKey_History[_historyCounter] = _zobristKey;
    _hasCheckFlag_History[_historyCounter] = _hasCheck;
    _hasMateFlag_History[_historyCounter] = _hasMate;
    _historyCounter++;

    // reset check and mate flag
    _hasCheck = Flag.TBD;
    _hasMate = Flag.TBD;

    // make move
    switch (Move.getMoveType(move)) {
      case NORMAL:
        invalidateCastlingRights(fromSquare, toSquare);
        makeNormalMove(fromSquare, toSquare, piece, target);
        // clear en passant
        if (_enPassantSquare != Square.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = Square.NOSQUARE;
        }
        break;
      case PAWNDOUBLE:
        assert fromSquare.isPawnBaseRow(piece.getColor());
        assert !piece.getColor().isNone();
        movePiece(fromSquare, toSquare, piece);
        // clear old en passant
        if (_enPassantSquare != Square.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // in
        }
        // set new en passant target field - always one "behind" the toSquare
        _enPassantSquare = piece.getColor().isWhite() ? toSquare.getSouth() : toSquare.getNorth();
        _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // in
        _halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case ENPASSANT:
        assert target != Piece.NOPIECE;
        assert target.getType() == PieceType.PAWN;
        assert !target.getColor().isNone();
        Square targetSquare =
            target.getColor().isWhite() ? toSquare.getNorth() : toSquare.getSouth();
        removePiece(targetSquare, target);
        movePiece(fromSquare, toSquare, piece);
        // clear en passant
        if (_enPassantSquare != Square.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = Square.NOSQUARE;
        }
        _halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case CASTLING:
        makeCastlingMove(fromSquare, toSquare, piece);
        // clear en passant
        if (_enPassantSquare != Square.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = Square.NOSQUARE;
        }
        _halfMoveClock++;
        break;
      case PROMOTION:
        if (target != Piece.NOPIECE) removePiece(toSquare, target);
        invalidateCastlingRights(fromSquare, toSquare);
        removePiece(fromSquare, piece);
        putPiece(toSquare, promotion);
        // clear en passant
        if (_enPassantSquare != Square.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = Square.NOSQUARE;
        }
        _halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case NOMOVETYPE:
      default:
        throw new IllegalArgumentException();
    }

    // update halfMoveNumber
    _nextHalfMoveNumber++;

    // change color (active player)
    _nextPlayer = _nextPlayer.getInverseColor();
    _zobristKey ^= _nextPlayer_Zobrist;
  }

  /** Takes back the last move from the board */
  public void undoMove() {
    // Get state for undoMove
    _historyCounter--;

    int move = _moveHistory[_historyCounter];

    // reset move history
    _moveHistory[_historyCounter] = Move.NOMOVE;

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
    _castlingWK = _castlingWK_history[_historyCounter];
    _castlingWQ = _castlingWQ_history[_historyCounter];
    _castlingBK = _castlingBK_history[_historyCounter];
    _castlingBQ = _castlingBQ_history[_historyCounter];

    // restore en passant square
    _enPassantSquare = _enPassantSquare_History[_historyCounter];

    // restore halfMoveClock
    _halfMoveClock = _halfMoveClock_History[_historyCounter];

    // decrease _halfMoveNumber
    _nextHalfMoveNumber--;

    // change back color
    _nextPlayer = _nextPlayer.getInverseColor();

    // zobristKey - just overwrite - should be the same as before the move
    _zobristKey = _zobristKey_History[_historyCounter];

    // get the check and mate flag from history
    _hasCheck = _hasCheckFlag_History[_historyCounter];
    _hasMate = _hasMateFlag_History[_historyCounter];
  }

  /**
   * @param fromSquare
   * @param toSquare
   * @param piece
   * @param target
   */
  private void makeNormalMove(
    Square fromSquare, Square toSquare, Piece piece, Piece target) {

    if (target != Piece.NOPIECE) {
      removePiece(toSquare, target);
      _halfMoveClock = 0; // reset half move clock because of capture
    } else if (piece.getType() == PieceType.PAWN) {
      _halfMoveClock = 0; // reset half move clock because of pawn move
    } else {
      _halfMoveClock++;
    }

    movePiece(fromSquare, toSquare, piece);
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
      if (_castlingWK) _zobristKey ^= _castlingWK_Zobrist;
      _castlingWK = false;
      if (_castlingWQ) _zobristKey ^= _castlingWQ_Zobrist;
      _castlingWQ = false;
    }
    if (fromSquare == Square.e8 || toSquare == Square.e8) {
      // only take out zobrist if the castling was true before.
      if (_castlingBK) _zobristKey ^= _castlingBK_Zobrist;
      _castlingBK = false;
      if (_castlingBQ) _zobristKey ^= _castlingBQ_Zobrist;
      _castlingBQ = false;
    }
    if (fromSquare == Square.a1 || toSquare == Square.a1) {
      if (_castlingWQ) _zobristKey ^= _castlingWQ_Zobrist;
      _castlingWQ = false;
    }
    if (fromSquare == Square.h1 || toSquare == Square.h1) {
      if (_castlingWK) _zobristKey ^= _castlingWK_Zobrist;
      _castlingWK = false;
    }
    if (fromSquare == Square.a8 || toSquare == Square.a8) {
      if (_castlingBQ) _zobristKey ^= _castlingBQ_Zobrist;
      _castlingBQ = false;
    }
    if (fromSquare == Square.h8 || toSquare == Square.h8) {
      if (_castlingBK) _zobristKey ^= _castlingBK_Zobrist;
      _castlingBK = false;
    }
  }

  /**
   * @param fromSquare
   * @param toSquare
   * @param piece
   */
  private void makeCastlingMove(Square fromSquare, Square toSquare, Piece piece) {
    assert piece.getType() == PieceType.KING;

    Piece rook = Piece.NOPIECE;
    Square rookFromSquare = Square.NOSQUARE;
    Square rookToSquare = Square.NOSQUARE;

    switch (toSquare) {
      case g1: // white kingside
        assert (_castlingWK);
        rook = Piece.WHITE_ROOK;
        rookFromSquare = Square.h1;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = Square.f1;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case c1: // white queenside
        assert (_castlingWQ);
        rook = Piece.WHITE_ROOK;
        rookFromSquare = Square.a1;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = Square.d1;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case g8: // black kingside
        assert (_castlingBK);
        rook = Piece.BLACK_ROOK;
        rookFromSquare = Square.h8;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = Square.f8;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case c8: // black queenside
        assert (_castlingBQ);
        rook = Piece.BLACK_ROOK;
        rookFromSquare = Square.a8;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
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
    // update castling rights
    Piece rook = Piece.NOPIECE;
    Square rookFromSquare = Square.NOSQUARE;
    Square rookToSquare = Square.NOSQUARE;

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

  /** Makes a null move. Essentially switches sides within same position. */
  public void makeNullMove() {

    // Save state for undoMove
    _castlingWK_history[_historyCounter] = _castlingWK;
    _castlingWQ_history[_historyCounter] = _castlingWQ;
    _castlingBK_history[_historyCounter] = _castlingBK;
    _castlingBQ_history[_historyCounter] = _castlingBQ;
    _enPassantSquare_History[_historyCounter] = _enPassantSquare;
    _halfMoveClock_History[_historyCounter] = _halfMoveClock;
    _zobristKey_History[_historyCounter] = _zobristKey;
    _hasCheckFlag_History[_historyCounter] = _hasCheck;
    _hasMateFlag_History[_historyCounter] = _hasMate;
    _historyCounter++;

    // reset check and mate flag
    _hasCheck = Flag.TBD;
    _hasMate = Flag.TBD;

    // clear en passant
    if (_enPassantSquare != Square.NOSQUARE) {
      _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
      _enPassantSquare = Square.NOSQUARE;
    }

    // increase half move clock
    _halfMoveClock++;

    // increase halfMoveNumber
    _nextHalfMoveNumber++;

    // change color (active player)
    _nextPlayer = _nextPlayer.getInverseColor();
    _zobristKey ^= _nextPlayer_Zobrist;
  }

  /** Undo a null move. Essentially switches back sides within same position. */
  public void undoNullMove() {
    // Get state for undoMove
    _historyCounter--;

    // restore castling rights
    _castlingWK = _castlingWK_history[_historyCounter];
    _castlingWQ = _castlingWQ_history[_historyCounter];
    _castlingBK = _castlingBK_history[_historyCounter];
    _castlingBQ = _castlingBQ_history[_historyCounter];

    // restore en passant square
    _enPassantSquare = _enPassantSquare_History[_historyCounter];

    // restore halfMoveClock
    _halfMoveClock = _halfMoveClock_History[_historyCounter];

    // decrease _halfMoveNumber
    _nextHalfMoveNumber--;

    // change back color
    _nextPlayer = _nextPlayer.getInverseColor();

    // zobristKey - just overwrite - should be the same as before the move
    _zobristKey = _zobristKey_History[_historyCounter];

    // get the check and mate flag from history
    _hasCheck = _hasCheckFlag_History[_historyCounter];
    _hasMate = _hasMateFlag_History[_historyCounter];
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
    assert (_x88Board[fromSquare.ordinal()] == piece) // check if moved piece is indeed there
        : "piece to move not there";
    assert (_x88Board[toSquare.ordinal()] == Piece.NOPIECE) // // should be empty
        : "to square should be empty";
    // due to performance we do not call remove and put
    // no need to update counters when moving
    // remove
    _x88Board[fromSquare.ordinal()] = Piece.NOPIECE;
    _zobristKey ^= _piece_Zobrist[piece.ordinal()][fromSquare.ordinal()]; // out
    // update piece lists
    final int color = piece.getColor().ordinal();
    removeFromPieceLists(fromSquare, piece, color);
    // put
    _x88Board[toSquare.ordinal()] = piece;
    _zobristKey ^= _piece_Zobrist[piece.ordinal()][toSquare.ordinal()]; // in
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
    assert _x88Board[square.ordinal()] == Piece.NOPIECE; // should be empty
    // put
    _x88Board[square.ordinal()] = piece;
    _zobristKey ^= _piece_Zobrist[piece.ordinal()][square.ordinal()]; // in
    // update piece lists
    final int color = piece.getColor().ordinal();
    addToPieceLists(square, piece, color);
    // update material
    _material[color] += piece.getType().getValue();
  }

  /** @return the removed piece */
  private Piece removePiece(Square square, Piece piece) {
    assert square.isValidSquare();
    assert piece != Piece.NOPIECE;
    assert _x88Board[square.ordinal()] == piece // check if removed piece is indeed there
        : "piece to be removed not there";
    // remove
    Piece old = _x88Board[square.ordinal()];
    _x88Board[square.ordinal()] = Piece.NOPIECE;
    _zobristKey ^= _piece_Zobrist[piece.ordinal()][square.ordinal()]; // out
    // update piece lists
    final int color = piece.getColor().ordinal();
    removeFromPieceLists(square, piece, color);
    // update material
    _material[color] -= piece.getType().getValue();
    // return the remove piece
    return old;
  }

  /**
   * @param toSquare
   * @param piece
   * @param color
   */
  private void addToPieceLists(Square toSquare, Piece piece, final int color) {
    switch (piece.getType()) {
      case PAWN:
        _pawnSquares[color].add(toSquare);
        break;
      case KNIGHT:
        _knightSquares[color].add(toSquare);
        break;
      case BISHOP:
        _bishopSquares[color].add(toSquare);
        break;
      case ROOK:
        _rookSquares[color].add(toSquare);
        break;
      case QUEEN:
        _queenSquares[color].add(toSquare);
        break;
      case KING:
        _kingSquares[color] = toSquare;
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
        _pawnSquares[color].remove(fromSquare);
        break;
      case KNIGHT:
        _knightSquares[color].remove(fromSquare);
        break;
      case BISHOP:
        _bishopSquares[color].remove(fromSquare);
        break;
      case ROOK:
        _rookSquares[color].remove(fromSquare);
        break;
      case QUEEN:
        _queenSquares[color].remove(fromSquare);
        break;
      case KING:
        _kingSquares[color] = Square.NOSQUARE;
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
   * @param kingPosition
   * @return true if under attack
   */
  public boolean isAttacked(Color attackerColor, Square kingPosition) {
    assert (kingPosition != Square.NOSQUARE);
    assert (!attackerColor.isNone());

    final int os_Index = kingPosition.ordinal();
    final boolean isWhite = attackerColor.isWhite();

    /*
     * Checks are ordered for likelihood to return from this as fast as possible
     */

    // check pawns
    // reverse direction to look for pawns which could attack
    final int pawnDir = isWhite ? -1 : 1;
    final Piece attackerPawn = isWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
    for (int d : Square.pawnAttackDirections) {
      final int i = os_Index + d * pawnDir;
      if ((i & 0x88) == 0 && _x88Board[i] == attackerPawn) return true;
    }

    final int attackerColorIndex = attackerColor.ordinal();

    // check sliding horizontal (rook + queen) if there are any
    if (!(_rookSquares[attackerColorIndex].isEmpty()
        && _queenSquares[attackerColorIndex].isEmpty())) {
      for (int d : Square.rookDirections) {
        int i = os_Index + d;
        while ((i & 0x88) == 0) { // slide while valid square
          if (_x88Board[i] != Piece.NOPIECE) { // not empty
            if (_x88Board[i].getColor() == attackerColor // attacker piece
                && (_x88Board[i].getType() == PieceType.ROOK
                    || _x88Board[i].getType() == PieceType.QUEEN)) {
              return true;
            }
            break; // not an attacker color or attacker piece blocking the way.
          }
          i += d; // next sliding field in this direction
        }
      }
    }

    // check sliding diagonal (bishop + queen) if there are any
    if (!(_bishopSquares[attackerColorIndex].isEmpty()
        && _queenSquares[attackerColorIndex].isEmpty())) {
      for (int d : Square.bishopDirections) {
        int i = os_Index + d;
        while ((i & 0x88) == 0) { // slide while valid square
          if (_x88Board[i] != Piece.NOPIECE) { // not empty
            if (_x88Board[i].getColor() == attackerColor // attacker piece
                && (_x88Board[i].getType() == PieceType.BISHOP
                    || _x88Board[i].getType() == PieceType.QUEEN)) {
              return true;
            }
            break; // not an attacker color or attacker piece blocking the way.
          }
          i += d; // next sliding field in this direction
        }
      }
    }

    // check knights if there are any
    if (!(_knightSquares[attackerColorIndex].isEmpty())) {
      for (int d : Square.knightDirections) {
        int i = os_Index + d;
        if ((i & 0x88) == 0) { // valid square
          if (_x88Board[i] != Piece.NOPIECE // not empty
              && _x88Board[i].getColor() == attackerColor // attacker piece
              && (_x88Board[i].getType() == PieceType.KNIGHT)) {
            return true;
          }
        }
      }
    }

    // check king
    for (int d : Square.kingDirections) {
      int i = os_Index + d;
      if ((i & 0x88) == 0) { // valid square
        if (_x88Board[i] != Piece.NOPIECE // not empty
            && _x88Board[i].getColor() == attackerColor // attacker piece
            && (_x88Board[i].getType() == PieceType.KING)) {
          return true;
        }
      }
    }

    // check en passant
    if (this._enPassantSquare != Square.NOSQUARE) {
      if (isWhite // white is attacker
          && _x88Board[_enPassantSquare.getSouth().ordinal()]
             == Piece.BLACK_PAWN // black is target
          && this._enPassantSquare.getSouth()
              == kingPosition) { // this is indeed the en passant attacked square
        // left
        int i = os_Index + Square.W;
        if ((i & 0x88) == 0 && _x88Board[i] == Piece.WHITE_PAWN) return true;
        // right
        i = os_Index + Square.E;
        if ((i & 0x88) == 0 && _x88Board[i] == Piece.WHITE_PAWN) return true;
      } else if (!isWhite // black is attacker (assume not noColor)
                 && _x88Board[_enPassantSquare.getNorth().ordinal()]
                    == Piece.WHITE_PAWN // white is target
                 && this._enPassantSquare.getNorth()
              == kingPosition) { // this is indeed the en passant attacked square
        // attack from left
        int i = os_Index + Square.W;
        if ((i & 0x88) == 0 && _x88Board[i] == Piece.BLACK_PAWN) return true;
        // attack from right
        i = os_Index + Square.E;
        if ((i & 0x88) == 0 && _x88Board[i] == Piece.BLACK_PAWN) return true;
      }
    }

    return false;
  }

  /** @return true if current position has check for next player */
  public boolean hasCheck() {
    if (_hasCheck != Flag.TBD) return _hasCheck == Flag.TRUE ? true : false;
    boolean check = isAttacked(_nextPlayer.getInverseColor(), _kingSquares[_nextPlayer.ordinal()]);
    _hasCheck = check ? Flag.TRUE : Flag.FALSE;
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
    if (_hasMate != Flag.TBD) return _hasMate == Flag.TRUE ? true : false;
    if (!_mateCheckMG.hasLegalMove(this)) {
      _hasMate = Flag.TRUE;
      return true;
    }
    _hasMate = Flag.FALSE;
    return false;
  }

  /**
   * The fifty-move rule if during the previous 50 moves no pawn has been moved and no capture has
   * been made, either player may claim a draw.
   *
   * @return true if during the previous 50 moves no pawn has been moved and no capture has been
   *     made
   */
  public boolean check50Moves() {
    return this._halfMoveClock >= 100;
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

    if (_historyCounter < 8) return false;
    int counter = 0;
    int i = _historyCounter - 4;
    while (i >= 0) {
      if (_zobristKey == _zobristKey_History[i]) counter++;
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
    if (_pawnSquares[Color.WHITE.ordinal()].size() == 0
        && _pawnSquares[Color.BLACK.ordinal()].size() == 0
        && _rookSquares[Color.WHITE.ordinal()].size() == 0
        && _rookSquares[Color.BLACK.ordinal()].size() == 0
        && _queenSquares[Color.WHITE.ordinal()].size() == 0
        && _queenSquares[Color.BLACK.ordinal()].size() == 0) {

      // white king bare KK*
      if (_knightSquares[Color.WHITE.ordinal()].size() == 0
          && _bishopSquares[Color.WHITE.ordinal()].size() == 0) {

        // both kings bare KK, KKN, KKNN
        if (_knightSquares[Color.BLACK.ordinal()].size() <= 2
            && _bishopSquares[Color.BLACK.ordinal()].size() == 0) {
          return true;
        }

        // KKB
        if (_knightSquares[Color.BLACK.ordinal()].size() == 0
            && _bishopSquares[Color.BLACK.ordinal()].size() == 1) {
          return true;
        }

      }
      // only black king bare K*K
      else if (_knightSquares[Color.BLACK.ordinal()].size() == 0
          && _bishopSquares[Color.BLACK.ordinal()].size() == 0) {

        // both kings bare KK, KNK, KNNK
        if (_knightSquares[Color.WHITE.ordinal()].size() <= 2
            && _bishopSquares[Color.WHITE.ordinal()].size() == 0) {
          return true;
        }

        // KBK
        if (_knightSquares[Color.BLACK.ordinal()].size() == 0
            && _bishopSquares[Color.BLACK.ordinal()].size() == 1) {
          return true;
        }
      }

      // KBKB - B same field color
      else if (_knightSquares[Color.BLACK.ordinal()].size() == 0
          && _bishopSquares[Color.BLACK.ordinal()].size() == 1
          && _knightSquares[Color.WHITE.ordinal()].size() == 0
          && _bishopSquares[Color.WHITE.ordinal()].size() == 1) {

        /*
         * Bishops are on the same field color if the sum of the
         * rank and file of the fields are on both even or both odd :
         * (file + rank) % 2 == 0 = black field
         * (file + rank) % 2 == 1 = white field
         */
        final Square whiteBishop = _bishopSquares[Color.WHITE.ordinal()].get(0);
        int file_w = whiteBishop.getFile().get();
        int rank_w = whiteBishop.getRank().get();
        final Square blackBishop = _bishopSquares[Color.BLACK.ordinal()].get(0);
        int file_b = blackBishop.getFile().get();
        int rank_b = blackBishop.getRank().get();
        if (((file_w + rank_w) % 2) == ((file_b + rank_b) % 2)) {
          return true;
        }
      }
    }
    return false;
  }

  /** @return the zobristKey */
  public long getZobristKey() {
    return this._zobristKey;
  }

  /**
   * @param c Color
   * @return the material value
   */
  public int getMaterial(Color c) {
    return this._material[c.ordinal()];
  }

  /** @return color of next player */
  public Color getNextPlayer() {
    return _nextPlayer;
  }

  /**
   * Returns the last move. Returns Move.NOMOVE if there is no last move.
   *
   * @return int representing a move
   */
  public int getLastMove() {
    if (_historyCounter == 0) return Move.NOMOVE;
    return _moveHistory[_historyCounter - 1];
  }

  /** @param fen */
  private void initBoard(String fen) {
    // clear board
    Arrays.fill(_x88Board, Piece.NOPIECE);
    // Standard Start Board
    setupFromFEN(fen);
    // used for debugging
    // setupFromFEN("8/1P6/6k1/8/8/8/p1K5/8 w - - 0 1");
  }

  /** @param fen */
  private void setupFromFEN(String fen) {
    assert _zobristKey == 0;

    if (fen.isEmpty()) throw new IllegalArgumentException("FEN Syntax not valid - empty string");

    String[] parts = fen.trim().split(" ");
    if (parts.length < 1)
      throw new IllegalArgumentException(
          "FEN Syntax not valid - need at least two parts separated with space");

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
        } else throw new IllegalArgumentException("FEN Syntax not valid - expected a-hA-H");
        file++;
      } else if (s.matches("[1-8]")) {
        int e = Integer.parseInt(s);
        file += e;
      } else if (s.equals("/")) {
        rank--;
        file = 1;
      } else throw new IllegalArgumentException("FEN Syntax not valid - expected (1-9a-hA-H/)");

      if (file > 9) {
        throw new IllegalArgumentException("FEN Syntax not valid - expected (1-9a-hA-H/)");
      }
    }

    // next player
    _nextPlayer = null;
    if (parts.length >= 2) {
      s = parts[1];
      if (s.equals("w")) _nextPlayer = Color.WHITE;
      else if (s.equals("b")) {
        _nextPlayer = Color.BLACK;
        _zobristKey ^= _nextPlayer_Zobrist; // only when black to have the right in/out rhythm
      } else throw new IllegalArgumentException("FEN Syntax not valid - expected w or b");
    } else { // default "w"
      _nextPlayer = Color.WHITE;
    }

    // castling
    // reset all castling first
    _castlingWK = _castlingWQ = _castlingBK = _castlingBQ = false;
    if (parts.length >= 3) { // default "-"
      for (i = 0; i < parts[2].length(); i++) {
        s = parts[2].substring(i, i + 1);
        switch (s) {
          case "K":
            _castlingWK = true;
            _zobristKey ^= _castlingWK_Zobrist;
            break;
          case "Q":
            _castlingWQ = true;
            _zobristKey ^= _castlingWQ_Zobrist;
            break;
          case "k":
            _castlingBK = true;
            _zobristKey ^= _castlingBK_Zobrist;
            break;
          case "q":
            _castlingBQ = true;
            _zobristKey ^= _castlingBQ_Zobrist;
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
        _enPassantSquare = Square.fromUCINotation(s);
        if (_enPassantSquare.equals(Square.NOSQUARE)) {
          throw new IllegalArgumentException(
              "FEN Syntax not valid - expected valid en passant square");
        }
      }
    }
    // set en passant if not NOSQUARE
    if (_enPassantSquare != Square.NOSQUARE) {
      _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // in
    }

    // half move clock
    if (parts.length >= 5) { // default "0"
      s = parts[4];
      _halfMoveClock = Integer.parseInt(s);
    } else {
      _halfMoveClock = 0;
    }

    // full move number - mapping to half move number
    if (parts.length >= 6) { // default "1"
      s = parts[5];
      _nextHalfMoveNumber = (2 * Integer.parseInt(s));
      if (_nextHalfMoveNumber == 0) _nextHalfMoveNumber = 2;
    } else {
      _nextHalfMoveNumber = 2;
    }
    if (_nextPlayer.isWhite()) _nextHalfMoveNumber--;

    // double check correct numbering
    assert ((_nextPlayer.isWhite() && _nextHalfMoveNumber % 2 == 1)
        || _nextPlayer.isBlack() && _nextHalfMoveNumber % 2 == 0);
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

        Piece piece = _x88Board[Square.getSquare(file, rank).ordinal()];

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
    fen += this._nextPlayer.toChar();
    fen += ' ';

    // Castling
    boolean castlingAvailable = false;
    if (_castlingWK) {
      castlingAvailable = true;
      fen += "K";
    }
    if (_castlingWQ) {
      castlingAvailable = true;
      fen += "Q";
    }
    if (_castlingBK) {
      castlingAvailable = true;
      fen += "k";
    }
    if (_castlingBQ) {
      castlingAvailable = true;
      fen += "q";
    }
    if (!castlingAvailable) {
      fen += '-';
    }
    fen += ' ';

    // En passant
    if (this._enPassantSquare != Square.NOSQUARE) {
      fen += _enPassantSquare.toString();
    } else {
      fen += '-';
    }
    fen += ' ';

    // Half move clock
    fen += this._halfMoveClock;
    fen += ' ';

    // Full move number
    fen += (_nextHalfMoveNumber + 1) / 2;

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
        Piece p = _x88Board[Square.getSquare(file, rank).ordinal()];
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
      boardString
          .append(' ')
          .append(Square.File.get(file).toString().toUpperCase())
          .append("  ");
    }
    boardString.append("\n\n");

    boardString.append(toFENString());

    return boardString.toString();
  }

  /** @see Object#hashCode() */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (this._zobristKey ^ (this._zobristKey >>> 32));
    return result;
  }

  /** @see Object#equals(Object) */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BoardPosition)) {
      return false;
    }
    BoardPosition other = (BoardPosition) obj;
    if (this._zobristKey != other._zobristKey) {
      return false;
    }
    return true;
  }
}
