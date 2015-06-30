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

// NOTE work in progress
public class MutableNumber {
  private static final long MASK32 = 0xFFFFFFFFL;
  
  private static final char[] DIGITS = new char[] {
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };
  
  protected final int[] value;

  protected MutableNumber(final int[] value) {
    assert value != null;
    assert value.length > 0;
    this.value = value;
  }
  
  /**
   * Construct a MutableNumber from a copy of a little-endian int array.
   * @param value
   * @return
   * @throws IllegalArgumentException If {@code value.length == 0}
   * @throws NullPointerException If {@code value == null}
   */
  public static MutableNumber valueOf(final int[] value) {
    if(value == null)
      throw new NullPointerException("value must not be null");
    if(value.length == 0)
      throw new IllegalArgumentException("value must have non-zero length");
    return new MutableNumber(value.clone());
  }
  
  /**
   * Construct a MutableNumber from a BigInteger using the minimal number of
   * limbs.
   * @param value A non-negative BigInteger.
   * @return
   * @throws IllegalArgumentException If {@code value.signum() < 0}
   * @throws NullPointerException If {@code value == null}
   */
  public static MutableNumber valueOf(BigInteger value) throws
    IllegalArgumentException,
    NullPointerException
  {
    if(value == null)
      throw new NullPointerException("value must not be null");
    
    if(value.signum() == 0)
      return valueOf(value, 1);
    
    int length = 1 + (value.bitLength() - 1) / 32;
    return valueOf(value, length);
  }
  
  /**
   * Construct a MutableNumber from a BigInteger using the specified number of
   * limbs.
   * @param value A non-negative BigInteger.
   * @param size The number of limbs to encode.
   * @return
   * @throws IllegalArgumentException If {@code value.signum() < 0}
   * @throws IllegalArgumentException If {@code size <= 0}
   * @throws IllegalArgumentException If {@code value} cannot be represented
   * exactly with {@code size} limbs.
   * @throws NullPointerException If {@code value == null}
   */
  public static MutableNumber valueOf(BigInteger value, int size) throws
    IllegalArgumentException,
    NullPointerException
  {
    if(value == null)
      throw new NullPointerException("value must not be null");
    if(value.signum() < 0)
      throw new IllegalArgumentException("value must be non-negative");
    if(size <= 0)
      throw new IllegalArgumentException("size must be strictly positive");
    
    // Value as a big-endian byte array
    byte[] bv = value.toByteArray();
    
    if(bv.length > 4 * size)
      throw new IllegalArgumentException("value is too large for the specified number of limbs");
    
    // Convert the byte array to a little-endian int array
    int[] iv = new int[size];
    for(int i = bv.length, j = 0; i > 0; i -= 4, ++j) {
      int limb = 0;
      switch(i) {
      default:
        limb |= ((bv[i-4] & 0xFF) << 24);
        // Drop through
      case 3:
        limb |= ((bv[i-3] & 0xFF) << 16);
        // Drop through
      case 2:
        limb |= ((bv[i-2] & 0xFF) << 8);
        // Drop through
      case 1:
        limb |= (bv[i-1] & 0xFF);
      }
      iv[j] = limb;
    }
    
    return new MutableNumber(iv);
  }
  
  /**
   * @return The number of limbs.
   */
  public final int size() { return value.length; }
  
  /**
   * Access a single limb.
   * @param i The index of the limb.
   * @return The limb at index {@code i}.
   */
  public final int limb(int i) { return value[i]; }
  
  protected final void assertSameSize(MutableNumber other) {
    assertSameSize(other.value);
  }
  
  protected final void assertSameSize(int[] other) {
    assert value.length == other.length;
  }
  
  protected final void assertDoubleSize(MutableNumber other) {
    assertDoubleSize(other.value);
  }
  
  protected final void assertDoubleSize(int[] other) {
    assert 2 * value.length == other.length;
  }
  
  protected final void assertHalfSize(MutableNumber other) {
    assertHalfSize(other.value);
  }
  
  protected final void assertHalfSize(int[] other) {
    assert value.length == 2 * other.length;
  }
  
  protected final void checkSameSize(MutableNumber other) {
    checkSameSize(other.value);
  }
  
  protected final void checkSameSize(int[] other) {
    if(value.length != other.length)
      throw new IllegalArgumentException();
  }
  
  protected final void checkDoubleSize(MutableNumber other) {
    checkDoubleSize(other.value);
  }
  
  protected final void checkDoubleSize(int[] other) {
    if(2 * value.length != other.length)
      throw new IllegalArgumentException();
  }
  
  protected final void checkHalfSize(MutableNumber other) {
    checkHalfSize(other.value);
  }
  
  protected final void checkHalfSize(int[] other) {
    if(value.length != 2 * other.length)
      throw new IllegalArgumentException();
  }
  
  /**
   * Clone this number. Any changes to either {@code this} or the return value
   * will not affect the other.
   */
  public final MutableNumber clone() {
    return new MutableNumber(value.clone());
  }
  
