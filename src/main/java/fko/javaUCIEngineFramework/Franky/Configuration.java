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

/**
 * This is the engines configuration. All fields are package visible
 * and can be changed during runtime. They are deliberately not static
 * so we can change white and black differently when engine vs. engine.
 */
public class Configuration {

  /** test the search without any pruning and count perft value */
  public boolean PERFT = false;

  /** Do quiescence evaluation and search extension for non quiet positions */
  public boolean USE_QUIESCENCE = false;

  /** Hash Size */
  public int HASH_SIZE = 512;

  /** Ponder */
  public boolean PONDER = true;

  /** Debug */
  public boolean DEBUG = false;

  /** ##################################################
   * OPTIMIZATIONS
   * ###################################################*/

  /** Sort root moves after iterations. */
  public boolean USE_ROOT_MOVES_SORT = true;

  /** Use AlphaBeta Pruning */
  public boolean USE_ALPHABETA_PRUNING = true;

  /** Principal Variation Search */
  boolean USE_PVS = true;

  /** Use Transposition Tables for visited nodes (needs extra memory) */
  boolean TRANSPOSITION_TABLE = true;

  /** Use Cache for Board evaluations - very expensive, only worth
   * with expensive evaluation - (needs extra memory) */
  boolean USE_EVALUATION_CACHE = true;

  /** Mate Distance Pruning */
  boolean MATE_DISTANCE_PRUNING = true;

  /** Minor Promotion Pruning */
  boolean USE_MINOR_PROMOTION_PRUNING = true;


  // TODO vvvvvvvv
  /**
   * null evaluation
   **/
  boolean DO_NULL_EVALUATION = false;

  /**
   * If set to true we will use the opening book
   */
  boolean _USE_BOOK = false;





  /**
   * Null Move Pruning
   */
  boolean _USE_NMP        = false;
  boolean _USE_VERIFY_NMP = false;


  // Verbose
  /**
   * If set to false this object will produce info output to System.out
   */
  boolean VERBOSE_TO_SYSOUT = false;

  /**
   * verbose alphabeta search
   **/
  boolean VERBOSE_ALPHABETA = false;

  /**
   * verbose variation
   **/
  boolean VERBOSE_VARIATION = false;

  /**
   * verbose variation
   **/
  boolean VERBOSE_STATS = false;


  /**
   * value for folder to books
   */
  String _OB_FolderPath    = "/book/";
  /**
   * opening book file
   */
  String _OB_fileNamePlain = "8moves_GM_LB.pgn";
  //String _OB_fileNamePlain = "book_graham.txt";
  //String _OB_fileNamePlain = "book.txt";
  /** default opening book value */
  //Mode _OB_Mode = Mode.PGN;
  //Mode _OB_Mode = Mode.SAN;
  //Mode _OB_Mode = Mode.SIMPLE;

}
