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

import fko.javaUCIEngineFramework.UCI.UCIProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 */
public class testTimings_EvalVsCache {

    final AtomicLong a_long = new AtomicLong();
    final LongAdder long_a = new LongAdder();
    long a = 0;

    private String _fen;
    private BoardPosition _omegaPosition;
    private MoveGenerator _omg;
    private Evaluation _evaluation;
    private EvaluationCache _cache;

    /**
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        _fen = UCIProtocolHandler.START_FEN;
        _omegaPosition = new BoardPosition(_fen);
        _omg = new MoveGenerator();
        _evaluation = new Evaluation();
        _cache = new EvaluationCache(32);
    }


    @Test
    public void testTiming() {

        prepare();

        int ROUNDS = 5;
        int DURATION = 5;

        int ITERATIONS = 0;

        Instant start;

        System.out.println("Running Timing Test Test 1 vs. Test 2");

        for (int j=0; j<ROUNDS ;j++) {

            System.gc();

            start = Instant.now();
            ITERATIONS=0;
            for(;;) {
                ITERATIONS++;
                test1();
                if (Duration.between(start,Instant.now()).getSeconds() >= DURATION) break;
            }
            System.out.println(String.format("Test 1 (eval): %,7d runs/s", ITERATIONS/DURATION));

            start = Instant.now();
            ITERATIONS=0;
            for(;;) {
                ITERATIONS++;
                test2();
                if (Duration.between(start,Instant.now()).getSeconds() >= DURATION) break;
            }
            System.out.println(String.format("Test 2 (cache): %,7d runs/s", ITERATIONS/DURATION));

        }

    }

    /**
     *
     */
    private void prepare() {
        _cache.put(_omegaPosition.zobristKey, _evaluation.evaluate(_omegaPosition));
    }

    private void test1() {
        _evaluation.evaluate(_omegaPosition);
    }

    private void test2() {
        final int v = _cache.get(_omegaPosition.zobristKey);
        _cache.put(_omegaPosition.zobristKey, v);
    }



}
