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

package fko.javaUCIEngineFramework.Franky;

/**
 * A simple and fast list for OmegaSquares for use as piece lists.<br>
 */
public class SquareList {

    private static final int MAX_SIZE = 65;

    private final Square[] elements = new Square[MAX_SIZE];
    /**
     * @param i
     * @return the element at index i
     */
    public Square get(int i) {
        if (i>=size) throw new ArrayIndexOutOfBoundsException();
        return this.elements[i];
    }

    private int size = 0;
    /**
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Adds the given square to the beginning of the list
     * @param square
     */
    public void add(Square square) {
        if (size >= MAX_SIZE-1)
            throw new ArrayStoreException("SquareList is full");

        /*
         * we need to keep the order stable because when
         * iterating over the list and removing and adding
         * the same element should not change the list at all!
         * (e.g. when makeMove/undoMove happens)
         */

        final int ordinal = square.ordinal();

        // go backward and move all elements one place to the right
        // when the right place is found insert the new element.
        for (int i = size-1; i >= 0; i--) {
            if (ordinal > elements[i].ordinal()) {
                elements[i+1] = square;
                size++;
                return;
            }
            elements[i+1] = elements[i]; // move the element to the right
        }
        // we did not a place -> element seams to be first in order
        // add it to place 0 as this is now free
        elements[0] = square;
        size++;
    }

    /**
     * Remove the given element from the list.
     * Does nothing if element is not in the list.
     * @param square
     */
    public void remove(Square square) {

        /*
         * Go over the array from left to right until you found the element.
         * copy the next element to the elements place (overwriting it).
         * Done.
         */
        Square toBeRemoved = square;
        for (int i=0; i<size; i++) {
            if (toBeRemoved == elements[i]) { // hit
                elements[i] = elements[i+1]; // overwrite the element with the one from the right
                toBeRemoved = elements[i+1]; // now the next to the right can be removed
            }
        }
        // only reduce if we found our element in the list
        if (toBeRemoved!=square)
            size--;

    }

    /**
     * @return true if size == 0
     */
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public String toString() {
        String s = "["+size()+"] ";
        for (int i=0; i<size(); i++) {
            s += elements[i] + " ";
        }
        return s;
    }

    /**
     * Returns a deep copy of the list
     */
    @Override
    public SquareList clone() {
        SquareList clone = new SquareList();
        System.arraycopy(this.elements, 0, clone.elements, 0, elements.length);
        clone.size = this.size;
        return clone;
    }


}
