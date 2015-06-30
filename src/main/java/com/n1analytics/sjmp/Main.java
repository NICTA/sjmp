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

import java.math.BigInteger;
import java.util.Random;

public class Main {
//  public static final long LONG_MASK = 0xFFFFFFFFL;
//	public static final Random random = new Random();
//	
//	public static void timeAdd() {
//    final Random random = new Random();
//    final int n = 1024*8;
//    final int k = 256;
//    final PrimitiveNumber[] primitives = new PrimitiveNumber[n];
//    for(int i = 0; i < n; ++i) {
//      int[] value = new int[k];
//      for(int j = 0; j < k; ++j)
//        value[j] = random.nextInt();
//      primitives[i] = new PrimitiveNumber(value, 0L);
//    }
//    
//    long startTime = System.currentTimeMillis();
//    for(int i = 0; i < n; ++i)
//      for(int j = 0; j < n; ++j)
//        primitives[i].add(primitives[j]);
//    System.out.println(System.currentTimeMillis() - startTime);
//
//    startTime = System.currentTimeMillis();
//    final PrimitiveNumber result = new PrimitiveNumber(new int[k], 0L);
//    for(int i = 0; i < n; ++i)
//      for(int j = 0; j < n; ++j)
//        primitives[i].add(primitives[j], result);
//    System.out.println(System.currentTimeMillis() - startTime);
//  }
//	
//	public static void testAddition() {
//		long m = Math.abs(random.nextLong());
//		long l1 = Math.abs(random.nextLong()) % m;
//		long l2 = Math.abs(random.nextLong()) % m;
//		int[] modulus = new int[] { (int)m, (int)(m >>> 32) };
//		int[] mag1 = new int[] { (int)l1, (int)(l1 >>> 32) };
//		int[] mag2 = new int[] { (int)l2, (int)(l2 >>> 32) };
//		ModularInteger v1 = new ModularInteger(mag1, modulus);
//		ModularInteger v2 = new ModularInteger(mag2, modulus);
//		assert v1.add(v2).value[0] == (int)((l1+l2)%m);
//		assert v1.add(v2).value[1] == (int)(((l1+l2)%m) >>> 32);
//		assert v1.subtract(v2).value[0] == (int)((l1-l2)%m);
//		assert v1.subtract(v2).value[1] == (int)(((l1-l2)%m) >>> 32);
//		assert v1.negate().value[0] == (int)(m-l1);
//		assert v1.negate().value[1] == (int)((m-l1) >>> 32);
//		//*
//		System.out.format(
//			"      m = %016X\n" +
//			"     l1 = %016X\n" +
//			"     l2 = %016X\n" +
//			"l1 + l2 = %016X   %08X %08X   %s\n" +
//			"l1 - l2 = %016X   %08X %08X   %s\n" +
//			"    -l1 = %016X   %08X %08X   %s\n\n",
//			m, l1, l2,
//			(l1+l2)%m, (int)(((l1+l2)%m) >>> 32), (int)((l1+l2)%m), v1.add(v2).toString(),
//			(l1-l2)%m, (int)(((l1-l2)%m) >>> 32), (int)((l1-l2)%m), v1.subtract(v2).toString(),
//			m-l1, (int)((m-l1) >>> 32), (int)(m-l1), v1.negate().toString()
//		);
//		//*/
//	}
//	
//	// TODO
//	/*
//	public static void testAdditionSpeed(int moduliCount, int numberCount, int intCount) {
//		int[][] moduli = new int[moduliCount][intCount];
//		int[][][] mags = new int[moduliCount][numberCount][intCount];
//		for(int i = 0; i < moduliCount; ++i) {
//			for(int j = 0; j < intCount; ++j)
//				moduli[i][j] = random.nextInt();
//			for(int j = 0; j < numberCount; ++j) {
//				for(int k = 0; k < intCount; ++k)
//					mags[i][j][k] = random.nextInt();
//				mags
//			}			
//		}
//	}
//	*/
//	
//	public static String toString(int[] x) {
//    StringBuilder builder = new StringBuilder();
//    for(int i = x.length - 1; i >= 0; --i) {
//      builder.append(String.format("%08X", x[i] & ModularInteger.LONG_MASK));
//      if(i > 0)
//        builder.append(" ");
//    }
//    return builder.toString();
//  }
//
  
