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
   *
   * @return
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
   * move <move1> is refuted by the line <move2> ... <movei>, i can be any number >= 1.<br>
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

  /**
   * An Option for a FrankyEngine
   * <p>
   * <p>* option<br />
   * This command tells the GUI which parameters can be changed in the engine.<br />
   * This should be sent once at engine startup after the "uci" and the "id" commands<br />
   * if any parameter can be changed in the engine.<br />
   * The GUI should parse this and build a dialog for the user to change the settings.<br />
   * Note that not every option needs to appear in this dialog as some options like<br />
   * "Ponder", "UCI_AnalyseMode", etc. are better handled elsewhere or are set automatically.<br />
   * If the user wants to change some settings, the GUI will send a "setoption" command to the engine.<br />
   * Note that the GUI need not send the setoption command when starting the engine for every option if<br />
   * it doesn't want to change the default value.<br />
   * For all allowed combinations see the examples below,<br />
   * as some combinations of this tokens don't make sense.<br />
   * One string will be sent for each parameter.<br />
   * * name <id><br />
   * The option has the name id.<br />
   * Certain options have a fixed value for <id>, which means that the semantics of this option is fixed.<br />
   * Usually those options should not be displayed in the normal engine options window of the GUI but<br />
   * get a special treatment. "Pondering" for example should be set automatically when pondering is<br />
   * enabled or disabled in the GUI options. The same for "UCI_AnalyseMode" which should also be set<br />
   * automatically by the GUI. All those certain options have the prefix "UCI_" except for the<br />
   * first 6 options below. If the GUI gets an unknown Option with the prefix "UCI_", it should just<br />
   * ignore it and not display it in the engine's options dialog.<br />
   * * <id> = Hash, type is spin<br />
   * the value in MB for memory for hash tables can be changed,<br />
   * this should be answered with the first "setoptions" command at program boot<br />
   * if the engine has sent the appropriate "option name Hash" command,<br />
   * which should be supported by all engines!<br />
   * So the engine should use a very small hash first as default.<br />
   * * <id> = NalimovPath, type string<br />
   * this is the path on the hard disk to the Nalimov compressed format.<br />
   * Multiple directories can be concatenated with ";"<br />
   * * <id> = NalimovCache, type spin<br />
   * this is the sizeInBytes in MB for the cache for the nalimov table bases<br />
   * These last two options should also be present in the initial options exchange dialog<br />
   * when the engine is booted if the engine supports it<br />
   * * <id> = Ponder, type check<br />
   * this means that the engine is able to ponderOption.<br />
   * The GUI will send this whenever pondering is possible or not.<br />
   * Note: The engine should not start pondering on its own if this is enabled, this option is only<br />
   * needed because the engine might change its time management algorithm when pondering is allowed.<br />
   * * <id> = OwnBook, type check<br />
   * this means that the engine has its own book which is accessed by the engine itself.<br />
   * if this is set, the engine takes care of the opening book and the GUI will never<br />
   * execute a move out of its book for the engine. If this is set to false by the GUI,<br />
   * the engine should not access its own book.<br />
   * * <id> = MultiPV, type spin<br />
   * the engine supports multi best line or k-best mode. the default value is 1<br />
   * * <id> = UCI_ShowCurrLine, type check, should be false by default,<br />
   * the engine can show the current line it is calculating. see "info currline" above.<br />
   * * <id> = UCI_ShowRefutations, type check, should be false by default,<br />
   * the engine can show a move and its refutation in a line. see "info refutations" above.<br />
   * * <id> = UCI_LimitStrength, type check, should be false by default,<br />
   * The engine is able to limit its strength to a specific Elo number,<br />
   * This should always be implemented together with "UCI_Elo".<br />
   * * <id> = UCI_Elo, type spin<br />
   * The engine can limit its strength in Elo within this interval.<br />
   * If UCI_LimitStrength is set to false, this value should be ignored.<br />
   * If UCI_LimitStrength is set to true, the engine should play with this specific strength.<br />
   * This should always be implemented together with "UCI_LimitStrength".<br />
   * * <id> = UCI_AnalyseMode, type check<br />
   * The engine wants to behave differently when analysing or playing a game.<br />
   * For example when playing it can use some kind of learning.<br />
   * This is set to false if the engine is playing a game, otherwise it is true.<br />
   * * <id> = UCI_Opponent, type string<br />
   * With this command the GUI can send the name, title, elo and if the engine is playing a human<br />
   * or computer to the engine.<br />
   * The format of the string has to be [GM|IM|FM|WGM|WIM|none] [<elo>|none] [computer|human] <name><br />
   * Examples:<br />
   * "setoption name UCI_Opponent value GM 2800 human Gary Kasparov"<br />
   * "setoption name UCI_Opponent value none none computer Shredder"<br />
   * * <id> = UCI_EngineAbout, type string<br />
   * With this command, the engine tells the GUI information about itself, for example a license text,<br />
   * usually it doesn't make sense that the GUI changes this text with the setoption command.<br />
   * Example:<br />
   * "option name UCI_EngineAbout type string default Shredder by Stefan Meyer-Kahlen, see www.shredderchess.com"<br />
   * * <id> = UCI_ShredderbasesPath, type string<br />
   * this is either the path to the folder on the hard disk containing the Shredder endgame databases or<br />
   * the path and filename of one Shredder endgame datbase.<br />
   * * <id> = UCI_SetPositionValue, type string<br />
   * the GUI can send this to the engine to tell the engine to use a certain value in centipawns from white's<br />
   * point of view if evaluating this specifix position. <br />
   * The string can have the formats:<br />
   * <value> + <fen> | clear + <fen> | clearall<br />
   * <br />
   * * type <t><br />
   * The option has type t.<br />
   * There are 5 different types of options the engine can send<br />
   * * check<br />
   * a checkbox that can either be true or false<br />
   * * spin<br />
   * a spin wheel that can be an integer in a certain range<br />
   * * combo<br />
   * a combo box that can have different predefined strings as a value<br />
   * * button<br />
   * a button that can be pressed to send a command to the engine<br />
   * * string<br />
   * a text field that has a string as a value,<br />
   * an empty string has the value "<empty>"<br />
   * * default <x><br />
   * the default value of this parameter is x<br />
   * * min <x><br />
   * the minimum value of this parameter is x<br />
   * * max <x><br />
   * the maximum value of this parameter is x<br />
   * * var <x><br />
   * a predefined value of this parameter is x<br />
   * Examples:<br />
   * Here are 5 strings for each of the 5 possible types of options<br />
   * "option name Nullmove type check default true\n"<br />
   * "option name Selectivity type spin default 2 min 0 max 4\n"<br />
   * "option name Style type combo default Normal var Solid var Normal var Risky\n"<br />
   * "option name NalimovPath type string default c:\\n"<br />
   * "option name Clear Hash type button\n"<br />
   * </p>
   */
  interface IUCIOption {

    String getNameID();

    UCIOptionType getOptionType();

    String getDefaultValue();

    String getMinValue();

    String getMaxValue();

    String getVarValue();

  }

  /**
   * UCI Option can have these types
   */
  enum UCIOptionType {check, spin, combo, button, string, unknown}
}
