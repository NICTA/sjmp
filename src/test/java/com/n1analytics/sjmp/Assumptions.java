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

import static org.junit.Assert.assertEquals;

// This doesn't test any of the code - just some of the assumptions made when
// writing it.
public class Assumptions {
  public static final long LONGMASK = 0xFFFFFFFFL; 
  
  public static final int MIN32 = Integer.MIN_VALUE;
  public static final int MAX32 = Integer.MAX_VALUE;
  
  //@Test
  public void testBasicArithmetic() {
    assertEquals(
      0xFFFFFFFE00000001L,
      (0xFFFFFFFF & LONGMASK) * (0xFFFFFFFF & LONGMASK));
    for(int i = 0; i < (2 << 16); ++i) {
      testBasicArithmetic(MIN32+i);
      testBasicArithmetic(-i);
      testBasicArithmetic(i);
      testBasicArithmetic(MAX32-i);
    }
  }
  
  public void testBasicArithmetic(final int a) {
    for(int i = 0; i < (2 << 16); ++i) {
      testBasicArithmetic(a, Integer.MIN_VALUE+i);
      testBasicArithmetic(a, -i);
      testBasicArithmetic(a, i);
      testBasicArithmetic(a, Integer.MAX_VALUE-i);
    }
  }
  
  public void testBasicArithmetic(final int a, final int b) {
    final long aLong = a & LONGMASK;
    final long bLong = b & LONGMASK;
    assertEquals((aLong + bLong) & LONGMASK, (a + b) & LONGMASK);
    assertEquals((aLong - bLong) & LONGMASK, (a - b) & LONGMASK);
    assertEquals((aLong * bLong) & LONGMASK, (a * b) & LONGMASK);
  }
}
