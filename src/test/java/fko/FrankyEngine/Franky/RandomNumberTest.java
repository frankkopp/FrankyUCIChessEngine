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

import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Random;

/**
 * RandomNumberTest
 */
class RandomNumberTest {

  private static final Logger LOG = LoggerFactory.getLogger(RandomNumberTest.class);

  private       Random              random64       = new Random(1234567890);
  private final SecureRandom        random64Secure = new SecureRandom();
  private final RandomDataGenerator random64Apache = new RandomDataGenerator();


  long random64() {
    return random64.nextLong();
  }

  long random64Secure() {
    return random64Secure.nextLong();
  }

  long random64Apache() {
    return random64Apache.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
  }

  @Test
  @Disabled
  void testDistribution() {
    int sampleSize = 2000;
    int sampleSeconds = 10;
    long startTime = System.currentTimeMillis();
    long endTime = startTime + (sampleSeconds * 1000);
    int[] distArray64;
    int[] distArray64S;
    int[] distArray64A;
    distArray64 = new int[sampleSize];
    distArray64S = new int[sampleSize];
    distArray64A = new int[sampleSize];
    while (System.currentTimeMillis() < endTime) {
      for (int i = 0; i < 10000; i++) {
        distArray64[(int) (random64() % (sampleSize / 2)) + (sampleSize / 2)]++;
        distArray64S[(int) (random64Secure() % (sampleSize / 2)) + (sampleSize / 2)]++;
        distArray64A[(int) (random64Apache() % (sampleSize / 2)) + (sampleSize / 2)]++;
      }
    }

    System.out.println("Paste this into Excel columns and generate graphs to see distribution!");
    for (int i = 0; i < sampleSize; i++) {
      System.out.printf("%d %d %d%n", distArray64[i], distArray64S[i], distArray64A[i]);
    }
  }


}
