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

import fko.FrankyEngine.Franky.openingbook.OpeningBookImpl;

/**
 * This is the engines configuration. All fields are package visible
 * and can be changed during runtime. They are deliberately not static
 * so we can change white and black differently when engine vs. engine.
 */
public class Configuration {

  /**
   * test the search without any pruning and count perft value
   */
  public boolean PERFT = false;

  /**
   * Hash Size
   */
  public int HASH_SIZE = 1024;

  /**
   * Ponder
   */
  public boolean PONDER = true;

  /**
   * Debug
   */
  public boolean DEBUG = false;

  /**
   * UCI Options
   */
  public boolean UCI_ShowCurrLine = true;

  /**
   * If set to true we will use the opening book
   */
  public boolean USE_BOOK = true;

  /**
   * value for folder to books
   */
  public String OB_FolderPath    = "/book/";
  /**
   * opening book file
   */
  public String OB_fileNamePlain = "8moves_GM_LB.pgn";
  //String _OB_fileNamePlain = "book_graham.txt";
  //String _OB_fileNamePlain = "book.txt";

  /**
   * default opening book value
   */
  OpeningBookImpl.Mode OB_Mode = OpeningBookImpl.Mode.PGN;
  //Mode _OB_Mode = Mode.SAN;
  //Mode _OB_Mode = Mode.SIMPLE;

  /** ##################################################
   * OPTIMIZATIONS
   * ###################################################*/

  /**
   * Do quiescence evaluation and search extension for non quiet positions
   */
  public boolean USE_QUIESCENCE = true;

  /**
   * Uses TT to determine best move of previous searches and also start depth
   */
  public boolean USE_TT_ROOT = true;

  /**
   * Push last PV move to search first position for iterations.
   */
  public boolean USE_PVS_MOVE_ORDERING = true;

  /**
   * Use AlphaBeta Pruning
   */
  public boolean USE_ALPHABETA_PRUNING = true;

  /**
   * Principal Variation Search
   */
  public boolean USE_PVS = true;

  /**
   * Use Transposition Tables for visited nodes (needs extra memory)
   */
  public boolean USE_TRANSPOSITION_TABLE = true;

  /**
   * Mate Distance Pruning
   */
  public boolean USE_MATE_DISTANCE_PRUNING = true;

  /**
   * Minor Promotion Pruning
   */
  public boolean USE_MINOR_PROMOTION_PRUNING = true;

  /**
   * Null Move Pruning
   */
  public boolean USE_NULL_MOVE_PRUNING            = true;
  public int     NULL_MOVE_DEPTH                  = 2;
  public boolean USE_VERIFY_NMP                   = true;
  public int     NULL_MOVE_REDUCTION_VERIFICATION = 3;

  /**
   * Eval Pruning - early cut for low static evals
   * Reverse Futility Pruning
   * https://www.chessprogramming.org/Reverse_Futility_Pruning
   */
  public boolean USE_STATIC_NULL_PRUNING    = true;
  public int     STATIC_NULL_PRUNING_DEPTH  = 2;
  public int     STATIC_NULL_PRUNING_MARGIN = 300;

  /**
   * Razor  - early qsearch for low static evals
   */
  public boolean USE_RAZOR_PRUNING    = true;
  public int     RAZOR_PRUNING_DEPTH  = 3;
  public int     RAZOR_PRUNING_MARGIN = 600;

  /**
   * Killer moves - move which caused cut offs in previous iterations
   */
  public boolean USE_KILLER_MOVES = true;
  public int     NO_KILLER_MOVES  = 2;


  /**
   * Late Move Reduction
   */
  public boolean USE_LMR       = true;
  public int     LMR_MIN_DEPTH = 2;
  public int     LMR_REDUCTION = 1;

  /**
   * Use Aspiration Window in root search
   * TODO: Make evaluation more stable between different depths - otherwise
   * Aspiration will not be efficient.
   */
  public boolean USE_ASPIRATION_WINDOW  = true;
  public int     ASPIRATION_START_DEPTH = 5;

  /**
   * Experimental sorting of moves in move generation.
   * Generating moves already has good ordering - extra sorting is expensive
   * and extra sorting can be worse than standard sorting
   * <p>
   * TODO: too slow yet
   * Round 4 Test 1 avg: 0,085 sec // OFF
   * Round 4 Test 2 avg: 0,174 sec // ON
   * Same depth search:
   * Round 2 Test 1 avg: 18,033 sec
   * Nodes visited: 6.508.055 Boards Evaluated: 6.449.511
   * Round 2 Test 2 avg: 25,571 sec
   * Nodes visited: 6.625.362 Boards Evaluated: 6.566.329
   */
  public boolean USE_SORT_ALL_MOVES = false;


  // TODO vvvvvvvv

  @Override
  public String toString() {
    return "Configuration{" + "PERFT=" + PERFT + " HASH_SIZE=" + HASH_SIZE + " PONDER=" + PONDER
           + " DEBUG=" + DEBUG + " UCI_ShowCurrLine=" + UCI_ShowCurrLine + " USE_BOOK=" + USE_BOOK
           + " OB_FolderPath='" + OB_FolderPath + '\'' + " OB_fileNamePlain='" + OB_fileNamePlain
           + '\'' + " OB_Mode=" + OB_Mode + " USE_QUIESCENCE=" + USE_QUIESCENCE
           + " USE_PVS_MOVE_ORDERING=" + USE_PVS_MOVE_ORDERING + " USE_ALPHABETA_PRUNING="
           + USE_ALPHABETA_PRUNING + " USE_PVS=" + USE_PVS + " USE_TRANSPOSITION_TABLE="
           + USE_TRANSPOSITION_TABLE + " USE_MATE_DISTANCE_PRUNING=" + USE_MATE_DISTANCE_PRUNING
           + " USE_MINOR_PROMOTION_PRUNING=" + USE_MINOR_PROMOTION_PRUNING
           + " USE_NULL_MOVE_PRUNING=" + USE_NULL_MOVE_PRUNING + " NULL_MOVE_DEPTH="
           + NULL_MOVE_DEPTH + " USE_STATIC_NULL_PRUNING=" + USE_STATIC_NULL_PRUNING
           + " STATIC_NULL_PRUNING_DEPTH=" + STATIC_NULL_PRUNING_DEPTH
           + " STATIC_NULL_PRUNING_MARGIN=" + STATIC_NULL_PRUNING_MARGIN + " USE_RAZOR_PRUNING="
           + USE_RAZOR_PRUNING + " RAZOR_PRUNING_DEPTH=" + RAZOR_PRUNING_DEPTH
           + " RAZOR_PRUNING_MARGIN=" + RAZOR_PRUNING_MARGIN + " USE_KILLER_MOVES="
           + USE_KILLER_MOVES + " USE_SORT_ALL_MOVES=" + USE_SORT_ALL_MOVES + " USE_LMR=" + USE_LMR
           + " LMR_MIN_DEPTH=" + LMR_MIN_DEPTH + " LMR_REDUCTION=" + LMR_REDUCTION
           + " USE_ASPIRATION_WINDOW="
           + USE_ASPIRATION_WINDOW + '}';
  }
}
