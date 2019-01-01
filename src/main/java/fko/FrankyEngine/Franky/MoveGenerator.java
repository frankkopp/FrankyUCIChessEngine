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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * The move generator for Omega Engine.
 * <p>
 * It generates pseudo legal and legal moves for a given position.
 * <p>
 * Moves are generated captures first with Most Valuable Victim - Least Valuable Aggressor order
 *
 * @author Frank Kopp
 */
public class MoveGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(MoveGenerator.class);

  boolean SORT_CAPTURING_MOVES = true;
  boolean SORT_MOVES           = false;

  // the current position we generate the move for
  // is set in the getMoves methods
  private Position position = null;

  // which color do we generate moves for
  private Color activePlayer;

  // should we only generate capturing moves
  private boolean captureMovesOnly = false;

  // these are are working lists as fields to avoid to have to
  // create them every time. Instead of creating the need to be cleared before use.
  private final MoveList legalMoves        = new MoveList();
  // these are all pseudo legal
  private final MoveList pseudoLegalMoves  = new MoveList(); // all moves
  private final MoveList capturingMoves    = new MoveList(); // only capturing moves
  private final MoveList nonCapturingMoves = new MoveList(); // only non capturing moves
  private final MoveList castlingMoves     = new MoveList(); // only castling moves

  // These fields control the on demand generation of moves.
  private OnDemandState generationCycleState = OnDemandState.NEW;
  private int[]         killerMoves          = null;
  private int           pvMove               = Move.NOMOVE;

  private enum OnDemandState {
    NEW, CAPTURING, NON_CAPTURING, ALL
  }

  private static final int GEN_CAPTURES    = 1;
  private static final int GEN_NONCAPTURES = 2;
  private static final int GEN_ALL         = 3;
  private              int genMode         = GEN_ALL;

  private MoveList onDemandMoveList = new MoveList();
  private long     onDemandZobristLastPosition;

  // Comparator for move value victim least value attacker
  private static final Comparator<Integer> mvvlvaComparator = Comparator.comparingInt(
    (Integer move) -> (Move.getPiece(move).getType().getValue() -
                       Move.getTarget(move).getType().getValue()));

  /**
   * Creates a new {@link MoveGenerator}
   */
  public MoveGenerator() {
  }

  /**
   * Creates a new {@link MoveGenerator}
   */
  public MoveGenerator(Position position) {
    setPosition(position);
  }

  /**
   * Sets the position of this move generator.
   *
   * Setting the position resets the move generator. Killers or PV moves set via
   * <code>setKillerMoves</code> and <code>setPVMove</code> are deleted.
   *
   * @param position
   */
  public void setPosition(Position position) {
    assert position != null : "parameter null not allowed";
    this.position = position;
    this.activePlayer = position.getNextPlayer();
    this.onDemandZobristLastPosition = position.getZobristKey();
    this.generationCycleState = OnDemandState.NEW;
    this.killerMoves = null;
    this.pvMove = Move.NOMOVE;
    clearLists();
  }

  /**
   * Sets the PV move so it will be returned first
   *
   * @param move
   */
  public void setPVMove(int move) {
    this.pvMove = move;
  }

  /**
   * sets killer moves which will be inserted after capturing moves
   *
   * @param killerMoves
   */
  public void setKillerMoves(int[] killerMoves) {
    assert killerMoves != null : "parameter null not allowed";
    this.killerMoves = killerMoves.clone();
  }

  /**
   * Returns the next move of the current generation cycle.<br>
   * This method uses an on-demand generation of moves starting with potentially high value moves
   * first to improve cut off rates in AlphaBeta pruning and therefore avoiding the cost of
   * generating all possible moves.<br>
   * The generation cycle starts new if the position changes, capturingOnly is changed or if
   * clearOnDemand() is called.<br>
   *
   * @return int representing the next legal Move. Return Move.NOMOVE if none available
   */
  public int getNextPseudoLegalMove(boolean capturingOnly) {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }

    /*
     * If the list is currently empty and we have not generated all moves yet
     * generate the next batch until we have new moves or all moves are generated
     * and there are no more moves to generate
     */
    while (onDemandMoveList.empty() && !(generationCycleState == OnDemandState.ALL)) {

      switch (generationCycleState) {
        case NEW: // fall through
        case CAPTURING:
          genMode = GEN_CAPTURES;
          generatePawnMoves();
          generateKnightMoves();
          generateBishopMoves();
          generateRookMoves();
          generateQueenMoves();
          generateKingMoves();

          if (SORT_CAPTURING_MOVES) {
            capturingMoves.sort(mvvlvaComparator);
          }

          // setting pv move
          if (pvMove != Move.NOMOVE && !Move.getTarget(pvMove).equals(Piece.NOPIECE)) {
            capturingMoves.pushToHeadStable(pvMove);
          }

          // push pv move if any
          onDemandMoveList.add(capturingMoves);

          generationCycleState = OnDemandState.NON_CAPTURING;

          break;
        case NON_CAPTURING: // we have all non capturing
          if (capturingOnly) break;
          genMode = GEN_NONCAPTURES;

          generatePawnMoves();
          generateKnightMoves();
          generateBishopMoves();
          generateRookMoves();
          generateQueenMoves();
          generateKingMoves();

          generateCastlingMoves();
          nonCapturingMoves.add(castlingMoves);

          // setting pv move
          if (pvMove != Move.NOMOVE && Move.getTarget(pvMove).equals(Piece.NOPIECE)) {
            nonCapturingMoves.pushToHeadStable(pvMove);
          }

          // full sort of non-capturing moves
          moveListSort(nonCapturingMoves);

          onDemandMoveList.add(nonCapturingMoves);

          generationCycleState = OnDemandState.ALL;

          break;
        case ALL: // we have all moves - do nothing
        default:
          break;
      }
    }

    captureMovesOnly = capturingOnly;

    // return a move and delete it form the list
    if (!onDemandMoveList.empty()) {
      return onDemandMoveList.removeFirst();
    }

    return Move.NOMOVE;
  }

  /**
   * Reset the on demand move generation use by <code>getNextLegalMove()</code>. The move generation
   * cycle resets automatically if a new board position is used. Calling this method resets it
   * manually.
   */
  public void resetOnDemand() {
    onDemandZobristLastPosition = 0;
    capturingMoves.clear();
    nonCapturingMoves.clear();
    castlingMoves.clear();
    onDemandMoveList.clear();
  }

  /**
   * Streams <b>all</b> legal moves for a position.<br>
   * Legal moves have been checked if they leave the king in check or not. Repeated calls to this
   * will return a cached list as long the position has not changed in between.<br>
   * This method basically calls <code>getPseudoLegalMoves</code> and then filters the non legal
   * moves out of the provided list by checking each move if it leaves the king in check.<br>
   *
   * @param fullSort true if list should sorted in full, false provides only pre-sorted list
   * @return legal moves
   */
  public IntStream streamLegalMoves(boolean fullSort) {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }
    return this.getLegalMoves(fullSort).stream();
  }

  /**
   * Streams <b>all</b> legal moves for a position.<br>
   * Legal moves have been checked if they leave the king in check or not. Repeated calls to this
   * will return a cached list as long the position has not changed in between.<br>
   * This method basically calls <code>getPseudoLegalMoves</code> and then filters the non legal
   * moves out of the provided list by checking each move if it leaves the king in check.<br>
   *
   * @return legal moves
   */
  public IntStream streamLegalMoves() {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }
    return this.getLegalMoves(false).stream();
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
   * @param fullSort true if list should sorted in full, false provides only pre-sorted list
   * @return reference to a list of legal moves
   */
  public MoveList getLegalMoves(boolean fullSort) {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }

    captureMovesOnly = false;

    // call the move generators
    generatePseudoLegaMoves();

    // filter legal moves
    for (int move : pseudoLegalMoves) {
      if (isLegalMove(move)) legalMoves.add(move);
    }

    if (fullSort) moveListSort(legalMoves);

    return legalMoves;
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
   * @return reference to a list of legal moves
   */
  public MoveList getLegalMoves() {
    return getLegalMoves(false);
  }

  /**
   * Streams <b>all</b> moves for a position. These moves may leave the king in check and may be
   * illegal.<br>
   * Before committing them to a board they need to be checked if they leave the king in check.
   * Repeated calls to this will return a cached list as long the position has not changed in
   * between.<br>
   *
   * @param capturingOnly
   * @return list of moves which may leave the king in check
   */
  public IntStream streamPseudoLegalMoves(boolean capturingOnly) {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }
    return this.getPseudoLegalMoves().stream();
  }

  /**
   * Generates <b>all</b> moves for a position. These moves may leave the king in check and may be
   * illegal.<br>
   * Before committing them to a board they need to be checked if they leave the king in check.
   *
   * <p><b>Attention:</b> returns a reference to the list of moves which will change after calling
   * this again.<br>
   * Make a clone if this is not desired.
   *
   * @return a reference to the list of moves which may leave the king in check
   */
  public MoveList getPseudoLegalMoves() {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }

    this.captureMovesOnly = false;

    // call the move generators
    generatePseudoLegaMoves();

    // return a clone of the list as we will continue to reuse
    return pseudoLegalMoves;
  }

  /**
   * Generates moves for a position during quiescence search.<br>
   * It will be a subset of all possible pseudo legal moves as in qsearch we
   * usually only search "non-quiet" positions.
   * <p>
   * These moves may leave the king in check and may be illegal.<br>
   * Before committing them to a board they need to be checked if they leave the king in check.
   *
   * <b>Attention:</b> returns a reference to the list of moves which will change after calling
   * this again.<br>
   * Make a clone if this is not desired.
   *
   * @return a reference to the list of moves which may leave the king in check
   */
  public MoveList getPseudoLegalQSearchMoves() {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }

    // if not in check generate only capturing moves for qsearch
    captureMovesOnly = !position.hasCheck();

    // call the move generators
    generatePseudoLegaMoves();

    // if check return all moves - non evation moves will be filtered by legal check
    if (!captureMovesOnly) {
      return pseudoLegalMoves;
    }

    // lower amount of captures searched in qsearch by only looking at "good" captures
    MoveList qSearchMoves = new MoveList();
    for (int move : pseudoLegalMoves) {
      // pawn captures
      if (Move.getPiece(move).getType().equals(PieceType.PAWN)) {
        qSearchMoves.add(move);
      }
      // Lower value piece captures higher value piece (with a margin)
      else if (Move.getPiece(move).getType().getValue() + 200 <
               Move.getTarget(move).getType().getValue()) {
        qSearchMoves.add(move);
      }
      // undefended pieces captures are good
      else if (!position.isAttacked(position.getOpponent(), Move.getEnd(move))) {
        qSearchMoves.add(move);
      }
    }

    // return a clone of the list as we will continue to reuse
    return qSearchMoves;
  }

  /**
   * Generates all pseudo legal moves from the given position.
   */
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

    // at this point we have all capturing moves - return if this is all we need
    if (captureMovesOnly) {
      genMode = GEN_CAPTURES;
      generatePawnMoves();
      generateKnightMoves();
      generateBishopMoves();
      generateRookMoves();
      generateQueenMoves();
      generateKingMoves();
      // sort the capturing moves for mvvlva order
      // (Most Valuable Victim - Least Valuable Aggressor)
      if (SORT_CAPTURING_MOVES) capturingMoves.sort(mvvlvaComparator);
      // now we have all capturing moves
      pseudoLegalMoves.add(capturingMoves);
      return;
    }

    genMode = GEN_ALL;
    generatePawnMoves();
    generateKnightMoves();
    generateBishopMoves();
    generateRookMoves();
    generateQueenMoves();
    generateKingMoves();
    // generate castling moves late as they never capture
    generateCastlingMoves();

    // put castling to front of non capture moves
    nonCapturingMoves.addFront(castlingMoves);

    // sort all moves
    if (SORT_MOVES) {
      // sort over all moves
      pseudoLegalMoves.add(capturingMoves);
      pseudoLegalMoves.add(nonCapturingMoves);
      moveListSort(pseudoLegalMoves);

    } else {
      // sort only capturing moves mvvlva order
      // Most Valuable Victim - Least Valuable Aggressor
      if (SORT_CAPTURING_MOVES) {
        capturingMoves.sort(mvvlvaComparator);
        pushKillerMoves(nonCapturingMoves);
      }
      pseudoLegalMoves.add(capturingMoves);
      pseudoLegalMoves.add(nonCapturingMoves);
    }

    // push pv move if any to head of list
    if (pvMove != Move.NOMOVE) {
      pseudoLegalMoves.pushToHeadStable(pvMove);
    }

  }

  /**
   * Sort value for all moves. Smaller values heapsort first
   */
  private int getSortValue(int move) {

    // capturing moves
    if (!Move.getTarget(move).equals(Piece.NOPIECE)) {
      return 1000 + Move.getPiece(move).getType().getValue() -
             Move.getTarget(move).getType().getValue();
    }
    // non capturing
    else {
      if (killerMoves != null) {
        for (int i = killerMoves.length - 1; i >= 0; i--) {
          if (killerMoves[i] == move) {
            return 5000 + i;
          }
        }
      }
      // promotions
      final PieceType pieceType = Move.getPromotion(move).getType();
      if (!pieceType.equals(PieceType.NOTYPE)) {
        switch (pieceType) {
          case QUEEN:
            return 9000;
          case KNIGHT:
            return 9100;
          case ROOK:
            return 10900;
          case BISHOP:
            return 10900;
        }
      }
      // castling
      else if (Move.getMoveType(move).equals(MoveType.CASTLING)) {
        return 9200;
      }
      // all other moves
      return 10000 - Evaluation.getPositionValue(position, move);
    }
  }

  /**
   * Special moveListSort to use index array as comparator
   *
   * @param moveList
   */
  private void moveListSort(MoveList moveList) {
    // create index array
    int[] sortIdx = new int[moveList.size()];
    for (int i = 0; i < moveList.size(); i++) {
      sortIdx[i] = getSortValue(moveList.get(i));
    }
    // insertion sort
    int ts;
    for (int i = 0; i < moveList.size(); i++) {
      for (int j = i; j > 0; j--) {
        if (sortIdx[j] - sortIdx[j - 1] < 0) {
          moveList.swap(j - 1, j);
          ts = sortIdx[j];
          sortIdx[j] = sortIdx[j - 1];
          sortIdx[j - 1] = ts;
        }
      }
    }
  }

  /**
   * Pushing killer moves to fron of moveList of there are any
   *
   * @param moveList
   */
  private void pushKillerMoves(final MoveList moveList) {
    if (killerMoves != null && nonCapturingMoves.size() > 0) {
      for (int i = killerMoves.length - 1; i >= 0; i--) {
        if (killerMoves[i] != Move.NOMOVE && killerMoves[i] != nonCapturingMoves.get(0)) {
          moveList.pushToHeadStable(killerMoves[i]);
        }
      }
    }
  }

  private void generateKillerMoves() {
    if (killerMoves != null) {
      for (int i = killerMoves.length - 1; i >= 0; i--) {
        if (killerMoves[i] != Move.NOMOVE && killerMoves[i] != nonCapturingMoves.get(0)) {
          nonCapturingMoves.add(killerMoves[i]);
        }
      }
    }
  }

  private void generatePawnMoves() {
    // reverse direction of pawns for black
    final int pawnDir = activePlayer.isBlack() ? -1 : 1;

    // iterate over all squares where we have a pawn
    final SquareList squareList = position.getPawnSquares()[activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);

      assert position.getX88Board()[square.ordinal()].getType() == PieceType.PAWN;

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
          final Piece piece = Piece.getPiece(PieceType.PAWN, activePlayer);
          final Piece target = position.getX88Board()[to];
          final Piece promotion = Piece.NOPIECE;

          // capture
          if (d != Square.N) { // not straight
            if ((genMode & GEN_CAPTURES) > 0 && target != Piece.NOPIECE // not empty
                && (target.getColor() == activePlayer.getInverseColor())) { // opponents color
              assert target.getType() != PieceType.KING; // did we miss a check?
              // capture & promotion
              if (to > 111) { // rank 8
                assert activePlayer.isWhite(); // checking for  color is probably redundant
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_QUEEN));
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_KNIGHT));
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_ROOK));
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_BISHOP));
              } else if (to < 8) { // rank 1
                assert activePlayer.isBlack(); // checking for  color is probably redundant
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_QUEEN));
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_KNIGHT));
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_ROOK));
                capturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_BISHOP));
              } else { // normal capture
                capturingMoves.add(
                  Move.createMove(type, fromSquare, toSquare, piece, target, promotion));
              }
            } else { // empty but maybe en passant
              if (toSquare == position.getEnPassantSquare()) { //  en passant capture
                // which target?
                final int t = activePlayer.isWhite()
                              ? position.getEnPassantSquare().getSouth().ordinal()
                              : position.getEnPassantSquare().getNorth().ordinal();
                capturingMoves.add(Move.createMove(MoveType.ENPASSANT, fromSquare, toSquare, piece,
                                                   position.getX88Board()[t], promotion));
              }
            }
          }
          // no capture
          else { // straight
            if ((genMode & GEN_NONCAPTURES) > 0 &&
                target == Piece.NOPIECE) { // way needs to be free
              // promotion
              if (to > 111) { // rank 8
                assert activePlayer.isWhite(); // checking for color is probably redundant
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_QUEEN));
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_KNIGHT));
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_ROOK));
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.WHITE_BISHOP));
              } else if (to < 8) { // rank 1
                assert activePlayer.isBlack(); // checking for color is probably redundant
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_QUEEN));
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_KNIGHT));
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_ROOK));
                nonCapturingMoves.add(
                  Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                                  Piece.BLACK_BISHOP));
              } else {
                // pawndouble
                if (activePlayer.isWhite() && fromSquare.isWhitePawnBaseRow() &&
                    (position.getX88Board()[fromSquare.ordinal() + (2 * Square.N)]) ==
                    Piece.NOPIECE) {
                  // on rank 2 && rank 4 is free(rank 3 already checked via target)
                  nonCapturingMoves.add(
                    Move.createMove(MoveType.PAWNDOUBLE, fromSquare, toSquare.getNorth(), piece,
                                    target, promotion));
                } else if (activePlayer.isBlack() && fromSquare.isBlackPawnBaseRow() &&
                           position.getX88Board()[fromSquare.ordinal() + (2 * Square.S)] ==
                           Piece.NOPIECE) {
                  // on rank 7 && rank 5 is free(rank 6 already checked via target)
                  nonCapturingMoves.add(
                    Move.createMove(MoveType.PAWNDOUBLE, fromSquare, toSquare.getSouth(), piece,
                                    target, promotion));
                }
                // normal pawn move
                nonCapturingMoves.add(
                  Move.createMove(type, fromSquare, toSquare, piece, target, promotion));
              }
            }
          }
        }
      }
    }
  }

  private void generateKnightMoves() {
    PieceType type = PieceType.KNIGHT;
    // iterate over all squares where we have a piece
    final SquareList squareList = position.getKnightSquares()[activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert position.getX88Board()[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.knightDirections);
    }
  }

  private void generateBishopMoves() {
    PieceType type = PieceType.BISHOP;
    // iterate over all squares where we have this piece type
    final SquareList squareList = position.getBishopSquares()[activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert position.getX88Board()[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.bishopDirections);
    }
  }

  private void generateRookMoves() {
    PieceType type = PieceType.ROOK;
    // iterate over all squares where we have this piece type
    final SquareList squareList = position.getRookSquares()[activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert position.getX88Board()[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.rookDirections);
    }
  }

  private void generateQueenMoves() {
    PieceType type = PieceType.QUEEN;
    // iterate over all squares where we have this piece type
    final SquareList squareList = position.getQueenSquares()[activePlayer.ordinal()];
    final int size = squareList.size();
    for (int i = 0; i < size; i++) {
      final Square square = squareList.get(i);
      assert position.getX88Board()[square.ordinal()].getType() == type;
      generateMoves(type, square, Square.queenDirections);
    }
  }

  private void generateKingMoves() {
    PieceType type = PieceType.KING;
    Square square = position.getKingSquares()[activePlayer.ordinal()];
    assert position.getX88Board()[square.ordinal()].getType() == type;
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
    for (int d : pieceDirections) {
      int to = square.ordinal() + d;

      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = position.getX88Board()[to];

        // free square - non capture
        if (target == Piece.NOPIECE) { // empty
          if ((genMode & GEN_NONCAPTURES) > 0) {
            nonCapturingMoves.add(
              Move.createMove(MoveType.NORMAL, Square.getSquare(square.ordinal()),
                              Square.getSquare(to), Piece.getPiece(type, activePlayer), target,
                              Piece.NOPIECE));
          }
        }
        // occupied square - capture if opponent and stop sliding
        else {
          if ((genMode & GEN_CAPTURES) > 0) {
            if (target.getColor() == activePlayer.getInverseColor()) { // opponents color
              assert target.getType() != PieceType.KING; // did we miss a check?
              capturingMoves.add(
                Move.createMove(MoveType.NORMAL, Square.getSquare(square.ordinal()),
                                Square.getSquare(to), Piece.getPiece(type, activePlayer), target,
                                Piece.NOPIECE));

            }
          }
          break;
        }
        if (type.isSliding()) {
          to += d; // next sliding field in this direction
        } else {
          break; // no sliding piece type
        }
      }
    }
  }

  private void generateCastlingMoves() {
    // no castling if we are in check
    if (position.hasCheck() || (genMode & GEN_NONCAPTURES) == 0) return;
    // iterate over all available castlings at this position
    if (activePlayer.isWhite()) {
      if (position.isCastlingWK()) {
        // f1 free, g1 free and f1 not attacked
        // we will not check if g1 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getX88Board()[Square.f1.ordinal()] == Piece.NOPIECE // passing square free
            && !position.isAttacked(activePlayer.getInverseColor(), Square.f1)
            // passing square not attacked
            && position.getX88Board()[Square.g1.ordinal()] == Piece.NOPIECE) // to square free
        {
          castlingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e1, Square.g1, Piece.WHITE_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
        }
      }
      if (position.isCastlingWQ()) {
        // d1 free, c1 free and d1 not attacked
        // we will not check if d1 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getX88Board()[Square.d1.ordinal()] == Piece.NOPIECE // passing square free
            && position.getX88Board()[Square.b1.ordinal()] == Piece.NOPIECE
            // rook passing square free
            && !position.isAttacked(activePlayer.getInverseColor(), Square.d1)
            // passing square not attacked
            && position.getX88Board()[Square.c1.ordinal()] == Piece.NOPIECE) // to square free
        {
          castlingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e1, Square.c1, Piece.WHITE_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
        }
      }
    } else {
      if (position.isCastlingBK()) {
        // f8 free, g8 free and f8 not attacked
        // we will not check if g8 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getX88Board()[Square.f8.ordinal()] == Piece.NOPIECE // passing square free
            && !position.isAttacked(activePlayer.getInverseColor(), Square.f8)
            // passing square not attacked
            && position.getX88Board()[Square.g8.ordinal()] == Piece.NOPIECE) // to square free
        {
          castlingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e8, Square.g8, Piece.BLACK_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
        }
      }
      if (position.isCastlingBQ()) {
        // d8 free, c8 free and d8 not attacked
        // we will not check if d8 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getX88Board()[Square.d8.ordinal()] == Piece.NOPIECE // passing square free
            && position.getX88Board()[Square.b8.ordinal()] == Piece.NOPIECE
            // rook passing square free
            && !position.isAttacked(activePlayer.getInverseColor(), Square.d8)
            // passing square not attacked
            && position.getX88Board()[Square.c8.ordinal()] == Piece.NOPIECE) // to square free
        {
          castlingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e8, Square.c8, Piece.BLACK_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
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
  public boolean hasLegalMove(Position position) {
    setPosition(position);
    return hasLegalMove();
  }

  /**
   * This method checks if the position has at least one legal move. It will mainly be used to
   * determine mate and stale mate position. This method returns as quick as possible as it is
   * sufficient to have found at least one legal move to see that the position is not a mate
   * position. It only has to check all moves if it is indeed a mate position which in general is a
   * rare case.
   *
   * @return true if there is at least one legal move
   */
  public boolean hasLegalMove() {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }

    /*
     * Find a move by finding at least one moves for a piece type
     */
    return findKingMove() || findPawnMove() || findKnightMove() || findQueenMove() ||
           findRookMove() || findBishopMove();
  }

  /**
   * Find a King move and return immediately if found. No need to check castling extra to find a
   * legal move.
   *
   * @return true if a move has been found
   */
  private boolean findKingMove() {
    PieceType type = PieceType.KING;
    Square square = position.getKingSquares()[activePlayer.ordinal()];
    return findMove(type, square, Square.kingDirections);
  }

  /**
   * Find a Knight move and return immediately if found.
   *
   * @return true if a move has been found
   */
  private boolean findKnightMove() {
    PieceType type = PieceType.KNIGHT;
    final SquareList squareList = position.getKnightSquares()[activePlayer.ordinal()];
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
    final SquareList squareList = position.getQueenSquares()[activePlayer.ordinal()];
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
    final SquareList squareList = position.getBishopSquares()[activePlayer.ordinal()];
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
    final SquareList squareList = position.getRookSquares()[activePlayer.ordinal()];
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
    int move;
    for (int d : pieceDirections) {
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = position.getX88Board()[to];
        // free square - non capture
        if (target == Piece.NOPIECE) { // empty
          move = Move.createMove(MoveType.NORMAL, Square.getSquare(square.ordinal()),
                                 Square.getSquare(to), Piece.getPiece(type, activePlayer), target,
                                 Piece.NOPIECE);
          if (isLegalMove(move)) return true;
        }
        // occupied square - capture if opponent and stop sliding
        else {
          if (target.getColor() == activePlayer.getInverseColor()) { // opponents color
            move = Move.createMove(MoveType.NORMAL, Square.getSquare(square.ordinal()),
                                   Square.getSquare(to), Piece.getPiece(type, activePlayer), target,
                                   Piece.NOPIECE);
            if (isLegalMove(move)) return true;
          }
          break; // stop sliding;
        }
        if (type.isSliding()) {
          to += d; // next sliding field in this direction
        } else {
          break; // no sliding piece type
        }
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
    int move;

    // reverse direction of pawns for black
    final int pawnDir = activePlayer.isBlack() ? -1 : 1;

    // iterate over all squares where we have a pawn
    final SquareList squareList = position.getPawnSquares()[activePlayer.ordinal()];
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
          final Piece piece = Piece.getPiece(PieceType.PAWN, activePlayer);
          final Piece target = position.getX88Board()[to];
          final Piece promotion = Piece.NOPIECE;
          // capture
          if (d != Square.N) { // not straight
            if (target != Piece.NOPIECE // not empty
                && (target.getColor() == activePlayer.getInverseColor())) { // opponents color
              move = Move.createMove(type, fromSquare, toSquare, piece, target, promotion);
              if (isLegalMove(move)) return true;
            } else { // empty but maybe en passant
              if (toSquare == position.getEnPassantSquare()) { //  en passant capture
                // which target?
                final int t = activePlayer.isWhite()
                              ? position.getEnPassantSquare().getSouth().ordinal()
                              : position.getEnPassantSquare().getNorth().ordinal();
                move = Move.createMove(MoveType.ENPASSANT, fromSquare, toSquare, piece,
                                       position.getX88Board()[t], promotion);
                if (isLegalMove(move)) return true;
              }
            }
          }
          // no capture
          else { // straight
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
    position.makeMove(move);
    // check if the move leaves the king in check
    if (!position.isAttacked(activePlayer.getInverseColor(),
                             position.getKingSquares()[activePlayer.ordinal()])) {
      position.undoMove();
      return true;
    }
    position.undoMove();
    return false;
  }

  /**
   * Clears all lists
   */
  private void clearLists() {
    onDemandMoveList.clear();
    legalMoves.clear();
    pseudoLegalMoves.clear();
    capturingMoves.clear();
    nonCapturingMoves.clear();
    castlingMoves.clear();
  }

}
