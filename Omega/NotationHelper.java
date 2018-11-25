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

import fko.chessly.game.pieces.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author fkopp */
public class NotationHelper {

  public static final String StandardBoardFEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /**
   * Create a new Move from a PGN SAN notation String "e4"
   *
   * <p>Also accepts LAN notation.
   *
   * @param board
   * @param origNotation String
   * @return
   * @throws InvalidMoveException
   */
  public static GameMove createNewMoveFromSANNotation(GameBoard board, String origNotation)
      throws InvalidMoveException {

    //       * <p><SAN move descriptor piece moves> ::= <Piece symbol>[<from file>|<from rank>|<from
    //       * square>]['x']<to square> <SAN move descriptor pawn captures> ::= <from file>[<from
    // rank>] 'x'
    //       * <to square>[<promoted to>] <SAN move descriptor pawn push> ::= <to square>[<promoted
    // to>] <SAN
    //       * move descriptor castle> ::= O-O[-O]

    GameColor color = board.getNextPlayerColor();

    String notation = origNotation;
    GameMove move = null;
    GamePiece piece = null;
    GamePiece promotedPiece = null;
    GamePosition toField = null;
    boolean captureMove = false;
    String fromFile = "";
    String fromRank = "";
    GamePosition fromField = null;

    // Remove # or + or ++ signs = don't need them for generating a move
    notation = notation.replaceAll("[!?#+=]", "");
    // Remove optional "e.p."
    notation = notation.replaceAll("e.p.", "");

    // legal input pattern
    String patternPiece = "([KQRBN])([a-h]?)([1-8]?)([-x]?)([a-h])([1-8])";
    String patternPawn = "([a-h]?)([1-8]?)([-x]?)([a-h])([1-8])([KQRBN]?)";
    String patternCastle = "([Oo0]-[Oo0])(-[Oo0])?";

    // Piece move
    if (notation.matches(patternPiece)) {

      Pattern pattern = Pattern.compile(patternPiece);
      Matcher matcher = pattern.matcher(notation);
      matcher.matches();

      // Piece
      String pieceLetter = matcher.group(1);
      switch (pieceLetter) {
        case "N":
          piece = Knight.create(color);
          break;
        case "B":
          piece = Bishop.create(color);
          break;
        case "R":
          piece = Rook.create(color);
          break;
        case "Q":
          piece = Queen.create(color);
          break;
        case "K":
          piece = King.create(color);
          break;
        default:
          InvalidMoveException e =
              new InvalidMoveException("SAN Syntax not valid - expected KQRBN");
          //// System.err.println(e.toString());
          throw e;
      }
      notation =
          notation.substring(matcher.end(1), notation.length()); // remove from the input string

      // Pawn move
    } else if (notation.matches(patternPawn)) {

      Pattern pattern = Pattern.compile(patternPawn);
      Matcher matcher = pattern.matcher(notation);
      matcher.matches();

      // Piece
      piece = Pawn.create(color);

      // Promotion
      String promotionLetter = matcher.group(6);
      if (!promotionLetter.isEmpty()) {
        switch (promotionLetter) {
          case "N":
            promotedPiece = Knight.create(color);
            break;
          case "B":
            promotedPiece = Bishop.create(color);
            break;
          case "R":
            promotedPiece = Rook.create(color);
            break;
          case "Q":
            promotedPiece = Queen.create(color);
            break;
          default:
            InvalidMoveException e =
                new InvalidMoveException("SAN Syntax not valid - expected KQRBN");
            // System.err.println(e.toString());
            throw e;
        }
        notation = notation.substring(0, matcher.start(6)); // remove from the input string
      }

      // Castle
    } else if (notation.matches(patternCastle)) {

      Pattern pattern = Pattern.compile(patternCastle);
      Matcher matcher = pattern.matcher(notation);
      matcher.matches();

      // set giece
      piece = King.create(color);

      // castling
      GameCastling castling = GameCastling.NOCASTLING;
      fromFile = "e";
      fromRank = color.isWhite() ? "1" : "8";
      fromField = GamePosition.getGamePosition(fromFile + fromRank);
      if (matcher.group(2) == null) { // king side
        toField = GamePosition.getGamePosition("g" + fromRank);
        castling = color.isWhite() ? GameCastling.WHITE_KINGSIDE : GameCastling.BLACK_KINGSIDE;
      } else { // queen side
        toField = GamePosition.getGamePosition("c" + fromRank);
        castling = color.isWhite() ? GameCastling.WHITE_QUEENSIDE : GameCastling.BLACK_QUEENSIDE;
      }
      move = new GameMoveImpl(fromField, toField, piece);
      move.setCastlingType(castling);

    } else {
      InvalidMoveException e = new InvalidMoveException("Not a valid SAN Notation: " + notation);
      // System.err.println(e.toString());
      throw e;
    }

    if (move == null) { // was not a castle

      // Rest should be like this: [a-h]?[1-8]?x?[a-h][1-8]
      Pattern pattern = Pattern.compile("([a-h]?)([1-8]?)([-x]?)([a-h][1-8])");
      Matcher matcher = pattern.matcher(notation);

      if (!matcher.matches()) {
        InvalidMoveException e = new InvalidMoveException("Not a valid SAN Notation: " + notation);
        // System.err.println(e.toString());
        throw e;
      }
      ;

      // To Field
      toField = GamePosition.getGamePosition(matcher.group(4));

      // Capture
      if (matcher.group(3).equals("x")) {
        captureMove = true;
      }

      // get from field information if any
      if (!matcher.group(1).isEmpty() && !matcher.group(2).isEmpty()) {
        fromField = GamePosition.getGamePosition(matcher.group(1) + matcher.group(2));
      } else if (!matcher.group(1).isEmpty()) {
        fromFile = matcher.group(1);
      } else if (!matcher.group(2).isEmpty()) {
        fromRank = matcher.group(2);
      }

      // find matching moves from position
      GameMoveList moveList = board.generateMoves();
      GameMoveList sameToField = new GameMoveList();
      for (GameMove m : moveList) { // all possible legal moves
        GamePosition m_toField = m.getToField();
        GamePiece m_Piece = m.getMovedPiece();
        if (piece.equals(m_Piece) && toField.equals(m_toField)) {
          // when promotion move also check promoted to piece
          if (promotedPiece != null) {
            if (promotedPiece.equals(m.getPromotedTo())) {
              sameToField.add(m);
            }
          } else {
            sameToField.add(m);
          }
        }
      }

      // check ambiguity
      if (sameToField.isEmpty()) { // invalid input
        InvalidMoveException e = new InvalidMoveException("Not a valid move: " + origNotation);
        // System.err.println(e.toString());
        throw e;

      } else if (sameToField.size() == 1) { // only one move matches
        move = sameToField.get(0);

      } else { // several matches
        GameMoveList matchingMoves = new GameMoveList();
        // iterate over all moves with same toField and add all matching
        // moves to a list. When all is good there should only be one move in the list.
        for (GameMove m : sameToField) {
          if (fromField != null) { // we already know our fromField
            if (m.getFromField().equals(fromField)) {
              matchingMoves.add(m);
            }
          } else if (!fromRank.isEmpty()) {
            if (m.getFromField().getRank() == Integer.parseInt(fromRank)) {
              matchingMoves.add(m);
            }
          } else if (!fromFile.isEmpty()) {
            if (GamePosition.getColString(m.getFromField().getFile()).equals(fromFile)) {
              matchingMoves.add(m);
            }
          } else {
            InvalidMoveException e =
                new InvalidMoveException(
                    "Ambiguous move but no file or rank info: " + origNotation);
            // System.err.println(e.toString());
            throw e;
          }
        }

        // do we now have a unambiguous move?
        if (matchingMoves.size() == 1) {
          move = matchingMoves.get(0);
        } else if (matchingMoves.size() == 0) {
          InvalidMoveException e = new InvalidMoveException("Not a valid move: " + origNotation);
          // System.err.println(e.toString());
          throw e;
        } else {
          InvalidMoveException e = new InvalidMoveException("Ambiguous move: " + origNotation);
          // System.err.println(e.toString());
          throw e;
        }
      }
    }

    // valid move?
    if (!board.isLegalMove(move)) {
      InvalidMoveException e = new InvalidMoveException("Not a valid move: " + origNotation);
      // System.err.println(e.toString());
      throw e;
    }

    // is this move indeed a capture?
    if (captureMove && move.getCapturedPiece() == null) {
      InvalidMoveException e =
          new InvalidMoveException("Not a valid capture move (no capture): " + origNotation);
      // System.err.println(e.toString());
      throw e;
      // this move makes a capture without indicating so
    } else if (!captureMove && move.getCapturedPiece() != null) {
      InvalidMoveException e =
          new InvalidMoveException("Not a valid non capture move (had capture): " + origNotation);
      // System.err.println(e.toString());
      throw e;
    }

    // all good
    return move;
  }

