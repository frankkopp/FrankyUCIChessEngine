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
 * OmegaBoardPosition.
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
public class OmegaBoardPosition {

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
  OmegaPiece[] _x88Board = new OmegaPiece[BOARDSIZE];
  // hash for pieces - piece, board
  static final long[][] _piece_Zobrist =
      new long[OmegaPiece.values.length][OmegaSquare.values.length];

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
  OmegaSquare _enPassantSquare = OmegaSquare.NOSQUARE;
  OmegaSquare[] _enPassantSquare_History = new OmegaSquare[MAX_HISTORY];
  // hash for castling rights
  static final long[] _enPassantSquare_Zobrist = new long[OmegaSquare.values.length];

  // half move clock - number of half moves since last capture
  int _halfMoveClock = 0;
  int[] _halfMoveClock_History = new int[MAX_HISTORY];
  // has no zobrist key

  // next player color
  OmegaColor _nextPlayer = OmegaColor.WHITE;
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
  final OmegaSquareList[] _pawnSquares = new OmegaSquareList[OmegaColor.values.length];

  final OmegaSquareList[] _knightSquares = new OmegaSquareList[OmegaColor.values.length];
  final OmegaSquareList[] _bishopSquares = new OmegaSquareList[OmegaColor.values.length];
  final OmegaSquareList[] _rookSquares = new OmegaSquareList[OmegaColor.values.length];
  final OmegaSquareList[] _queenSquares = new OmegaSquareList[OmegaColor.values.length];
  final OmegaSquare[] _kingSquares = new OmegaSquare[OmegaColor.values.length];

  // Material value will always be up to date
  int[] _material;

  // caches a hasCheck and hasMate Flag for the current position. Will be set after
  // a call to hasCheck() and reset to TBD every time a move is made or unmade.
  private Flag _hasCheck = Flag.TBD;
  Flag[] _hasCheckFlag_History = new Flag[MAX_HISTORY];
  private Flag _hasMate = Flag.TBD;
  Flag[] _hasMateFlag_History = new Flag[MAX_HISTORY];

  private final OmegaMoveGenerator _mateCheckMG = new OmegaMoveGenerator();

  private enum Flag {
    TBD,
    TRUE,
    FALSE
  }

