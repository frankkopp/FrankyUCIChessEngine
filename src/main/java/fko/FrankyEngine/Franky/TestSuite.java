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

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class running chess test using EPD positions and test operations
 * from a preconfigured file.
 */
public class TestSuite {

  private static final Logger LOG = LoggerFactory.getLogger(TestSuite.class);

  private static final String TESTSUITE_PROPERTIES = "TestSuiteConfig.properties";

  private Path filePath;

  private int searchTime;

  private List<TestCase> testCases = new ArrayList<>();

  public TestSuite() {
    final Properties properties = new Properties();
    try {
      properties.load(Objects.requireNonNull(
        this.getClass().getClassLoader().getResourceAsStream(TESTSUITE_PROPERTIES)));
    } catch (IOException e) {
      LOG.error("Could load properties file: " + TESTSUITE_PROPERTIES, e);
      e.printStackTrace();
      System.exit(1);
    }

    // Testfile
    setFilePath(properties.getProperty("testfile"));

    // search time
    searchTime = Integer.valueOf(properties.getProperty("search_time"));
    if (searchTime <= 0) {
      searchTime = 60;
    }
  }

  public TestSuite(String testFile) {
    this();
    setFilePath(testFile);
  }

  private void setFilePath(final String testFile) {
    filePath = FileSystems.getDefault().getPath(testFile);
    // Check if file exists
    if (Files.notExists(filePath, LinkOption.NOFOLLOW_LINKS)) {
      LOG.error("While reading epd file: File {} could not be found.",
                filePath.getFileName().toString());
      System.exit(1);
    }
  }

  public void startTests(final int searchTime) {
    this.searchTime = searchTime;
    startTests();
  }

  public void startTests() {

    System.out.println("Testsuite Test Started!");
    System.out.println("========================================");
    System.out.printf("Opening Testsuite file: %s\n", filePath);

    readTestCsaes();

    System.out.printf("Testsuite has %d test cases.\n\n", testCases.size());

    // Do tests
    for (TestCase testCase : testCases) {
      System.out.printf("Test %d: %s Fen: %s\n", testCases.indexOf(testCase) + 1, testCase.id,
                        testCase.fen);
      switch (testCase.opCode) {
        case "bm":
          searchBestMoveTest(testCase);
          break;
        case "dm":
          searchDirectMateTest(testCase);
          break;
        default:
          testCase.result = Result.SKIPPED;
          System.out.println("SKIPPED! OpCode unknown or not implemented: " + testCase.opCode);
          System.out.println();
          continue;
      }
      System.out.println();
    }

    // Print results
    int successCounter = 0;
    int failedCounter = 0;
    int skippedCounter = 0;
    int notTestedCounter = 0;
    System.out.println();
    System.out.printf("Results with search time %,dms for file %s\n", searchTime, filePath);
    System.out.println("==================================================================================");
    System.out.println("No  | Result     | Engine   | Value | Best Move       | Fen ");
    System.out.println("==================================================================================");
    for (TestCase testCase : testCases) {
      switch (testCase.result) {
        case SUCCESS:
          successCounter++;
          break;
        case FAILED:
          failedCounter++;
          break;
        case SKIPPED:
          skippedCounter++;
          break;
        case NOT_TESTED:
          notTestedCounter++;
          break;
      }
      System.out.printf("%-4d| %10s | %8s | %5s | %-15s | %s \n", testCases.indexOf(testCase) + 1,
                        testCase.result.toString(), Move.toSimpleString(testCase.engineMove),
                        testCase.engineValue == Evaluation.NOVALUE
                        ? "N/A"
                        : "" + testCase.engineValue, testCase.bestMoves.toNotationString(),
                        testCase.fen);
    }
    System.out.println("==================================================================================");
    System.out.printf("Successful: %,3d (%d Prozent)\n", successCounter,
                      100 * successCounter / testCases.size());
    System.out.printf("Failed:     %,3d (%d Prozent)\n", failedCounter,
                      100 * failedCounter / testCases.size());
    System.out.printf("Skipped:    %,3d (%d Prozent)\n", skippedCounter,
                      100 * skippedCounter / testCases.size());
    System.out.printf("Not tested: %,3d (%d Prozent)\n", notTestedCounter,
                      100 * notTestedCounter / testCases.size());
    System.out.println();

  }

