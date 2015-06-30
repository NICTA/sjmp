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

import static com.n1analytics.sjmp.Constants.MASK32;

// TODO return overflows should be ints?
public class Convert {  
  /**
   * Convert a BigInteger to a little-endian, base-16 string.
   * @param value
   * @return
   */
  public static String print(BigInteger value) {
    StringBuilder builder = new StringBuilder();
    String seperator = "";
    while(value.signum() != 0) {
      builder.append(String.format("%s%08X", seperator, value.intValue()));
      seperator = " ";
      value = value.shiftRight(32);
    }
    return builder.toString();
  }
  
  /**
   * Convert an array representing a number to a little-endian, base-16 string.
   * @param value
   * @return
   */
  public static String print(final int[] value) {
    StringBuilder builder = new StringBuilder();
    for(int i = 0; i < value.length; ++i)
      builder.append(String.format("%s%08X", (i == 0 ? "" : " "), value[i]));
    return builder.toString();
  }
  
  // TODO sometimes this doesn't work maybe?
  public static final int[] fromBigInteger(BigInteger value, int[] result) {
    // TODO optimise
    assert value != null;
    assert result != null;
    assert value.signum() >= 0;
    assert result.length > 0;
    for(int i = 0; i < result.length; ++i) {
      result[i] = (int)value.longValue();
      value = value.shiftRight(32);
    }
    assert value.signum() == 0;
    return result;
  }
  
  public static final BigInteger toBigInteger(int[] value) {
    return toBigInteger(value, 0L);
  }
  
  public static final BigInteger toBigInteger(int[] value, long overflow) {
    // TODO optimise
    assert value != null;
    BigInteger result = BigInteger.valueOf(overflow);
    for(int i = value.length-1; i >= 0; --i)
      result = result.shiftLeft(32).add(BigInteger.valueOf(value[i] & MASK32));
    return result;
  }
  
  public static final int[] copy(final int[] src) {
    assert src != null;
    final int[] dest = new int[src.length];
    for(int i = 0; i < src.length; ++i)
      dest[i] = src[i];
    return dest;
  }
  
  public static final int[] copy(final int[] src, final int[] dest) {
    assert src != null;
    assert dest != null;
    assert src.length == dest.length;
    for(int i = 0; i < src.length; ++i)
      dest[i] = src[i];
    return dest;
  }
}