  // **********************************************************
  // static initialization
  static {
    // all pieces on all squares
    for (OmegaPiece p : OmegaPiece.values) {
      for (OmegaSquare s : OmegaSquare.values) {
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
    for (OmegaSquare s : OmegaSquare.values) {
      _enPassantSquare_Zobrist[s.ordinal()] = Math.abs(random.nextLong());
    }
    // set or unset this for the two color options
    _nextPlayer_Zobrist = Math.abs(random.nextLong());
  }

  // Constructors START -----------------------------------------

  /** Creates a standard Chessly board and initializes it with standard chess setup. */
  public OmegaBoardPosition() {
    this(STANDARD_BOARD_FEN);
  }

  /**
   * Creates a standard Chessly board and initializes it with a fen position
   *
   * @param fen
   */
  public OmegaBoardPosition(String fen) {
    initializeLists();
    initBoard(fen);
  }

  /**
   * Copy constructor - creates a copy of the given OmegaBoardPosition
   *
   * @param op
   */
  public OmegaBoardPosition(OmegaBoardPosition op) {
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
   * Copy constructor from GameBoard - creates a equivalent OmegaBoardPosition from the give
   * GameBoard
   *
   * @param oldBoard
   */
  // public OmegaBoardPosition(GameBoard oldBoard) {
  //        this(oldBoard.toFENString());
  //    }

  /** Initialize the lists for the pieces and the material counter */
  private void initializeLists() {
    for (int i = 0; i <= 1; i++) { // foreach color
      _pawnSquares[i] = new OmegaSquareList();
      _knightSquares[i] = new OmegaSquareList();
      _bishopSquares[i] = new OmegaSquareList();
      _rookSquares[i] = new OmegaSquareList();
      _queenSquares[i] = new OmegaSquareList();
      _kingSquares[i] = OmegaSquare.NOSQUARE;
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
    assert (move != OmegaMove.NOMOVE);

    OmegaSquare fromSquare = OmegaMove.getStart(move);
    assert fromSquare.isValidSquare();
    OmegaSquare toSquare = OmegaMove.getEnd(move);
    assert toSquare.isValidSquare();
    OmegaPiece piece = OmegaMove.getPiece(move);
    assert piece != OmegaPiece.NOPIECE;
    OmegaPiece target = OmegaMove.getTarget(move);
    OmegaPiece promotion = OmegaMove.getPromotion(move);

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
    switch (OmegaMove.getMoveType(move)) {
      case NORMAL:
        invalidateCastlingRights(fromSquare, toSquare);
        makeNormalMove(fromSquare, toSquare, piece, target);
        // clear en passant
        if (_enPassantSquare != OmegaSquare.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = OmegaSquare.NOSQUARE;
        }
        break;
      case PAWNDOUBLE:
        assert fromSquare.isPawnBaseRow(piece.getColor());
        assert !piece.getColor().isNone();
        movePiece(fromSquare, toSquare, piece);
        // clear old en passant
        if (_enPassantSquare != OmegaSquare.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // in
        }
        // set new en passant target field - always one "behind" the toSquare
        _enPassantSquare = piece.getColor().isWhite() ? toSquare.getSouth() : toSquare.getNorth();
        _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // in
        _halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case ENPASSANT:
        assert target != OmegaPiece.NOPIECE;
        assert target.getType() == OmegaPieceType.PAWN;
        assert !target.getColor().isNone();
        OmegaSquare targetSquare =
            target.getColor().isWhite() ? toSquare.getNorth() : toSquare.getSouth();
        removePiece(targetSquare, target);
        movePiece(fromSquare, toSquare, piece);
        // clear en passant
        if (_enPassantSquare != OmegaSquare.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = OmegaSquare.NOSQUARE;
        }
        _halfMoveClock = 0; // reset half move clock because of pawn move
        break;
      case CASTLING:
        makeCastlingMove(fromSquare, toSquare, piece);
        // clear en passant
        if (_enPassantSquare != OmegaSquare.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = OmegaSquare.NOSQUARE;
        }
        _halfMoveClock++;
        break;
      case PROMOTION:
        if (target != OmegaPiece.NOPIECE) removePiece(toSquare, target);
        invalidateCastlingRights(fromSquare, toSquare);
        removePiece(fromSquare, piece);
        putPiece(toSquare, promotion);
        // clear en passant
        if (_enPassantSquare != OmegaSquare.NOSQUARE) {
          _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
          _enPassantSquare = OmegaSquare.NOSQUARE;
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
    _moveHistory[_historyCounter] = OmegaMove.NOMOVE;

    // undo piece move / restore board
    OmegaSquare fromSquare = OmegaMove.getStart(move);
    assert fromSquare.isValidSquare();
    OmegaSquare toSquare = OmegaMove.getEnd(move);
    assert toSquare.isValidSquare();
    OmegaPiece piece = OmegaMove.getPiece(move);
    assert piece != OmegaPiece.NOPIECE;
    OmegaPiece target = OmegaMove.getTarget(move);
    OmegaPiece promotion = OmegaMove.getPromotion(move);

    switch (OmegaMove.getMoveType(move)) {
      case NORMAL:
        movePiece(toSquare, fromSquare, piece);
        if (target != OmegaPiece.NOPIECE) {
          putPiece(toSquare, target);
        }
        break;
      case PAWNDOUBLE:
        movePiece(toSquare, fromSquare, piece);
        break;
      case ENPASSANT:
        OmegaSquare targetSquare =
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
        if (target != OmegaPiece.NOPIECE) putPiece(toSquare, target);
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
      OmegaSquare fromSquare, OmegaSquare toSquare, OmegaPiece piece, OmegaPiece target) {

    if (target != OmegaPiece.NOPIECE) {
      removePiece(toSquare, target);
      _halfMoveClock = 0; // reset half move clock because of capture
    } else if (piece.getType() == OmegaPieceType.PAWN) {
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
  private void invalidateCastlingRights(OmegaSquare fromSquare, OmegaSquare toSquare) {
    // check for castling rights invalidation
    // no else here - combination of these can occur! BIG BUG before :)
    if (fromSquare == OmegaSquare.e1 || toSquare == OmegaSquare.e1) {
      // only take out zobrist if the castling was true before.
      if (_castlingWK) _zobristKey ^= _castlingWK_Zobrist;
      _castlingWK = false;
      if (_castlingWQ) _zobristKey ^= _castlingWQ_Zobrist;
      _castlingWQ = false;
    }
    if (fromSquare == OmegaSquare.e8 || toSquare == OmegaSquare.e8) {
      // only take out zobrist if the castling was true before.
      if (_castlingBK) _zobristKey ^= _castlingBK_Zobrist;
      _castlingBK = false;
      if (_castlingBQ) _zobristKey ^= _castlingBQ_Zobrist;
      _castlingBQ = false;
    }
    if (fromSquare == OmegaSquare.a1 || toSquare == OmegaSquare.a1) {
      if (_castlingWQ) _zobristKey ^= _castlingWQ_Zobrist;
      _castlingWQ = false;
    }
    if (fromSquare == OmegaSquare.h1 || toSquare == OmegaSquare.h1) {
      if (_castlingWK) _zobristKey ^= _castlingWK_Zobrist;
      _castlingWK = false;
    }
    if (fromSquare == OmegaSquare.a8 || toSquare == OmegaSquare.a8) {
      if (_castlingBQ) _zobristKey ^= _castlingBQ_Zobrist;
      _castlingBQ = false;
    }
    if (fromSquare == OmegaSquare.h8 || toSquare == OmegaSquare.h8) {
      if (_castlingBK) _zobristKey ^= _castlingBK_Zobrist;
      _castlingBK = false;
    }
  }

  /**
   * @param fromSquare
   * @param toSquare
   * @param piece
   */
  private void makeCastlingMove(OmegaSquare fromSquare, OmegaSquare toSquare, OmegaPiece piece) {
    assert piece.getType() == OmegaPieceType.KING;

    OmegaPiece rook = OmegaPiece.NOPIECE;
    OmegaSquare rookFromSquare = OmegaSquare.NOSQUARE;
    OmegaSquare rookToSquare = OmegaSquare.NOSQUARE;

    switch (toSquare) {
      case g1: // white kingside
        assert (_castlingWK);
        rook = OmegaPiece.WHITE_ROOK;
        rookFromSquare = OmegaSquare.h1;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = OmegaSquare.f1;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case c1: // white queenside
        assert (_castlingWQ);
        rook = OmegaPiece.WHITE_ROOK;
        rookFromSquare = OmegaSquare.a1;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = OmegaSquare.d1;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case g8: // black kingside
        assert (_castlingBK);
        rook = OmegaPiece.BLACK_ROOK;
        rookFromSquare = OmegaSquare.h8;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = OmegaSquare.f8;
        invalidateCastlingRights(fromSquare, toSquare);
        break;
      case c8: // black queenside
        assert (_castlingBQ);
        rook = OmegaPiece.BLACK_ROOK;
        rookFromSquare = OmegaSquare.a8;
        assert (_x88Board[rookFromSquare.ordinal()] == rook) // check if rook is indeed there
            : "rook to castle not there";
        rookToSquare = OmegaSquare.d8;
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
  private void undoCastlingMove(OmegaSquare fromSquare, OmegaSquare toSquare, OmegaPiece piece) {
    // update castling rights
    OmegaPiece rook = OmegaPiece.NOPIECE;
    OmegaSquare rookFromSquare = OmegaSquare.NOSQUARE;
    OmegaSquare rookToSquare = OmegaSquare.NOSQUARE;

    switch (toSquare) {
      case g1: // white kingside
        rook = OmegaPiece.WHITE_ROOK;
        rookFromSquare = OmegaSquare.h1;
        rookToSquare = OmegaSquare.f1;
        break;
      case c1: // white queenside
        rook = OmegaPiece.WHITE_ROOK;
        rookFromSquare = OmegaSquare.a1;
        rookToSquare = OmegaSquare.d1;
        break;
      case g8: // black kingside
        rook = OmegaPiece.BLACK_ROOK;
        rookFromSquare = OmegaSquare.h8;
        rookToSquare = OmegaSquare.f8;
        break;
      case c8: // black queenside
        rook = OmegaPiece.BLACK_ROOK;
        rookFromSquare = OmegaSquare.a8;
        rookToSquare = OmegaSquare.d8;
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
    if (_enPassantSquare != OmegaSquare.NOSQUARE) {
      _zobristKey ^= _enPassantSquare_Zobrist[_enPassantSquare.ordinal()]; // out
      _enPassantSquare = OmegaSquare.NOSQUARE;
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
  private void movePiece(OmegaSquare fromSquare, OmegaSquare toSquare, OmegaPiece piece) {
    assert fromSquare.isValidSquare();
    assert toSquare.isValidSquare();
    assert piece != OmegaPiece.NOPIECE;
    // assert
    assert (_x88Board[fromSquare.ordinal()] == piece) // check if moved piece is indeed there
        : "piece to move not there";
    assert (_x88Board[toSquare.ordinal()] == OmegaPiece.NOPIECE) // // should be empty
        : "to square should be empty";
    // due to performance we do not call remove and put
    // no need to update counters when moving
    // remove
    _x88Board[fromSquare.ordinal()] = OmegaPiece.NOPIECE;
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
  private void putPiece(OmegaSquare square, OmegaPiece piece) {
    assert square.isValidSquare();
    assert piece != OmegaPiece.NOPIECE;
    assert _x88Board[square.ordinal()] == OmegaPiece.NOPIECE; // should be empty
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
  private OmegaPiece removePiece(OmegaSquare square, OmegaPiece piece) {
    assert square.isValidSquare();
    assert piece != OmegaPiece.NOPIECE;
    assert _x88Board[square.ordinal()] == piece // check if removed piece is indeed there
        : "piece to be removed not there";
    // remove
    OmegaPiece old = _x88Board[square.ordinal()];
    _x88Board[square.ordinal()] = OmegaPiece.NOPIECE;
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
  private void addToPieceLists(OmegaSquare toSquare, OmegaPiece piece, final int color) {
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
  private void removeFromPieceLists(OmegaSquare fromSquare, OmegaPiece piece, final int color) {
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
        _kingSquares[color] = OmegaSquare.NOSQUARE;
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
  public boolean isAttacked(OmegaColor attackerColor, OmegaSquare kingPosition) {
    assert (kingPosition != OmegaSquare.NOSQUARE);
    assert (!attackerColor.isNone());

    final int os_Index = kingPosition.ordinal();
    final boolean isWhite = attackerColor.isWhite();

    /*
     * Checks are ordered for likelihood to return from this as fast as possible
     */

    // check pawns
    // reverse direction to look for pawns which could attack
    final int pawnDir = isWhite ? -1 : 1;
    final OmegaPiece attackerPawn = isWhite ? OmegaPiece.WHITE_PAWN : OmegaPiece.BLACK_PAWN;
    for (int d : OmegaSquare.pawnAttackDirections) {
      final int i = os_Index + d * pawnDir;
      if ((i & 0x88) == 0 && _x88Board[i] == attackerPawn) return true;
    }

    final int attackerColorIndex = attackerColor.ordinal();

    // check sliding horizontal (rook + queen) if there are any
    if (!(_rookSquares[attackerColorIndex].isEmpty()
        && _queenSquares[attackerColorIndex].isEmpty())) {
      for (int d : OmegaSquare.rookDirections) {
        int i = os_Index + d;
        while ((i & 0x88) == 0) { // slide while valid square
          if (_x88Board[i] != OmegaPiece.NOPIECE) { // not empty
            if (_x88Board[i].getColor() == attackerColor // attacker piece
                && (_x88Board[i].getType() == OmegaPieceType.ROOK
                    || _x88Board[i].getType() == OmegaPieceType.QUEEN)) {
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
      for (int d : OmegaSquare.bishopDirections) {
        int i = os_Index + d;
        while ((i & 0x88) == 0) { // slide while valid square
          if (_x88Board[i] != OmegaPiece.NOPIECE) { // not empty
            if (_x88Board[i].getColor() == attackerColor // attacker piece
                && (_x88Board[i].getType() == OmegaPieceType.BISHOP
                    || _x88Board[i].getType() == OmegaPieceType.QUEEN)) {
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
      for (int d : OmegaSquare.knightDirections) {
        int i = os_Index + d;
        if ((i & 0x88) == 0) { // valid square
          if (_x88Board[i] != OmegaPiece.NOPIECE // not empty
              && _x88Board[i].getColor() == attackerColor // attacker piece
              && (_x88Board[i].getType() == OmegaPieceType.KNIGHT)) {
            return true;
          }
        }
      }
    }

    // check king
    for (int d : OmegaSquare.kingDirections) {
      int i = os_Index + d;
      if ((i & 0x88) == 0) { // valid square
        if (_x88Board[i] != OmegaPiece.NOPIECE // not empty
            && _x88Board[i].getColor() == attackerColor // attacker piece
            && (_x88Board[i].getType() == OmegaPieceType.KING)) {
          return true;
        }
      }
    }

    // check en passant
    if (this._enPassantSquare != OmegaSquare.NOSQUARE) {
      if (isWhite // white is attacker
          && _x88Board[_enPassantSquare.getSouth().ordinal()]
              == OmegaPiece.BLACK_PAWN // black is target
          && this._enPassantSquare.getSouth()
              == kingPosition) { // this is indeed the en passant attacked square
        // left
        int i = os_Index + OmegaSquare.W;
        if ((i & 0x88) == 0 && _x88Board[i] == OmegaPiece.WHITE_PAWN) return true;
        // right
        i = os_Index + OmegaSquare.E;
        if ((i & 0x88) == 0 && _x88Board[i] == OmegaPiece.WHITE_PAWN) return true;
      } else if (!isWhite // black is attacker (assume not noColor)
          && _x88Board[_enPassantSquare.getNorth().ordinal()]
              == OmegaPiece.WHITE_PAWN // white is target
          && this._enPassantSquare.getNorth()
              == kingPosition) { // this is indeed the en passant attacked square
        // attack from left
        int i = os_Index + OmegaSquare.W;
        if ((i & 0x88) == 0 && _x88Board[i] == OmegaPiece.BLACK_PAWN) return true;
        // attack from right
        i = os_Index + OmegaSquare.E;
        if ((i & 0x88) == 0 && _x88Board[i] == OmegaPiece.BLACK_PAWN) return true;
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
    if (_pawnSquares[OmegaColor.WHITE.ordinal()].size() == 0
        && _pawnSquares[OmegaColor.BLACK.ordinal()].size() == 0
        && _rookSquares[OmegaColor.WHITE.ordinal()].size() == 0
        && _rookSquares[OmegaColor.BLACK.ordinal()].size() == 0
        && _queenSquares[OmegaColor.WHITE.ordinal()].size() == 0
        && _queenSquares[OmegaColor.BLACK.ordinal()].size() == 0) {

      // white king bare KK*
      if (_knightSquares[OmegaColor.WHITE.ordinal()].size() == 0
          && _bishopSquares[OmegaColor.WHITE.ordinal()].size() == 0) {

        // both kings bare KK, KKN, KKNN
        if (_knightSquares[OmegaColor.BLACK.ordinal()].size() <= 2
            && _bishopSquares[OmegaColor.BLACK.ordinal()].size() == 0) {
          return true;
        }

        // KKB
        if (_knightSquares[OmegaColor.BLACK.ordinal()].size() == 0
            && _bishopSquares[OmegaColor.BLACK.ordinal()].size() == 1) {
          return true;
        }

      }
      // only black king bare K*K
      else if (_knightSquares[OmegaColor.BLACK.ordinal()].size() == 0
          && _bishopSquares[OmegaColor.BLACK.ordinal()].size() == 0) {

        // both kings bare KK, KNK, KNNK
        if (_knightSquares[OmegaColor.WHITE.ordinal()].size() <= 2
            && _bishopSquares[OmegaColor.WHITE.ordinal()].size() == 0) {
          return true;
        }

        // KBK
        if (_knightSquares[OmegaColor.BLACK.ordinal()].size() == 0
            && _bishopSquares[OmegaColor.BLACK.ordinal()].size() == 1) {
          return true;
        }
      }

      // KBKB - B same field color
      else if (_knightSquares[OmegaColor.BLACK.ordinal()].size() == 0
          && _bishopSquares[OmegaColor.BLACK.ordinal()].size() == 1
          && _knightSquares[OmegaColor.WHITE.ordinal()].size() == 0
          && _bishopSquares[OmegaColor.WHITE.ordinal()].size() == 1) {

        /*
         * Bishops are on the same field color if the sum of the
         * rank and file of the fields are on both even or both odd :
         * (file + rank) % 2 == 0 = black field
         * (file + rank) % 2 == 1 = white field
         */
        final OmegaSquare whiteBishop = _bishopSquares[OmegaColor.WHITE.ordinal()].get(0);
        int file_w = whiteBishop.getFile().get();
        int rank_w = whiteBishop.getRank().get();
        final OmegaSquare blackBishop = _bishopSquares[OmegaColor.BLACK.ordinal()].get(0);
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
   * @param c OmegaColor
   * @return the material value
   */
  public int getMaterial(OmegaColor c) {
    return this._material[c.ordinal()];
  }

  /** @return color of next player */
  public OmegaColor getNextPlayer() {
    return _nextPlayer;
  }

  /**
   * Returns the last move. Returns OmegaMove.NOMOVE if there is no last move.
   *
   * @return int representing a move
   */
  public int getLastMove() {
    if (_historyCounter == 0) return OmegaMove.NOMOVE;
    return _moveHistory[_historyCounter - 1];
  }

  /** @param fen */
  private void initBoard(String fen) {
    // clear board
    Arrays.fill(_x88Board, OmegaPiece.NOPIECE);
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
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.BLACK_PAWN);
              break;
            case "n":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.BLACK_KNIGHT);
              break;
            case "b":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.BLACK_BISHOP);
              break;
            case "r":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.BLACK_ROOK);
              break;
            case "q":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.BLACK_QUEEN);
              break;
            case "k":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.BLACK_KING);
              break;
            default:
              throw new IllegalArgumentException("FEN Syntax not valid - expected a-hA-H");
          }
        } else if (s.toUpperCase().equals(s)) { // white
          switch (s) {
            case "P":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.WHITE_PAWN);
              break;
            case "N":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.WHITE_KNIGHT);
              break;
            case "B":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.WHITE_BISHOP);
              break;
            case "R":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.WHITE_ROOK);
              break;
            case "Q":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.WHITE_QUEEN);
              break;
            case "K":
              putPiece(OmegaSquare.getSquare(file, rank), OmegaPiece.WHITE_KING);
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
      if (s.equals("w")) _nextPlayer = OmegaColor.WHITE;
      else if (s.equals("b")) {
        _nextPlayer = OmegaColor.BLACK;
        _zobristKey ^= _nextPlayer_Zobrist; // only when black to have the right in/out rhythm
      } else throw new IllegalArgumentException("FEN Syntax not valid - expected w or b");
    } else { // default "w"
      _nextPlayer = OmegaColor.WHITE;
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
        _enPassantSquare = OmegaSquare.fromNotation(s);
        if (_enPassantSquare.equals(OmegaSquare.NOSQUARE)) {
          throw new IllegalArgumentException(
              "FEN Syntax not valid - expected valid en passant square");
        }
      }
    }
    // set en passant if not NOSQUARE
    if (_enPassantSquare != OmegaSquare.NOSQUARE) {
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

        OmegaPiece piece = _x88Board[OmegaSquare.getSquare(file, rank).ordinal()];

        if (piece == OmegaPiece.NOPIECE) {
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
    if (this._enPassantSquare != OmegaSquare.NOSQUARE) {
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
        OmegaPiece p = _x88Board[OmegaSquare.getSquare(file, rank).ordinal()];
        if (p == OmegaPiece.NOPIECE) {
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
          .append(OmegaSquare.File.get(file).toString().toUpperCase())
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
    if (!(obj instanceof OmegaBoardPosition)) {
      return false;
    }
    OmegaBoardPosition other = (OmegaBoardPosition) obj;
    if (this._zobristKey != other._zobristKey) {
      return false;
    }
    return true;
  }
}
