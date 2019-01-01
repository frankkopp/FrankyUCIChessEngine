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

import java.util.concurrent.TimeUnit;

/**
 * Perft check using only the MoveGenerator and an own minimax
 *
 * @author Frank
 */
public class PERFT {

    private static final boolean DIVIDE=false;

    private long _nodes = 0;
    private long _checkCounter = 0;
    private long _checkMateCounter = 0;
    private long _captureCounter = 0;
    private long _enpassantCounter = 0;
    private String _fen = "";

    /**
     * @param fen
     */
    public PERFT(String fen) {
        _fen = fen;
    }

    /**
     *
     */
    public PERFT() {
        _fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }

    /**
     * @param maxDepth
     */
    public void testPerft(int maxDepth) {

        resetCounters();

        int depth = maxDepth;

        System.out.format("Testing at depth %d%n", depth);

        MoveGenerator[] mg = new MoveGenerator[maxDepth];
        for (int i=0; i<maxDepth; i++) {
            mg[i] = new MoveGenerator();
        }

        Position position = new Position(_fen);

        long result;

        long startTime = System.currentTimeMillis();
        mg[0].setPosition(position);
        result = mg[0]
                .streamLegalMoves()
                .mapToLong((move) -> dividePerft(depth, mg, position, move))
                .sum();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        _nodes = result;
        printResult(result, duration);
    }

    /**
     * @param depth
     * @param mg
     * @param board
     * @param move
     * @return
     */
    private long dividePerft(int depth, MoveGenerator[] mg, Position board, int move) {
        if (DIVIDE) System.out.print(Move.toSimpleString(move) + " ");
        board.makeMove(move);
        long r = miniMax(depth - 1, board, mg, 1);
        if (DIVIDE) System.out.println(r);
        board.undoMove();
        return r;
    }

    private long miniMax(int depthleft, Position board, MoveGenerator[] mg, int ply) {

        // PERFT only looks at leaf nodes
        if (depthleft == 0) {
            updateCounter(board);
            return 1;
        }

        // Iterate over moves
        long totalNodes = 0L;

        // moves to search recursively
        // some convenience fields
        Color _activePlayer = board.getNextPlayer();
        Color _passivePlayer = board.getNextPlayer().getInverseColor();
        mg[ply].setPosition(board);
        MoveList moves = mg[ply].getPseudoLegalMoves();
        for(int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            board.makeMove(move);
            if (!board.isAttacked(_passivePlayer, board.getKingSquares()[_activePlayer.ordinal()])) {
                totalNodes += miniMax(depthleft-1, board, mg, ply+1);
            }
            board.undoMove();
        }

        return totalNodes;
    }

    /**
     * @param board
     */
    private void updateCounter(Position board) {
        if (board.hasCheck()) {
            _checkCounter++;
            if (board.hasCheckMate()) {
                _checkMateCounter++;
            }
        }
        int lastMove = board.getLastMove();
        if (Move.getTarget(lastMove) != Piece.NOPIECE) {
            _captureCounter++;
        }
        if (Move.getMoveType(lastMove) == MoveType.ENPASSANT) {
            _enpassantCounter++;
        }
    }

    /**
     * Reset the counters
     */
    private void resetCounters() {
        _nodes = 0;
        _checkCounter = 0;
        _checkMateCounter = 0;
        _captureCounter = 0;
        _enpassantCounter = 0;
    }

    /**
     * @param result
     * @param duration
     */
    private void printResult(final long result, final long duration) {
        System.out.format("Leaf Nodes: %,d Captures: %,d EnPassant: %,d Checks: %,d Checkmates: %,d %n",
                result, _captureCounter, _enpassantCounter, _checkCounter, _checkMateCounter);
        System.out.format("Duration: %02d:%02d:%02d.%03d%n",
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
                        .toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                        .toMinutes(duration)),
                duration
                - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS
                        .toSeconds(duration)));

        System.out.format("n/s: %,d%n", result*1000/(duration+1));
        System.out.println();
    }

    @SuppressWarnings("javadoc")
    public long get_nodes() {
        return _nodes;
    }

    @SuppressWarnings("javadoc")
    public long get_checkCounter() {
        return _checkCounter;
    }

    @SuppressWarnings("javadoc")
    public long get_checkMateCounter() {
        return _checkMateCounter;
    }

    @SuppressWarnings("javadoc")
    public long get_captureCounter() {
        return _captureCounter;
    }

    @SuppressWarnings("javadoc")
    public long get_enpassantCounter() {
        return _enpassantCounter;
    }

}
