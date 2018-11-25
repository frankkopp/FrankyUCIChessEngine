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

package fko.javaUCIEngineFramework.Omega;

/**
 * Enumeration of all piece types in chess
 */
@SuppressWarnings("javadoc")
public enum OmegaPieceType {

    // order has influence on OmegaPiece
    NOTYPE ("-", false, 0),    // 0
    PAWN   ("P", false, 100),  // 1
    KNIGHT ("N", false, 320),  // 2
    BISHOP ("B", true,  330),  // 3
    ROOK   ("R", true,  500),  // 4
    QUEEN  ("Q", true,  900),  // 5
    KING   ("K", false, 20000);// 6

    static final OmegaPieceType[] values = {
            NOTYPE,
            PAWN  ,
            KNIGHT,
            BISHOP,
            ROOK  ,
            QUEEN ,
            KING  ,
    };

    private final String _shortName;
    private final int _value;
    private boolean _sliding;

    private OmegaPieceType(String shortName, boolean sliding, int value) {
        _shortName = shortName;
        _sliding = sliding;
        _value = value;
    }

    /**
     * @return the shortName
     */
    protected String getShortName() {
        return this._shortName;
    }

    /**
     * @return the sliding
     */
    public boolean isSliding() {
        return _sliding;
    }

    /**
     * @return the value
     */
    protected int getValue() {
        return this._value;
    }

    @Override
    public String toString() {
        return _shortName;
    }

}