  /**
   * Unsigned division of a long by an int.
   * @param n1
   * @param n0
   * @param d
   * @param dinv
   * @return
   */
  public static int divideLongByInt(final int n1, final int n0, final int d, final int dinv) {
    final long MASK = 0xFFFFFFFFL;
    final long n0Long = n0 & MASK;
    final long n1Long = n1 & MASK;
    final long dLong = d & MASK;
    final long dinvLong = dinv & MASK;
    long n = (n1Long << 32) | n0Long;
    long q = ((n1Long * dinvLong) >>> 32) + n1Long;
    long p = q * dLong;
    long r = n - p;
    while(r > dLong) { // Repeated at most three times
      ++q;
      r -= dLong;
    }
    return (int)q;
  }
  
  
  
  public static void testDivide() {
    // 1 billion divisions in about 10 seconds
    Random random = new Random();
    int n = 1000000;
    int k = 1000;
    int[] n1Array = new int[k];
    int[] n0Array = new int[k];
    for(int i = 0; i < k; ++i) {
      n1Array[i] = random.nextInt();
      n0Array[i] = random.nextInt();
    }
    long start = System.currentTimeMillis();
    long optimizerPreventer = 0;
    for(int i = 0; i < n; ++i) {
      if((i % 10000) == 0)
        System.out.println(i);
      // Note that ~d = 2^32 - 1 - d
      int d = random.nextInt() | 0x80000000;
      int dinv = Operations.invert(d);
      //BigInteger xbig = BigInteger.valueOf(d & 0xFFFFFFFFL);
      for(int j = 0; j < k; ++j) {
        //int n1 = random.nextInt();
        //int n0 = random.nextInt();
        int n1 = n1Array[j];
        int n0 = n0Array[j];
        long qr = Operations.divide(n1, n0, d, dinv);
        long q = qr >>> 32;
        long r = qr & 0xFFFFFFFFL;
        optimizerPreventer ^= q;
        /*
        BigInteger nbig = BigInteger
          .valueOf(n1 & 0xFFFFFFFFL)
          .shiftLeft(32)
          .add(BigInteger.valueOf(n0 & 0xFFFFFFFFL));
        BigInteger qbig = nbig.divide(xbig);
        if(qbig.compareTo(BigInteger.ONE.shiftLeft(32)) >= 0)
          continue;
        if(!qbig.equals(BigInteger.valueOf(q))) {
          System.out.format("n = %s\nd = %s\nq = %x\nq = %s\n\n", nbig.toString(16), xbig.toString(16), q, qbig.toString(16));
          throw new RuntimeException();
        }
        //*/
      }
    }
    System.out.format(
      "time = %d ms\noptimizerPreventer = %d\n",
      System.currentTimeMillis() - start,
      optimizerPreventer);
  }
  
