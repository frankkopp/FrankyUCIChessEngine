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

import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * The move generator for Omega Engine.
 *
 * It generates pseudo legal and legal moves for a given position.
 *
 * Moves are generated captures first with Most Valuable Victim - Least Valuable Aggressor order
 *
 * <b>This class is not thread safe as it uses static variables to avoid generating them during each
 * object creation.</b><br>
 *
 * @author Frank Kopp
 */
@SuppressWarnings("unused")
public class MoveGenerator {

  private static final boolean CACHE = false;
  private static final boolean SORT = true;

  // remember the last position to control cache validity
  private long _zobristLastPosition = 0;

  // the current position we generate the move for
  // is set in the getMoves methods
  private BoardPosition _position = null;

  // which color do we generate moves for
  private Color _activePlayer;

  // should we only generate capturing moves (for quiscence search)
  private boolean _capturingOnly = false;

  // these are are working lists as fields to avoid to have to
  // create them every time. Instead of creating the need to be cleared before use.
  private final MoveList _legalMoves        = new MoveList();
  // these are all pseudo legal
  private final MoveList _pseudoLegalMoves  = new MoveList(); // all moves
  private final MoveList _capturingMoves    = new MoveList(); // only capturing moves
  private final MoveList _nonCapturingMoves = new MoveList(); // only non capturing moves
  private final MoveList _castlingMoves     = new MoveList(); // only non castling moves
  private final MoveList _evasionMoves      = new MoveList(); // only evasion moves

  // These fields control the on demand generation of moves.
  private OnDemandState _generationCycleState = OnDemandState.NEW;

  private enum OnDemandState {
    NEW,
    PAWN,
    KNIGHTS,
    BISHOPS,
    ROOKS,
    QUEENS,
    KINGS,
    ALL
  }

  private MoveList _onDemandMoveList = new MoveList();
  private long     _onDemandZobristLastPosition;

  // Comparator for move value victim least value attacker
  private static final Comparator<Integer> _mvvlva_comparator =
          Comparator.comparingInt((Integer a) -> (Move.getPiece(a).getType().getValue()
                                                  - Move.getTarget(a).getType().getValue()));

  /** Creates a new {@link MoveGenerator} */
  public MoveGenerator() {}