  private void searchBestMoveTest(final TestCase testCase) {
    System.out.printf("Search for best move: %s\n", testCase.operand);

    FrankyEngine engine = new FrankyEngine();
    Search search = engine.getSearch();

    Position position = new Position(testCase.fen);
    testCase.bestMoves = getMoveFromOperand(position, testCase.operand);

    System.out.printf("Best Moves: %s\n", testCase.bestMoves);

    SearchMode searchMode = new SearchMode(0, 0, 0, 0, 0, 0, 0, 0, searchTime, null, false, false,
                                           false);
    search.startSearch(position, searchMode);
    waitWhileSearching(search);

    testCase.engineMove = search.getLastSearchResult().bestMove;
    testCase.engineValue = search.getLastSearchResult().resultValue;

    testCase.result = Result.FAILED;
    for (int move : testCase.bestMoves) {
      if (move == testCase.engineMove) {
        System.out.printf("SUCCESS: Found Best Move: %s\n", Move.toString(testCase.engineMove));
        testCase.result = Result.SUCCESS;
      } else {
        System.out.printf("FAILED: Best Move not found: %s\n", Move.toString(testCase.engineMove));
      }
    }

    //    System.out.printf("Best Move: %s Value: %s Ponder %s\n\n",
    //                      Move.toSimpleString(search.getLastSearchResult().bestMove),
    //                      search.getLastSearchResult().resultValue / 100f,
    //                      Move.toSimpleString(search.getLastSearchResult().ponderMove));

  }

  private void searchDirectMateTest(final TestCase testCase) {
    System.out.printf("Search for mate in %s moves\n", testCase.operand);
  }

  private MoveList getMoveFromOperand(final Position position, final String operand) {

    MoveList bestMoves = new MoveList();

    //System.out.printf("OPERAND SAN: %s\n", operand);

    // remove x from operand
    String movesString = operand.replaceAll("x", "");
    movesString = movesString.replaceAll("!", "");

    // there could be multiple moves as best move
    String[] sanMoves = movesString.split(" ");

    // convert each sanString to a move
    for (String sanMove : sanMoves) {

      //System.out.printf(" OPERAND clean: %s\n", movesString);

      // pattern recognition
      Pattern pattern = Pattern.compile(
        "([NBRQK])?([a-h])?([1-8])?([a-h][1-8]|O-O-O|O-O)(=([NBRQ]))?([+#])?");
      // "\\[(\\w+) +\"(([^\\\\\"]+|\\\\([btnfr\"'\\\\]|[0-3]?[0-7]{1,2}|u[0-9a-fA-F]{4}))*)\"\\]");

      Matcher matcher = pattern.matcher(sanMove);

      // find one or more tag pairs
      matcher.find();
      String piece = matcher.group(1);
      String disambFile = matcher.group(2);
      String disambRank = matcher.group(3);
      String targetSquare = matcher.group(4);
      String promotion = matcher.group(6);
      String checkSign = matcher.group(7);
      // System.out.printf("Piece: %s File: %s Row: %s Target: %s Promotion: %s CheckSign: %s\n",
      //   piece, disambFile, disambRank, targetSquare, promotion, checkSign);

      // generate all legal moves from the position
      // and try to find a matching move
      MoveGenerator mg = new MoveGenerator();
      MoveList moveList = mg.getLegalMoves(position);
      int moveFromSAN = Move.NOMOVE;
      int movesFound = 0;
      for (int move : moveList) {
        //System.out.printf("Move %d: %s\n", i, Move.toString(move));
        // castling
        if (Move.getMoveType(move) == MoveType.CASTLING) {
          //System.out.println("Castling");
          if (!targetSquare.equals(Move.toString(move))) {
            continue;
          }
          //System.out.println("Castling MATCH " + Move.toString(move));
          moveFromSAN = move;
          movesFound++;
          continue;
        }
        // same end square
        if (Move.getEnd(move).name().equals(targetSquare)) {
          if (piece != null) {
            if (Move.getPiece(move).getType().getShortName().equals(piece)) {
              //System.out.println("Piece MATCH " + Move.getPiece(move).getType().toString());
            } else if (Move.getPiece(move).getType().equals(PieceType.PAWN)) {
              //System.out.println("Piece MATCH PAWN");
            } else {
              //System.out.println("Piece NO MATCH");
              continue;
            }
          }
          // Disambiguation
          if (disambFile != null) {
            if (Move.getStart(move).getFile().name().equals(disambFile)) {
              //System.out.println("File MATCH " + Move.getStart(move).getFile().name());
            } else {
              //System.out.println("File NO MATCH " + Move.getStart(move).getFile().name());
              continue;
            }
          }
          if (disambRank != null) {
            if (("" + Move.getStart(move).getRank().get()).equals(disambRank)) {
              //System.out.println("Rank MATCH "+ Move.getStart(move).getRank().get());
            } else {
              //System.out.println("Rank NO MATCH "+ Move.getStart(move).getRank().get());
              continue;
            }
          }
          // promotion
          if (promotion != null) {
            if (Move.getPromotion(move).getType().getShortName().equals(promotion)) {
              //System.out.println("Promotion MATCH");
            } else {
              //System.out.println("Promotion NO MATCH");
              continue;
            }
          }
          moveFromSAN = move;
          movesFound++;
        }
      }
      if (movesFound > 1) {
        LOG.error("SAN move is ambiguous!");
      } else if (movesFound == 0 || !Move.isValid(moveFromSAN)) {
        LOG.error("SAN move not valid!");
      } else {
        bestMoves.add(moveFromSAN);
      }
    }
    return bestMoves;
  }

