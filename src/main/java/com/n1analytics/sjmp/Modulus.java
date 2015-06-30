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
import static com.n1analytics.sjmp.Operations.*;

// NOTE this is not a general-purpose modulus class. It only works for moduli
//      which are odd (least significant bit == 1) and normalised (most
//      significant bit of most significant limb == 1). This is fine for the
//      applications we have in mind.
// TODO work out which implementation of square to use. Also attempt to fuse
//      square with montgomeryReduce like we did with multiplyReduce.
// TODO also implement divide (see commit
//      14f8a930806fade10f0a723b1ddfdc11e0914782 for an untested implementation)
public class Modulus {
  
  /**
   * The modulus in little-endian format. Each element of the array is treated
   * as an unsigned 32-bit integer. The modulus is assumed to be odd (ie. {@code
   * modulus[0] & 1 != 0}) and normalised (i.e.
   * {@code modulus[modulus.length-1] & 0x80000000 != 0}).
   */
  private final int[] modulus;

  /**
   * The modulus left-shifted by 16. Has length equal to modulus.length + 1.
   */
  private final int[] modulusShift16;
  
  /**
   * An approximate 1-limb inverse of the modulus.
   */
  private final int modulusInverse;

  /**
   * The value {@code -modulus^{-1} mod 2^32}.
   */
  private final int montgomeryNegativeInverse;
  
  /**
   * Miscellaneous scratch space. Typically used by the Montgomery operations.
   */
  private final int[] scratch;
    
  private static final int MONTGOMERY_POWERS_SIZE = 2; // TODO 4?
  static { assert MONTGOMERY_POWERS_SIZE >= 2; }
  
  protected Modulus(int[] modulus) {
    assert modulus != null;
    assert modulus.length > 0;
    assert (modulus[0] & 1) != 0; // Odd
    assert modulus[modulus.length-1] < 0; // Highest bit is set
    
    this.modulus = modulus;
    
    // modulusShift16 = modulus << 16
    modulusShift16 = new int[modulus.length + 1];
    modulusShift16[0] = modulus[0] << 16;
    for(int i = 1; i < modulus.length; ++i)
      modulusShift16[i] = (modulus[i-1] >>> 16) | (modulus[i] << 16);
    modulusShift16[modulus.length] = modulus[modulus.length-1] >>> 16;

    // A one-limb under-approximation of the modulus inverse
    final int mtop = modulus[modulus.length-1];
    modulusInverse = (int)(((((~mtop) & MASK32) << 32) | MASK32) / (mtop & MASK32));

    // -modulus^{-1} mod 2^32
    montgomeryNegativeInverse = (int)(0x100000000L - (bInvertLimb(modulus[0]) & MASK32));
    
    // Scratch space for various Montgomery functions
    scratch = new int[2 * modulus.length];
  }
  
  /**
   * Construct a new {@code Modulus} object from a copy of {@code modulus}.
   * 
   * The {@code modulus} array is interpreted as a little-endian positive
   * integer with 32-bit unsigned limbs. That is {@code modulus[0] +
   * modulus[1] * 2^32 + ... + modulus[modulus.length-1] * 2^(32*(length-1))}
   * where each element {@code modulus[i]} is interpreted as an unsigned 32-bit
   * integer. The modulus must be odd (modulus[0] & 1 != 0) and normalised
   * (modulus[modulus.length-1] & 0x80000000 != 0).
   * 
   * A defensive copy of {@code modulus} is made.
   * 
   * @param modulus The value of the modulus.
   * @return A Modulus object. 
   * @throws NullPointerException If {@code modulus == null}.
   * @throws IllegalArgumentException If {@code modulus.length == 0}.
   * @throws IllegalArgumentException If modulus is an even number (i.e. if
   * {@code modulus[0] % 2 == 0})
   * @throws IllegalArgumentException If modulus is not normalised (i.e. if
   * {@code modulus[modulus.length-1] & 0x80000000 == 0}).
   */
  public static final Modulus valueOf(final int[] modulus) throws
    IllegalArgumentException,
    NullPointerException
  {
    if(modulus == null)
      throw new NullPointerException("modulus must not be null");
    if(modulus.length == 0)
      throw new IllegalArgumentException("modulus must not be empty");
    if((modulus[0] & 1) == 0)
      throw new IllegalArgumentException("modulus must be odd");
    if((modulus[modulus.length-1] & 0x80000000) == 0)
      throw new IllegalArgumentException("modulus must be normalised");
    return new Modulus(modulus.clone());
  }
  
