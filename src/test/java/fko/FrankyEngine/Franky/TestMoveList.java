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


import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Frank
 */
public class TestMoveList {

  @Test
  public void testListWithInts() {
    MoveList list = new MoveList();

    // empty list
    testEmptyList(list);

    // add one entry
    int move1 =
      Move.createMove(MoveType.NORMAL, Square.b1, Square.c3, Piece.WHITE_KNIGHT, Piece.NOPIECE,
                      Piece.NOPIECE);

    int value = move1;

    list.add(value);
    assertEquals(1, list.size());
    assertFalse(list.empty());
    assertEquals(list.get(0), value);
    assertEquals(list.getFirst(), value);
    assertEquals(list.getLast(), value);

    // remove one entry
    int element = list.removeLast();
    assertEquals(element, value);
    testEmptyList(list);
    list.add(value);
    element = list.removeFirst();
    assertEquals(element, value);
    testEmptyList(list);

    // add 10 entries
    for (int i = 100; i < 110; i++) {
      int move = Move.createMove(MoveType.NORMAL, Square.getValueList().get(i - 100),
                                 Square.getValueList().get(i - 100), Piece.WHITE_PAWN,
                                 Piece.NOPIECE, Piece.NOPIECE);
      list.add(move);
      assertEquals(list.size(), i - 100 + 1);
      assertFalse(list.empty());
    }

    // get one entry
    assertEquals(list.get(4), Move.createMove(MoveType.NORMAL, Square.values[4], Square.values[4],
                                              Piece.WHITE_PAWN, Piece.NOPIECE, Piece.NOPIECE));

    // remove one entry
    element = list.removeLast();
    assertEquals(element, Move.createMove(MoveType.NORMAL, Square.b2, Square.b2, Piece.WHITE_PAWN,
                                          Piece.NOPIECE, Piece.NOPIECE));
    assertEquals(list.getLast(),
                 Move.createMove(MoveType.NORMAL, Square.a2, Square.a2, Piece.WHITE_PAWN,
                                 Piece.NOPIECE, Piece.NOPIECE));
    element = list.removeFirst();
    assertEquals(element, Move.createMove(MoveType.NORMAL, Square.a1, Square.a1, Piece.WHITE_PAWN,
                                          Piece.NOPIECE, Piece.NOPIECE));
    assertEquals(list.getFirst(),
                 Move.createMove(MoveType.NORMAL, Square.b1, Square.b1, Piece.WHITE_PAWN,
                                 Piece.NOPIECE, Piece.NOPIECE));
    assertEquals(8, list.size());

    // get one entry
    assertEquals(list.get(4),
                 Move.createMove(MoveType.NORMAL, Square.f1, Square.f1, Piece.WHITE_PAWN,
                                 Piece.NOPIECE, Piece.NOPIECE));

    // get entry higher than size
    try {
      list.get(11);
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    // get entry lower zero
    try {
      list.get(-1);
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }

  }

  /**
   * @param list
   */
  private static void testEmptyList(MoveList list) {
    // list is empty
    assertEquals(0, list.size());
    assertTrue(list.empty());

    // remove from empty list
    try {
      list.removeFirst();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
    // remove from empty list
    try {
      list.removeLast();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    // retrieve from empty list
    try {
      list.get(0);
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
    try {
      list.getFirst();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
    try {
      list.getLast();
      fail("");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testSort() {
    MoveGenerator moveGenerator = new MoveGenerator();
    Position board =
      new Position("r3k2r/1ppn3p/2q1q1n1/4P3/2q1Pp2/B5R1/pbp2PPP/1R4K1 b kq e3 0 113");
    MoveList moves = moveGenerator.getPseudoLegalQSearchMoves(board);

    // Comparator for move value victim least value attacker
    Comparator<Integer> reverseMvvlvaComparator = Comparator.comparingInt(
      (Integer a) -> (Move.getTarget(a).getType().getValue()) -
                     Move.getPiece(a).getType().getValue());

    // Comparator for move value victim least value attacker
    Comparator<Integer> mvvlvaComparator = Comparator.comparingInt(
      (Integer a) -> (Move.getPiece(a).getType().getValue() -
                      Move.getTarget(a).getType().getValue()));

    MoveList movesNew = new MoveList(moves);
    movesNew.sort(reverseMvvlvaComparator);

    for (int i = 0; i < moves.size(); i++) {
      System.out.printf("%-20s %-20s %n", Move.toString(moves.get(i)),
                        Move.toString(movesNew.get(i)));
    }

    movesNew.sort(mvvlvaComparator);

    assertEquals(moves, movesNew);

  }

  @Test
  void savePV() {
    MoveList list1 = new MoveList(5);
    MoveList list2 = new MoveList(5);

    // add 10 entries
    for (int i = 100; i < 110; i++) {
      int move = Move.createMove(MoveType.NORMAL, Square.getValueList().get(i - 100),
                                 Square.getValueList().get(i - 100 + 16), Piece.WHITE_PAWN,
                                 Piece.NOPIECE, Piece.NOPIECE);
      list1.add(move);
      list2.add(move);
    }

    int pVMove = Move.createMove(MoveType.NORMAL, Square.getValueList().get(16),
                               Square.getValueList().get(48), Piece.WHITE_PAWN,
                               Piece.NOPIECE, Piece.NOPIECE);

    MoveList.savePV(pVMove, list2, list1);

    assertEquals(pVMove, list1.getFirst());
    assertEquals(list2.getFirst(), list1.get(1));

  }
}
