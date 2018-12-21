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

/**
 * This is the engines configuration. All fields are package visible
 * and can be changed during runtime. They are deliberately not static
 * so we can change white and black differently when engine vs. engine.
 */
public class Configuration {

  /**
   * test the search without any pruning and count perft value
   */
  boolean PERFT = false;

  /**
   * Hash Size
   */
  int HASH_SIZE = 128;

  /**
   * Ponder
   */
  boolean PONDER = true;

  /**
   * Debug
   */
  boolean DEBUG = false;

  /**
   * UCI Options
   */
  boolean UCI_ShowCurrLine = true;

  /** ##################################################
   * OPTIMIZATIONS
   * ###################################################*/

  /**
   * Do quiescence evaluation and search extension for non quiet positions
   */
  boolean USE_QUIESCENCE = true;

  /**
   * Sort root moves after iterations.
   */
  boolean USE_ROOT_MOVES_SORT = true;

  /**
   * Push last PV move to search first position for iterations.
   */
  boolean USE_PVS_MOVE_ORDERING = true;

  /**
   * Use AlphaBeta Pruning
   */
  boolean USE_ALPHABETA_PRUNING = true;

  /**
   * Principal Variation Search
   */
  boolean USE_PVS = true;

  /**
   * Use Transposition Tables for visited nodes (needs extra memory)
   */
  boolean USE_TRANSPOSITION_TABLE = true;

  /**
   * Mate Distance Pruning
   */
  boolean USE_MATE_DISTANCE_PRUNING = true;

  /**
   * Minor Promotion Pruning
   */
  boolean USE_MINOR_PROMOTION_PRUNING = true;

  /**
   * Null Move Pruning
   */
  boolean USE_NULL_MOVE_PRUNING            = true;
  int     NULL_MOVE_DEPTH                  = 2;
  //boolean USE_VERIFY_NMP                   = false;
  //int     NULL_MOVE_REDUCTION_VERIFICATION = 3;

  /**
   * Eval Pruning - early cut for low static evals
   * Reverse Futility Pruning
   * https://www.chessprogramming.org/Reverse_Futility_Pruning
   */
  boolean USE_EVAL_PRUNING    = true;
  int     EVAL_PRUNING_DEPTH  = 2;
  int     EVAL_PRUNING_MARGIN = 120;

  /**
   * Razor  - early qsearch for low static evals
   */
  boolean USE_RAZOR_PRUNING    = true;
  int     RAZOR_PRUNING_DEPTH  = 3;
  int     RAZOR_PRUNING_MARGIN = 600;

  // TODO vvvvvvvv

  /**
   * Use Aspiration Window in root search
   */
  boolean USE_ASPIRATION_WINDOW = false;

  /**
   * If set to true we will use the opening book
   */
  boolean _USE_BOOK = false;

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

  /**
   * default opening book value
   */
  //Mode _OB_Mode = Mode.PGN;
  //Mode _OB_Mode = Mode.SAN;
  //Mode _OB_Mode = Mode.SIMPLE;
  @Override
  public String toString() {
    return "Configuration{" + "PERFT=" + PERFT + ", HASH_SIZE=" + HASH_SIZE + ", PONDER=" + PONDER +
           ", DEBUG=" + DEBUG + ", UCI_ShowCurrLine=" + UCI_ShowCurrLine + ", USE_QUIESCENCE=" +
           USE_QUIESCENCE + ", USE_ROOT_MOVES_SORT=" + USE_ROOT_MOVES_SORT +
           ", USE_ALPHABETA_PRUNING=" + USE_ALPHABETA_PRUNING + ", USE_ASPIRATION_WINDOW=" +
           USE_ASPIRATION_WINDOW + ", USE_PVS=" + USE_PVS + ", USE_TRANSPOSITION_TABLE=" +
           USE_TRANSPOSITION_TABLE + ", USE_MATE_DISTANCE_PRUNING=" + USE_MATE_DISTANCE_PRUNING +
           ", USE_MINOR_PROMOTION_PRUNING=" + USE_MINOR_PROMOTION_PRUNING + '}';
  }
}
