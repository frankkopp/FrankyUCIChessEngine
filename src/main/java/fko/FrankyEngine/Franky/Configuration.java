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

import fko.FrankyEngine.Franky.openingbook.*;

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
  public int HASH_SIZE = 256;

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
   * Sort root moves after iterations.
   */
  public boolean USE_ROOT_MOVES_SORT = true;

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
  //boolean USE_VERIFY_NMP                   = false;
  //int     NULL_MOVE_REDUCTION_VERIFICATION = 3;

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

  /**
   * Experimental sorting of moves in move generation.
   * Generating moves already has good ordering - extra sorting is expensive
   * and extra sorting can be worse than standard sorting
   */
  public boolean USE_SORT_ALL_MOVES = true;



  // TODO vvvvvvvv

  /**
   * Use Aspiration Window in root search
   */
  public boolean USE_ASPIRATION_WINDOW = false;


  @Override
  public String toString() {
    return "Configuration{" + "PERFT=" + PERFT + ", HASH_SIZE=" + HASH_SIZE + ", PONDER=" + PONDER +
           ", DEBUG=" + DEBUG + ", UCI_ShowCurrLine=" + UCI_ShowCurrLine + ", USE_BOOK=" +
           USE_BOOK + ", OB_FolderPath='" + OB_FolderPath + '\'' + ", OB_fileNamePlain='" +
           OB_fileNamePlain + '\'' + ", OB_Mode=" + OB_Mode + ", USE_QUIESCENCE=" + USE_QUIESCENCE +
           ", USE_ROOT_MOVES_SORT=" + USE_ROOT_MOVES_SORT + ", USE_PVS_MOVE_ORDERING=" +
           USE_PVS_MOVE_ORDERING + ", USE_ALPHABETA_PRUNING=" + USE_ALPHABETA_PRUNING +
           ", USE_PVS=" + USE_PVS + ", USE_TRANSPOSITION_TABLE=" + USE_TRANSPOSITION_TABLE +
           ", USE_MATE_DISTANCE_PRUNING=" + USE_MATE_DISTANCE_PRUNING +
           ", USE_MINOR_PROMOTION_PRUNING=" + USE_MINOR_PROMOTION_PRUNING +
           ", USE_NULL_MOVE_PRUNING=" + USE_NULL_MOVE_PRUNING + ", NULL_MOVE_DEPTH=" +
           NULL_MOVE_DEPTH + ", USE_STATIC_NULL_PRUNING=" + USE_STATIC_NULL_PRUNING +
           ", STATIC_NULL_PRUNING_DEPTH=" + STATIC_NULL_PRUNING_DEPTH +
           ", STATIC_NULL_PRUNING_MARGIN=" + STATIC_NULL_PRUNING_MARGIN + ", USE_RAZOR_PRUNING=" +
           USE_RAZOR_PRUNING + ", RAZOR_PRUNING_DEPTH=" + RAZOR_PRUNING_DEPTH +
           ", RAZOR_PRUNING_MARGIN=" + RAZOR_PRUNING_MARGIN + ", USE_ASPIRATION_WINDOW=" +
           USE_ASPIRATION_WINDOW + '}';
  }
}
