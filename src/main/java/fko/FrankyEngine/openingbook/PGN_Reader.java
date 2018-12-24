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
package fko.FrankyEngine.openingbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads a given list of Strings (lines) and creates a List of PGN Games. Each PGN Game
 * has is basically a list of move plus metadata.
 *
 * <p><b>PGN Formal Syntax</b><br>
 * 18: Formal syntax <code>
 * see below in comments in code
 * </code>
 *
 * @author fkopp
 */
public class PGN_Reader {

  //    <PGN-database> ::= <PGN-game> <PGN-database>
  //                       <empty>
  //
  //    <PGN-game> ::= <tag-section> <movetext-section>
  //
  //    <tag-section> ::= <tag-pair> <tag-section>
  //                      <empty>
  //
  //    <tag-pair> ::= [ <tag-name> <tag-value> ]
  //
  //    <tag-name> ::= <identifier>
  //
  //    <tag-value> ::= <string>
  //
  //    <movetext-section> ::= <element-sequence> <game-termination>
  //
  //    <element-sequence> ::= <element> <element-sequence>
  //                           <recursive-variation> <element-sequence>
  //                           <empty>
  //
  //    <element> ::= <move-number-indication>
  //                  <SAN-move>
  //                  <numeric-annotation-glyph>
  //
  //    <recursive-variation> ::= ( <element-sequence> )
  //
  //    <game-termination> ::= 1-0
  //                           0-1
  //                           1/2-1/2
  //                           *
  //
  //    <empty> ::=

  /** If set to true this object will produce info output to System.out */
  public static boolean VERBOSE = false;

  // holds the original input lines
  private final List<String> _lines;

  // holds all correctly interpreted games
  private List<pgnGame> _games = null;

  private boolean _readyFlag = false;

  /**
   * Creates new PGN_Reader but does not start interpreting. To start interpreting call
   * startProcess().
   *
   * @param lines of strings (typically lines read from file)
   */
  public PGN_Reader(List<String> lines) {
    this._lines = lines;
  }

  /**
   * Starts interpreting the lines.
   *
   * @return success
   */
  public boolean startProcessing() {

    // if we already have processed the lines we are sucessfully done.
    if (_games != null) return true;

    long start = 0, time = 0;
    if (VERBOSE) {
      start = System.currentTimeMillis();
      System.out.format("PGN Reader interpreting %d lines...", _lines.size());
    }

    // create the _games array
    _games = new ArrayList<>(10);

    boolean result = startInterpreting();

    // now we have something to return;
    if (result) _readyFlag = true;
    ;

    if (VERBOSE) {
      time = System.currentTimeMillis() - start;
      System.out.format("Finished! (%f sec)", (time / 1000f));
    }

    return result;
  }

  /** @return the list of correctly interpreted PGN games or null if not yet processed. */
  public List<pgnGame> getGames() {
    return _readyFlag ? _games : null;
  }

  // Internal ------------------

  /**
   * Loops and calls processOneGame to loop over each game seperatly.
   *
   * @return success
   */
  private boolean startInterpreting() {

    // loop over all lines - calls subroutine for each game
    int currentLine = 0;
    int c = 0;
    while (currentLine < _lines.size()) {
      currentLine = processOneGame(currentLine);
      if (VERBOSE && ++c % 1000 == 0) {
        System.out.format(String.format("%7d ", c));
        if (VERBOSE && c % 10000 == 0) {
          System.out.format(String.format(" %d ", currentLine));
        }
      }
    }
    return true;
  }