  public static final Modulus valueOf(final BigInteger modulus) {
    if(modulus == null)
      throw new NullPointerException("modulus must not be null");
    if(modulus.signum() <= 0)
      throw new IllegalArgumentException("modulus must be positive");
    if(!modulus.testBit(0))
      throw new IllegalArgumentException("modulus must be odd");
    if(modulus.bitLength() % 32 != 0)
      throw new IllegalArgumentException("modulus must be normalised");
    
    // Convert the big-endian representation of modulus to a little-endian
    // array of ints.
    int size = modulus.bitLength() / 32;
    int[] m = new int[size];
    byte[] b = modulus.toByteArray();
    for(int i = b.length, j = 0; i > 0; i -= 4, ++j) {
      m[j] = (b[i-1] & 0xFF)
        | ((b[i-2] & 0xFF) << 8)
        | ((b[i-3] & 0xFF) << 16)
        | ((b[i-4] & 0xFF) << 24);
    }
    
    return new Modulus(m);
  }
  
  /**
   * Find the multiplicative inverse of {@code x} modulo 2^32 (treating
   * {@code x}) as an unsigned integer).
   * 
   * @param x An odd integer.
   * @return The int {@code xinv} such that {@code x * xinv == 1}.
   */
  protected static final int bInvertLimb(int x) {
    // NOTE this also works if all the calculations are done as 32-bit integers
    //      however it ends up being slightly slower...
    assert (x & 1) == 1; // x must be odd
    final long xLong = x & MASK32;
    long inv = 1L;
    inv = (2L * inv - inv * inv * xLong);
    inv = (2L * inv - inv * inv * xLong);
    inv = (2L * inv - inv * inv * xLong);
    inv = (2L * inv - inv * inv * xLong);
    inv = (2L * inv - inv * inv * xLong);
    assert ((x * inv) & MASK32) == 1L;
    assert (((x & MASK32) * (inv & MASK32)) & MASK32) == 1L;
    return (int)inv;
  }
  
  /**
   * Checks in constant-time if {@code value} is less than this modulus.
   * @param value A little-endian number.
   * @return
   */
  protected final boolean isLessThanModulus(int[] value) {
    assert value != null;
    assert value.length >= modulus.length;
    long overflow = 0L;
    for(int i = 0; i < modulus.length; ++i) {
      final long v = value[i] & MASK32;
      final long m = modulus[i] & MASK32;
      overflow = (overflow + v - m) >> 32;
    }
    return overflow < 0;
  }
  