  /**
   * Representation of this MutableNumber as a space-separated, little-endian
   * list of 32-bit hexadecimal numbers.
   */
  public final String toString() {
    char[] s = new char[8 * size() + size() - 1];
    for(int vi = 0, si = 0; vi < size(); ++vi) {
      if(vi > 0)
        s[si++] = ' ';
      final int v = value[vi];
      s[si++] = DIGITS[(v >>> 28) & 0xF];
      s[si++] = DIGITS[(v >>> 24) & 0xF];
      s[si++] = DIGITS[(v >>> 20) & 0xF];
      s[si++] = DIGITS[(v >>> 16) & 0xF];
      s[si++] = DIGITS[(v >>> 12) & 0xF];
      s[si++] = DIGITS[(v >>> 8) & 0xF];
      s[si++] = DIGITS[(v >>> 4) & 0xF];
      s[si++] = DIGITS[v & 0xF];
    }
    return new String(s);
  }
  
  /**
   * @return Representation of this MutableNumber as a BigInteger.
   */
  public final BigInteger toBigInteger() {
    boolean isZero = true;
    byte[] v = new byte[4 * size()];
    for(int i = 0, j = 4 * size(); i < size(); ++i) {
      if(value[i] != 0)
        isZero = false;
      v[--j] = (byte)value[i];
      v[--j] = (byte)(value[i] >>> 8);
      v[--j] = (byte)(value[i] >>> 16);
      v[--j] = (byte)(value[i] >>> 24);
    }
    return new BigInteger(isZero ? 0 : 1, v);
  }
  
  // TODO how to do compareto?
  
  /**
   * Compare {@code this} with {@code other} in constant time.
   * @param other A {@code MutableNumber} with the same number of limbs as
   * {@code this}.
   * @return {@code -1} if {@code this < other}, {@code 0} if {@code this ==
   * other}, or {@code 1} if {@code this > other}.
   */
  public final int compare(MutableNumber other) {
    return compare(other.value);
  }
  
  protected final int compare(int[] other) {
    assertSameSize(other);
    long overflow0 = 0L;
    long overflow1 = 0L;
    long size = size();
    for(int i = 0; i < size; ++i) {
      long a = value[i] & MASK32;
      long b = other[i] & MASK32;
      overflow0 = (overflow0 + a - b) >> 32;
      overflow1 = (overflow1 + a - b) >> 32;
    }
    assert
      (overflow0 == -1L && overflow1 ==  0L) ||
      (overflow0 ==  0L && overflow1 ==  0L) ||
      (overflow0 ==  0L && overflow1 == -1L);
    return (int)(overflow0 - overflow1);
  }
  
  public final int add(MutableNumber other) {
    return add(other.value);
  }
  
  /**
   * Add {@code other} to {@code this} and return the carry.
   * @param other A {@code MutableNumber} with the same number of limbs as
   * {@code this}.
   * @return An integer carry of either {@code 0} or {@code 1}.
   */
  protected final int add(int[] other) {
    assertSameSize(other);
    long overflow = 0L;
    final int size = size();
    for(int i = 0; i < size; ++i) {
      overflow += (value[i] & MASK32) + (other[i] & MASK32);
      value[i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }
  
  public final int subtract(MutableNumber other) {
    return subtract(other.value);
  }
  
  /**
   * Subtract {@code other} from {@code this} and return the borrow.
   * @param other A {@code MutableNumber} with the same number of limbs as
   * {@code this}.
   * @return An integer borrow of either {@code 0} or {@code -1}.
   */
  protected final int subtract(int[] other) {
    assertSameSize(other);
    long overflow = 0L;
    final int size = size();
    for(int i = 0; i < size; ++i) {
      overflow += (value[i] & MASK32) - (other[i] & MASK32);
      value[i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }
  
  public final void multiply(MutableNumber other, MutableNumber result) {
    multiply(other.value, result.value);
  }
  
  protected final void multiply(int[] other, int[] result) {
    assertSameSize(other);
    assertDoubleSize(result);
    int size = size();
    long overflow = 0L;
    long limb = value[0] & MASK32;
    for(int i = 0; i < size; ++i) {
      overflow += limb * (other[i] & MASK32);
      result[i] = (int)overflow;
      overflow >>>= 32;
    }
    result[size] = (int)overflow;
    for(int i = 1; i < size; ++i) {
      overflow = 0L;
      limb = value[i] & MASK32;
      for(int j = 0; j < size; ++j) {
        overflow += limb * (other[j] & MASK32) + (result[i+j] & MASK32);
        result[i+j] = (int)overflow;
        overflow >>>= 32;
      }
      result[i+size] = (int)overflow;
    }
  }
  
  protected final int multiplyAddLimb(int other, int[] result) {
    int size = size();
    long overflow = 0L;
    long limb = other & MASK32;
    for(int i = 0; i < size; ++i) {
      overflow += limb * (value[i] & MASK32) + (result[i] & MASK32);
      result[i] = (int)overflow;
      overflow >>>= 32;
    }
    return (int)overflow;
  }
}
