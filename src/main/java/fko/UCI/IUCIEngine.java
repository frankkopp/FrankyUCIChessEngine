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

package fko.UCI;

import java.util.List;

/**
 * Interface for UCI Engines<p>
 */
public interface IUCIEngine {

  /**
   * this is sent to the engine when the user wants to change the internal parameters
   * of the engine. For the "button" type no value is needed.
   * One string will be sent for each parameter and this will only be sent when the engine is waiting.
   * The name and value of the option in <id> should not be case sensitive and can inlude spaces.
   * The substrings "value" and "name" should be avoided in <id> and <x> to allow unambiguous parsing,
   * for example do not use <name> = "draw value".
   * Here are some strings for the example below:
   * "setoption name Nullmove value true\n"
   * "setoption name Selectivity value 3\n"
   * "setoption name Style value Risky\n"
   * "setoption name Clear Hash\n"
   * "setoption name NalimovPath value c:\chess\tb\4;c:\chess\tb\5\n"
   */
  void setOption(String name, String value);

  /**
   * Returns the name of the engine. Used by the protocol when communication with UI starts
   *
   * @return name of engine
   */
  String getIDName();

  /**
   * Returns the author of the engine. Used by the protocol when communication with UI starts.
   *
   * @return author of engine
   */
  String getIDAuthor();

  /**
   * Returns a list of option this engine has. The UCI protocol will send these option to the
   * UI and the UI will build a configuration dialog with them.
   *
   * @return list of options this engine defines for configuring through the UI
   */
  List<IUCIOption> getOptions();

  /**
   * Switch the debug mode of the engine on and off.
   * In debug mode the engine should sent additional infos to the GUI, e.g. with the "info string" command,
   * to help debugging, e.g. the commands that the engine has received etc.
   * This mode should be switched off by default and this command can be sent
   * any time, also when the engine is thinking.
   *
   * @param debugOption
   */
  void setDebugMode(boolean debugOption);

  /**
   * Switch the debug mode of the engine on and off.
   * In debug mode the engine should sent additional infos to the GUI, e.g. with the "info string" command,
   * to help debugging, e.g. the commands that the engine has received etc.
   * This mode should be switched off by default and this command can be sent
   * any time, also when the engine is thinking.
   *
   * @return true if debug mode is on, false otherwise.
   */
  default boolean getDebugMode() {
    return false;
  }

  /**
   * This is sent to the engine when the next search (started with "position" and "go") will be from
   * a different game. This can be a new game the engine should play or a new game it should analyse but
   * also the next position from a testsuite with positions only.
   * If the GUI hasn't sent a "ucinewgame" before the first "position" command, the engine shouldn't
   * expect any further ucinewgame commands as the GUI is probably not supporting the ucinewgame command.
   * So the engine should not rely on this command even though all new GUIs should support it.
   * As the engine's reaction to "ucinewgame" can take some time the GUI should always send "isready"
   * after "ucinewgame" to wait for the engine to finish its operation.
   */
  void newGame();

  /**
   * Set up the position described in fenstring on the internal board.
   * if the game was played  from the start position the string "startpos" will be sent
   * Note: no "new" command is needed. However, if this position is from a different game than
   * the last position sent to the engine, the GUI should have sent a "ucinewgame" inbetween.
   * <p>
   * UCI: position [fen <fenstring> | startpos ]  moves <move1> .... <movei>
   *
   * @param startFen
   */
  void setPosition(String startFen);

  /**
   * Play the moves from the position command on the internal chess board. WIll be called by the protocol handler
   *
   * @param move as simple UCI notation e.g. e2e2 or a7a8
   */
  void doMove(String move);

  /**
   * This is used to synchronize the engine with the GUI. When the GUI has sent a command or
   * multiple commands that can take some time to complete,
   * this command can be used to wait for the engine to be ready again or
   * to ping the engine to find out if it is still alive.
   * E.g. this should be sent after setting the path to the tablebases as this can take some time.
   * This command is also required once before the engine is asked to do any search
   * to wait for the engine to finish initializing.
   * This command must always be answered with "readyok" and can be sent also when the engine is calculating
   * in which case the engine should also immediately answer with "readyok" without stopping the search.
   *
   * @return true if engine is ready, false otherwise
   */
  default boolean isReady() {
    return false;
  }

  /**
   * Start calculating on the current position set up with the "position" command.<br/>
   * Possible search parameters are encoded in SearchMode<br/>
   *
   * @param searchMode possible search parameters
   * @see fko.FrankyEngine.Franky.SearchMode
   */
  void startSearch(IUCISearchMode searchMode);

  /**
   * @return true is engine is currently searching, false otherwise
   */
  boolean isSearching();

  /**
   * Stops a currently running search. Should do nothing if now search is running.
   * Don't forget to send the "bestmove" and possibly the "ponderOption" token
   * when finishing the search
   */
  void stopSearch();

