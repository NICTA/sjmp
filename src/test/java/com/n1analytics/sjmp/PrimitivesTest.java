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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// TODO test that they work when result equals one of the values...
public class PrimitivesTest {
//  // TODO maybe allow overflow to be Integer.MIN_VALUE since the fact that the
//  //      magnitude is non-negative will make up for it?
//  public static final PrimitiveNumber[] ORDERED_PRIMITIVE_NUMBERS = new PrimitiveNumber[] {
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, Integer.MIN_VALUE),
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, -2), // -2^65
//    new PrimitiveNumber(new int[] {0xFFFFFFFE, 0xFFFFFFFF}, -2), // -2^64+1
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, -1), // -2^64
//    new PrimitiveNumber(new int[] {0x00000001, 0x00000000}, -1), // -2^64+1
//    new PrimitiveNumber(new int[] {0xFFFFFFFF, 0x00000000}, -1), // -2^64+2^32-1
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000001}, -1), // -2^64+2^32  
//    new PrimitiveNumber(new int[] {0x00000001, 0x00000001}, -1), // -2^64+2^32+1
//    new PrimitiveNumber(new int[] {0xFFFFFFFE, 0xFFFFFFFF}, -1), // -2
//    new PrimitiveNumber(new int[] {0xFFFFFFFF, 0xFFFFFFFF}, -1), // -1
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, 0),  // 0
//    new PrimitiveNumber(new int[] {0x00000001, 0x00000000}, 0),  // 1
//    new PrimitiveNumber(new int[] {0x00000002, 0x00000000}, 0),  // 2
//    new PrimitiveNumber(new int[] {0xFFFFFFFF, 0x00000000}, 0),  // 2^32-1
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000001}, 0),  // 2^32
//    new PrimitiveNumber(new int[] {0x00000001, 0x00000001}, 0),  // 2^32+1
//    new PrimitiveNumber(new int[] {0xFFFFFFFF, 0xFFFFFFFF}, 0),  // 2^64-1
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, 1),  // 2^64
//    new PrimitiveNumber(new int[] {0x00000001, 0x00000000}, 1),  // 2^64+1
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, 2),  // 2^65
//    new PrimitiveNumber(new int[] {0x00000000, 0x00000000}, Integer.MAX_VALUE)
//  };
  
  public void assertArrayEquals(int[] a, int[] b) {
    assertEquals(a.length, b.length);
    for(int i = 0; i < a.length; ++i) {
      if(a[i] != b[i])
        throw new AssertionError(
          String.format("expected <%08X> but was <%08X> at index %d", a[i], b[i], i));
    }
  }
  
  @Test
  public void testFromBigInteger() {
    assertArrayEquals(
        new int[] {0},
        Convert.fromBigInteger(BigInteger.ZERO, new int[1]));
    assertArrayEquals(
        new int[] {0, 0},
        Convert.fromBigInteger(BigInteger.ZERO, new int[2]));
    assertArrayEquals(
        new int[] {1},
        Convert.fromBigInteger(BigInteger.ONE, new int[1]));
    assertArrayEquals(
        new int[] {1, 0},
        Convert.fromBigInteger(BigInteger.ONE, new int[2]));
    assertArrayEquals(
        new int[] {0, 1},
        Convert.fromBigInteger(BigInteger.valueOf(0x100000000L), new int[2]));
    assertArrayEquals(
        new int[] {0, 1, 0, 0},
        Convert.fromBigInteger(BigInteger.valueOf(0x100000000L), new int[4]));
    try {
      Convert.fromBigInteger(BigInteger.valueOf(0), new int[0]);
      fail();
    } catch(AssertionError e) {
    }
    try {
      Convert.fromBigInteger(BigInteger.valueOf(0x100000000L), new int[1]);
      fail();
    } catch(AssertionError e) {
    }
  }
  
  @Test
  public void testToBigInteger() {
    assertEquals(BigInteger.ZERO, Convert.toBigInteger(new int[0]));
    assertEquals(BigInteger.ZERO, Convert.toBigInteger(new int[] {0}));
    assertEquals(BigInteger.ZERO, Convert.toBigInteger(new int[] {0, 0}));
    assertEquals(BigInteger.ONE, Convert.toBigInteger(new int[] {1}));
    assertEquals(BigInteger.ONE, Convert.toBigInteger(new int[] {1, 0}));
    assertEquals(BigInteger.valueOf(0xFFFFFFFFL), Convert.toBigInteger(new int[] {0xFFFFFFFF}));
    assertEquals(BigInteger.valueOf(0xFFFFFFFFL), Convert.toBigInteger(new int[] {0xFFFFFFFF, 0}));
    assertEquals(BigInteger.valueOf(0x100000000L), Convert.toBigInteger(new int[] {0, 1}));
    assertEquals(BigInteger.valueOf(0x100000001L), Convert.toBigInteger(new int[] {1, 1}));
    assertEquals(BigInteger.valueOf(-0x100000000L), Convert.toBigInteger(new int[] {0}, -1L));
    assertEquals(BigInteger.valueOf(-0xFFFFFFFFL), Convert.toBigInteger(new int[] {1}, -1L));
    assertEquals(BigInteger.valueOf(-1L), Convert.toBigInteger(new int[] {0xFFFFFFFF}, -1L));
    assertEquals(BigInteger.valueOf(0x100000000L), Convert.toBigInteger(new int[] {0}, 1L));
    assertEquals(BigInteger.valueOf(0x1000000000L), Convert.toBigInteger(new int[] {0}, 0x10L));
  }
