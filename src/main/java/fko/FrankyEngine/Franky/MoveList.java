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

import fko.FrankyEngine.util.SimpleIntList;

import java.util.Comparator;

/**
 * Simple and fast list class for OmegaMoves which are in fact integers.
 * Grows as needed.
 *
 * @author Frank
 */
public class MoveList extends SimpleIntList {

    /**
     * Creates a list with a initial capacity of 75 elements
     * Max numbers of possible moves per position is 218
     * fen = "R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1"; // 218 moves to make
     * Max for pseudo legal moves seems to be 225
     * we use 75 and let it grow to save space as more than 75 is rare
     */
    public MoveList() {
        super(75);
    }

    /**
     * Creates a list with a maximum of max_site elements
     * @param max
     */
    public MoveList(int max) {
        super(max);
    }

    /**
     * Creates a list as a copy of the provided list.
     * @param old
     */
    public MoveList(MoveList old) {
        super(old);
    }

    @Override
    public void add(int move) {
        if (!Move.isValid(move))
            throw new IllegalArgumentException("not a valid move: "+move);
        super.add(move);
    }

    @Override
    public void add(SimpleIntList newList) {
        if (!(newList instanceof MoveList))
            throw new IllegalArgumentException("not a valid MoveList: "+newList);
        super.add(newList);
    }

    @Override
    public void addFront(SimpleIntList newList) {
        if (!(newList instanceof MoveList))
            throw new IllegalArgumentException("not a valid MoveList: "+newList);
        super.addFront(newList);
    }

    @Override
    public void sort(Comparator<Integer> comparator) {
        super.sort(comparator);
    }

    @Override
    public String toString() {
        String s = "MoveList size="+size()+" available capacity="+getAvailableCapacity()+" [";
        for (int i=0; i<size(); i++) {
            s += get(i) + " (" + Move.toString(get(i)) + ")";
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
            s += Move.toSimpleString(get(i)) + " ";
        }
        return s;
    }

    /**
     * clones the list
     */
    @Override
    public MoveList clone() {
        return new MoveList(this);
    }

    /**
     * Copies the content of src array into dest array at index 1
     * and sets index 0 of dest array to the specified move.
     * @param move
     * @param src
     * @param dest
     */
    static void savePV(int move, MoveList src, MoveList dest) {
        dest.clear();
        dest.add(move);
        dest.add(src);
    }
}
