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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;

public class TestMoveSorting {

  private Position position;
  private int[]    killerMoves;

  @Test
  @Disabled
  public void testSorting() {

    position = new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");

    MoveGenerator mG = new MoveGenerator();

    int killer1 = 67320564;
    int killer2 = 67318516;
    killerMoves = new int[]{killer1, killer2};

    //mG.setKillerMoves(killerMoves);
    mG.SORT_MOVES = false;
    mG.SORT_CAPTURING_MOVES = false;

    MoveList m1 = mG.getPseudoLegalMoves(position);
    MoveList m2 = new MoveList(m1);
    m2.sort(movesComparator);

    System.out.println();
    System.out.printf("%-20s %-20s %n", "Test 1", "Test 2");
    for (int i = 0; i < m1.size(); i++) {
      System.out.printf("%-20s %-20s %n", Move.toString(m1.get(i)), Move.toString(m2.get(i)));
    }

  }

  /**
   * Comperator for all moves. Smaller values heapsort first
   */
  private final Comparator<Integer> movesComparator = Comparator.comparingInt(this::getSortValue);

  private int getSortValue(int move) {
    // capturing moves
    if (!Move.getTarget(move).equals(Piece.NOPIECE)) {
      return 1000 + Move.getPiece(move).getType().getValue() -
             Move.getTarget(move).getType().getValue();
    }
    // non capturing
    else {
      if (killerMoves != null) {
        for (int i = killerMoves.length - 1; i >= 0; i--) {
          if (killerMoves[i] == move) {
            return 5000 + i;
          }
        }
      }
      // promotions
      PieceType pieceType = Move.getPromotion(move).getType();
      if (!pieceType.equals(PieceType.NOTYPE)) {
        switch (pieceType) {
          case QUEEN:
            return 9000;
          case KNIGHT:
            return 9100;
          case ROOK:
            return 10900;
          case BISHOP:
            return 10900;
        }
      }
      // castling
      else if (Move.getMoveType(move).equals(MoveType.CASTLING)) {
        return 9200;
      }
      // all other moves
      return 10000 + Evaluation.getPositionValue(position, move);
    }
  }

  @Test
  @Disabled
  public void testTiming() {

    ArrayList<String> result = new ArrayList<>();

    int[] m1 = new int[]{};
    int[] m2 = new int[]{};

    prepare();

    int ROUNDS = 5;
    int ITERATIONS = 10;
    int REPETITIONS = 10000;

    for (int round = 0; round < ROUNDS; round++) {
      long start = 0, end = 0, sum = 0;

      System.out.printf("Running round %d of Timing Test Test 1 vs. Test 2%n", round + 1);
      System.gc();

      int i = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          m1 = test1();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg1 = ((float) sum / ITERATIONS) / 1e9f;

      i = 0;
      sum = 0;
      while (++i <= ITERATIONS) {
        start = System.nanoTime();
        for (int j = 0; j < REPETITIONS; j++) {
          m2 = test2();
        }
        end = System.nanoTime();
        sum += end - start;
      }
      float avg2 = ((float) sum / ITERATIONS) / 1e9f;

      result.add(String.format("Round %d Test 1 avg: %,.3f sec", round + 1, avg1));
      result.add(String.format("Round %d Test 2 avg: %,.3f sec", round + 1, avg2));
    }

    System.out.println();
    System.out.printf("%-20s %-20s %n", "Test 1", "Test 2");
    for (int i = 0; i < m1.length; i++) {
      System.out.printf("%-20s %-20s %n", Move.toString(m1[i]), Move.toString(m2[i]));
    }

    System.out.println();
    for (String s : result) {
      System.out.println(s);
    }

  }

  private void prepare() {
  }

  private int[] test1() {
    position = new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");

    MoveGenerator mG = new MoveGenerator();
    mG.SORT_MOVES = false;
    mG.SORT_CAPTURING_MOVES = true;

    killerMoves = new int[]{67320564, 67318516};
    mG.setKillerMoves(killerMoves);

    final MoveList moves = mG.getPseudoLegalMoves(position);
    int[] m = moves.toArray();
    return m;

  }

  private int[] test2() {
    position = new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");

    MoveGenerator mG = new MoveGenerator();
    mG.SORT_MOVES = false;
    mG.SORT_CAPTURING_MOVES = false;

    killerMoves = new int[]{67320564, 67318516};
    mG.setKillerMoves(killerMoves);

    final MoveList moves = mG.getPseudoLegalMoves(position);

    int[] m = moves.toArray();
    int[] sortIdx = new int[m.length];
    for (int i = 0; i < m.length; i++) {
      sortIdx[i] = getSortValue(m[i]);
    }
    heapsort(m, sortIdx);
    //quicksort(0, m.length-1, m, sortIdx);

    return m;
  }

  private void heapsort(int[] m, int[] s) {
    // heapsort
    int tm, ts;
    for (int i = 0; i < m.length; i++) {
      for (int j = i; j > 0; j--) {
        if (s[j] - s[j - 1] < 0) {
          tm = m[j];
          ts = s[j];
          m[j] = m[j - 1];
          s[j] = s[j - 1];
          m[j - 1] = tm;
          s[j - 1] = ts;
        }
      }
    }
  }


  /*
  TESTING QUICKSORT BELOW
   */

  @Test
  @Disabled
  public void testQuickSort() {

    position = new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");

    MoveGenerator mG = new MoveGenerator();
    mG.SORT_MOVES = false;
    mG.SORT_CAPTURING_MOVES = false;

    int[] m1 = mG.getPseudoLegalMoves(position).toArray();
    int[] m2 = mG.getPseudoLegalMoves(position).toArray();

    int[] sortIdx = new int[m2.length];
    for (int i = 0; i < m2.length; i++) {
      sortIdx[i] = getSortValue(m2[i]);
    }
    heapsort(m2, sortIdx);
    //quicksort(0, m2.length-1, m2, sortIdx);

    System.out.println();
    System.out.printf("%-20s %-20s %n", "Test 1", "Test 2");
    for (int i = 0; i < m1.length; i++) {
      System.out.printf("%-20s %-20s %n", Move.toString(m1[i]), Move.toString(m2[i]));
    }
  }

  private void quicksort(int head, int tail, int[] list, int[] sortIdx) {
    // quicksort
    int low = head;
    int high = tail;
    int midValue = sortIdx[(head + tail) / 2];

    while (low <= high) {
      while (sortIdx[low] - midValue < 0) {
        low++;
      }
      while (sortIdx[high] - midValue > 0) {
        high--;
      }
      if (low <= high) {
        exchange(low, high, list, sortIdx);
        low++;
        high--;
      }
      if (head < high) {
        quicksort(head, high, list, sortIdx);
      }
      if (low < tail) {
        quicksort(low, tail, list, sortIdx);
      }
    }
  }

  private void exchange(final int i, final int j, final int[] list, final int[] sortIdx) {
    final int tm = list[i];
    final int ts = sortIdx[i];
    list[i] = list[j];
    sortIdx[i] = sortIdx[j];
    list[j] = tm;
    sortIdx[j] = ts;
  }

  public boolean isSorted(int[] array) {
    int prev = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] < prev) {
        return false;
      }
      prev = array[i];
    }
    return true;
  }
}