  /**
   * Reads lines until a game is completely read.
   *
   * @param currentLine
   * @return last line
   */
  private int processOneGame(int currentLine) {

    boolean gameEndReached = false;

    pgnGame tmpGame = new pgnGame();

    do {

      // get next line and trim
      String line = _lines.get(currentLine);

      // cleanup
      line = line.trim();

      // escape token
      if (line.startsWith("%")) continue;

      // empty line before start of Tag pairs
      if (line.isEmpty()) continue;

      // clean up all unnecessary stuff we don't need (yet)
      line = lineCleanUp(line);

      // tag pair section
      if (line.matches("^\\[\\w+ +\".*\"\\]")) { // handle TAG Pair line
        // TODO: ignore Tag Pairs for now
        // currentLine = handleTagPairSection(currentLine, tmpGame);
        continue;
      }

      // move text section
      if (line.matches("^([1-9]|[a-h]|[KQRBN]).*")) { // handle movetext section line
        currentLine = handleMoveSection(currentLine, tmpGame);
        gameEndReached = true;
      }

    } while (currentLine++ < _lines.size() && !gameEndReached);

    _games.add(tmpGame);

    return currentLine;
  }

  /**
   * Handles Tag Pair lines. E.g. [Event "jeu.echecs.com rated blitz game"]
   *
   * @param currentLine
   * @param tmpGame
   */
  @SuppressWarnings("unused")
  private int handleTagPairSection(int currentLine, pgnGame tmpGame) {

    // complex because of possible escaping characters like "
    Pattern pattern =
        Pattern.compile(
            "\\[(\\w+) +\"(([^\\\\\"]+|\\\\([btnfr\"'\\\\]|[0-3]?[0-7]{1,2}|u[0-9a-fA-F]{4}))*)\"\\]");

    do {

      // get next line and trim
      String line = _lines.get(currentLine);
      line = line.trim();

      // clean up all unnecessary stuff we don't need (yet)
      line = lineCleanUp(line);

      // escape token
      if (line.startsWith("%")) continue;

      // empty line before start of Tag pairs
      if (line.isEmpty()) continue;

      // get pattern
      Matcher matcher = pattern.matcher(line);

      boolean tagpairFound = false;

      // find one or more tag pairs
      while (matcher.find()) {
        tagpairFound = true;
        String key = matcher.group(1);
        String value = matcher.group(2);
        tmpGame.addTags(key, value);
      }

      // if we did not find at least one we finished tag section
      if (!tagpairFound) {
        currentLine--;
        break;
      }

    } while (currentLine++ <= _lines.size()); // until break or no more lines

    return currentLine;
  }

  private int handleMoveSection(int currentLine, pgnGame tmpGame) {

    StringBuffer moveSection = new StringBuffer();

    // get all in concatenated
    while (currentLine < _lines.size()) {
      // get next line and trim
      String line = _lines.get(currentLine++);
      line = line.trim();
      // add original line to pgnGame
      // tmpGame.setOrigNotation(tmpGame.getOrigNotation()+System.lineSeparator()+line);
      // remove semicolon comments at end of line
      line = line.replaceFirst(";.*$", " ");
      // escape token or empty line
      if (line.startsWith("%") || line.isEmpty()) continue;
      moveSection.append(line).append(" ");
      // look for end pattern
      if (line.matches(".*((1-0)|(0-1)|(1/2-1/2)|\\*)$")) {
        break;
      }
    }

    // Concatenated line
    String line = moveSection.toString();

    // eliminate unwanted stuff
    line = line.replaceAll("(\\$\\d{1,3})", " "); // no NAG annotation supported
    line = line.replaceAll("\\{[^{}]*\\}", " "); // bracket comments
    line = line.replaceAll("<[^<>]*>", " "); // reserved symbols < >

    // handle nested RAV variation comments
    do { // no RAV variations supported (could be nested)
      line = line.replaceAll("\\([^()]*\\)", " ");
    } while (line.matches(".*\\([^()]*\\).*"));

    // get rid of result
    line = line.replaceAll("((1-0)|(0-1)|(1/2-1/2)|\\*)", " ");

    // get rid of move numbers - don't need them yet
    line = line.replaceAll("\\d{1,3}( )*(\\.{1,3})", " ");

    // other general cleanup
    line = lineCleanUp(line);

    // add the moves to the game object
    String[] moves = line.split(" ");
    for (String m : moves) {
      tmpGame.getMoves().add(m);
    }

    return currentLine;
  }