  public static void testMod() {
    // Takes about 30ms to modularise 10,000 1024-bit numbers
    //             1s to modularise 300,000 1024-bit numbers
    int n = 1000;
    int d1 = 8;
    int d2 = 10000;
    int d3 = 32;
    int[][] tmpDividend = new int[d2][2*d3];
    int[][][] dividend = new int[d1][d2][2*d3];
    int[][][] divisor = new int[d1][d2][d3];
    int[][][] modulus = new int[d1][d2][2*d3];
    long[][] times = new long[n][d1];
    Random random = new Random();
    System.err.println("Generating test vectors");
    for(int i = 0; i < d1; ++i) {
      for(int j = 0; j < d2; ++j) {
        for(int k = 0; k < d3; ++k) {
          dividend[i][j][2*k] = (4*i < d3 - 2*k ? random.nextInt() : 0);
          dividend[i][j][2*k+1] = (4*i < d3 - 2*k - 1 ? random.nextInt() : 0);
          divisor[i][j][k] = random.nextInt();
        }
        divisor[i][j][d3-1] |= 0x80000000;
        Convert.fromBigInteger(
          Convert.toBigInteger(dividend[i][j]).mod(Convert.toBigInteger(divisor[i][j])),
          modulus[i][j]);
      }
    }
    System.gc();
    //try { System.err.println("Waiting"); Thread.sleep(10000); } catch(Exception e) {}
    System.err.println("Testing");
    int[] scratch = new int[d3+1];
    for(int i = 0; i < n; ++i) {
      for(int j = 0; j < d1; ++j) {
        
        // Copy dividends
        for(int k = 0; k < d2; ++k)
          for(int l = 0; l < 2*d3; ++l)
            tmpDividend[k][l] = dividend[j][k][l];

        // Run the mods
        final long start = System.nanoTime();
        for(int k = 0; k < d2; ++k)
          Operations.mod(tmpDividend[k], divisor[j][k], scratch);
        final long end = System.nanoTime();
        
        // Print the run time
        times[i][j] = end - start;
        System.out.format("%d %d %d\n", i, j, end - start);
        
        // Check the results are correct
        //*
        for(int k = 0; k < d2; ++k) {
          for(int l = 0; l < 2*d3; ++l) {
            if(tmpDividend[k][l] != modulus[j][k][l]) {
              System.out.format(
                "%d %d %d %s\n%s\n\n",
                i, j, k,
                Convert.print(modulus[j][k]),
                Convert.print(tmpDividend[k]));
              throw new RuntimeException("modulus[j][k] != tmpDividend[k]");
            }
          }
        }
        //*/
      }
    }
    for(int j = 0; j < d1; ++j) {
      for(int i = 0; i < n; ++i) {
        System.out.format("%s%d", i == 0 ? "" : " ", times[i][j]);
      }
      System.out.println();
    }
  }
  
  public static void timePowMod() {
    // TODO plot time vs magnitude...
    int d0 = 10000;
    int d1 = 32;
    int[][] m = new int[d0][d1];
    int[][] b = new int[d0][d1];
    int[][] e = new int[d0][d1];
    int[] r = new int[d1];
    Modulus[] modulus = new Modulus[d0];
    BigInteger[] mBig = new BigInteger[d0];
    BigInteger[] bBig = new BigInteger[d0];
    BigInteger[] eBig = new BigInteger[d0];
    Random random = new Random();
    for(int i0 = 0; i0 < d0; ++i0) {
      for(int i1 = 0; i1 < d1; ++i1) {
        m[i0][i1] = random.nextInt();
        e[i0][i1] = random.nextInt();
      }
      boolean invalid = true;
      do {
        for(int i1 = 0; i1 < d1; ++i1)
          b[i0][i1] = random.nextInt();
        for(int i1 = d1 - 1; i1 >= 0; --i1) {
          b[i0][i1] = random.nextInt();
          if(invalid) {
            if(b[i0][i1] > m[i0][i1])
              break;
            if(b[i0][i1] < m[i0][i1])
              invalid = false;
          }
        }
      } while(invalid);
      modulus[i0] = new Modulus(m[i0]);
      //mBig[i0] = Convert.toBigInteger(m[i0]);
      //bBig[i0] = Convert.toBigInteger(b[i0]);
      //eBig[i0] = Convert.toBigInteger(e[i0]);
    }
    
    //System.gc();
    long total1 = 0L;
    long total2 = 0L;
    for(int i0 = 0; i0 < d0; ++i0) {
      long start = System.nanoTime();
      modulus[i0].powMod(b[i0], e[i0], r);
      long time1 = System.nanoTime() - start;
      total1 += time1;
      
      //start = System.nanoTime();
      //bBig[i0].modPow(eBig[i0], mBig[i0]);
      //long time2 = System.nanoTime() - start;
      //total2 += time2;
      //System.err.format("%d %d\n", time1, time2);
    }
    //System.out.format("%d %d\n", total1, total2);
  }
  
