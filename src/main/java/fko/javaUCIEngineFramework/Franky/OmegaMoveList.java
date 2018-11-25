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

import fko.chessly.util.SimpleIntList;

import java.util.Comparator;

/**
 * Simple and fast list class for OmegaMoves which are in fact integers.
 * Grows as needed.
 *
 * @author Frank
 */
public class OmegaMoveList extends SimpleIntList {

    /**
     * Creates a list with a maximum of 75 elements
     * Max numbers of possible moves per position is 218
     * fen = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1"; // 218 moves to make
     * Max for pseudo legal moves seems to be 225
     * we use 75 and let it grow to save space as more than 75 is rare
     */
    public OmegaMoveList() {
        super(75);
    }

    /**
     * Creates a list with a maximum of max_site elements
     * @param max
     */
    public OmegaMoveList(int max) {
        super(max);
    }

    /**
     * Creates a list as a copy of the provided list.
     * @param old
     */
    public OmegaMoveList(OmegaMoveList old) {
        super(old);
    }

    /* (non-Javadoc)
     * @see fko.chessly.player.computer.Omega.OmegaIntegerList#add(int)
     */
    @Override
    public void add(int move) {
        if (!OmegaMove.isValid(move))
            throw new IllegalArgumentException("not a valid move: "+move);
        super.add(move);
    }

    /* (non-Javadoc)
     * @see fko.chessly.player.computer.Omega.OmegaIntegerList#add(fko.chessly.player.computer.Omega.OmegaIntegerList)
     */
    @Override
    public void add(SimpleIntList newList) {
        if (!(newList instanceof OmegaMoveList))
            throw new IllegalArgumentException("not a valid OmegaMoveList: "+newList);
        super.add(newList);
    }

    /* (non-Javadoc)
     * @see fko.chessly.util.SimpleIntList#sort(java.util.Comparator)
     */
    @Override
    public void sort(Comparator<Integer> comparator) {
        int temp;
        for (int i = _head + 1; i < _tail; i++) {
            for (int j = i; j > _head; j--) {
                if (OmegaMove.getPiece(_list[j]).getType().getValue() - OmegaMove.getTarget(_list[j]).getType().getValue()
                        -(OmegaMove.getPiece(_list[j-1]).getType().getValue() - OmegaMove.getTarget(_list[j-1]).getType().getValue()) < 0) {
                    temp = _list[j];
                    _list[j] = _list[j-1];
                    _list[j-1] = temp;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see fko.chessly.player.computer.Omega.OmegaIntegerList#toString()
     */
    @Override
    public String toString() {
        String s = "MoveList size="+size()+" available capacity="+getAvailableCapacity()+" [";
        for (int i=0; i<size(); i++) {
            s += get(i) + " ("+OmegaMove.toString(get(i))+")";
            if (i<size()-1) s += ", ";
        }
        s+="]";
        return s;
    }

    /**
     * Print the list as a string of move simple move notations.<br>
     * e2-e4 e7-e5 ....
     * @return string containing the moves of the list
     */
    public String toNotationString() {
        String s = "";
        for (int i=0; i<size(); i++) {
            s += OmegaMove.toSimpleString(get(i))+" ";
        }
        return s;
    }

    /**
     * clones the list
     */
    @Override
    public OmegaMoveList clone() {
        return new OmegaMoveList(this);
    }

    /**
     * Copies the content of src array into dest array at index 1
     * and sets index 0 of dest array to the specified move.
     * @param move
     * @param value
     * @param src
     * @param dest
     */
    static void savePV(int move, OmegaMoveList src, OmegaMoveList dest) {
        dest._list[0] = move;
        System.arraycopy(src._list, src._head, dest._list, 1, src.size());
        dest._tail = src.size() + 1;
    }
}