  public final void mod(final int[] value) {
    assert value != null;
    
    // Shortcut (note we only need constant-time for operands of the same length)
    if(value.length < modulus.length)
      return;
        
    // Numerator (dividend)
    final int[] n = value;
    final int nn = n.length;
    
    // Denominator (divisor)
    final int[] d = modulus;
    final int dn = d.length;
    final long dInv = modulusInverse & MASK32;
    
    int q;
    long nTop = 0L;
    for(int i = nn - dn - 1; i >= 0; --i) {
      // Quotient high half-limb
      nTop = (nTop << 16) | ((n[i+dn] & MASK32) >>> 16);
      q = (int)(((nTop * dInv) >>> 32) + nTop); // Approx nTop / d[dn-1]
      multiplySubtract(dn+1, n, i, modulusShift16, 0, q);
      
      // Quotient low half-limb
      nTop = n[i+dn] & MASK32;
      q = (int)(((nTop * dInv) >>> 32) + nTop); // Approx nTop / d[dn-1]
      nTop += multiplySubtract(dn, n, i, d, 0, q);
    }

    // NOTE in all the masked operations below:
    //   mask == 0x00000000 if nTop == 0
    //   mask == 0xFFFFFFFF if nTop != 0
    // since:
    //   a) nTop >>> 63 == 0 if nTop >= 0
    //      nTop >>> 63 == 1 if nTop < 0
    //   b) (-nTop) >>> 63 == 0 if nTop <= 0
    //      (-nTop) >>> 63 == 1 if nTop > 0 (except for nTop = MIN_VALUE)
    //   c) (nTop|(-nTop)) >>> 63 == 0 if nTop == 0
    //      (nTop|(-nTop)) >>> 63 == 1 if nTop != 0
    //   d) -(int)(nTop|(-nTop)) >>> 63) == 0x00000000 if nTop == 0
    //      -(int)(nTop|(-nTop)) >>> 63) == 0xFFFFFFFF if nTop != 0
    
    // 1st adjustment
    int mask = -(int)((nTop|(-nTop)) >>> 63);
    nTop += maskedSubtract(dn, n, 0, d, 0, mask);
    
    // 2nd adjustment
    nTop = subtract(dn, n, 0, d, 0) - nTop;
    mask = -(int)((nTop|(-nTop)) >>> 63);        
    maskedAdd(dn, n, 0, d, 0, mask);
    
    // 3rd adjustment
    nTop = subtract(dn, n, 0, d, 0);
    mask = -(int)((nTop|(-nTop)) >>> 63);
    maskedAdd(dn, n, 0, d, 0, mask);
    
    // Zero-out high-order bits of reduced dividend
    for(int i = dn; i < nn; ++i)
      n[i] = 0;
  }
  
  /**
   * Invert a normalised value.
   * @param value
   * @return
   */
  protected static final int invert32(final int value) {
    assert (value & 0x80000000) != 0; // High bit must be set
    final int nl = ~0;
    final int nh = ~value;
    final long numerator = ((nh & MASK32) << 32) | (nl & MASK32);
    final long inverse = numerator / (value & MASK32);
    assert numerator >= 0;
    assert numerator >= value; // ??
    assert inverse <= 0xFFFFFFFFL;
    assert (value & MASK32) * inverse == 1L;
    return (int)inverse;
  }
  
