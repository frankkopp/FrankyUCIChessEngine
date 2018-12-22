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

package fko.javaUCIEngineFramework.UCI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UCIOption */
public class UCIOption implements IUCIEngine.IUCIOption {

  private static final Logger LOG = LoggerFactory.getLogger(UCIOption.class);

  final String nameID;
  final IUCIEngine.UCIOptionType type;
  final String defaultValue;
  final String minValue;
  final String maxValue;
  final String varValue;

  String chosenValue;

  public UCIOption(
      final String nameID,
      final IUCIEngine.UCIOptionType type,
      final String defaultValue,
      final String minValue,
      final String maxValue,
      final String varValue) {

    this.nameID = nameID;
    this.type = type;
    this.defaultValue = defaultValue;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.varValue = varValue;
    this.chosenValue = "";
  }

  @Override
  public String getNameID() {
    return nameID;
  }

  @Override
  public IUCIEngine.UCIOptionType getOptionType() {
    return type;
  }

  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String getMinValue() {
    return minValue;
  }

  @Override
  public String getMaxValue() {
    return maxValue;
  }

  @Override
  public String getVarValue() {
    return varValue;
  }

}
