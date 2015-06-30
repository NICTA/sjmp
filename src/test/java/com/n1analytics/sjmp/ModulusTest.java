/*
Copyright 2015 NICTA. All Rights Reserved.

This file is part of SJMP.

SJMP is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

SJMP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with SJMP.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.n1analytics.sjmp;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ModulusTest {
  public static long LONG_MASK = 0xFFFFFFFFL;
  
  private static Random random = new Random();
  
  private static final int INT_SIZE = 32;
  private static final int KEY_LENGTH = 32;
  
  /**
   * Test that bInvertLimb works for all odd 32-bit integers.
   */
  @Test // TODO
  public void testBInvertLimb() {
    // TIME: approximately 20 seconds
    int x = Integer.MIN_VALUE;
    if(x % 2 == 0)
      ++x;
    while(true) {
      int inverse = Modulus.bInvertLimb(x);
      assertEquals(1, x * inverse);
      assertEquals(1L, (x * inverse) & LONG_MASK);
      assertEquals(1L, ((x & LONG_MASK) * (inverse & LONG_MASK)) & LONG_MASK);
      if(x + 2 < x)
        return;
      x += 2;
    }
  }
  
  @Test
  public void fuzzTestMontgomeryIdentity() {
    for(int i = 0; i < 10000; ++i) {
      int n = random.nextInt(KEY_LENGTH) + 1;
      int[] identity = new int[n];
      int[] m = TestUtil.randomOddNormalisedInteger(n);
      Modulus modulus = new Modulus(m);
      modulus.calculateMontgomeryIdentity(identity);
      assertEquals(
        BigInteger.ONE.shiftLeft(INT_SIZE * n).mod(Convert.toBigInteger(m)),
        Convert.toBigInteger(identity));
    }
  }
  
  @Test // TODO
  public void fuzzTestCalculateMontgomeryPowers() {
    for(int i = 0; i < 10000; ++i) {
      int n = random.nextInt(KEY_LENGTH) + 1;
      int[] m = TestUtil.randomOddNormalisedInteger(n);
      int[] b = TestUtil.randomModularValue(m);
      int[][] powers = new int[16][n];
      Modulus modulus = new Modulus(m);
      modulus.calculateMontgomeryPowers(b, powers);
      BigInteger mBig = Convert.toBigInteger(m);
      BigInteger bBig = Convert.toBigInteger(b);
      for(int j = 0; j < powers.length; ++j)
        assertEquals(
          bBig.pow(j).shiftLeft(INT_SIZE * n).mod(mBig),
          Convert.toBigInteger(powers[j]));
    }
  }
  
  @Test // TODO
  public void fuzzTestMontgomery() {
    // TIME: 1,000,000 in approximately 20 seconds (mostly the BigInteger stuff)
    // Each iteration, generate a random modulus m and two random numbers v1 and
    // v2 (each less than m). Perform a Montgomery transform of v1 and v2 w.r.t
    // m, multiply the product of these values and perform a Montgomery
    // reduction. Confirm that all results are as expected.
    for(int i = 0; i < 1000000; ++i) {
      int n = random.nextInt(KEY_LENGTH)+1;
      int[] m = TestUtil.randomOddNormalisedInteger(n);
      int[] v1 = TestUtil.randomModularValue(m);
      int[] v2 = TestUtil.randomModularValue(m);
      int[] v3 = new int[2*n];
      
      Modulus modulus = new Modulus(m);

      BigInteger mBig = Convert.toBigInteger(m);
      BigInteger v1Big = Convert.toBigInteger(v1);
      BigInteger v2Big = Convert.toBigInteger(v2);
      BigInteger v3Big = v1Big.multiply(v2Big);
      BigInteger r1Big = v1Big.shiftLeft(INT_SIZE * n).mod(mBig);
      BigInteger r2Big = v2Big.shiftLeft(INT_SIZE * n).mod(mBig);
      BigInteger r3Big = v3Big.shiftLeft(INT_SIZE * n).mod(mBig);
      
      modulus.montgomeryTransform(v1);
      assertEquals(r1Big, Convert.toBigInteger(v1));
      modulus.montgomeryTransform(v2);
      assertEquals(r2Big, Convert.toBigInteger(v2));
      Operations.multiply(v1, v2, v3);
      assertEquals(r1Big.multiply(r2Big), Convert.toBigInteger(v3));
      modulus.montgomeryReduce(v3);
      assertEquals(r3Big, Convert.toBigInteger(v3));
    }
  }
  
  @Test // TODO
  public void fuzzTestMontgomeryMultiplyReduce() {
    // TIME: 1,000,000 in approximately 20 seconds (mostly the BigInteger stuff)
    // Each iteration, generate a random modulus m and two random numbers v1 and
    // v2 (each less than m). Perform a Montgomery transform of v1 and v2 w.r.t
    // m, multiply the product of these values and perform a Montgomery
    // reduction. Confirm that all results are as expected.
    for(int i = 0; i < 1000000; ++i) {
      int n = random.nextInt(KEY_LENGTH)+1;
      int[] m = TestUtil.randomOddNormalisedInteger(n);
      int[] v1 = TestUtil.randomModularValue(m);
      int[] v2 = TestUtil.randomModularValue(m);
      int[] v3 = new int[n];
      
      Modulus modulus = new Modulus(m);

      BigInteger mBig = Convert.toBigInteger(m);
      BigInteger v1Big = Convert.toBigInteger(v1);
      BigInteger v2Big = Convert.toBigInteger(v2);
      BigInteger v3Big = v1Big.multiply(v2Big);
      BigInteger r1Big = v1Big.shiftLeft(INT_SIZE * n).mod(mBig);
      BigInteger r2Big = v2Big.shiftLeft(INT_SIZE * n).mod(mBig);
      BigInteger r3Big = v3Big.shiftLeft(INT_SIZE * n).mod(mBig);
      
      modulus.montgomeryTransform(v1);
      assertEquals(r1Big, Convert.toBigInteger(v1));
      modulus.montgomeryTransform(v2);
      assertEquals(r2Big, Convert.toBigInteger(v2));
      for(int j = 0; j < n; ++j)
        v3[j] = v1[j];
      modulus.multiplyReduce(v3, v2);
      //System.out.format("%s\n%s\n%s\n\n", mBig.toString(16), r3Big.toString(16), Convert.toBigInteger(v3).toString(16));
      assertEquals(r3Big, Convert.toBigInteger(v3));
    }
  }
  
  @Test // TODO
  public void fuzzTestSquare() {
    for(int i = 0; i < 100000; ++i) {
      int n = 32;
      int[] a = TestUtil.randomInteger(n);
      int[] b = new int[2*n];
      BigInteger aBig = Convert.toBigInteger(a);
      BigInteger bBig = aBig.multiply(aBig);
      Modulus.square(a, b);
      //System.out.println(Convert.print(a));
      //System.out.println(Convert.print(a));
      //System.out.println(Convert.print(bBig));
      //System.out.println(Convert.print(b));
      assertEquals(bBig, Convert.toBigInteger(b));
    }
  }
  
  //@Test
  public void timeSquareVsMultiply() {
    int d0 = 10000;
    int d1 = 512;
    int[][] a = new int[d0][d1];
    int[] b = new int[2*d1];
    for(int i0 = 0; i0 < d0; ++i0)
      TestUtil.randomInteger(a[i0]);
    
    for(int i = 0; i < 100; ++i) {
      long start = System.nanoTime();
      for(int i0 = 0; i0 < d0; ++i0)
        Modulus.square(a[i0], b);
      System.out.println(System.nanoTime() - start);
      
      start = System.nanoTime();
      for(int i0 = 0; i0 < d0; ++i0)
        Modulus.square2(a[i0], b);
      System.out.println(System.nanoTime() - start);
      
      start = System.nanoTime();
      for(int i0 = 0; i0 < d0; ++i0)
        Modulus.square3(a[i0], b);
      System.out.println(System.nanoTime() - start);
      
      start = System.nanoTime();
      for(int i0 = 0; i0 < d0; ++i0)
        Modulus.square4(a[i0], b);
      System.out.println(System.nanoTime() - start);
  
      start = System.nanoTime();
      for(int i0 = 0; i0 < d0; ++i0)
        Operations.multiply(a[i0], a[i0], b);
      System.out.println(System.nanoTime() - start);
      System.out.println();
    }
  }
  
  @Test
  public void fuzzTestPowMod() {
    long total0 = 0L;
    long total1 = 0L;
    long start;
    for(int i = 0; i < 10000; ++i) {
      //int n = random.nextInt(KEY_LENGTH) + 1;
      int n = 32;
      int[] m = TestUtil.randomOddNormalisedInteger(n);
      int[] b = TestUtil.randomModularValue(m);
      int[][] p = new int[16][n];
      int[] e = TestUtil.randomInteger(n);
      int[] r = new int[n];
      Modulus modulus = new Modulus(m);
      modulus.calculateMontgomeryPowers(b, p);
      start = System.nanoTime();
      modulus.powMod(b, e, r);
      total0 += System.nanoTime() - start;
      BigInteger mBig = Convert.toBigInteger(m);
      BigInteger bBig = Convert.toBigInteger(b);
      BigInteger eBig = Convert.toBigInteger(e);
      start = System.nanoTime();
      BigInteger rBig = bBig.modPow(eBig, mBig);
      total1 += System.nanoTime() - start;
      /*
      System.out.format(
        "m:\n  %s\nb:\n  %s\ne:\n  %s\nr:\n  %s\n  %s\n\n",
        mBig.toString(16),
        bBig.toString(16),
        eBig.toString(16),
        rBig.toString(16),
        Convert.toBigInteger(r).toString(16));
      //*/
      assertEquals(rBig, Convert.toBigInteger(r));
    }
  }
}