  /**
   * the user has played the expected move. This will be sent if the engine was
   * told to ponder on the same move	the user has played. The engine should
   * continue searching but switch from pondering to normal search.
   */
  void ponderHit();

  /**
   * To be able to send the UI information the engine needs a reference to the protocol handler.
   * If none is given it will not send anything when <code>sendInfoToUCI</code> is called.
   *
   * @param uciProtocolHandler protocol hhandler to enable engine to send information to the UI
   */
  void registerProtocolHandler(IUCIProtocolHandler uciProtocolHandler);

  /**
   * Call back from the engine's search when a result has been found and should
   * be send via the UCIProtocolHandler to the UI.
   * <p>
   * the engine has stopped searching and found the move <move> best in this position.
   * the engine can send the move it likes to ponderOption on. The engine must not start pondering automatically.
   * this command must always be sent if the engine stops searching, also in pondering mode if there is a
   * "stop" command, so for every "go" command a "bestmove" command is needed!
   * Directly before that the engine should send a final "info" command with the final search information,
   * the the GUI has the complete statistics about the last search.
   *
   * @param bestMove
   * @param ponderMove
   */
  void sendResult(int bestMove, int ponderMove);

  /**
   * UCI info
   * <p>
   * <p>the engine wants to send information to the GUI. This should be done whenever one of the info has changed.<br>
   * The engine can send only selected infos or multiple infos with one info command,<br>
   * e.g. "info currmove e2e4 currmovenumber 1" or<br>
   * "info depth 12 nodes 123456 nps 100000".<br>
   * Also all infos belonging to the pv should be sent together<br>
   * e.g. "info depth 2 score cp 214 time 1242 nodes 2124 nps 34928 pv e2e4 e7e5 g1f3"<br>
   * I suggest to start sending "currmove", "currmovenumber", "currline" and "refutation" only after one second<br>
   * to avoid too much traffic.<br>
   * Additional info:<br>
   * * depth <x><br>
   * search depth in plies<br>
   * * seldepth <x><br>
   * selective search depth in plies,<br>
   * if the engine sends seldepth there must also be a "depth" present in the same string.<br>
   * * time <x><br>
   * the time searched in ms, this should be sent together with the pv.<br>
   * * nodes <x><br>
   * x nodes searched, the engine should send this info regularly<br>
   * * pv <move1> ... <movei><br>
   * the best line found<br>
   * * multipv <num><br>
   * this for the multi pv mode.<br>
   * for the best move/pv add "multipv 1" in the string when you send the pv.<br>
   * in k-best mode always send all k variants in k strings together.<br>
   * * score<br>
   * * cp <x><br>
   * the score from the engine's point of view in centipawns.<br>
   * * mate <y><br>
   * mate in y moves, not plies.<br>
   * If the engine is getting mated use negative values for y.<br>
   * * lowerbound<br>
   * the score is just a lower bound.<br>
   * * upperbound<br>
   * the score is just an upper bound.<br>
   * * currmove <move><br>
   * currently searching this move<br>
   * * currmovenumber <x><br>
   * currently searching move number x, for the first move x should be 1 not 0.<br>
   * * hashfull <x><br>
   * the hash is x permill full, the engine should send this info regularly<br>
   * * nps <x><br>
   * x nodes per second searched, the engine should send this info regularly<br>
   * * tbhits <x><br>
   * x positions where found in the endgame table bases<br>
   * * sbhits <x><br>
   * x positions where found in the shredder endgame databases<br>
   * * cpuload <x><br>
   * the cpu usage of the engine is x permill.<br>
   * * string <str><br>
   * any string str which will be displayed be the engine,<br>
   * if there is a string command the rest of the line will be interpreted as <str>.<br>
   * * refutation <move1> <move2> ... <movei><br>
   * move <move1> is refuted by the line <move2> ... <movei>, i can be any number &gt;= 1.<br>
   * Example: after move d1h5 is searched, the engine can send<br>
   * "info refutation d1h5 g6h5"<br>
   * if g6h5 is the best answer after d1h5 or if g6h5 refutes the move d1h5.<br>
   * if there is no refutation for d1h5 found, the engine should just send<br>
   * "info refutation d1h5"<br>
   * The engine should only send this if the option "UCI_ShowRefutations" is set to true.<br>
   * * currline <cpunr> <move1> ... <movei><br>
   * this is the current line the engine is calculating. <cpunr> is the number of the cpu if<br>
   * the engine is running on more than one cpu. <cpunr> = 1,2,3....<br>
   * if the engine is just using one cpu, <cpunr> can be omitted.<br>
   * If <cpunr> is greater than 1, always send all k lines in k strings together.<br>
   * The engine should only send this if the option "UCI_ShowCurrLine" is set to true.<br>
   * </p>
   *
   * @param infoString
   */
  void sendInfoToUCI(String infoString);

}
