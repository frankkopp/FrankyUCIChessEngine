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
 * If the user wants to change some settings, the GUI will send a "setoption" command to the
 * engine.<br />
 * Note that the GUI need not send the setoption command when starting the engine for every
 * option if<br />
 * it doesn't want to change the default value.<br />
 * For all allowed combinations see the examples below,<br />
 * as some combinations of this tokens don't make sense.<br />
 * One string will be sent for each parameter.<br />
 * * name <id><br />
 * The option has the name id.<br />
 * Certain options have a fixed value for <id>, which means that the semantics of this option is
 * fixed.<br />
 * Usually those options should not be displayed in the normal engine options window of the GUI
 * but<br />
 * get a special treatment. "Pondering" for example should be set automatically when pondering
 * is<br />
 * enabled or disabled in the GUI options. The same for "UCI_AnalyseMode" which should also be
 * set<br />
 * automatically by the GUI. All those certain options have the prefix "UCI_" except for the<br />
 * first 6 options below. If the GUI gets an unknown Option with the prefix "UCI_", it should
 * just<br />
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
 * Note: The engine should not start pondering on its own if this is enabled, this option is
 * only<br />
 * needed because the engine might change its time management algorithm when pondering is allowed
 * .<br />
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
 * With this command the GUI can send the name, title, elo and if the engine is playing a
 * human<br />
 * or computer to the engine.<br />
 * The format of the string has to be [GM|IM|FM|WGM|WIM|none] [<elo>|none] [computer|human]
 * <name><br />
 * Examples:<br />
 * "setoption name UCI_Opponent value GM 2800 human Gary Kasparov"<br />
 * "setoption name UCI_Opponent value none none computer Shredder"<br />
 * * <id> = UCI_EngineAbout, type string<br />
 * With this command, the engine tells the GUI information about itself, for example a license
 * text,<br />
 * usually it doesn't make sense that the GUI changes this text with the setoption command.<br />
 * Example:<br />
 * "option name UCI_EngineAbout type string default Shredder by Stefan Meyer-Kahlen, see www
 * .shredderchess.com"<br />
 * * <id> = UCI_ShredderbasesPath, type string<br />
 * this is either the path to the folder on the hard disk containing the Shredder endgame
 * databases or<br />
 * the path and filename of one Shredder endgame datbase.<br />
 * * <id> = UCI_SetPositionValue, type string<br />
 * the GUI can send this to the engine to tell the engine to use a certain value in centipawns
 * from white's<br />
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
public interface IUCIOption {

  String getNameID();

  UCIOptionType getOptionType();

  String getDefaultValue();

  String getMinValue();

  String getMaxValue();

  String getVarValue();

  /**
   * UCI Option can have these types
   */
  enum UCIOptionType {check, spin, combo, button, string, unknown}
}
