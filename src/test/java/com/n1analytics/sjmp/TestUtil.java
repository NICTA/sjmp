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

import static com.n1analytics.sjmp.Constants.MASK32;

public class TestUtil {
  public static Random random = new Random();

  public static BigInteger toBigInteger(final int[] value) {
    BigInteger b = BigInteger.ZERO;
    for(int i = value.length - 1; i >= 0; --i)
      b = b.shiftLeft(32).add(BigInteger.valueOf(value[i] & MASK32));
    return b;
  }

  public static BigInteger toBigInteger(final int[] value, final int overflow) {
    BigInteger b = BigInteger.valueOf(overflow);
    for(int i = value.length - 1; i >= 0; --i)
      b = b.shiftLeft(32).add(BigInteger.valueOf(value[i] & MASK32));
    return b;
  }

  public static BigInteger toBigInteger(final int[] value, final long overflow) {
    BigInteger b = BigInteger.valueOf(overflow);
    for(int i = value.length - 1; i >= 0; --i)
      b = b.shiftLeft(32).add(BigInteger.valueOf(value[i] & MASK32));
    return b;
  }
  
  public static int[] copy(final int[] source, final int[] destination) {
    for(int i = 0; i < source.length; ++i)
      destination[i] = source[i];
    return destination;
  }
  
  public static int[] randomInteger(final int[] x) {
    for(int i = 0; i < x.length; ++i)
      x[i] = random.nextInt();
    return x;
  }

  public static int[] randomInteger(final int length) {
    return randomInteger(new int[length]);
  }
  
  public static int[] randomOddInteger(final int[] x) {
    randomInteger(x);
    x[0] |= 1;
    return x;
  }
  
  public static int[] randomOddInteger(final int length) {
    return randomOddInteger(new int[length]);
  }
  
  public static int[] randomNormalisedInteger(final int[] x) {
    randomInteger(x);
    x[x.length - 1] |= 0x80000000;
    return x;
  }
  
  public static int[] randomNormalisedInteger(final int length) {
    return randomNormalisedInteger(new int[length]);
  }
  
  public static int[] randomOddNormalisedInteger(final int[] x) {
    randomInteger(x);
    x[0] |= 1;
    x[x.length - 1] |= 0x80000000;
    return x;
  }
  
  public static int[] randomOddNormalisedInteger(final int length) {
    return randomOddNormalisedInteger(new int[length]);
  }
  
  public static Modulus randomModulus(final int length) {
    return new Modulus(randomOddNormalisedInteger(length));
  }
  
  public static int[] randomModularValue(final int[] modulus) {
    int[] x = new int[modulus.length];
    while(true) {
      randomInteger(x);
      boolean valid = false;
      for(int i = modulus.length - 1; i >= 0; --i) {
        if(x[i] != modulus[i]) {
          valid = x[i] < modulus[i];
          break;
        }
      }
      if(valid)
        return x;
    }
  }
}