  /**
   * Creates a move from a simple fromto notation. E.g. e2e4 or e2-e4
   *
   * @param board
   * @param origNotation
   * @return GameMove
   * @throws InvalidMoveException
   */
  public static GameMove createNewMoveFromSimpleNotation(GameBoard board, String origNotation)
      throws InvalidMoveException {

    GameColor color = board.getNextPlayerColor();

    String notation = origNotation;
    GamePiece promotedPiece = null;
    GamePosition toField = null;
    GamePosition fromField = null;

    // Remove # or + or ++ signs = don't need them for generating a move
    notation = notation.replaceAll("[#+=-]", "");
    // Remove optional "e.p."
    notation = notation.replaceAll("e.p.", "");

    // legal input pattern
    String legalPattern = "([a-h][1-8])([a-h][1-8])([QRBN]?)";

    Pattern pattern = Pattern.compile(legalPattern);
    Matcher matcher = pattern.matcher(notation);

    if (!matcher.matches()) {
      InvalidMoveException e = new InvalidMoveException("SIMPLE Syntax not valid" + origNotation);
      //// System.err.println(e.toString());
      throw e;
    }

    fromField = GamePosition.getGamePosition(matcher.group(1));
    toField = GamePosition.getGamePosition(matcher.group(2));
    // Promotion - could be promotion without the explicit
    // designation - then we would assume promotion to Queen
    String promotionLetter = matcher.group(3);
    if (promotionLetter.isEmpty()) promotionLetter = "Q";
    switch (promotionLetter) {
      case "N":
        promotedPiece = Knight.create(color);
        break;
      case "B":
        promotedPiece = Bishop.create(color);
        break;
      case "R":
        promotedPiece = Rook.create(color);
        break;
      case "Q":
        promotedPiece = Queen.create(color);
        break;
      default:
        InvalidMoveException e = new InvalidMoveException("SAN Syntax not valid - expected KQRBN");
        // System.err.println(e.toString());
        throw e;
    }

    // find matching moves from position
    GameMove matchingMove = null;
    GameMoveList moveList = board.generateMoves();
    for (GameMove m : moveList) { // all possible legal moves
      if (fromField.equals(m.getFromField()) && toField.equals(m.getToField())) {

        // m was promotion but not the same promotedTo Piece
        if (m.getPromotedTo() != null && !promotedPiece.equals(m.getPromotedTo())) {
          continue;
        }

        matchingMove = m;
        break;
      }
    }
    if (matchingMove == null) {
      InvalidMoveException e =
          new InvalidMoveException("Not a valid move on this position: " + origNotation);
      //// System.err.println(e.toString());
      throw e;
    }

    return matchingMove;
  }

  /**
   * Create a new Move from a simple notation String "e2-e4"
   *
   * @param notation String
   * @param _pieceMoved
   * @return
   */
  @Deprecated
  public static GameMoveImpl createNewMoveFromSimpleNotation(
      String notation, GamePiece _pieceMoved) {
    if (!notation.matches("[a-h][1-8]-?[a-h][1-8]"))
      throw new InvalidMoveException("Not a valid move string: " + notation);

    notation = notation.replaceAll("-", "");

    return new GameMoveImpl(
        GamePosition.getGamePosition(notation.substring(0, 2)),
        GamePosition.getGamePosition(notation.substring(2, 4)),
        _pieceMoved);
  }
}