	public static void main(String[] args) {
    final long LONG_MASK = 0xFFFFFFFFL;
    System.out.println("Sleep");
    try { Thread.sleep(20000); } catch(Exception e) {};
    timePowMod();
    //fuzzTestPowMod();
    //testMod();
    //testDivide();
    /*
	  Random random = new Random();
	  for(int i = 0; i < 100000000; ++i) {
	    if(i % 1000 == 0)
	      System.out.println(i);
	    // Note that ~d = 2^32 - 1 - d
	    int x = random.nextInt() | 0x80000000;
	    //int nh = ~x;
	    //int nl = 0xFFFFFFFF;
	    long n = (((~x) & LONG_MASK) << 32) | 0xFFFFFFFFL;
	    long xinv = n / (x & LONG_MASK);
	    //System.out.format("x = %016X n = %016X xinv = %016X (long)x * xinv = %016X x * (int)xinv = %016X\n", x & LONG_MASK, n, xinv, (x & LONG_MASK) * xinv, x * (int)xinv);
	    BigInteger xbig = BigInteger.valueOf(x & LONG_MASK);
	    //BigInteger nbig = BigInteger.valueOf(n);
	    //BigInteger xinvbig = nbig.divide(xbig);
	    //System.out.println(xbig.toString(16));
	    //System.out.println(nbig.toString(16));
	    //System.out.println(xinvbig.toString(16));
	    //System.out.println(xbig.multiply(xinvbig).toString(16));
	    for(int j = 0; j < 1000; ++j) {
	      int n1 = random.nextInt();
	      int n0 = random.nextInt();
	      long qr = divide(n1, n0, x, (int)xinv);
	      long q = qr >>> 32;
	      long r = qr & 0xFFFFFFFFL;
	      BigInteger nbig = BigInteger.valueOf(n1 & LONG_MASK).shiftLeft(32).add(BigInteger.valueOf(n0 & LONG_MASK));
	      BigInteger qbig = nbig.divide(xbig);
	      if(qbig.compareTo(BigInteger.ONE.shiftLeft(32)) >= 0)
	        continue;
	      //assert qbig.equals(BigInteger.valueOf(q));
	      if(!qbig.equals(BigInteger.valueOf(q))) {
	        System.out.format("n = %s\nd = %s\nq = %x\nq = %s\n\n", nbig.toString(16), xbig.toString(16), q, qbig.toString(16));
	        throw new RuntimeException();
	      }
	    }
	    //System.out.format("%08X\n", q);
      //System.out.format("~x = %016X %016X\n", ~x, ((~x) & LONG_MASK) << 32);
      //System.out.format("n =  %016X\n", n);
      //System.out.println(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE.add(BigInteger.valueOf(x & LONG_MASK)).shiftLeft(32)).toString(16));
	  }
	  */
	  
//	  timeAdd();
//    int[] modulus = new int[] { 0, 0, 1 };
//    
//    int[] a1 = new int[] { 3, 2, 0 };
//    int[] a2 = new int[] { 3, 3, 0 };
//    int[] a3 = new int[3];
//    int[] a4 = new int[3];
//    System.out.println(ModularInteger.trustedSubtractWithBorrow(a1, a2, a3));
//    System.out.println(ModularInteger.trustedAddWithCarry(a3, modulus, a4));
//    System.out.println(toString(a3));
//    System.out.println(toString(a4));
//    
//    int i = -1;
//    System.out.format("%016X\n", i & ModularInteger.LONG_MASK);
//    System.out.format("%08X\n", (int)(-1L));
//	  /*
//		long t = System.currentTimeMillis();
//		for(int i = 0; i < 100; ++i)
//			testAddition();
//		System.out.println(System.currentTimeMillis() - t);
//    int[] modulus = new int[] { 0, 1 };
//		ModularInteger a1 = new ModularInteger(new int[] { 1, 0 }, modulus);
//		ModularInteger a2 = new ModularInteger(new int[] { 2, 0 }, modulus);
//		System.out.println(a1.toString());
//		System.out.println(a2.toString());
//		System.out.println(a1.add(a2).toString());
//		System.out.println(a1.subtract(a2).toString());
//		*/
	}

}
