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

public interface IUCIProtocolHandler {

  String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /**
   * Receiver loop should listen for any piped copmmands from the UI. Usually this will be
   * done in a separate thread to make sure to be able to asynchronously get all commands
   */
  void receiveLoop();

  /**
   * Sends the UCI "info *" command plus the provided msg to the UI
   * @param msg
   */
  void sendInfoToUCI(String msg);

  /**
   * Sends the UCI "info string *" command plus the provided msg to the UI
   * @param msg
   */
  void sendInfoStringToUCI(String msg);

  /**
   * Sends the UCI "bestmove *" command to the UI
   * @param result
   */
  void sendResultToUCI(String result);

  /**
   * Sends the UCI "bestmove * ponder *" command to the UI
   * @param result
   * @param ponder
   */
  void sendResultToUCI(String result, String ponder);


}