  private void waitWhileSearching(Search search) {
    while (search.isSearching()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void readTestCsaes() {
    // read all lines from file
    Charset charset = Charset.forName("ISO-8859-1");
    List<String> lines = null;
    try {
      lines = Files.readAllLines(filePath, charset);
    } catch (CharacterCodingException e) {
      LOG.error("EPD file " + filePath + "has wrong charset (needs to be ISO-8859-1) - not loaded!",
                e);
      System.exit(1);
    } catch (IOException e) {
      LOG.error("EPD file " + filePath + " could not be loaded!", e);
      System.exit(1);
    }

    for (String line : lines) {
      // skip empty line and comments
      line = line.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      // https://www.chessprogramming.org/Extended_Position_Description
      // tokenize the string
      String[] tokens = line.split("\\s");

      // get fen from tokens 0-3
      String fen = tokens[0] + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + " ";

      // setup a test case with fen
      TestCase testCase = new TestCase(fen);

      // get the operation part of the input
      String op = "";
      for (int i = 4; i < tokens.length; i++) {
        op += " " + tokens[i];
      }
      String[] ops = op.split("\\s*;\\s*");

      // for each operation split opCode from operand and store them in the testCase.
      // For now we are only interested in bm and dm
      for (int i = 0; i < ops.length; i++) {
        ops[i] = ops[i].trim();

        String opCode = ops[i].substring(0, ops[i].indexOf(" ")).trim();
        String operand = ops[i].substring(ops[i].indexOf(" ")).trim();

        switch (opCode) {
          case "id": // id of test
            testCase.id = operand;
            break;
          case "bm": // best move
            testCase.opCode = "bm";
            testCase.operand = operand;
            break;
          case "dm": // direct mate
            testCase.opCode = "dm";
            testCase.operand = operand;
            break;
          default:
            // ignore non implemented or unknown opcodes
            break;
        }
      }
      testCases.add(testCase);
    }
  }

  class TestCase {

    String   fen;
    String   id          = "no id";
    String   opCode      = "noop";
    String   operand     = "";
    Result   result      = Result.NOT_TESTED;
    MoveList bestMoves   = new MoveList();
    int      engineMove  = Move.NOMOVE;
    int      engineValue = Evaluation.NOVALUE;

    TestCase(String fen) {
      this.fen = fen;
    }

    @Override
    public String toString() {
      return "TestCase{" + "fen='" + fen + '\'' + ", id='" + id + '\'' + ", opCode='" + opCode +
             '\'' + ", operand='" + operand + '\'' + ", result=" + result + '}';
    }
  }

  enum Result {
    NOT_TESTED,
    SUCCESS,
    FAILED,
    SKIPPED
  }


}