  /**
   * Returns the next move of the current generation cycle.<br>
   * This method uses an on-demand generation of moves starting with potentially high value moves
   * first to improve cut off rates in AlphaBeta pruning and therefore avoiding the cost of
   * generating all possible moves.<br>
   * The generation cycle starts new if the position changes, capturingOnly is changed or if
   * clearOnDemand() is called.<br>
   *
   * @param position
   * @param capturingOnly
   * @return int representing the next legal Move. Return Move.NOMOVE if none available
   */
  public int getNextPseudoLegalMove(BoardPosition position, boolean capturingOnly) {

    // TODO zobrist could collide - then this will break.
    if (position.getZobristKey() != _onDemandZobristLastPosition
        || _capturingOnly != capturingOnly) {
      _generationCycleState = OnDemandState.NEW;
      clearLists();
      // remember the last position to see when it has changed
      this._onDemandZobristLastPosition = position.getZobristKey();
    }

    // update position
    _position = position;
    _activePlayer = _position._nextPlayer;

    _capturingOnly = capturingOnly;

    // clear lists
    _capturingMoves.clear();
    _nonCapturingMoves.clear();
    _castlingMoves.clear();

    /*
     * If the list is currently empty and we have not generated all moves yet
     * generate the next batch until we have new moves or all moves are generated
     * and there are no more moves to generate
     */
    while (_onDemandMoveList.empty() && !(_generationCycleState == OnDemandState.ALL)) {
      switch (_generationCycleState) {
        case NEW: // no moves yet generate pawn moves first
          // generate pawn moves
          generatePawnMoves();
          if (SORT) _capturingMoves.sort(_mvvlva_comparator);
          _onDemandMoveList.add(_capturingMoves);
          _onDemandMoveList.add(_nonCapturingMoves);
          _generationCycleState = OnDemandState.PAWN;
          break;
        case PAWN: // we have all moves but knight, bishop, rook, queen and king moves
          generateKnightMoves();
          if (SORT) _capturingMoves.sort(_mvvlva_comparator);
          _onDemandMoveList.add(_capturingMoves);
          _onDemandMoveList.add(_nonCapturingMoves);
          _generationCycleState = OnDemandState.KNIGHTS;
          break;
        case KNIGHTS: // we have all moves but bishop, rook, queen and king moves
          generateBishopMoves();
          if (SORT) _capturingMoves.sort(_mvvlva_comparator);
          _onDemandMoveList.add(_capturingMoves);
          _onDemandMoveList.add(_nonCapturingMoves);
          _generationCycleState = OnDemandState.BISHOPS;
          break;
        case BISHOPS: // we have all moves but rook, queen and king moves
          generateRookMoves();
          if (SORT) _capturingMoves.sort(_mvvlva_comparator);
          _onDemandMoveList.add(_capturingMoves);
          _onDemandMoveList.add(_nonCapturingMoves);
          _generationCycleState = OnDemandState.ROOKS;
          break;
        case ROOKS: // we have all moves but queen and king moves
          generateQueenMoves();
          if (SORT) _capturingMoves.sort(_mvvlva_comparator);
          _onDemandMoveList.add(_capturingMoves);
          _onDemandMoveList.add(_nonCapturingMoves);
          _generationCycleState = OnDemandState.QUEENS;
          break;
        case QUEENS: // we have all moves but king moves
          generateKingMoves();
          if (SORT) _capturingMoves.sort(_mvvlva_comparator);
          _onDemandMoveList.add(_capturingMoves);
          _onDemandMoveList.add(_nonCapturingMoves);
          _generationCycleState = OnDemandState.KINGS;
          break;
        case KINGS: // we have all non capturing
          generateCastlingMoves();
          _onDemandMoveList.add(_castlingMoves);
          _generationCycleState = OnDemandState.ALL;
          break;
        case ALL:
            // we have all moves - do nothing
        default:
          break;
      }
    }

    // return a move a delete it form the list
    if (!_onDemandMoveList.empty()) {
      return _onDemandMoveList.removeFirst();
    }

    return Move.NOMOVE;
  }

  /**
   * Reset the on demand move generation use by <code>getNextLegalMove()</code>. The move generation
   * cycle resets automatically if a new board position is used. Calling this method resets it
   * manually.
   */
  public void resetOnDemand() {
    _onDemandMoveList.clear();
    _onDemandZobristLastPosition = 0;
  }

  /**
   * Streams <b>all</b> legal moves for a position.<br>
   * Legal moves have been checked if they leave the king in check or not. Repeated calls to this
   * will return a cached list as long the position has not changed in between.<br>
   * This method basically calls <code>getPseudoLegalMoves</code> and then filters the non legal
   * moves out of the provided list by checking each move if it leaves the king in check.<br>
   *
   * @param position
   * @param capturingOnly if only capturing moves should be generated for quiescence moves
   * @return legal moves
   */
  public IntStream streamLegalMoves(BoardPosition position, boolean capturingOnly) {
    return this.getLegalMoves(position, capturingOnly).stream();
  }

  /**
   * Generates <b>all</b> legal moves for a position. Legal moves have been checked if they leave
   * the king in check or not. Repeated calls to this will return a cached list as long the position
   * has not changed in between.<br>
   * This method basically calls <code>getPseudoLegalMoves</code> and the filters the non legal
   * moves out of the provided list by checking each move if it leaves the king in check.<br>
   * <b>Attention:</b> returns a reference to the list of move which will change after calling this
   * again.<br>
   * Make a clone if this is not desired.
   *
   * @param position
   * @param capturingOnly if only capturing moves should be generated for quiescence moves
   * @return reference to a list of legal moves
   */
  public MoveList getLegalMoves(BoardPosition position, boolean capturingOnly) {
    if (position == null)
      throw new IllegalArgumentException("position may not be null to generate moves");

    // update position
    _position = position;
    _activePlayer = _position._nextPlayer;

    _capturingOnly = capturingOnly;

    // remember the last position to see when it has changed
    // if changed the cache is always invalid
    this._zobristLastPosition = position.getZobristKey();

    // clear all lists
    clearLists();

    // filter legal moves
    assert _legalMoves.size() == 0;
    getPseudoLegalMoves(position, capturingOnly);
    for (int move : _pseudoLegalMoves) {
      if (isLegalMove(move)) _legalMoves.add(move);
    }

    // return a clone of the list as we will continue to use the list as a static list
    return _legalMoves;
  }