  /**
   * Calculate the Montgomery representation of the number "1".
   * @param result
   * @return
   */
  protected final void calculateMontgomeryIdentity(final int[] result) {
    // Calculate 2^(32*n) mod m where m = modulus and n = modulus.length. This
    // is equivalent to 2^(32*n) - m, since 2^(32*n) > m since the highest bit
    // of m is set. The procedure below/ is equivalent to
    // subtract(new int[modulus.length], 1L, modulus, 0L, result)
    // but avoids explicitly representing the first argument.
    assert result.length >= modulus.length;
    long overflow = 0L;
    for(int i = 0; i < modulus.length; ++i) {
      overflow -= modulus[i] & MASK32;
      result[i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == -1L;
  }
  
  // Transform a value into the montgomery domain
  // TODO is this necessary if we have calculateMontgomeryPowers?
  protected final void montgomeryTransform(final int[] value) {
    assert (modulus[0] & 1) == 1; // Must be odd
    assert (modulus[modulus.length-1] & 0x80000000) != 0; // Must be normalised
    assert value.length <= modulus.length;
    // TODO assert value < modulus
    
    // v << (32 * mn.length)
    for(int i = 0; i < modulus.length; ++i) {
      scratch[i] = 0;
      scratch[i + modulus.length] = value[i]; 
    }
    
    // (v << (32 * mn)) % m
    mod(scratch); // TODO barret reduction?
    
    // ((v << (32 * mn)) % m) & ((1 << (32 * mn + 1)) - 1)
    for(int i = 0; i < modulus.length; ++i)
      value[i] = scratch[i];
  }
  
  // Reduce a value in the montgomery domain
  protected final void montgomeryReduce(final int[] value) {
    //assert (modulus[0] & 0x00000001) != 0; // Odd number
    //assert (modulus[modulus.length - 1] & 0x80000000) != 0; // MSB is set
    //assert value.length == 2 * modulus.length;
    
    final int[] v = value;
    final int[] m = modulus;
    final int mn = m.length;
    final long m0inv = montgomeryNegativeInverse & MASK32;
    //assert (int)((m[0] & LONG_MASK) * m0inv) == -1;
    
    // Reduction - store the overflows in the low `mn` limbs of v
    for(int i = 0; i < mn; ++i) {
      long overflow = 0L;
      final long multiplier = ((v[i] & MASK32) * m0inv) & MASK32;
      for(int j = 0; j < mn; ++j) {
        overflow += (v[i+j] & MASK32) + ((m[j] & MASK32) * multiplier);
        v[i+j] = (int)overflow;
        overflow >>>= 32;
      }
      //assert v[i] == 0;
      v[i] = (int)overflow;
    }
    
    // Shift right and integrate overflows, check if we need to subtract modulus
    // TODO integrate maskedSubtract
    long overflow = 0L;
    long mask = 0L;
    for(int i = 0; i < mn; ++i) {
      overflow += (v[i] & MASK32) + (v[i+mn] & MASK32);
      v[i] = (int)overflow;
      v[i+mn] = 0;
      overflow >>>= 32;
    
      mask += (modulus[i] & MASK32) - (v[i] & MASK32);
      mask >>= 32;
    }
    mask -= overflow;
    //assert overflow == 0L || overflow == 1L;
    //assert mask == 0L || mask == -1L;
    //assert overflow == 0L || mask == -1L;
    maskedSubtract(mn, v, 0, modulus, 0, (int)mask);
    //assert isLessThanModulus(value);
    
    // TODO do we really need the result to be mod modulus or can it just be
    //      less than the next power of two???
  }
  
  protected final void calculateMontgomeryPowers(int[] base, int[][] result) {
    assert base.length == modulus.length;
    assert isLessThanModulus(base);
    
    if(result.length < 1)
      return;
    
    // Calculate montgomery representation of base^0
    calculateMontgomeryIdentity(result[0]);
    
    if(result.length < 2)
      return;

    // Calculate montgomery representation of base^1
    for(int i = 0; i < modulus.length; ++i)
      result[1][i] = base[i];
    montgomeryTransform(result[1]);

    // Calculate montgomery representation of base^i for i > 1
    for(int i = 2; i < result.length; ++i) {
      multiply(result[1], result[i-1], scratch);
      montgomeryReduce(scratch);
      for(int j = 0; j < modulus.length; ++j)
        result[i][j] = scratch[j];
    }
  }
  
  /**
   * Multiply the {@code multiplicand} by {@code multiplier}, perform a
   * Montgomery reduction, and place the result in the {@code multiplicand}.
   * @param multiplicand
   * @param multiplier
   */
  protected final void multiplyReduce(
    final int[] multiplicand,
    final int[] multiplier)
  {
    // NOTE these length assertions could probably be relaxed
    assert multiplicand != null;
    assert multiplier != null;
    assert multiplicand.length == modulus.length;
    assert multiplier.length == modulus.length;
    
    // Perform a simultaneous multiplication and Mongomery reduction.
    final long MASK = 0xFFFFFFFFL;
    final int[] a = multiplicand;
    final int[] b = multiplier;
    final int[] m = modulus;
    final int[] s = scratch;
    final int length = m.length;
    final long m0inv = montgomeryNegativeInverse & MASK;
    for(int i = 0; i < s.length; ++i)
      s[i] = 0;
    for(int i = 0; i < length; ++i) {
      final long multiplierLimb = b[i] & MASK;
      long multiplyOverflow = multiplierLimb * (a[0] & MASK) + (s[i] & MASK);
      
      final long reducer = ((multiplyOverflow & MASK) * m0inv) & MASK;
      long reduceOverflow = reducer * (m[0] & MASK) + (multiplyOverflow & MASK);
      
      for(int j = 1, ij = i + j; j < length; ++j, ++ij) {
        multiplyOverflow >>>= 32;
        multiplyOverflow += multiplierLimb * (a[j] & MASK) + (s[ij] & MASK);
        reduceOverflow >>>= 32;
        reduceOverflow += reducer * (m[j] & MASK) + (multiplyOverflow & MASK);
        s[ij] = (int)reduceOverflow;
      }
      s[i+length] = (int)(multiplyOverflow >>> 32);
      s[i] = (int)(reduceOverflow >>> 32);
    }
    
    // We now have to "shift right" the result by mn limbs, integrate the
    // carries that were saved in the lower limbs, and potentially adjust the
    // result by subtracting the modulus.
    // 1. Add c[mn, ..., 2*mn-1] to c[0, ..., mn-1] and store the result in
    //    c[0, ..., mn-1].
    // 2. Concurrently subtract the modulus from the result of this addition and
    //    store the result in c[mn, ..., 2*mn-1].
    // 3. Choose which of (1) or (2) to return by masking.
    long overflow0 = 0L;
    long overflow1 = 0L;
    for(int i = 0; i < length; ++i) {
      overflow0 += (s[i] & MASK) + (s[i+length] & MASK);
      overflow1 += (overflow0 & MASK) - (m[i] & MASK);
      s[i] = (int)overflow0;
      s[i+length] = (int)overflow1;
      overflow0 >>>= 32;
      overflow1 >>= 32;
    }
    overflow1 += overflow0;
    int mask0 = (int)overflow1;
    int mask1 = ((int)(overflow1 >>> 63))-1;
    for(int i = 0; i < length; ++i)
      a[i] = (s[i] & mask0) | (s[i+length] & mask1);
    // TODO check that this isn't being optimised oddly
  }
  
  /**
   * Square the {@code multiplicand}, perform a Montgomery reduction, and place
   * the result in the {@code multiplicand}.
   * @param multiplicand
   */
  protected final void squareReduce(final int[] multiplicand) {
    multiplyReduce(multiplicand, multiplicand); // TODO optimise
  }
  
  protected final void select(
    final int index,
    final int[][] table,
    final int[] result)
  {
    for(int i = 0; i < result.length; ++i)
      result[i] = 0;
    for(int i = 0; i < table.length; ++i) {
      // mask == 0x00000000 if i != index
      // mask == 0xFFFFFFFF if i == index
      final int mask = (((index-i)|(i-index)) >>> 31)-1;
      for(int j = 0; j < result.length; ++j)
        result[j] |= table[i][j] & mask;
    }
  }
  
  /*
  protected static final void square3(final int[] a, final int[] b) {
    final long MASK = 0xFFFFFFFFL;
    long product = (a[0] & MASK) * (a[0] & MASK);
    long value0;
    long value1;
    long overflow = product >>> 32;
    b[0] = (int)product;
    
    int i = 1;
    while(i < b.length - 1) {
      value0 = overflow & MASK;
      value1 = overflow >>> 32;
      overflow = 0L;
      
      int j0min = Math.max(0, i - a.length + 1);
      int j0max = Math.min(i, a.length - 1);
      int j1min = Math.max(0, i - a.length + 2);
      int j1max = Math.min(i + 1, a.length - 1);
      while(j0min < j0max && j1min < j1max) {
        product = (a[j0min++] & MASK) * (a[j0max--] & MASK);
        value0 += MASK & (product << 1);
        value1 += product >>> 31;
        product = (a[j1min++] & MASK) * (a[j1max--] & MASK);
        value1 += MASK & (product << 1);
        overflow += product >>> 31;
      }
      jmin = Math.max(0, i - a.length + 2);
      jmax = Math.min(i + 1, a.length - 1);
      while(jmin < jmax) {
        product = (a[jmin++] & MASK) * (a[jmax--] & MASK);
        value1 += MASK & (product << 1);
        overflow += product >>> 31;
      }
      b[i++] = (int)value0;
      
      product = (a[i/2] & MASK);
      product *= product;
      value1 += MASK & product;
      value1 += value0 >>> 32;
      overflow += product >>> 32;
      overflow += value1 >>> 32;
      b[i++] = (int)value1;
    }
    b[i] = (int)overflow;
  }
  */

  protected static final void square4(final int[] a, final int[] b) {
    final long MASK = 0xFFFFFFFFL;
    long ai, product, value, overflow;
    for(int i = 0; i < b.length; ++i)
      b[i] = 0;
    for(int i = 0; i < a.length - 1; ++i) {
      ai = a[i] & MASK;
      product = ai * ai;
      value = (b[2*i] & MASK) + (product & MASK);
      overflow = (value >>> 32) + (product >>> 32);
      b[2*i] = (int)value;
      for(int j = i + 1; j < a.length; ++j) {
        product = ai * (a[j] & MASK);
        value = (overflow & MASK) + (b[i+j] & MASK) + ((product << 1) & MASK);
        overflow = (overflow >>> 32) + (value >>> 32) + (product >>> 31);
        b[i+j] = (int)value;
      }
      value = (overflow & MASK) + (b[i+a.length] & MASK);
      overflow = (overflow >>> 32) + (value >>> 32);
      b[i+a.length] = (int)value;
      value = (overflow & MASK) + (b[i+a.length+1] & MASK);            
      b[i+a.length+1] = (int)value;
    }
    ai = a[a.length - 1];
    product = ai * ai;
    value = (b[b.length - 2] & MASK) + (product & MASK);
    overflow = (value >>> 32) + (product >>> 32);
    b[b.length - 2] = (int)value;
    b[b.length - 1] = (int)overflow;
  }  
  
  protected static final void square3(final int[] a, final int[] b) {
    final long MASK = 0xFFFFFFFFL;
    //long product = (a[0] & MASK) * (a[0] & MASK);
    long product;
    long value = (a[0] & MASK) * (a[0] & MASK);
    long overflow = value >>> 32;
    b[0] = (int)value;
    
    for(int i = 1; i < a.length; ++i) {
      value = overflow & MASK;
      overflow >>>= 32;
      
      int jmin = 0;
      int jmax = i;
      while(jmin < jmax) {
        product = (a[jmin++] & MASK) * (a[jmax--] & MASK);
        value += MASK & (product << 1);
        overflow += product >>> 31;
      }
      product = a[i>>1] & MASK & ((i&1)-1);
      product *= product;
      value += MASK & product;
      overflow += (product >>> 32) + (value >>> 32);
      b[i] = (int)value;
    }
    
    for(int i = a.length; i < b.length - 1; ++i) {
      value = overflow & MASK;
      overflow >>>= 32;
      int jmax = a.length - 1;
      int jmin = i - jmax;
      while(jmin < jmax) {
        product = (a[jmin++] & MASK) * (a[jmax--] & MASK);
        value += MASK & (product << 1);
        overflow += product >>> 31;
      }
      product = a[i>>1] & MASK & ((i&1)-1);
      product *= product;
      value += MASK & product;
      overflow += (product >>> 32) + (value >>> 32);
      b[i] = (int)value;
    }
    b[b.length - 1] = (int)overflow;
  }
    
  protected static final void square2(final int[] a, final int[] b) {
    final long MASK = 0xFFFFFFFFL;
    long product;
    long value = (a[0] & MASK) * (a[0] & MASK);
    long overflow = value >>> 32;
    b[0] = (int)value;
    
    for(int i = 1; i < b.length - 1; ++i) {
      value = overflow & MASK;
      overflow >>>= 32;
      
      int jmin = Math.max(0, i - a.length + 1);
      int jmax = i - jmin;
      while(jmin < jmax) {
        product = (a[jmin++] & MASK) * (a[jmax--] & MASK);
        value += MASK & (product << 1);
        overflow += product >>> 31;
      }
      product = a[i>>1] & MASK & ((i&1)-1);
      product *= product;
      value += MASK & product;
      overflow += (product >>> 32);
      overflow += (value >>> 32);
      b[i] = (int)value;
    }
    b[b.length - 1] = (int)overflow;
  }
  
  protected static final void square(final int[] a, final int[] b) {
    final long MASK = 0xFFFFFFFFL;
    final int aLength = a.length;
    final int bLength = b.length;
    int jmin, jmax;
    long product;
    long value = (a[0] & MASK) * (a[0] & MASK);
    long overflow = value >>> 32;
    b[0] = (int)value;
    for(int i = 1; i < bLength - 1;) {
      value = overflow & MASK;
      overflow >>>= 32;
      jmin = Math.max(0, i - aLength + 1);
      jmax = i - jmin;
      while(jmin < jmax) {
        product = (a[jmin++] & MASK) * (a[jmax--] & MASK);
        value += MASK & (product << 1);
        overflow += product >>> 31;
      }
      overflow += (value >>> 32);
      b[i++] = (int)value;
      
      value = overflow & MASK;
      overflow >>>= 32;
      jmin = Math.max(0, i - aLength + 1);
      jmax = i - jmin;
      while(jmin < jmax) {
        product = (a[jmin++] & MASK) * (a[jmax--] & MASK);
        value += MASK & (product << 1);
        overflow += product >>> 31;
      }
      product = (a[jmin] & MASK) * (a[jmax] & MASK);
      value += (MASK & product);
      overflow += (value >>> 32) + (product >>> 32);
      b[i++] = (int)value;
    }
    b[bLength - 1] = (int)overflow;
  }

  protected final void powMod(
    final int[] base,
    final int[] exponent,
    final int[] result)
  {
    assert isLessThanModulus(base);
    assert base.length == modulus.length; // TODO necessary?
    assert result.length == modulus.length; 
    
    final int[] e = exponent;
    final int[] m = modulus;
    final int en = exponent.length;
    final int mn = modulus.length;

    // TODO can we reduce large exponents?
    // TODO tune window size
    
    // Calculate powers of `base` in their montgomery representation
    final int wn = 4;
    final int wmask = 0xF;
    final int[][] powers = new int[1 << wn][modulus.length];
    final int[] power = new int[modulus.length];
    calculateMontgomeryPowers(base, powers);    
    //assert 32 % wn == 0;

    // Sliding window exponentiation
    int exponentWindow = (e[en-1] >>> (32 - wn)) & wmask;
    for(int i = 0; i < mn; ++i)
      result[i] = powers[exponentWindow][i];
    for(int i = en - 1; i >= 0; --i) {
      int jStart = (i == en - 1) ? 32 - 2 * wn : 32 - wn;
      for(int j = jStart; j >= 0; j -= wn) {
        for(int k = 0; k < wn; ++k)
          squareReduce(result);
        exponentWindow = (e[i] >>> j) & wmask;
        select(exponentWindow, powers, power);
        multiplyReduce(result, power);
      }
    }
    
    for(int i = 0; i < mn; ++i) {
      scratch[i] = result[i];
      scratch[i + mn] = 0;
    }
    montgomeryReduce(scratch);
    for(int i = 0; i < mn; ++i)
      result[i] = scratch[i];
  }
}