  /**
   * @param line
   * @return
   */
  private static String lineCleanUp(String line) {

    // eliminate unwanted stuff
    line = line.replaceAll("\\s", " ");

    // strip comments (comments not used yet);
    line = line.replaceFirst(";.*$", " "); // semicolon comments at end of line
    line = line.replaceAll(" +", " "); // multiple space to one

    line = line.trim();

    return line;
  }

  /**
   * Represents one imported PGN game. Has a list of moves and metadata (key, value pairs)
   *
   * @author fkopp
   */
  public class pgnGame {

    private String                  _origNotation = "";
    private HashMap<String, String> _tags         = new HashMap<>();
    private List<String>            _moves        = new ArrayList<>();

    pgnGame() {}

    @Override
    public String toString() {
      String s = "";
      for (String key : _tags.keySet()) {
        s += "[" + key + " \"" + _tags.get(key) + "\"]" + System.lineSeparator();
      }
      s += System.lineSeparator() + _moves + System.lineSeparator();
      s +=
        System.lineSeparator()
        + "Orignal Notation:"
        + System.lineSeparator()
        + _origNotation
        + System.lineSeparator();
      return s;
    }

    @SuppressWarnings("javadoc")
    public String getOrigNotation() {
      return _origNotation;
    }

    @SuppressWarnings("javadoc")
    public void setOrigNotation(String origNotation) {
      this._origNotation = origNotation;
    }

    @SuppressWarnings("javadoc")
    public HashMap<String, String> getTags() {
      return _tags;
    }

    @SuppressWarnings("javadoc")
    public void addTags(String key, String value) {
      _tags.put(key, value);
    }

    /**
     * Returns a reference to the List of Moves (String) for direct manipulation.
     *
     * @return list of Strings of moves (SAN notation)
     */
    public List<String> getMoves() {
      return _moves;
    }
  }

  /**
   * # perl regular expressions for game parsing my $re_result = qr{(?:1\-0|0\-1|1\/2\-1\/2|\*)}; my
   * $re_move = qr{[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:\=?[QRBN])?}; # piece ^^^^^ # unambiguous
   * column or line ^^^ ^^^ # capture ^ # destination square ^^^ ^^^ # promotion ^ ^^^^^ my
   * $re_castling = qr/O\-O(?:\-O)?/; my $re_check = qr/(?:(?:\#|\+(\+)?))?/; my $re_any_move =
   * qr/(?:$re_move|$re_castling)$re_check/; my $re_nag = qr/\$\d+/; my $re_number =
   * qr/\d+\.(?:\.\.)?/; my $re_escape = qr/^\%[^\n]*\n/; my $re_eol_comment= qr/;.*$/; my $re_rav =
   * $re_parens; my $re_comment = $re_brace;
   *
   * <p># ============================================== # These two regular expressions were
   * produced by # Damian Conway's module Regexp::Common #
   * ---------------------------------------------- # On the author's suggestion, these lines # #
   * use Regexp::Common; # print "$RE{balanced}{-parens=>'()'}\n"; # print
   * "$RE{balanced}{-parens=>'{}'}\n"; # # produced the RegEx code, which was edited # and inserted
   * here for efficiency reasons. # ==============================================
   *
   * <p>our $re_parens; ## no critic $re_parens = qr/ (?:(?:(?:[(](?:(?>[^)(]+)
   * |(??{$re_parens}))*[)])) |(?:(?!))) /x;
   *
   * <p>our $re_brace; ## no critic $re_brace = qr/
   * (?:(?:(?:[{](?:(?>[^}{]+)|(??{$re_brace}))*[}]))|(?:(?!))) /x;
   */
}