  /**
   * Streams <b>all</b> moves for a position. These moves may leave the king in check and may be
   * illegal.<br>
   * Before committing them to a board they need to be checked if they leave the king in check.
   * Repeated calls to this will return a cached list as long the position has not changed in
   * between.<br>
   *
   * @param position
   * @param capturingOnly
   * @return list of moves which may leave the king in check
   */
  public IntStream streamPseudoLegalMoves(BoardPosition position, boolean capturingOnly) {
    return this.getPseudoLegalMoves(position, capturingOnly).stream();
  }

  /**
   * Generates <b>all</b> moves for a position. These moves may leave the king in check and may be
   * illegal.<br>
   * Before committing them to a board they need to be checked if they leave the king in check.
   *
   * <p><b>Attention:</b> returns a reference to the list of move which will change after calling
   * this again.<br>
   * Make a clone if this is not desired.
   *
   * @param position
   * @param capturingOnly
   * @return a reference to the list of moves which may leave the king in check
   */
  public MoveList getPseudoLegalMoves(BoardPosition position, boolean capturingOnly) {
    if (position == null)
      throw new IllegalArgumentException("position may not be null to generate moves");

    // update position
    _position = position;
    _activePlayer = _position._nextPlayer;

    _capturingOnly = capturingOnly;

    // remember the last position to see when it has changed
    // if changed the cache is always invalid
    this._zobristLastPosition = position.getZobristKey();

    // clear all lists
    clearLists();

    /*
     * call the move generators
     * TODO: if check we might be able to implement a faster generation with
     * TODO: only evasion moves
     */
    generatePseudoLegaMoves();

    // return a clone of the list as we will continue to reuse
    return _pseudoLegalMoves;
  }

  /** Generates all pseudo legal moves from the given position. */
  private void generatePseudoLegaMoves() {
    /*
     * Start with capturing move
     *      - lower pieces to higher pieces
     * Then non capturing
     *      - lower to higher pieces
     *      - ideally:
     *      - moves to better positions first
     *      - e.g. Knights in the middle
     *      - sliding pieces middle border position with much control over board
     *      - King at the beginning in castle or corners, at the end in middle
     *      - middle pawns forward in the beginning
     * Use different lists to add moves to avoid repeated looping
     * Too expensive to create several lists every call?
     * Make them a field and clear them instead of creating!!
     */

    generatePawnMoves();
    generateKnightMoves();
    generateBishopMoves();
    generateRookMoves();
    generateQueenMoves();
    generateKingMoves();

    // sort the capturing moves for mvvlva order (Most Valuable Victim - Least Valuable Aggressor)
    if (SORT) _capturingMoves.sort(_mvvlva_comparator);

    // now we have all capturing moves
    _pseudoLegalMoves.add(_capturingMoves);
    if (_capturingOnly) return;

    // add castlings (never capture)
    generateCastlingMoves();

    _pseudoLegalMoves.add(_castlingMoves);
    _pseudoLegalMoves.add(_nonCapturingMoves);
  }

  private void generatePawnMoves() {
    // reverse direction of pawns for black
    final int pawnDir = _activePlayer.isBlack() ? -1 : 1;

    // iterate over all squares where we have a pawn
    final SquareList squareList = _position._pawnSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);

      assert _position._x88Board[square.ordinal()].getType() == PieceType.PAWN;

