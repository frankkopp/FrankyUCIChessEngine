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

import fko.FrankyEngine.Franky.Move;
import fko.FrankyEngine.Franky.Position;
import fko.FrankyEngine.util.HelperTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements an Opening Book for chess engines. Reads different formats and caches
 * result in serialized bin files. Implemented are PGN, SAN, SIMPLE and SER.
 * <p>
 * Returns a random move from the loaded book.
 *
 * @author Frank Kopp
 */
public class OpeningBookImpl implements OpeningBook, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(OpeningBookImpl.class);

  private static final long serialVersionUID = -6462934049609479248L;

  /* Standard Board Setup as FEN */
  public static final String STANDARD_BOARD_FEN =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  Openingbook_Configuration _config = new Openingbook_Configuration();

  /**
   * this is the book mapping itself - Key, Value
   * Key is FEN notation
   * Value is BookEntry object
   * Trying different Collection implementations for it.
   */
  private Map<String, OpeningBook_Entry> bookMap =
    Collections.synchronizedMap(new HashMap<>(10000));

  private String  _path;
  private boolean _isInitialized = false;

  // -- helps with process output
  private       int    _counter     = 0;
  private final Object _counterLock = new Object();

  /**
   * Constructor
   */
  public OpeningBookImpl() {
    _path = _config._folderPath + _config._fileNamePlain;
  }

  /**
   * Constructor
   *
   * @param pathToOpeningBook
   * @param mode
   */
  public OpeningBookImpl(String pathToOpeningBook, Mode mode) {
    _path = pathToOpeningBook;
    _config._mode = mode;
  }

  // --- START OF INTERFACE METHODS

  /**
   * Selects a random move from available opening book moves of a position.
   *
   * @see OpeningBook#getBookMove(String)
   */
  @Override
  public int getBookMove(String fen) {

    int move = Move.NOMOVE;

    if (bookMap.containsKey(fen)) {
      ArrayList<Integer> moveList;
      moveList = bookMap.get(fen).moves;
      if (moveList.isEmpty()) return Move.NOMOVE;
      Collections.shuffle(moveList);
      move = moveList.get(0);
    }

    return move;
  }

  /**
   * @see OpeningBook#initialize()
   */
  @Override
  public void initialize() {

    if (_isInitialized) return;

    if (_config.VERBOSE) {
      LOG.info("Opening Book initialization...");
    }

    System.gc();
    long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    long start = System.currentTimeMillis();

    // setup the root node
    bookMap.put(STANDARD_BOARD_FEN, new OpeningBook_Entry(STANDARD_BOARD_FEN));

    readBookfromFile(_path);

    long time = System.currentTimeMillis() - start;

    System.gc();
    long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    if (_config.VERBOSE) {
      LOG.info(String.format("Memory used at Start: %s MB", HelperTools.getMBytes(memStart)));
      LOG.info(String.format("Memory used at End: %s MB", HelperTools.getMBytes(memEnd)));
      LOG.info(String.format("Memory used by Opening Book: ~%s MB",
                             HelperTools.getMBytes(memEnd - memStart)));
      LOG.info(String.format("Opening Book initialization took %f sec.", (time / 1000f)));
    }

    _isInitialized = true;
  }

  // --- END OF INTERFACE METHODS

  /**
   * Reads the opening book from file. Determines if a cache file
   * (serialization) of the book is already present. If present calls
   * readBookfromSERFile().<br>
   * <p>
   * TODO: trigger regeneration not only when missing
   * and when file data of original file is newer than the cache file If not
   * uses _mode flag to determine which format to use.<br>
   * TODO: determine format automatically.
   *
   * @param pathString
   */
  private void readBookfromFile(String pathString) {

    if (_config.VERBOSE) {
      LOG.info(String.format("Opening Book: %s", pathString));
    }

    if (tryFromCache(pathString)) return;

    // NON CACHE

    // book files might be in the JAR file so we can't use normal File mechanics but
    // need to use Class.getResource or Class.getResourceAsStream which uses URL or InputStream as
    // return classes

    InputStream bookFileInputStream = OpeningBookImpl.class.getResourceAsStream(pathString);

    if (bookFileInputStream == null) {
      LOG.error("Book File not found: " + pathString);
      return;
    } else {
      if (_config.VERBOSE) {
        LOG.info(String.format("Found Opening Book...: %s", pathString));
      }
    }

    switch (_config._mode) {
      case PGN:
        processBookfromPGNFile(bookFileInputStream);
        saveOpeningBooktoSERFile(pathString);
        break;
      case SAN:
        processAllLines(bookFileInputStream);
        saveOpeningBooktoSERFile(pathString);
        break;
      case SIMPLE:
        processAllLines(bookFileInputStream);
        saveOpeningBooktoSERFile(pathString);
        break;
      default:
        break;
    }
  }

  /**
   * Reads all lines from a file into list and returns it as a List<String>.
   *
   * @param bookFileInputStream
   * @return List<String> allLines
   */
  private List<String> readAllLinesFromFile(InputStream bookFileInputStream) {

    long start = System.currentTimeMillis();
    Charset charset = Charset.forName("ISO-8859-1");
    List<String> lines;

    if (_config.VERBOSE) LOG.info("Reading Opening Book...");

    InputStreamReader isr = new InputStreamReader(bookFileInputStream, charset);
    BufferedReader br = new BufferedReader(isr);
    lines = br.lines().collect(Collectors.toList());

    long time = System.currentTimeMillis() - start;

    if (_config.VERBOSE) {
      LOG.info(String.format("Finished reading %d lines. (%f sec)", lines.size(), (time / 1000f)));
    }

    return lines;
  }

  /**
   * Reads all lines from a file into list and returns it as a List<String>.
   *
   * @param bookFileInputStream
   * @return List<String> allLines
   */
  private Stream<String> getStreamOfLinesFromFile(InputStream bookFileInputStream) {

    long start = System.currentTimeMillis();
    Charset charset = Charset.forName("ISO-8859-1");
    Stream<String> lines;

    if (_config.VERBOSE) LOG.info("Reading Opening Book.");

    InputStreamReader isr = new InputStreamReader(bookFileInputStream, charset);
    BufferedReader br = new BufferedReader(isr);
    lines = br.lines();

    long time = System.currentTimeMillis() - start;
    if (_config.VERBOSE) {
      LOG.info(String.format("Finished creating stream of lines. (%f sec)", (time / 1000f)));
    }

    return lines;
  }

  private void processBookfromPGNFile(InputStream bookFileInputStream) {

    List<String> lines = readAllLinesFromFile(bookFileInputStream);

    PGN_Reader pgnReader = new PGN_Reader(lines);

    if (!pgnReader.startProcessing()) {
      LOG.error("Could not process lines from PGN file: " +
                FileSystems.getDefault().getPath(_config._folderPath, _config._fileNamePlain));
      return;
    }

    List<PGN_Reader.pgnGame> gameList = pgnReader.getGames();

    // give the gc a chance to delete the lines
    lines = null;
    pgnReader = null;

    long start = System.currentTimeMillis();
    if (_config.VERBOSE) {
      LOG.info("Creating internal book...");
    }

    synchronized (_counterLock) {
      _counter = 0;
    }

    // parallel lambda expression - very fast and cool - needs some synchronizing though
    gameList.parallelStream().forEach(game -> {
      //gameList.stream().forEach(game -> {
      String moves = game.getMoves().toString();
      moves = moves.replaceAll("[\\[\\],]", "");
      processLine(moves);
    });

    long time = System.currentTimeMillis() - start;
    if (_config.VERBOSE) {
      LOG.info(
        String.format("Opening Book ready! %d Positions (%f sec)", bookMap.size(), (time / 1000f)));
    }
  }

  /**
   * Reads the opening book from a plain file and generates the internal data
   * structure.
   *
   * @param bookFileInputStream
   */
  private void processAllLines(InputStream bookFileInputStream) {

    Stream<String> lines = getStreamOfLinesFromFile(bookFileInputStream);

    long start = System.currentTimeMillis();
    if (_config.VERBOSE) {
      LOG.info("Creating internal book...");
    }

    synchronized (_counterLock) {
      _counter = 0;
    }
    // parallel lambda expression - very fast and cool - needs some synchronizing though
    lines.parallel().forEach(this::processLine);

    long time = System.currentTimeMillis() - start;
    if (_config.VERBOSE) {
      LOG.info(
        String.format("Opening Book ready! %d Positions (%f sec)", bookMap.size(), (time / 1000f)));
    }
  }

  /**
   * Generates the data structure from one line of input form the plain book
   * file.
   *
   * @param line
   */
  private void processLine(String line) {
    final int c;
    synchronized (_counterLock) {
      c = ++_counter;
    }
    if (_config.VERBOSE && c % 1000 == 0) {
      LOG.info(String.format("%7d ", c));
      //      if (_config.VERBOSE && c % 10000 == 0) {
      //        LOG.info(String.format("%n"));
      //      }
    }

    OpeningBook_Entry rootEntry = bookMap.get(STANDARD_BOARD_FEN);
    rootEntry.occurenceCounter.getAndIncrement();

    switch (_config._mode) {
      case SAN:
        processSANLine(line);
        break;
      case SIMPLE:
        processSIMPLELine(line);
        break;
      case PGN:
        processSANLine(line);
        break;
      default:
        break; // NYI
    }

  }

  /**
   * Generates the data structure from one line of SAN input form the plain
   * book file.
   *
   * @param line
   */
  private void processSANLine(String line) {

    // separate each move
    String[] lineItems = line.split(" ");

    // board position
    Position currentPosition = new Position(STANDARD_BOARD_FEN);

    // iterate over line items
    for (String item : lineItems) {
      // trim whitespaces
      item = item.trim();

      // ignore numbering
      if (item.matches("\\d+\\.")) continue;

      // try to create a move from it
      int move = Move.fromSANNotation(currentPosition, item);
      if (move == Move.NOMOVE) return;

      // remember last position
      Position lastPosition = new Position(currentPosition);
      String lastFen = lastPosition.toFENString();

      // try to make move
      currentPosition.makeMove(move);

      // we have successfully made the move
      // get fen notation from position
      String currentFen = currentPosition.toFENString();

      addToBook(move, lastFen, currentFen);

    }
  }

  private void processSIMPLELine(String line) {

    // ignore lines start with a digit
    if (line.matches("^\\d+")) return;

    // separate each move
    Pattern pattern = Pattern.compile("([a-h][1-8][a-h][1-8])");
    Matcher matcher = pattern.matcher(line);

    ArrayList<String> lineItems = new ArrayList<>();
    while (matcher.find()) {
      lineItems.add(matcher.group());
    }

    // board position
    Position currentPosition = new Position(STANDARD_BOARD_FEN);

    // iterate over line items - not parallel / actually slower as we would have to copy the board.
    lineItems.forEach(item -> {
      processSIMPLELineItem(item, currentPosition);
    });

  }

  /**
   * @param item
   * @param currentPosition
   * @return
   */
  private boolean processSIMPLELineItem(String item, Position currentPosition) {
    // trim whitespaces
    item = item.trim();

    // try to create a move from it
    // try to create a move from it
    int move = Move.fromUCINotation(currentPosition, item);
    if (move == Move.NOMOVE) return false;

    // remember last position
    Position lastPosition = new Position(currentPosition);
    String lastFen = lastPosition.toFENString();

    // try to make move
    currentPosition.makeMove(move);

    // we have successfully made the move
    // get fen notation from position
    String currentFen = currentPosition.toFENString();

    addToBook(move, lastFen, currentFen);
    return true;
  }

  /**
   * @param bookMove
   * @param lastFen
   * @param currentFen
   */
  private synchronized void addToBook(int bookMove, String lastFen, String currentFen) {

    // add the new entry to map or increase occurrence counter to existing
    // entry
    if (bookMap.containsKey(currentFen)) {
      OpeningBook_Entry currentEntry = bookMap.get(currentFen);
      // Collision detection
      if (!currentFen.equals(currentEntry.position)) {
        throw new RuntimeException("Hashtable Collision!");
      }
      currentEntry.occurenceCounter.getAndIncrement();
    } else {
      bookMap.put(currentFen, new OpeningBook_Entry(currentFen));
    }

    // add move to last book entry (lastPosition)
    OpeningBook_Entry lastEntry = bookMap.get(lastFen);
    if (!lastEntry.moves.contains(bookMove)) {
      lastEntry.moves.add(bookMove);
    }
  }

  /**
   * Tries to find and read an existing cache file. If one exists <code>readBookfromSERFile</code>
   * will be called and bookMap will be filled with cached entries.<br>
   * If book cache folder does not exist it will be created.<br>
   *
   * @param pathString
   * @return true if cache file exists and has been read, false otherwise
   */
  boolean tryFromCache(String pathString) {

    // path of cache files always is external from JAR so we can use Files.exist()
    InputStream openingBookInputStream = null;
    Path cacheFolder = FileSystems.getDefault().getPath(_config._serPath);

    // Check if folder exists and if not try to create it.
    if (!Files.exists(cacheFolder)) {
      LOG.warn(String.format(
        "While reading book cache file: Path %s could not be found. Trying to create it.",
        cacheFolder.toString()));
      try {
        Files.createDirectories(cacheFolder);
      } catch (IOException e) {
        LOG.error(String.format(
          "While reading book cache file: Path %s could not be found. Couldn't create it.",
          cacheFolder.toString()));
      }
    }

    Path cacheFile = createCacheFileName(pathString);

    // read from cache file and return if not configured otherwise
    if (cacheFile.toFile().exists()) {
      if (!_config.FORCE_CREATE) {
        _config._mode = Mode.SER;
        try {
          openingBookInputStream = Files.newInputStream(cacheFile);
          readBookfromSERFile(openingBookInputStream);
          return true;
        } catch (IOException e) {
          LOG.error(String.format("While reading book cache file: File %s could not read.",
                                  cacheFolder.toString()));
        }
      } else {
        if (_config.VERBOSE) {
          LOG.info(String.format("Cache file exists but ignored as FORCE_CREATE is set."));
        }
      }
    }
    return false;
  }

  /**
   * @param openingBookInputStream
   * @return
   */
  @SuppressWarnings("unchecked")
  private boolean readBookfromSERFile(InputStream openingBookInputStream) {

    long start = System.currentTimeMillis();
    try (ObjectInputStream ois = new ObjectInputStream(
      new BufferedInputStream(openingBookInputStream))) {
      if (_config.VERBOSE) {
        LOG.info("Reading Opening Book from cachefile.");
      }

      bookMap = (Map<String, OpeningBook_Entry>) ois.readObject();

    } catch (FileNotFoundException x) {
      LOG.error("file does not exists: " + openingBookInputStream);
      return false;
    } catch (ClosedByInterruptException x) {
      // ignore - we probably closed the game
      return false;
    } catch (IOException e) {
      LOG.error("reading file error: " + openingBookInputStream + " " + e.toString());
      return false;
    } catch (ClassCastException e) {
      LOG.error("file format error: " + openingBookInputStream + " " + e.toString());
      return false;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    }
    long time = System.currentTimeMillis() - start;

    if (_config.VERBOSE) {
      LOG.info(
        String.format("Opening Book ready! %d Positions (%f sec)", bookMap.size(), (time / 1000f)));
    }

    return true;

  }

  /**
   * @return
   */
  boolean saveOpeningBooktoSERFile(String pathString) {

    boolean result;
    long start = 0, time = 0;

    if (_config.VERBOSE) {
      start = System.currentTimeMillis();
      LOG.info("Saving Open Book to cache file...");
    }

    Path cacheFile = createCacheFileName(pathString);

    try {
      final OutputStream newOutputStream = Files.newOutputStream(cacheFile);
      final ObjectOutputStream oos =
        new ObjectOutputStream(new BufferedOutputStream(newOutputStream));
      oos.writeObject(bookMap);
      oos.close();

      result = true;

    } catch (FileAlreadyExistsException x) {
      System.err.format("file named %s" + " already exists: ", cacheFile);
      try {
        Files.delete(cacheFile);
      } catch (IOException e1) {
        // ignore
      }
      result = false;
    } catch (IOException e) {
      System.err.format("Create file error: %s %s ", cacheFile, e.toString());
      try {
        Files.delete(cacheFile);
      } catch (IOException e1) {
        // ignore
      }
      result = false;
    }

    if (result) {
      if (_config.VERBOSE) {
        time = System.currentTimeMillis() - start;
        LOG.info(String.format("successful.(%f sec)", (time / 1000f)));
      }
    } else {
      if (_config.VERBOSE) {
        System.out.format("failed.");
      } else {
        System.err.format("Saving Opening Book to cache file failed. (%s)", cacheFile);
      }
    }

    return result;

  }

  /**
   * @param pathString
   * @return
   */
  private Path createCacheFileName(String pathString) {
    // remove folder structure and replace by "-" in filename
    pathString = pathString.substring(0, 1) + pathString.substring(1).replaceAll("/", "-");
    pathString = _config._serPath + pathString + ".ser";
    return FileSystems.getDefault().getPath(pathString);
  }

  /**
   * Different possible mode for book files. BIN - will be serialization of
   * java SAN - uses a line by line listing of move lists in SAN notation PNG
   * - uses a file of at least one PNG game SIMLE - uses a line with full
   * from-to description without - or ' '
   *
   * @author fkopp
   */
  public enum Mode {SER, SAN, PGN, SIMPLE}

  /**
   * Represents one book entry for our opening book. This is bacically a fen
   * notation of the position, the number of occurences of this position in
   * the book file and the moves as SAN notation to the subsequent position.
   *
   * @author fkopp
   */
  private static class OpeningBook_Entry implements Comparable<OpeningBook_Entry>,
                                                    Comparator<OpeningBook_Entry>, Serializable {

    private static final long serialVersionUID = 1573629955690947725L;

    // as fen notation
    String             position;
    // how often did this position occur in the opening book
    AtomicInteger      occurenceCounter = new AtomicInteger(0);
    // list of moves to next positions
    ArrayList<Integer> moves            = new ArrayList<>(5);

    // Constructor
    OpeningBook_Entry(String fen) {
      this.position = fen;
      this.occurenceCounter.set(1);
    }

    @Override
    public String toString() {
      return position + " (" + occurenceCounter + ") " + moves.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(OpeningBook_Entry b) {
      return b.occurenceCounter.get() - this.occurenceCounter.get();
    }

    /*
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(OpeningBook_Entry o1, OpeningBook_Entry o2) {
      return o2.occurenceCounter.get() - o1.occurenceCounter.get();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.position == null) ? 0 : this.position.hashCode());
      return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof OpeningBook_Entry)) {
        return false;
      }
      OpeningBook_Entry other = (OpeningBook_Entry) obj;
      if (this.position == null) {
        return other.position == null;
      } else {
        return this.position.equals(other.position);
      }
    }
  }


}
