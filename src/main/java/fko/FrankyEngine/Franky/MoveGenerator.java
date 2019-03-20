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

import fko.FrankyEngine.util.SimpleIntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * The move generator.
 * <p>
 * It generates pseudo legal and legal moves for a given position.
 * <p>
 * Either generate a MoveGenerator with a specific position or set the position with
 * <code>setPosition</code> before doing any searches. After setting a position you can
 * add killer moves <code>setKillerMoves</code> and pv moves <code>setPVMove</code>
 * to consider during move generation.
 * <p>
 * Setting a position resets the internal state of the MoveGenerator. Especially
 * for OnDemand move generation which needs to keep a state in between calls.
 *
 * @author Frank Kopp
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class MoveGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(MoveGenerator.class);

  private final Configuration config;

  boolean SORT_CAPTURING_MOVES = true;
  boolean SORT_MOVES           = false;

  // the current position we generate the move for
  // is set in the getMoves methods
  private Position position = null;

  // which color do we generate moves for
  private Color activePlayer;

  // these are are working lists as fields to avoid to have to
  // create them during each move generation. Instead of creating the need
  // to be cleared before use.
  private final MoveList legalMoves        = new MoveList();
  // these are all pseudo legal
  private final MoveList pseudoLegalMoves  = new MoveList(); // all moves
  private final MoveList capturingMoves    = new MoveList(); // only capturing moves
  private final MoveList nonCapturingMoves = new MoveList(); // only non capturing moves
  // special list for qsearch
  private       MoveList qSearchMoves      = new MoveList();

  // These fields control the on demand generation of moves.
  private              OnDemandState generationCycleState = OnDemandState.NEW;
  private static final int           GEN_CAPTURES         = 1;
  private static final int           GEN_NONCAPTURES      = 2;
  private static final int           GEN_ALL              = 3;
  private              int           genMode              = GEN_ALL;

  private enum OnDemandState {NEW, CAPTURING, NON_CAPTURING, ALL}

  private MoveList onDemandMoveList = new MoveList();

  // these field influence the move sorting as pv and killer moves are typically searched early
  private MoveList killerMoves = new MoveList(0);
  private int      pvMove      = Move.NOMOVE;

  // Comparator for move most value victim least value attacker (incl. promotion)
  private static final SimpleIntList.IntComparator mvvlvaComparator = (move1, move2) ->
    (Move.getPiece(move1).getType().getValue()
      - Move.getPromotion(move1).getType().getValue()
      - Move.getTarget(move1).getType().getValue())
      - (Move.getPiece(move2).getType().getValue()
      - Move.getPromotion(move2).getType().getValue()
      - Move.getTarget(move2).getType().getValue());

  /**
   * Creates a new {@link MoveGenerator}
   */
  public MoveGenerator() {
    this(new Configuration());
  }

  /**
   * Creates a new {@link MoveGenerator}
   * @param config
   */
  public MoveGenerator(Configuration config) {
    this.config = config;
  }

  /**
   * Creates a new {@link MoveGenerator}
   */
  public MoveGenerator(Position position) {
    this(new Configuration());
    setPosition(position);
  }

  /**
   * Creates a new {@link MoveGenerator}
   */
  public MoveGenerator(Position position, Configuration config) {
    this(config);
    setPosition(position);
  }

  /**
   * Sets the position of this move generator.
   * <p>
   * Setting the position resets the move generator. Killers or PV moves set via
   * <code>setKillerMoves</code> and <code>setPVMove</code> are deleted.
   *
   * @param position
   */
  public void setPosition(Position position) {
    assert position != null : "parameter null not allowed";
    this.position = position;
    this.activePlayer = position.getNextPlayer();
    this.generationCycleState = OnDemandState.NEW;
    this.pvMove = Move.NOMOVE;
    this.genMode = GEN_ALL;
    this.killerMoves.clear();
    clearLists();
  }

  /**
   * Sets the PV move so it will be returned first. Need to be set after each call to
   * <code>setPosition</code> as this reset the killer moves.
   * <p>
   * <b>Attention:</b> needs to be a valid move in the current position otherwise will break!
   * This could be the case when an unlikely TT Collision happens.
   *
   * @param move
   */
  public void setPVMove(int move) {
    this.pvMove = move;
  }

  /**
   * Sets killer moves which will be inserted after capturing moves. Need to be set after each call
   * to <code>setPosition</code> as this reset the killer moves.
   *
   * @param killerMoves
   */
  public void setKillerMoves(MoveList killerMoves) {
    assert killerMoves != null : "parameter null not allowed";
    this.killerMoves.clear();
    for (int m = 0, size = killerMoves.size(); m < size; m++) {
      this.killerMoves.add(killerMoves.get(m));
    }
  }

  /**
   * Returns the next move of the current generation cycle.<br>
   * This method uses an on-demand generation of moves starting with potentially high value moves
   * first to improve cut off rates in AlphaBeta pruning and therefore avoiding the cost of
   * generating all possible moves.
   * <p>
   * The generation cycle starts new with each new call to <code>setPosition</code>.
   *
   * @return int representing the next legal Move. Returns Move.NOMOVE if none available
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
    while (onDemandMoveList.empty() && generationCycleState != OnDemandState.ALL) {

      switch (generationCycleState) {
        case NEW:
          generationCycleState = OnDemandState.CAPTURING;
          // fall through

        case CAPTURING:

          genMode = GEN_CAPTURES;
          generateCapturingMoves();

          if (SORT_CAPTURING_MOVES) capturingMoves.sort(mvvlvaComparator);

          // Setting pv move
          // setting all pv moves for capturing and non capturing move
          // !!! must be valid move on the current position
          // Won't check/assert it here as this would be too expensive
          if (pvMove != Move.NOMOVE) {
            // pvMove is capturing
            if (Move.isCapturing(pvMove)) {
              if (!capturingMoves.pushToHeadStable(pvMove)) {
                LOG.warn("pvMove was not a valid capturing move");
              }
            }
            // pvMove is non capturing
            else {
              // only add it if we do want non capturing moves
              // Move does need to be removed from non capturing moves later
              // to not have it twice.
              if (!capturingOnly) onDemandMoveList.add(pvMove);
            }
          }

          // fill onDemand list
          onDemandMoveList.add(capturingMoves);

          // all capturing moves are generated
          if (capturingOnly) generationCycleState = OnDemandState.ALL;
          else generationCycleState = OnDemandState.NON_CAPTURING;
          break;

        case NON_CAPTURING:
          assert !capturingOnly;

          generateNonCapturingMoves();

          // full sort of non-capturing moves
          moveListSort(nonCapturingMoves);

          // fill onDemand list
          onDemandMoveList.add(nonCapturingMoves);

          // removing pv move as this has been handled in CAPTURING phase.
          if (pvMove != Move.NOMOVE && !Move.isCapturing(pvMove)) onDemandMoveList.remove(pvMove);

          generationCycleState = OnDemandState.ALL;
          break;
      }
    }

    // return a move and delete it form the list
    if (onDemandMoveList.empty()) return Move.NOMOVE;
    else return onDemandMoveList.removeFirst();
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
    return this.streamLegalMoves(false);
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
    return this.getLegalMoves(fullSort).stream();
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

    // call the move generators
    genMode = GEN_ALL;
    generatePseudoLegaMoves();

    // filter legal moves
    for (int move : pseudoLegalMoves) {
      if (isLegalMove(move)) legalMoves.add(move);
    }

    if (fullSort) moveListSort(legalMoves);

    return legalMoves;
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
    return this.getPseudoLegalMoves(capturingOnly).stream();
  }

  /**
   * Generates <b>capturing</b> moves for a position. These moves may leave the king in check and
   * may be
   * illegal.<br>
   * Before committing them to a board they need to be checked if they leave the king in check.
   *
   * <p><b>Attention:</b> returns a reference to the list of moves which will change after calling
   * this again.<br>
   * Make a clone if this is not desired.
   *
   * @return a reference to the list of moves which may leave the king in check
   */
  public MoveList getPseudoLegalMoves(boolean capturingOnly) {
    // protect against null position
    if (position == null) {
      throw new IllegalStateException("Position not set. Set position before calling this");
    }

    clearLists();

    if (capturingOnly) genMode = GEN_CAPTURES;
    else genMode = GEN_ALL;

    generatePseudoLegaMoves();

    // return a clone of the list as we will continue to reuse
    return pseudoLegalMoves;
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

    // call the move generators
    genMode = GEN_ALL;
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

    // if in check generate all moves otherwise only capture moves
    if (position.hasCheck()) {
      genMode = GEN_ALL;
      // call the move generators
      if (pseudoLegalMoves.empty()) {
        generatePseudoLegaMoves();
      }
      return pseudoLegalMoves;
    }
    // not in check - only generate captures
    else {
      genMode = GEN_CAPTURES;
      if (capturingMoves.empty()) {
        generateCapturingMoves();
      }
    }

    // lower amount of captures searched in quiescence search by only looking at "good" captures
    qSearchMoves.clear();
    for (int m = 0, size = capturingMoves.size(); m < size; m++) {
      int move = capturingMoves.get(m);

      // use full SEE to determine good captures
      if (config.USE_SEE) {
        if (Attacks.see(position, move) > 0) qSearchMoves.add(move);
      }

      // use simple good-capture filter
      else {
        // all pawn captures - they never loose material
        if (Move.getPiece(move).getType() == PieceType.PAWN) qSearchMoves.add(move);
          // recaptures
        else if (position.getLastMove() != Move.NOMOVE
          && Move.getEnd(position.getLastMove()) == Move.getEnd(move)
          && Move.getTarget(position.getLastMove()) != Piece.NOPIECE) {
          qSearchMoves.add(move);
        }
        // Lower value piece captures higher value piece
        // With a margin to also look at Bishop x Knight
        else if (Move.getPiece(move).getType().getValue() + 50
          <= Move.getTarget(move).getType().getValue()) {
          qSearchMoves.add(move);
        }
        // undefended pieces captures are good
        // If the defender is "behind" the attacker this will not be recognized here
        // This is not too bad as it only adds a move to qsearch which we could otherwise ignore
        else if (!position.isAttacked(position.getOpponent(), Move.getEnd(move))) {
          qSearchMoves.add(move);
        }
        // ignore all other captures
      }
    }
    if (SORT_CAPTURING_MOVES) qSearchMoves.sort(mvvlvaComparator);
    // return a clone of the list as we will continue to reuse
    return qSearchMoves;
  }

  /**
   * Generates all pseudo legal moves from the given position.
   */
  private void generatePseudoLegaMoves() {

    // CAPTURING ONLY
    if (genMode == GEN_CAPTURES) {

      // only generate if not already filled
      if (capturingMoves.empty()) {
        generateCapturingMoves();
        if (SORT_CAPTURING_MOVES) capturingMoves.sort(mvvlvaComparator);
      }

      // Setting pv move
      if (pvMove != Move.NOMOVE && Move.isCapturing(pvMove)) {
        capturingMoves.pushToHeadStable(pvMove);
      }

      // now we have all capturing moves
      pseudoLegalMoves.add(capturingMoves);
    }
    // ALL: CAPTURING & NON CAPTURING
    else {

      // do we already have a valid list?
      if (!pseudoLegalMoves.empty() && !capturingMoves.empty() && !nonCapturingMoves.empty()) {
        return;
      }

      // pseudo list needs to be generated
      if (capturingMoves.empty()) {
        generateCapturingMoves();
      }
      if (nonCapturingMoves.empty()) {
        generateNonCapturingMoves();
      }

      // sort all moves
      if (SORT_MOVES) {
        // sort over all moves
        pseudoLegalMoves.add(capturingMoves);
        pseudoLegalMoves.add(nonCapturingMoves);
        moveListSort(pseudoLegalMoves);
      }
      else {
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
  }

  private void generateAllMoves() {
    int oldMode = genMode;
    genMode = GEN_ALL;
    capturingMoves.clear();
    nonCapturingMoves.clear();
    generateCastlingMoves();
    generatePawnMoves();
    generateKnightMoves();
    generateBishopMoves();
    generateRookMoves();
    generateQueenMoves();
    generateKingMoves();
    genMode = oldMode;
  }

  private void generateNonCapturingMoves() {
    int oldMode = genMode;
    genMode = GEN_NONCAPTURES;
    nonCapturingMoves.clear();
    generateCastlingMoves();
    generatePawnMoves();
    generateKnightMoves();
    generateBishopMoves();
    generateRookMoves();
    generateQueenMoves();
    generateKingMoves();
    genMode = oldMode;
  }

  private void generateCapturingMoves() {
    int oldMode = genMode;
    capturingMoves.clear();
    genMode = GEN_CAPTURES;
    generatePawnMoves();
    generateKnightMoves();
    generateBishopMoves();
    generateRookMoves();
    generateQueenMoves();
    generateKingMoves();
    genMode = oldMode;
  }

  /**
   * Sort value for all moves. Smaller values heapsort first
   */
  private int getSortValue(int move) {
    // capturing moves including capturing promotions
    if (Move.getTarget(move) != Piece.NOPIECE) {
      return Move.getPiece(move).getType().getValue() - Move.getPromotion(move).getType().getValue()
        - Move.getTarget(move).getType().getValue();
    }
    // non capturing
    else {
      // killer moves
      final int idx = killerMoves.indexOf(move);
      if (idx >= 0) {
        return 8000 + idx;
      }
      // promotions
      final PieceType pieceType = Move.getPromotion(move).getType();
      if (pieceType != PieceType.NOTYPE) {
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
      else if (Move.getMoveType(move) == MoveType.CASTLING) {
        return 9200;
      }
      // all other moves
      return 10000 - Evaluation.getPositionValue(position, move);
    }
  }

  /**
   * Sorts the given movelist in ascending order according the results
   * of <code>getSortValue</code>.
   *
   * @param moveList
   */
  private int[] sortIdx = new int[250]; // prepare array for sort
  private void moveListSort(MoveList moveList) {
    // create index array - this is faster then to call getSortValue() every
    // time a compare takes place
    // we re-use a prepared array - does not have to be re-initialized as
    // it will be overwritten only used up to the overwritten index.
    for (int i = 0, size = moveList.size(); i < size; i++) {
      sortIdx[i] = getSortValue(moveList.get(i));
    }
    // insertion sort
    int ts;
    for (int i = 0, size = moveList.size(); i < size; i++) {
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
   * Pushing killer moves to the front of the given moveList if there are any and
   * if they are even in the list.
   *
   * @param moveList
   */
  private void pushKillerMoves(final MoveList moveList) {
    if (nonCapturingMoves.size() > 0) {
      for (int i = killerMoves.size() - 1; i >= 0; i--) {
        assert killerMoves.get(i) != Move.NOMOVE;
        moveList.pushToHeadStable(killerMoves.get(i));
      }
    }
  }

  private void generatePawnMoves() {
    // iterate over all squares where we have a pawn
    for (int i = 0, size = position.getPawnSquares()[activePlayer.ordinal()].size();
         i < size;
         i++) {

      final Square square = position.getPawnSquares()[activePlayer.ordinal()].get(i);
      assert position.getPiece(square).getType() == PieceType.PAWN;

      // get all possible x88 index values for pawn moves
      // these are basically int values to add or subtract from the
      // current square index. Very efficient with a x88 board.
      int[] pawnDirections = Square.pawnDirections;
      for (int j = 0, length = pawnDirections.length; j < length; j++) {
        int d = pawnDirections[j];

        // calculate the to square
        final int to = square.ordinal() + d * activePlayer.factor;

        if ((to & 0x88) == 0) { // valid square

          final MoveType type = MoveType.NORMAL;
          final Square fromSquare = Square.getSquare(square.ordinal());
          final Square toSquare = Square.getSquare(to);
          final Piece piece = Piece.getPiece(PieceType.PAWN, activePlayer);
          final Piece target = position.getPiece(to);
          final Piece promotion = Piece.NOPIECE;

          // capture
          if (d != Square.N) { // not straight
            if ((genMode & GEN_CAPTURES) > 0) {  // generating captures?
              if (target != Piece.NOPIECE // not empty
                && (target.getColor() == activePlayer.inverse())) { // opponents color
                assert target.getType() != PieceType.KING; // did we miss a check?
                // capture & promotion
                // rank 8
                if (to > 111)
                  makePromotionMove(fromSquare, toSquare, piece, target, capturingMoves);
                else // rank 1
                  if (to < 8)
                    makePromotionMove(fromSquare, toSquare, piece, target, capturingMoves);
                  else { // normal capture
                    capturingMoves.add(
                      Move.createMove(type, fromSquare, toSquare, piece, target, promotion));
                  }
              }
              else { // empty but maybe en passant
                if (toSquare == position.getEnPassantSquare()) { //  en passant capture
                  // which target?
                  final int t = activePlayer.isWhite()
                                ? position.getEnPassantSquare().getSouth().ordinal()
                                : position.getEnPassantSquare().getNorth().ordinal();
                  capturingMoves.add(
                    Move.createMove(MoveType.ENPASSANT, fromSquare, toSquare, piece,
                                    position.getPiece(t), promotion));
                }
              }
            }
          }
          // no capture
          else { // straight
            if ((genMode & GEN_NONCAPTURES) > 0 // generate non captures
              && target == Piece.NOPIECE) { // way needs to be free
              // promotion
              // rank 8
              if (to > 111)
                makePromotionMove(fromSquare, toSquare, piece, target, nonCapturingMoves);
              else // rank 1
                if (to < 8)
                  makePromotionMove(fromSquare, toSquare, piece, target, nonCapturingMoves);
                else {
                  // pawndouble
                  if (activePlayer.isWhite() && fromSquare.isWhitePawnBaseRow()
                    && (position.getPiece(fromSquare.ordinal() + (2 * Square.N)))
                    == Piece.NOPIECE) {
                    // on rank 2 && rank 4 is free(rank 3 already checked via target)
                    nonCapturingMoves.add(
                      Move.createMove(MoveType.PAWNDOUBLE, fromSquare, toSquare.getNorth(), piece,
                                      target, promotion));
                  }
                  else if (activePlayer.isBlack() && fromSquare.isBlackPawnBaseRow()
                    && position.getPiece(fromSquare.ordinal() + (2 * Square.S))
                    == Piece.NOPIECE) {
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

  private void makePromotionMove(Square fromSquare, Square toSquare, Piece piece, Piece target,
                                 MoveList capturingMoves) {
    capturingMoves.add(
      Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                      piece.getColor().isWhite() ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN));
    capturingMoves.add(
      Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                      piece.getColor().isWhite() ? Piece.WHITE_ROOK : Piece.BLACK_ROOK));
    capturingMoves.add(
      Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                      piece.getColor().isWhite() ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP));
    capturingMoves.add(
      Move.createMove(MoveType.PROMOTION, fromSquare, toSquare, piece, target,
                      piece.getColor().isWhite() ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT));
  }

  private void generateKnightMoves() {
    final PieceType type = PieceType.KNIGHT;
    // iterate over all squares where we have a piece
    for (int i = 0, size = position.getKnightSquares()[activePlayer.ordinal()].size();
         i < size;
         i++) {
      final Square square = position.getKnightSquares()[activePlayer.ordinal()].get(i);
      assert position.getPiece(square).getType() == type;
      generateMoves(type, square, Square.knightDirections);
    }
  }

  private void generateBishopMoves() {
    final PieceType type = PieceType.BISHOP;
    // iterate over all squares where we have this piece type
    for (int i = 0, size = position.getBishopSquares()[activePlayer.ordinal()].size();
         i < size;
         i++) {
      final Square square = position.getBishopSquares()[activePlayer.ordinal()].get(i);
      assert position.getPiece(square).getType() == type;
      generateMoves(type, square, Square.bishopDirections);
    }
  }

  private void generateRookMoves() {
    final PieceType type = PieceType.ROOK;
    // iterate over all squares where we have this piece type
    for (int i = 0, size = position.getRookSquares()[activePlayer.ordinal()].size();
         i < size;
         i++) {
      final Square square = position.getRookSquares()[activePlayer.ordinal()].get(i);
      assert position.getPiece(square).getType() == type;
      generateMoves(type, square, Square.rookDirections);
    }
  }

  private void generateQueenMoves() {
    final PieceType type = PieceType.QUEEN;
    // iterate over all squares where we have this piece type
    for (int i = 0, size = position.getQueenSquares()[activePlayer.ordinal()].size();
         i < size;
         i++) {
      final Square square = position.getQueenSquares()[activePlayer.ordinal()].get(i);
      assert position.getPiece(square).getType() == type;
      generateMoves(type, square, Square.queenDirections);
    }
  }

  private void generateKingMoves() {
    final PieceType type = PieceType.KING;
    Square square = position.getKingSquares()[activePlayer.ordinal()];
    assert position.getPiece(square.ordinal()).getType() == type;
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
    for (int i = 0, pieceDirectionsLength = pieceDirections.length;
         i < pieceDirectionsLength;
         i++) {

      int d = pieceDirections[i];
      int to = square.ordinal() + d;

      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = position.getPiece(to);

        // free square - non capture
        if (target == Piece.NOPIECE) { // empty
          if ((genMode & GEN_NONCAPTURES) > 0) { // generate non captures
            nonCapturingMoves.add(Move.createMove(MoveType.NORMAL, square, Square.getSquare(to),
                                                  Piece.getPiece(type, activePlayer), target,
                                                  Piece.NOPIECE));
          }
        }
        // occupied square - capture if opponent and stop sliding
        else {
          if ((genMode & GEN_CAPTURES) > 0) { // generate captures
            if (target.getColor() == activePlayer.inverse()) { // opponents color
              assert target.getType() != PieceType.KING; // did we miss a check?
              capturingMoves.add(Move.createMove(MoveType.NORMAL, square, Square.getSquare(to),
                                                 Piece.getPiece(type, activePlayer), target,
                                                 Piece.NOPIECE));
            }
          }
          break;
        }

        if (type.isSliding()) to += d; // next sliding field in this factor
        else break; // no sliding piece type
      }
    }
  }

  private void generateCastlingMoves() {

    if (position.hasCheck() // no castling if we are in check
      || (genMode & GEN_NONCAPTURES) == 0) // only when generating non captures
      return;

    // iterate over all available castlings at this position
    if (activePlayer.isWhite()) {
      if (position.isCastlingWK()) {
        // f1 free, g1 free and f1 not attacked
        // we will not check if g1 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getPiece(Square.f1.ordinal()) == Piece.NOPIECE // passing square free
          // TODO move this to search
          && !position.isAttacked(activePlayer.inverse(), Square.f1) // passing square not attacked
          && position.getPiece(Square.g1.ordinal()) == Piece.NOPIECE) // to square free
        {
          nonCapturingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e1, Square.g1, Piece.WHITE_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
        }
      }
      if (position.isCastlingWQ()) {
        // d1 free, c1 free and d1 not attacked
        // we will not check if d1 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getPiece(Square.d1.ordinal()) == Piece.NOPIECE // passing square free
          && position.getPiece(Square.b1.ordinal()) == Piece.NOPIECE // rook passing square free
          // TODO move this to search
          && !position.isAttacked(activePlayer.inverse(), Square.d1) // passing square not attacked
          && position.getPiece(Square.c1.ordinal()) == Piece.NOPIECE) // to square free
        {
          nonCapturingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e1, Square.c1, Piece.WHITE_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
        }
      }
    }
    else {
      if (position.isCastlingBK()) {
        // f8 free, g8 free and f8 not attacked
        // we will not check if g8 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getPiece(Square.f8.ordinal()) == Piece.NOPIECE // passing square free
          // TODO move this to search
          && !position.isAttacked(activePlayer.inverse(), Square.f8) // passing square not attacked
          && position.getPiece(Square.g8.ordinal()) == Piece.NOPIECE) // to square free
        {
          nonCapturingMoves.add(
            Move.createMove(MoveType.CASTLING, Square.e8, Square.g8, Piece.BLACK_KING,
                            Piece.NOPIECE, Piece.NOPIECE));
        }
      }
      if (position.isCastlingBQ()) {
        // d8 free, c8 free and d8 not attacked
        // we will not check if d8 is attacked as this is a pseudo legal move
        // and this to be checked separately e.g. when filtering for legal moves
        if (position.getPiece(Square.d8.ordinal()) == Piece.NOPIECE // passing square free
          && position.getPiece(Square.b8.ordinal()) == Piece.NOPIECE // rook passing square free
          // TODO move this to search
          && !position.isAttacked(activePlayer.inverse(), Square.d8)
          // passing square not attacked
          && position.getPiece(Square.c8.ordinal()) == Piece.NOPIECE) // to square free
        {
          nonCapturingMoves.add(
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
   * <p>
   * Sets a new position (see <code>setPosition</code>,
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
    return findKingMove() || findPawnMove() || findKnightMove() || findQueenMove() || findRookMove()
      || findBishopMove();
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
    for (int i = 0, size = squareList.size(); i < size; i++) {
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
    for (int i = 0, size = squareList.size(); i < size; i++) {
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
    for (int i = 0, size = squareList.size(); i < size; i++) {
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
    final SquareList rookSquare = position.getRookSquares()[activePlayer.ordinal()];
    for (int i = 0, size = rookSquare.size(); i < size; i++) {
      final Square os = rookSquare.get(i);
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
    for (int i = 0, pieceDirectionsLength = pieceDirections.length;
         i < pieceDirectionsLength;
         i++) {

      int d = pieceDirections[i];
      int to = square.ordinal() + d;
      while ((to & 0x88) == 0) { // slide while valid square
        final Piece target = position.getPiece(to);
        // free square - non capture
        if (target == Piece.NOPIECE) { // empty
          move = Move.createMove(MoveType.NORMAL, Square.getSquare(square.ordinal()),
                                 Square.getSquare(to), Piece.getPiece(type, activePlayer), target,
                                 Piece.NOPIECE);
          if (isLegalMove(move)) return true;
        }
        // occupied square - capture if opponent and stop sliding
        else {
          if (target.getColor() == activePlayer.inverse()) { // opponents color
            move = Move.createMove(MoveType.NORMAL, Square.getSquare(square.ordinal()),
                                   Square.getSquare(to), Piece.getPiece(type, activePlayer), target,
                                   Piece.NOPIECE);
            if (isLegalMove(move)) return true;
          }
          break; // stop sliding;
        }

        if (type.isSliding()) to += d; // next sliding field in this factor
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
    int move;

    // iterate over all squares where we have a pawn
    for (int i = 0, size = position.getPawnSquares()[activePlayer.ordinal()].size();
         i < size;
         i++) {

      final Square square = position.getPawnSquares()[activePlayer.ordinal()].get(i);

      // get all possible x88 index values for pawn moves
      // these are basically int values to add or subtract from the
      // current square index. Very efficient with a x88 board.
      int[] directions = Square.pawnDirections;
      for (int i1 = 0, directionsLength = directions.length; i1 < directionsLength; i1++) {
        int d = directions[i1];
        // calculate the to square
        final int to = square.ordinal() + d * activePlayer.factor;
        if ((to & 0x88) == 0) { // valid square
          final MoveType type = MoveType.NORMAL;
          final Square fromSquare = Square.getSquare(square.ordinal());
          final Square toSquare = Square.getSquare(to);
          final Piece piece = Piece.getPiece(PieceType.PAWN, activePlayer);
          final Piece target = position.getPiece(to);
          final Piece promotion = Piece.NOPIECE;
          // capture
          if (d != Square.N) { // not straight
            if (target != Piece.NOPIECE // not empty
              && (target.getColor() == activePlayer.inverse())) { // opponents color
              move = Move.createMove(type, fromSquare, toSquare, piece, target, promotion);
              if (isLegalMove(move)) return true;
            }
            else { // empty but maybe en passant
              if (toSquare == position.getEnPassantSquare()) { //  en passant capture
                // which target?
                final int t = activePlayer.isWhite()
                              ? position.getEnPassantSquare().getSouth().ordinal()
                              : position.getEnPassantSquare().getNorth().ordinal();
                move = Move.createMove(MoveType.ENPASSANT, fromSquare, toSquare, piece,
                                       position.getPiece(t), promotion);
                if (isLegalMove(move)) return true;
              }
            }
          }
          // no capture
          else { // straight
            if (target == Piece.NOPIECE) { // way needs to be free
              move = Move.createMove(type, fromSquare, toSquare, piece, target, promotion);
              if (isLegalMove(move)) return true;
              // double pawn push
              if (fromSquare.isPawnBaseRow(activePlayer)) {
                Square toSquare2 = Square.getSquare(to + d * activePlayer.factor);
                final Piece target2 = position.getPiece(toSquare2);
                if (target2 == Piece.NOPIECE) { // way needs to be free
                  final MoveType type2 = MoveType.PAWNDOUBLE;
                  move = Move.createMove(type2, fromSquare, toSquare2, piece, target2, promotion);
                  if (isLegalMove(move)) return true;
                }
              }
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
    // TODO check castling intermediate square

    // check if the move leaves the king in check
    if (!position.isAttacked(activePlayer.inverse(),
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
  }

}