      // get all possible x88 index values for pawn moves
      // these are basically int values to add or subtract from the
      // current square index. Very efficient with a x88 board.
      int[] directions = Square.pawnDirections;
      for (int d : directions) {

        // calculate the to square
        final int to = square.ordinal() + d * pawnDir;

        if ((to & 0x88) == 0) { // valid square

          final MoveType type = MoveType.NORMAL;
          final Square fromSquare = Square.getSquare(square.ordinal());
          final Square toSquare = Square.getSquare(to);
          final Piece piece = Piece.getPiece(PieceType.PAWN, _activePlayer);
          final Piece target = _position._x88Board[to];
          final Piece promotion = Piece.NOPIECE;

          // capture
          if (d != Square.N) { // not straight
            if (target != Piece.NOPIECE // not empty
                && (target.getColor() == _activePlayer.getInverseColor())) { // opponents color
              assert target.getType() != PieceType.KING; // did we miss a check?
              // capture & promotion
              if (to > 111) { // rank 8
                assert _activePlayer.isWhite(); // checking for  color is probably redundant
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_QUEEN));
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_ROOK));
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_BISHOP));
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_KNIGHT));
              } else if (to < 8) { // rank 1
                assert _activePlayer.isBlack(); // checking for  color is probably redundant
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_QUEEN));
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_ROOK));
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_BISHOP));
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_KNIGHT));
              } else { // normal capture
                _capturingMoves.add(
                  Move.createMove(type, fromSquare, toSquare, piece, target, promotion));
              }
            } else { // empty but maybe en passant
              if (toSquare == _position._enPassantSquare) { //  en passant capture
                // which target?
                final int t =
                    _activePlayer.isWhite()
                        ? _position._enPassantSquare.getSouth().ordinal()
                        : _position._enPassantSquare.getNorth().ordinal();
                _capturingMoves.add(
                  Move.createMove(
                    MoveType.ENPASSANT,
                    fromSquare,
                    toSquare,
                    piece,
                    _position._x88Board[t],
                    promotion));
              }
            }
          }
          // no capture
          else if (d == Square.N && !_capturingOnly) { // straight
            if (target == Piece.NOPIECE) { // way needs to be free
              // promotion
              if (to > 111) { // rank 8
                assert _activePlayer.isWhite(); // checking for color is probably redundant
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_QUEEN));
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_ROOK));
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_BISHOP));
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.WHITE_KNIGHT));
              } else if (to < 8) { // rank 1
                assert _activePlayer.isBlack(); // checking for color is probably redundant
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_QUEEN));
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_ROOK));
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_BISHOP));
                _nonCapturingMoves.add(
                  Move.createMove(
                    MoveType.PROMOTION,
                    fromSquare,
                    toSquare,
                    piece,
                    target,
                    Piece.BLACK_KNIGHT));
              } else {
                // pawndouble
                if (_activePlayer.isWhite()
                    && fromSquare.isWhitePawnBaseRow()
                    && (_position._x88Board[fromSquare.ordinal() + (2 * Square.N)])
                       == Piece.NOPIECE) {
                  // on rank 2 && rank 4 is free(rank 3 already checked via target)
                  _nonCapturingMoves.add(
                    Move.createMove(
                      MoveType.PAWNDOUBLE,
                      fromSquare,
                      toSquare.getNorth(),
                      piece,
                      target,
                      promotion));
                } else if (_activePlayer.isBlack()
                    && fromSquare.isBlackPawnBaseRow()
                    && _position._x88Board[fromSquare.ordinal() + (2 * Square.S)]
                       == Piece.NOPIECE) {
                  // on rank 7 && rank 5 is free(rank 6 already checked via target)
                  _nonCapturingMoves.add(
                    Move.createMove(
                      MoveType.PAWNDOUBLE,
                      fromSquare,
                      toSquare.getSouth(),
                      piece,
                      target,
                      promotion));
                }
                // normal pawn move
                _nonCapturingMoves.add(
                  Move.createMove(type, fromSquare, toSquare, piece, target, promotion));
              }
            }
          }
        }
      }
    }
    // });
  }

  private void generateKnightMoves() {
    PieceType type = PieceType.KNIGHT;
    // iterate over all squares where we have a piece
    final SquareList squareList = _position._knightSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert _position._x88Board[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.knightDirections);
    }
  }

  private void generateBishopMoves() {
    PieceType type = PieceType.BISHOP;
    // iterate over all squares where we have this piece type
    final SquareList squareList = _position._bishopSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert _position._x88Board[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.bishopDirections);
    }
  }

  private void generateRookMoves() {
    PieceType type = PieceType.ROOK;
    // iterate over all squares where we have this piece type
    final SquareList squareList = _position._rookSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert _position._x88Board[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.rookDirections);
    }
  }

  private void generateQueenMoves() {
    PieceType type = PieceType.QUEEN;
    // iterate over all squares where we have this piece type
    final SquareList squareList = _position._queenSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert _position._x88Board[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.queenDirections);
    }
  }

  private void generateKingMoves() {
    PieceType type = PieceType.KING;
    Square square = _position._kingSquares[_activePlayer.ordinal()];
    assert _position._x88Board[square.ordinal()].getType() == type;
    generateMoves(type, square, Square.kingDirections);
  }

  /**
   * @param type
   * @param square
   * @param pieceDirections
   */
  private void generateMoves(PieceType type, Square square, int[] pieceDirections) {
    // get all possible x88 index values for piece's moves
    // these are basically int values to add or subtract from the
    // current square index. Very efficient with a x88 board.
    int[] directions = pieceDirections;
    for (int d : directions) {
      int to = square.ordinal() + d;

      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = _position._x88Board[to];

        // free square - non capture
        if (target == Piece.NOPIECE) { // empty
          if (!_capturingOnly) {
            _nonCapturingMoves.add(
              Move.createMove(
                MoveType.NORMAL,
                Square.getSquare(square.ordinal()),
                Square.getSquare(to),
                Piece.getPiece(type, _activePlayer),
                target,
                Piece.NOPIECE));
          }
        }
        // occupied square - capture if opponent and stop sliding
        else {
          if (target.getColor() == _activePlayer.getInverseColor()) { // opponents color
            assert target.getType() != PieceType.KING; // did we miss a check?
            _capturingMoves.add(
              Move.createMove(
                MoveType.NORMAL,
                Square.getSquare(square.ordinal()),
                Square.getSquare(to),
                Piece.getPiece(type, _activePlayer),
                target,
                Piece.NOPIECE));
          }
          break; // stop sliding;
        }
        if (type.isSliding()) to += d; // next sliding field in this direction
        else break; // no sliding piece type
      }
    }
  }

  private void generateCastlingMoves() {
    if (_position.hasCheck()) return; // no castling if we are in check
    // iterate over all available castlings at this position
    if (_activePlayer.isWhite()) {
      if (_position._castlingWK) {
        // f1 free, g1 free and f1 not attacked
        // we will not check if g1 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (_position._x88Board[Square.f1.ordinal()]
            == Piece.NOPIECE // passing square free
            && !_position.isAttacked(
          _activePlayer.getInverseColor(), Square.f1) // passing square not attacked
            && _position._x88Board[Square.g1.ordinal()]
               == Piece.NOPIECE) // to square free
        {
          _castlingMoves.add(
            Move.createMove(
              MoveType.CASTLING,
              Square.e1,
              Square.g1,
              Piece.WHITE_KING,
              Piece.NOPIECE,
              Piece.NOPIECE));
        }
      }
      if (_position._castlingWQ) {
        // d1 free, c1 free and d1 not attacked
        // we will not check if d1 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (_position._x88Board[Square.d1.ordinal()]
            == Piece.NOPIECE // passing square free
            && _position._x88Board[Square.b1.ordinal()]
               == Piece.NOPIECE // rook passing square free
            && !_position.isAttacked(
          _activePlayer.getInverseColor(), Square.d1) // passing square not attacked
            && _position._x88Board[Square.c1.ordinal()]
               == Piece.NOPIECE) // to square free
        {
          _castlingMoves.add(
            Move.createMove(
              MoveType.CASTLING,
              Square.e1,
              Square.c1,
              Piece.WHITE_KING,
              Piece.NOPIECE,
              Piece.NOPIECE));
        }
      }
    } else {
      if (_position._castlingBK) {
        // f8 free, g8 free and f8 not attacked
        // we will not check if g8 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (_position._x88Board[Square.f8.ordinal()]
            == Piece.NOPIECE // passing square free
            && !_position.isAttacked(
          _activePlayer.getInverseColor(), Square.f8) // passing square not attacked
            && _position._x88Board[Square.g8.ordinal()]
               == Piece.NOPIECE) // to square free
        {
          _castlingMoves.add(
            Move.createMove(
              MoveType.CASTLING,
              Square.e8,
              Square.g8,
              Piece.BLACK_KING,
              Piece.NOPIECE,
              Piece.NOPIECE));
        }
      }
      if (_position._castlingBQ) {
        // d8 free, c8 free and d8 not attacked
        // we will not check if d8 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (_position._x88Board[Square.d8.ordinal()]
            == Piece.NOPIECE // passing square free
            && _position._x88Board[Square.b8.ordinal()]
               == Piece.NOPIECE // rook passing square free
            && !_position.isAttacked(
          _activePlayer.getInverseColor(), Square.d8) // passing square not attacked
            && _position._x88Board[Square.c8.ordinal()]
               == Piece.NOPIECE) // to square free
        {
          _castlingMoves.add(
            Move.createMove(
              MoveType.CASTLING,
              Square.e8,
              Square.c8,
              Piece.BLACK_KING,
              Piece.NOPIECE,
              Piece.NOPIECE));
        }
      }
    }
  }

  /**
   * This method checks if the position has at least one legal move. It will mainly be used to
   * determine mate and stale mate position. This method returns as quick as possible as it is
   * sufficient to have found at least one legal move to see that the position is not a mate
   * position. It only has to check all moves if it is indeed a mate position which in general is a
   * rare case.
   *
   * @param position
   * @return true if there is at least one legal move
   */
  public boolean hasLegalMove(BoardPosition position) {
    if (position == null)
      throw new IllegalArgumentException("position may not be null to find legal moves");

    // update position
    _position = position;
    _activePlayer = _position._nextPlayer;

    // clear all lists
    clearLists();

    /*
     * Find a move by finding at least one moves for a piece type
     */
    if (findKingMove()
        || findPawnMove()
        || findKnightMove()
        || findQueenMove()
        || findRookMove()
        || findBishopMove()) {

      return true;
    }
    return false;
  }

  /**
   * Find a King move and return immediately if found. No need to check castling extra to find a
   * legal move.
   *
   * @return true if a move has been found
   */
  private boolean findKingMove() {
    PieceType type = PieceType.KING;
    Square square = _position._kingSquares[_activePlayer.ordinal()];
    return findMove(type, square, Square.kingDirections);
  }

  /**
   * Find a Knight move and return immediately if found.
   *
   * @return true if a move has been found
   */
  private boolean findKnightMove() {
    PieceType type = PieceType.KNIGHT;
    final SquareList squareList = _position._knightSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square os = squareList.get(i);
      if (findMove(type, os, Square.knightDirections)) return true;
    }
    return false;
  }

  /**
   * Find a Queen move and return immediately if found.
   *
   * @return true if a move has been found
   */
  private boolean findQueenMove() {
    PieceType type = PieceType.QUEEN;
    final SquareList squareList = _position._queenSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square os = squareList.get(i);
      if (findMove(type, os, Square.queenDirections)) return true;
    }
    return false;
  }

  /**
   * Find a Bishop move and return immediately if found.
   *
   * @return true if a move has been found
   */
  private boolean findBishopMove() {
    PieceType type = PieceType.BISHOP;
    // iterate over all squares where we have this piece type
    final SquareList squareList = _position._bishopSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square os = squareList.get(i);
      if (findMove(type, os, Square.bishopDirections)) return true;
    }
    return false;
  }

  /**
   * Find a Rook move and return immediately if found.
   *
   * @return true if a move has been found
   */
  private boolean findRookMove() {
    PieceType type = PieceType.ROOK;
    // iterate over all squares where we have this piece type
    final SquareList squareList = _position._rookSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square os = squareList.get(i);
      if (findMove(type, os, Square.rookDirections)) return true;
    }
    return false;
  }

  /**
   * Finds moves of the given piece type and the given square. Returns immediately if a move has
   * been found.
   *
   * @param type
   * @param square
   * @param pieceDirections
   * @return true if a move has been found
   */
  private boolean findMove(PieceType type, Square square, int[] pieceDirections) {
    int move = Move.NOMOVE;
    int[] directions = pieceDirections;
    for (int d : directions) {
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = _position._x88Board[to];
        // free square - non capture
        if (target == Piece.NOPIECE) { // empty
          move =
              Move.createMove(
                MoveType.NORMAL,
                Square.getSquare(square.ordinal()),
                Square.getSquare(to),
                Piece.getPiece(type, _activePlayer),
                target,
                Piece.NOPIECE);
          if (isLegalMove(move)) return true;
        }
        // occupied square - capture if opponent and stop sliding
        else {
          if (target.getColor() == _activePlayer.getInverseColor()) { // opponents color
            move =
                Move.createMove(
                  MoveType.NORMAL,
                  Square.getSquare(square.ordinal()),
                  Square.getSquare(to),
                  Piece.getPiece(type, _activePlayer),
                  target,
                  Piece.NOPIECE);
            if (isLegalMove(move)) return true;
          }
          break; // stop sliding;
        }
        if (type.isSliding()) to += d; // next sliding field in this direction
        else break; // no sliding piece type
      }
    }
    return false;
  }

  /**
   * Find a Pawn move and return immediately if found. No need to check promotions or pawn doubles.
   *
   * @return true if a move has been found
   */
  private boolean findPawnMove() {
    int move = Move.NOMOVE;

    // reverse direction of pawns for black
    final int pawnDir = _activePlayer.isBlack() ? -1 : 1;

    // iterate over all squares where we have a pawn
    final SquareList squareList = _position._pawnSquares[_activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);

      // get all possible x88 index values for pawn moves
      // these are basically int values to add or subtract from the
      // current square index. Very efficient with a x88 board.
      int[] directions = Square.pawnDirections;
      for (int d : directions) {
        // calculate the to square
        final int to = square.ordinal() + d * pawnDir;
        if ((to & 0x88) == 0) { // valid square
          final MoveType type = MoveType.NORMAL;
          final Square fromSquare = Square.getSquare(square.ordinal());
          final Square toSquare = Square.getSquare(to);
          final Piece piece = Piece.getPiece(PieceType.PAWN, _activePlayer);
          final Piece target = _position._x88Board[to];
          final Piece promotion = Piece.NOPIECE;
          // capture
          if (d != Square.N) { // not straight
            if (target != Piece.NOPIECE // not empty
                && (target.getColor() == _activePlayer.getInverseColor())) { // opponents color
              move = Move.createMove(type, fromSquare, toSquare, piece, target, promotion);
              if (isLegalMove(move)) return true;
            } else { // empty but maybe en passant
              if (toSquare == _position._enPassantSquare) { //  en passant capture
                // which target?
                final int t =
                    _activePlayer.isWhite()
                        ? _position._enPassantSquare.getSouth().ordinal()
                        : _position._enPassantSquare.getNorth().ordinal();
                move =
                    Move.createMove(
                      MoveType.ENPASSANT,
                      fromSquare,
                      toSquare,
                      piece,
                      _position._x88Board[t],
                      promotion);
                if (isLegalMove(move)) return true;
              }
            }
          }
          // no capture
          else if (d == Square.N) { // straight
            if (target == Piece.NOPIECE) { // way needs to be free
              move = Move.createMove(type, fromSquare, toSquare, piece, target, promotion);
              if (isLegalMove(move)) return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Test if move is legal on the current position for the next player.
   *
   * @param move
   * @return true if king of active player is not attacked after the move
   */
  private boolean isLegalMove(int move) {
    assert Move.isValid(move);
    // make the move on the position
    _position.makeMove(move);
    // check if the move leaves the king in check
    if (!_position.isAttacked(
        _activePlayer.getInverseColor(), _position._kingSquares[_activePlayer.ordinal()])) {
      _position.undoMove();
      return true;
    }
    _position.undoMove();
    return false;
  }

  /** Clears all lists */
  private void clearLists() {
    _onDemandMoveList.clear();
    _legalMoves.clear();
    _pseudoLegalMoves.clear();
    _evasionMoves.clear();
    _capturingMoves.clear();
    _nonCapturingMoves.clear();
    _castlingMoves.clear();
  }
}
