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

  /** test the search without any pruning and count perft value **/
  static public boolean PERFT = false;

  /** null evaluation **/
  boolean DO_NULL_EVALUATION = false;

  // Verbose
  /** If set to true this object will produce info output to System.out */
  boolean VERBOSE_TO_SYSOUT = false;

  /** verbose alphabeta search **/
  boolean VERBOSE_ALPHABETA = false;

  /** verbose variation **/
  boolean VERBOSE_VARIATION = false;

  /** verbose variation **/
  boolean VERBOSE_STATS = true;


  /** If set to true we will use the opening book */
  boolean _USE_BOOK = true;

  /** Use Ponderer while waiting for opponents move - fills node_cache */
  boolean _USE_PONDERER = true;

  /** Use Transposition Tables for visited nodes  (needs extra memory) */
  boolean _USE_NODE_CACHE = true;

  /** Use Transposition Tables to store move list (needs extra memory)
   *  Very expensive as it creates many int[] arrays - worth it?*/
  boolean _USE_MOVE_CACHE = true;

  /** Use Cache for Board evaluations - very expensive, only worth
   * with expensive evaluation - (needs extra memory)                  */
  boolean _USE_BOARD_CACHE = true;

  /** Do quiescence evaluation and search extension for non quiet positions */
  boolean _USE_QUIESCENCE = true;

  /** Use AlphaBeta Pruning */
  boolean _USE_PRUNING = true;

  /** Principal Variation Search */
  boolean _USE_PVS = true;

  /** Mate Distance Pruning */
  boolean _USE_MDP = true;

  /** Minor Promotion Pruning */
  boolean _USE_MPP = true;

  /** Null Move Pruning */
  boolean _USE_NMP = true;
  boolean _USE_VERIFY_NMP = true;


  /** value for folder to books */
  String _OB_FolderPath = "/book/";
  /** opening book file */
  String _OB_fileNamePlain = "8moves_GM_LB.pgn";
  //String _OB_fileNamePlain = "book_graham.txt";
  //String _OB_fileNamePlain = "book.txt";
  /** default opening book value */
  //Mode _OB_Mode = Mode.PGN;
  //Mode _OB_Mode = Mode.SAN;
  //Mode _OB_Mode = Mode.SIMPLE;

}