//  
//  @Test
//  public void testCompare() {
//    final PrimitiveNumber[] primitives = ORDERED_PRIMITIVE_NUMBERS;
//    for(int i = 0; i < primitives.length; ++i)
//      for(int j = 0; j < primitives.length; ++j)
//        assertEquals(Long.signum(i-j), primitives[i].compare(primitives[j]));
//  }
//
//  /*
//  @Test
//  public void testLess() {
//    final PrimitiveNumber[] primitives = ORDERED_PRIMITIVE_NUMBERS;
//    for(int i = 0; i < primitives.length; ++i)
//      for(int j = 0; j < primitives.length; ++j)
//        assertEquals(i < j, primitives[i].less(primitives[j]));
//  }
//  
//  @Test
//  public void testLessOrEqual() {
//    final PrimitiveNumber[] primitives = ORDERED_PRIMITIVE_NUMBERS;
//    for(int i = 0; i < primitives.length; ++i)
//      for(int j = 0; j < primitives.length; ++j)
//        assertEquals(i <= j, primitives[i].lessOrEqual(primitives[j]));
//  }
//  
//  @Test
//  public void testEqual() {
//    final PrimitiveNumber[] primitives = ORDERED_PRIMITIVE_NUMBERS;
//    for(int i = 0; i < primitives.length; ++i)
//      for(int j = 0; j < primitives.length; ++j)
//        assertEquals(i == j, primitives[i].equal(primitives[j]));
//  }
//  
//  @Test
//  public void testGreaterOrEqual() {
//    final PrimitiveNumber[] primitives = ORDERED_PRIMITIVE_NUMBERS;
//    for(int i = 0; i < primitives.length; ++i)
//      for(int j = 0; j < primitives.length; ++j)
//        assertEquals(i >= j, primitives[i].greaterOrEqual(primitives[j]));
//  }
//  
//  @Test
//  public void testGreater() {
//    final PrimitiveNumber[] primitives = ORDERED_PRIMITIVE_NUMBERS;
//    for(int i = 0; i < primitives.length; ++i)
//      for(int j = 0; j < primitives.length; ++j)
//        assertEquals(i > j, primitives[i].greater(primitives[j]));
//  }
//  */
//  
//  @Test
//  public void testMath() {
//    int n = 1024;
//    int k = 1024;
//    Random random = new Random();
//    for(int i = 0; i < n; ++i) {
//      BigInteger i0 = new BigInteger(k, random);
//      BigInteger i1 = new BigInteger(k, random);
//      PrimitiveNumber n0 = PrimitiveNumber.valueOf(i0, k/32);
//      PrimitiveNumber n1 = PrimitiveNumber.valueOf(i1, k/32);
//      assertEquals(i0.add(i1), n0.add(n1).bigIntegerValue());
//      assertEquals(i0.subtract(i1), n0.subtract(n1).bigIntegerValue());
//      assertEquals(i0.multiply(i1), n0.multiply(n1).bigIntegerValue());
//      // TODO more operations here...
//    }
//  }
//
//  //@Test
//  public void timeAdd() {
//    final Random random = new Random();
//    final int n = 256;
//    final int k = 1024;
//    final PrimitiveNumber[] primitives = new PrimitiveNumber[n];
//    for(int i = 0; i < n; ++i) {
//      int[] value = new int[k];
//      for(int j = 0; j < k; ++j)
//        value[j] = random.nextInt();
//      primitives[i] = new PrimitiveNumber(value, 0);
//    }
//    
//    long startTime = System.currentTimeMillis();
//    for(int i = 0; i < n; ++i)
//      for(int j = 0; j < n; ++j)
//        primitives[i].add(primitives[j]);
//    System.out.println(System.currentTimeMillis() - startTime);
//
//    startTime = System.currentTimeMillis();
//    final PrimitiveNumber result = new PrimitiveNumber(new int[k], 0);
//    for(int i = 0; i < n; ++i)
//      for(int j = 0; j < n; ++j)
//        primitives[i].add(primitives[j], result);
//    System.out.println(System.currentTimeMillis() - startTime);
//  }
//  
//  @Test
//  public void testMultiply2() {
//    int[] a = new int[32];
//    int[] b = new int[32];
//    int[] c1 = new int[64];
//    int[] c2 = new int[64];
//    for(int i = 0; i < 1000000; ++i) {
//      TestUtil.randomInteger(a);
//      TestUtil.randomInteger(b);
//      Convert.multiply(a, b, c1);
//      Convert.multiply4(a, b, c2);
//      /*
//      System.out.println(Convert.print(a));
//      System.out.println(Convert.print(b));
//      System.out.println(Convert.print(c1));
//      System.out.println(Convert.print(c2));
//      */
//      assertArrayEquals(c1, c2);
//    }
//  }
//  
//  @Test
//  public void timeMultiplyUnrolled() {
//    long start;
//    int n = 100000;
//    int k = 32;
//    int[][] a = new int[n][k];
//    int[][] b = new int[n][k];
//    int[] c = new int[2*k];
//    for(int i = 0; i < n; ++i) {
//      TestUtil.randomInteger(a[i]);
//      TestUtil.randomInteger(b[i]);
//    }
//    
//    
//    start = System.nanoTime();
//    for(int j = 0; j < 100; ++j) {
//      for(int i = 0; i < n; ++i) {
//        Convert.multiply4(a[i], b[i], c);
//      }
//    }
//    System.out.println(System.nanoTime() - start);
//
//    start = System.nanoTime();
//    for(int j = 0; j < 100; ++j) {
//      for(int i = 0; i < n; ++i) {
//        Convert.multiply(a[i], b[i], c);
//      }
//    }
//    System.out.println(System.nanoTime() - start);
//  }
}
