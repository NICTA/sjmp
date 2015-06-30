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

import static com.n1analytics.sjmp.Constants.MASK32;

// TODO Do we want to allow add and subtract where the augend/minuend are longer
//      than the addend/subtrahend? How would that work?
public class Operations {
  
  // Calculate floor((2^64 - 1) / d) - 2^32 where 2^31 <= d < 2^32.
  public static int invert(final int d) {
    // Note the following:
    // 1. floor((2^64 - 1) / d) - 2^32
    //      == floor(((2^32 - 1 - d) * 2^32 + (2^32 - 1)) / d)
    // 2. ~d == -1 - d 
    // 3. (((~d) & MASK) << 32) | MASK == (2^32 - 1 - d) * 2^32 + (2^32 - 1)
    assert (d & 0x80000000) != 0;
    final long n = (((~d) & MASK32) << 32) | MASK32;
    return (int)(n / (d & MASK32));
  }
  
  // Side-channel free unsigned integer division of a two-word numerator by a one-word denominator
  // Works by approximating the quotient (never overestimating it) and doing up
  // to three incremental compensations. Chooses the correct value via masking.
  //
  // n1 = numerator high word
  // n0 = numerator low word
  // d = denominator
  // dinv = denominator approximate inverse (as calculated by invert())
  public static long divide(final int n1, final int n0, final int d, final int dinv) {
    return divide(((n1 & 0xFFFFFFFFL) << 32) | (n0 & 0xFFFFFFFFL), d, dinv);
  }
  
  // TODO what to call this? divideDWordByWord? divideLongByInt?
  public static long divide(final long n, final int d, final int dinv) {
    // Notes for side-channel-free masking if 0 <= r < d:
    //
    // (~r) >>> 63 = 0 if r < 0
    // (~r) >>> 63 = 1 if r >= 0
    //
    // (r - d) >>> 63 == 0 if r >= d
    // (r - d) >>> 63 == 1 if r < d
    //
    // ((~r) & (r - d)) >>> 63 == 0 if r < 0 or r >= d
    // ((~r) & (r - d)) >>> 63 == 1 if r >= 0 and r < d
    //
    // -(((~r) & (r - d)) >>> 63) ==  0 == 0x0000000000000000L
    // -(((~r) & (r - d)) >>> 63) == -1 == 0xFFFFFFFFFFFFFFFFL if 0 <= r < d
    
    final long n1 = n >>> 32;
    final long dLong = d & MASK32;
    final long dinvLong = dinv & MASK32;
    
    // Case 0 - correct initial estimate of quotient
    long q = ((n1 * dinvLong) >>> 32) + n1;
    long r = n - q * dLong;
    long m = -(((~r) & (r - dLong)) >>> 63); // Mask if 0 <= r < d
    long qReturn = q & m;
    long rReturn = r & m;
    
    // Case 1 - underestimated quotient by 1
    ++q;
    r -= dLong;
    m = -(((~r) & (r - dLong)) >>> 63); // Mask if 0 <= r < d
    qReturn |= q & m;
    rReturn |= r & m;
    
    // Case 2 - underestimated quotient by 2
    ++q;
    r -= dLong;
    m = -(((~r) & (r - dLong)) >>> 63); // Mask if 0 <= r < d
    qReturn |= q & m;
    rReturn |= r & m;
    
    // Case 3 - underestimated quotient by 3
    ++q;
    r -= dLong;
    m = -(((~r) & (r - dLong)) >>> 63); // Mask if 0 <= r < d
    qReturn |= q & m;
    rReturn |= r & m;
    
    assert qReturn >= 0;
    assert qReturn <= 0xFFFFFFFFL;
    assert rReturn >= 0;
    assert rReturn < dLong;
    return (qReturn << 32) | rReturn;
  }
  
  public static final int compare(final int[] a, final int[] b) {
    return compare(a, 0, b, 0);
  }
  
  public static final int compare(
    final int[] a,
    final int aOverflow,
    final int[] b,
    final int bOverflow)
  {
    assert a != null;
    assert b != null;
    assert a.length == b.length;
    long overflow0 = 0;
    long overflow1 = 0;
    final int length = a.length;
    for(int i = 0; i < length; ++i) {
      final long ai = a[i] & MASK32;
      final long bi = b[i] & MASK32;
      overflow0 = (overflow0 + ai - bi) >> 32;
      overflow1 = (overflow1 + bi - ai) >> 32;
    }
    overflow0 = (overflow0 + aOverflow - bOverflow) >> 32;
    overflow1 = (overflow1 + bOverflow - aOverflow) >> 32;
    assert overflow0 == 0 || overflow0 == -1;
    assert overflow1 == 0 || overflow1 == -1;
    assert
      (overflow0 == -1 && overflow1 ==  0) ||
      (overflow0 ==  0 && overflow1 ==  0) ||
      (overflow0 ==  0 && overflow1 == -1);
    // At this stage:
    //   overflow0 == -1 if a < b, 0 if a >= b
    //   overflow1 == -1 if a > b, 0 if a <= b
    //   overflow1 - overflow1 == -1 if a < b, 0 if a == b, 1 if a > b
    return (int)(overflow0 - overflow1);
  }
  
  /**
   * Add {@code addend} to {@code augend} and return a carry.
   * 
   * The result of the addition will be stored in {@code augend} while
   * {@code addend} will remain unmodified.
   * 
   * This method will take constant-time with respect to {@code addend.length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param augend The operand to add to. The result will be stored here.
   * @param addend The operand to add.
   * @return {@code 1} if the addition overflowed and {@code 0} otherwise.
   */
  public static final int add(int[] augend, int[] addend) {
    assert augend != null;
    assert addend != null;
    assert augend.length == addend.length;
    long overflow = 0L;
    for(int i = 0; i < addend.length; ++i) {
      long augendLimb = augend[i] & MASK32;
      long addendLimb = addend[i] & MASK32;
      overflow += augendLimb + addendLimb;
      augend[i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }
  
  /**
   * Add {@code addend} to {@code augend} and return a carry.
   * 
   * This method will add the two number specified by the sub-arrays
   * {@code augend[0, ..., length - 1]} and
   * {@code addend[0, ..., length - 1]}.
   * 
   * The result of the addition will be stored in the sub-array
   * {@code augend[0, ..., length - 1]}. The other elements of {@code augend}
   * will remain unmodified, as will all elements of {@code addend}.
   * 
   * This method will take constant-time with respect to {@code length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to add.
   * @param augend The operand to add to. The result will be stored here.
   * @param addend The operand to add.
   * @return {@code 1} if the addition overflowed and {@code 0} otherwise.
   */
  public static final int add(int length, int[] augend, int[] addend) {
    assert length >= 0;
    assert augend != null;
    assert addend != null;
    assert length <= augend.length;
    assert length <= addend.length;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      long augendLimb = augend[i] & MASK32;
      long addendLimb = addend[i] & MASK32;
      overflow += augendLimb + addendLimb;
      augend[i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }
  
  /**
   * Add {@code addend} to {@code augend} and return a carry.
   * 
   * This method will add the two numbers specified by the sub-arrays
   * {@code augend[augendOffset, ..., augendOffset + length - 1]} and
   * {@code addend[addendOffset, ..., addendOffset + length - 1]}.
   * 
   * The result of the addition will be stored in the sub-array
   * {@code augend[augendOffset, ..., augendOffset + length - 1]}. The
   * other elements of {@code augend} will remain unmodified, as will all
   * elements of {@code addend}. 
   * 
   * This method will take constant-time with respect to {@code length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   *   
   * @param length The length of the sub-arrays to add.
   * @param augend The operand to add to. The result will be stored here.
   * @param augendOffset The index of the first element of the augend sub-array.
   * @param addend The operand to add.
   * @param addendOffset The index of the first element of the addend sub-array.
   * @return {@code 1} if the addition overflowed and {@code 0} otherwise.
   */
  public static final int add(
    final int length,
    final int[] augend,
    final int augendOffset,
    final int[] addend,
    final int addendOffset)
  {
    assert length >= 0;
    assert augend != null;
    assert addend != null;
    assert augendOffset >= 0;
    assert addendOffset >= 0;
    assert augendOffset + length <= augend.length;
    assert addendOffset + length <= addend.length;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      long augendLimb = augend[augendOffset + i] & MASK32;
      long addendLimb = addend[addendOffset + i] & MASK32;
      overflow += augendLimb + addendLimb;
      augend[augendOffset + i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }

  /**
   * Conditionally add {@code addend} to {@code augend} and return a carry.
   * 
   * When {@code addendMask == 0} no addition will be performed. All parameters
   * will remain unmodified and the return value will be 0.
   * 
   * When {@code addendMask == 0xFFFFFFFF == -1} this method will act
   * identically to {@code add(augend, addend)}.
   * 
   * Other values of {@code addendMask} are invalid.
   * 
   * This method will take constant-time with respect to {@code addend.length}.
   * In particular, the value of {@code addendMask} will not affect the time
   * taken.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param augend The operand to add to. The result will be stored here.
   * @param addend The operand to add.
   * @param addendMask The mask to be applied to each limb of {@code addend}
   * before the addition takes place. Valid values are 0 and -1.
   * @return {@code 1} if the addition overflowed and {@code 0} otherwise.
   */
  public static final int maskedAdd(
    final int[] augend,
    final int[] addend,
    final int addendMask)
  {
    assert augend != null;
    assert addend != null;
    assert addend.length == augend.length;
    assert addendMask == 0x00000000 || addendMask == 0xFFFFFFFF;
    long overflow = 0L;
    for(int i = 0; i < addend.length; ++i) {
      long augendLimb = augend[i] & MASK32;
      long addendLimb = addend[i] & MASK32 & addendMask;
      overflow += augendLimb + addendLimb;
      augend[i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }

  /**
   * Conditionally add {@code addend} to {@code augend} and return a carry.
   * 
   * When {@code addendMask == 0} no addition will be performed. All parameters
   * will remain unmodified and the return value will be 0.
   * 
   * When {@code addendMask == 0xFFFFFFFF == -1} this method will act
   * identically to {@code add(length, augend, addend)}.
   * 
   * Other values of {@code addendMask} are invalid.
   * 
   * This method will take constant-time with respect to {@code length}. In
   * particular, the value of {@code addendMask} will not affect the time taken.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to add.
   * @param augend The operand to add to. The result will be stored here.
   * @param addend The operand to add.
   * @param addendMask The mask to be applied to each limb of {@code addend}
   * before the addition takes place. Valid values are 0 and -1.
   * @return {@code 1} if the addition overflowed and {@code 0} otherwise.
   */
  public static final int maskedAdd(
    final int length,
    final int[] augend,
    final int[] addend,
    final int addendMask)
  {
    assert length >= 0;
    assert augend != null;
    assert addend != null;
    assert length <= augend.length;
    assert length <= addend.length;
    assert addendMask == 0x00000000 || addendMask == 0xFFFFFFFF;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      long augendLimb = augend[i] & MASK32;
      long addendLimb = addend[i] & MASK32 & addendMask;
      overflow += augendLimb + addendLimb;
      augend[i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }

  /**
   * Conditionally add {@code addend} to {@code augend} and return a carry.
   * 
   * When {@code addendMask == 0} no addition will be performed. All parameters
   * will remain unmodified and the return value will be 0.
   * 
   * When {@code addendMask == 0xFFFFFFFF == -1} this method will act
   * identically to {@code add(length, augend, augendOffset, addend, addendOffset)}.
   * 
   * Other values of {@code addendMask} are invalid.
   * 
   * This method will take constant-time with respect to {@code length}. In
   * particular, the value of {@code addendMask} will not affect the time taken.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to add.
   * @param augend The operand to add to. The result will be stored here.
   * @param augendOffset The index of the first element of the augend sub-array.
   * @param addend The operand to add.
   * @param addendOffset The index of the first element of the addend sub-array.
   * @param addendMask The mask to be applied to each limb of {@code addend}
   * before the addition takes place. Valid values are 0 and -1.
   * @return {@code 1} if the addition overflowed and {@code 0} otherwise.
   */
  public static final int maskedAdd(
    final int length,
    final int[] augend,
    final int augendOffset,
    final int[] addend,
    final int addendOffset,
    final int addendMask)
  {
    assert length >= 0;
    assert augend != null;
    assert addend != null;
    assert augendOffset >= 0;
    assert addendOffset >= 0;
    assert augendOffset + length <= augend.length;
    assert addendOffset + length <= addend.length;
    assert addendMask == 0x00000000 || addendMask == 0xFFFFFFFF;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      long augendLimb = augend[augendOffset + i] & MASK32;
      long addendLimb = addend[addendOffset + i] & MASK32 & addendMask;
      overflow += augendLimb + addendLimb;
      augend[augendOffset + i] = (int)overflow;
      overflow >>>= 32;
    }
    assert overflow == 0L || overflow == 1L;
    return (int)overflow;
  }
  
  /**
   * Subtract {@code subtrahend} from {@code minuend} and return a borrow.
   * 
   * The result of the subtraction will be stored in {@code minuend} while
   * {@code subtrahend} will remain unmodified.
   * 
   * This method will take constant-time with respect to
   * {@code subtrahend.length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param minuend The operand to subtract from. The result will be stored here.
   * @param subtrahend The operand to subtract.
   * @return {@code -1} if the subtraction underflowed and {@code 0} otherwise.
   */
  public static final int subtract(
    final int[] minuend,
    final int[] subtrahend)
  {
    assert minuend != null;
    assert subtrahend != null;
    assert minuend.length == subtrahend.length;
    long overflow = 0L;
    for(int i = 0; i < subtrahend.length; ++i) {
      final long minuendLimb = minuend[i] & MASK32;
      final long subtrahendLimb = subtrahend[i] & MASK32;
      overflow += minuendLimb - subtrahendLimb;
      minuend[i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }
  
  /**
   * Subtract {@code subtrahend} from {@code minuend} and return a borrow.
   * 
   * This method will subtract the number specified by the sub-array
   * {@code subtrahend[0, ..., length - 1]} from the number specified by the
   * sub-array {@code minuend[0, ..., length - 1]}.
   * 
   * The result of the subtraction will be stored in the sub-array
   * {@code minuend[0, ..., length - 1]}. The other elements of {@code minuend}
   * will remain unmodified, as will all elements of {@code subtrahend}.
   * 
   * This method will take constant-time with respect to {@code length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to subract.
   * @param minuend The operand to subtract from. The result will be stored
   * here.
   * @param subtrahend The operand to subtract.
   * @return {@code -1} if the subtraction underflowed and {@code 0} otherwise.
   */
  public static final int subtract(
    final int length,
    final int[] minuend,
    final int[] subtrahend)
  {
    assert length >= 0;
    assert minuend != null;
    assert subtrahend != null;
    assert length <= minuend.length;
    assert length <= subtrahend.length;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      final long minuendLimb = minuend[i] & MASK32;
      final long subtrahendLimb = subtrahend[i] & MASK32;
      overflow += minuendLimb - subtrahendLimb;
      minuend[i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }
  
  /**
   * Subtract {@code subtrahend} from {@code minuend} and return a borrow.
   * 
   * This method will subtract the number specified by the sub-array
   * {@code subtrahend[subtrahendOffset, ..., subtrahendOffset + length - 1]}
   * from the number specified by the sub-array
   * {@code minuend[minuendOffset, ..., minuendOffset + length - 1]}.
   * 
   * The result of the subtraction will be stored in the sub-array
   * {@code minuend[minuendOffset, ..., minuendOffset + length - 1]}. The other
   * elements of {@code minuend} will remain unmodified, as will all elements of
   * {@code subtrahend}.
   * 
   * This method will take constant-time with respect to {@code length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to subract.
   * @param minuend The operand to subtract from. The result will be stored
   * here.
   * @param minuendOffset The index of the first element of the minuend
   * sub-array.
   * @param subtrahend The operand to subtract.
   * @param subtrahendOffset The index of the first element of the subtrahend
   * sub-array.
   * @return {@code -1} if the subtraction underflowed and {@code 0} otherwise.
   */
  public static final int subtract(
    final int length,
    final int[] minuend,
    final int minuendOffset,
    final int[] subtrahend,
    final int subtrahendOffset)
  {
    assert length >= 0;
    assert minuend != null;
    assert subtrahend != null;
    assert minuendOffset >= 0;
    assert subtrahendOffset >= 0;
    assert minuendOffset + length <= minuend.length;
    assert subtrahendOffset + length <= subtrahend.length;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      final long minuendLimb = minuend[minuendOffset + i] & MASK32;
      final long subtrahendLimb = subtrahend[subtrahendOffset + i] & MASK32;
      overflow += minuendLimb - subtrahendLimb;
      minuend[minuendOffset + i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }
  
  /**
   * Conditionally subtract {@code subtrahend} from {@code minuend} and return a
   * borrow.
   * 
   * When {@code subtrahendMask == 0} no subtraction will be performed. All
   * parameters will remain unmodified and the return value will be 0.
   * 
   * When {@code subtrahendMask == 0xFFFFFFFF == -1} this method will act
   * identically to {@code subtract(minuend, subtrahend)}.
   * 
   * Other values of {@code subtrahendMask} are invalid.
   * 
   * This method will take constant-time with respect to
   * {@code subtrahend.length}. In particular, the value of
   * {@code subtrahendMask} will not affect the time taken.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param minuend The operand to subtract from. The result will be stored here.
   * @param subtrahend The operand to subtract.
   * @param subtrahendMask The mask to be applied to each limb of
   * {@code subtrahend} before the subtraction takes place. Valid values are 0
   * and -1.
   * @return {@code -1} if the subtraction underflowed and {@code 0} otherwise.
   */
  public static final int maskedSubtract(
    final int[] minuend,
    final int[] subtrahend,
    final int subtrahendMask)
  {
    assert minuend != null;
    assert subtrahend != null;
    assert minuend.length == subtrahend.length;
    assert subtrahendMask == 0x00000000 || subtrahendMask == 0xFFFFFFFF;
    long overflow = 0L;
    for(int i = 0; i < subtrahend.length; ++i) {
      final long minuendLimb = minuend[i] & MASK32;
      final long subtrahendLimb = subtrahend[i] & MASK32 & subtrahendMask;
      overflow += minuendLimb - subtrahendLimb;
      minuend[i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }

  /**
   * Conditionally subtract {@code subtrahend} from {@code minuend} and return a
   * borrow.
   * 
   * When {@code subtrahendMask == 0} no subtraction will be performed. All
   * parameters will remain unmodified and the return value will be 0.
   * 
   * When {@code subtrahendMask == 0xFFFFFFFF == -1} this method will act
   * identically to {@code subtract(length, minuend, subtrahend)}.
   * 
   * Other values of {@code subtrahendMask} are invalid.
   * 
   * This method will take constant-time with respect to {@code length}. In
   * particular, the value of {@code subtrahendMask} will not affect the time
   * taken.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to subtract.
   * @param minuend The operand to subtract from. The result will be stored here.
   * @param subtrahend The operand to subtract.
   * @param subtrahendMask The mask to be applied to each limb of
   * {@code subtrahend} before the subtraction takes place. Valid values are 0
   * and -1.
   * @return {@code -1} if the subtraction underflowed and {@code 0} otherwise.
   */
  public static final int maskedSubtract(
    final int length,
    final int[] minuend,
    final int[] subtrahend,
    final int subtrahendMask)
  {
    assert length >= 0;
    assert minuend != null;
    assert subtrahend != null;
    assert length <= minuend.length;
    assert length <= subtrahend.length;
    assert subtrahendMask == 0x00000000 || subtrahendMask == 0xFFFFFFFF;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      final long minuendLimb = minuend[i] & MASK32;
      final long subtrahendLimb = subtrahend[i] & MASK32 & subtrahendMask;
      overflow += minuendLimb - subtrahendLimb;
      minuend[i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }

  /**
   * Conditionally subtract {@code subtrahend} from {@code minuend} and return a
   * borrow.
   * 
   * When {@code subtrahendMask == 0} no subtraction will be performed. All
   * parameters will remain unmodified and the return value will be 0.
   * 
   * When {@code subtrahendMask == 0xFFFFFFFF == -1} this method will act
   * identically to {@code subtract(length, minuend, minuendOffset, subtrahend, subtrahendOffset)}.
   * 
   * Other values of {@code subtrahendMask} are invalid.
   * 
   * This method will take constant-time with respect to {@code length}. In
   * particular, the value of {@code subtrahendMask} will not affect the time
   * taken.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param length The length of the sub-arrays to subtract.
   * @param minuend The operand to subtract from. The result will be stored here.
   * @param minuendOffset The index of the first element of the minuend
   * sub-array.
   * @param subtrahend The operand to subtract.
   * @param subtrahendOffset The index of the first element of the subtrahend
   * sub-array.
   * @param subtrahendMask The mask to be applied to each limb of
   * {@code subtrahend} before the subtraction takes place. Valid values are 0
   * and -1.
   * @return {@code -1} if the subtraction underflowed and {@code 0} otherwise.
   */
  public static final int maskedSubtract(
    final int length,
    final int[] minuend,
    final int minuendOffset,
    final int[] subtrahend,
    final int subtrahendOffset,
    final int subtrahendMask)
  {
    assert length >= 0;
    assert minuend != null;
    assert subtrahend != null;
    assert minuendOffset >= 0;
    assert subtrahendOffset >= 0;
    assert minuendOffset + length <= minuend.length;
    assert subtrahendOffset + length <= subtrahend.length;
    assert subtrahendMask == 0x00000000 || subtrahendMask == 0xFFFFFFFF;
    long overflow = 0L;
    for(int i = 0; i < length; ++i) {
      final long minuendLimb = minuend[minuendOffset + i] & MASK32;
      final long subtrahendLimb = subtrahend[subtrahendOffset + i] & MASK32 & subtrahendMask;
      overflow += minuendLimb - subtrahendLimb;
      minuend[minuendOffset + i] = (int)overflow;
      overflow >>= 32;
    }
    assert overflow == 0L || overflow == -1L;
    return (int)overflow;
  }
  
  /**
   * Multiply {@code multiplicand} by {@code multiplier} and store the result in
   * {@code result}.
   * 
   * This method will take constant-time with respect to the pair
   * {@code (multiplicand.length, multiplier.length)}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param multiplicand One of the numbers to multiply. 
   * @param multiplier One of the numbers to multiply.
   * @param result Holds the result. Must have length greater than or equal to
   * the sum of the lengths of {@code multiplicand} and {@code multiplier}.
   */
  public static final void multiply(
    final int[] multiplicand,
    final int[] multiplier,
    final int[] result)
  {
    assert multiplicand != null;
    assert multiplier != null;
    assert result != null;
    assert multiplicand.length + multiplier.length <= result.length;
    long overflow = 0L;
    long multiplierLimb = multiplier[0] & MASK32;
    for(int i = 0; i < multiplicand.length; ++i) {
      final long multiplicandLimb = multiplicand[i] & MASK32;
      overflow += multiplicandLimb * multiplierLimb;
      result[i] = (int)overflow;
      overflow >>>= 32;
    }
    result[multiplicand.length] = (int)overflow;
    for(int i = 1; i < multiplier.length; ++i) {
      overflow = 0L;
      multiplierLimb = multiplier[i] & MASK32;
      for(int j = 0; j < multiplicand.length; ++j) {
        final long multiplicandLimb = multiplicand[j] & MASK32;
        final long resultLimb = result[i + j] & MASK32;
        overflow += multiplicandLimb * multiplierLimb + resultLimb;
        result[i + j] = (int)overflow;
        overflow >>>= 32;
      }
      result[i + multiplicand.length] = (int)overflow;
    }
  }

  /**
   * Multiply {@code multiplicand} by {@code multiplier} and store the result in
   * {@code result}.
   * 
   * This method will multiply the two numbers specified by the sub-arrays
   * {@code multiplicand[0, ..., multiplicandLength - 1]} and
   * {@code multiplier[0, ..., multiplierLength - 1]} and store the result in
   * {@code result[0, ..., multiplicandLength * multiplierLength - 1]}.
   * 
   * This method will take constant-time with respect to the pair
   * {@code (multiplicand.length, multiplier.length)}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param multiplicand One of the numbers to multiply.
   * @param multiplicandLength The length of the multiplicand sub-array. 
   * @param multiplier One of the numbers to multiply.
   * @param multiplierLength The length of the multiplier sub-array.
   * @param result Holds the result. Must have length greater than or equal to
   * the sum of the lengths of {@code multiplicand} and {@code multiplier}.
   */
  public static final void multiply(
    final int[] multiplicand,
    final int multiplicandLength,
    final int[] multiplier,
    final int multiplierLength,
    final int[] result) 
  {
    assert multiplicand != null;
    assert multiplier != null;
    assert result != null;
    assert multiplicandLength >= 0;
    assert multiplierLength >= 0;
    assert multiplicandLength <= multiplicand.length;
    assert multiplierLength <= multiplier.length;
    assert multiplicandLength + multiplierLength <= result.length;
    long overflow = 0L;
    long multiplierLimb = multiplier[0] & MASK32;
    for(int i = 0; i < multiplicandLength; ++i) {
      final long multiplicandLimb = multiplicand[i] & MASK32;
      overflow += multiplicandLimb * multiplierLimb;
      result[i] = (int)overflow;
      overflow >>>= 32;
    }
    result[multiplicandLength] = (int)overflow;
    for(int i = 1; i < multiplierLength; ++i) {
      overflow = 0L;
      multiplierLimb = multiplier[i] & MASK32;
      for(int j = 0; j < multiplicandLength; ++j) {
        final long multiplicandLimb = multiplicand[j] & MASK32;
        final long resultLimb = result[i + j] & MASK32;
        overflow += multiplicandLimb * multiplierLimb + resultLimb;
        result[i + j] = (int)overflow;
        overflow >>>= 32;
      }
      result[i + multiplicandLength] = (int)overflow;
    }
  }

  /**
   * Multiply {@code multiplicand} by {@code multiplier} and store the result in
   * {@code result}.
   * 
   * This method will multiply the two numbers specified by the sub-arrays
   * {@code multiplicand[multiplicandOffset, ..., multplicandOffset + multiplicandLength - 1]} and
   * {@code multiplier[multiplierOffset, ..., multiplierOffset + multiplierLength - 1]}
   * and store the result in
   * {@code result[0, ..., multiplicandLength * multiplierLength - 1]}.
   * 
   * This method will take constant-time with respect to the pair
   * {@code (multiplicand.length, multiplier.length)}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param multiplicand One of the numbers to multiply.
   * @param multiplicandOffset The index of the first element of the
   * multiplicand sub-array.
   * @param multiplicandLength The length of the multiplicand sub-array. 
   * @param multiplier One of the numbers to multiply.
   * @param multiplierOffset The index of the first element of the multiplier
   * sub-array.
   * @param multiplierLength The length of the multiplier sub-array.
   * @param result Holds the result. Must have length greater than or equal to
   * the sum of the lengths of {@code multiplicand} and {@code multiplier}.
   */
  public static final void multiply(
    final int[] multiplicand,
    final int multiplicandOffset,
    final int multiplicandLength,
    final int[] multiplier,
    final int multiplierOffset,
    final int multiplierLength,
    final int[] result,
    final int resultOffset) 
  {
    assert multiplicand != null;
    assert multiplier != null;
    assert result != null;
    assert multiplicandOffset >= 0;
    assert multiplierOffset >= 0;
    assert resultOffset >= 0;
    assert multiplicandLength >= 0;
    assert multiplierLength >= 0;
    assert multiplicandOffset + multiplicandLength <= multiplicand.length;
    assert multiplierOffset + multiplierLength <= multiplier.length;
    assert multiplicandLength + multiplierLength + resultOffset <= result.length;
    long overflow = 0L;
    long multiplierLimb = multiplier[multiplierOffset] & MASK32;
    for(int i = 0; i < multiplicandLength; ++i) {
      final long multiplicandLimb = multiplicand[multiplicandOffset + i] & MASK32;
      overflow += multiplicandLimb * multiplierLimb;
      result[resultOffset + i] = (int)overflow;
      overflow >>>= 32;
    }
    result[resultOffset + multiplicandLength] = (int)overflow;
    for(int i = 1; i < multiplierLength; ++i) {
      overflow = 0L;
      multiplierLimb = multiplier[multiplierOffset + i] & MASK32;
      for(int j = 0; j < multiplicandLength; ++j) {
        final long multiplicandLimb = multiplicand[multiplicandOffset + j] & MASK32;
        final long resultLimb = result[resultOffset + i + j] & MASK32;
        overflow += multiplicandLimb * multiplierLimb + resultLimb;
        result[resultOffset + i + j] = (int)overflow;
        overflow >>>= 32;
      }
      result[resultOffset + i + multiplicandLength] = (int)overflow;
    }
  }
  
  /**
   * Multiply {@code multiplicand} by {@code multiplierLimb}, add to
   * {@code augend} and return a carry.
   * 
   * The result will be stored in {@code augend[0, ..., multiplicand.length - 1]}
   * while any overflow will be returned. The remaining elements of
   * {@code augend} and all the elements of {@code multiplierLimb} will remain
   * unmodified.
   * 
   * This method will take constant-time with respect to 
   * {@code multiplicand.length}.
   * 
   * For performance reasons, this method provides no error checking aside from
   * assertions.
   * 
   * @param augend The operand to add to. The result will be stored here.
   * @param multiplicand The operand to multiply.
   * @param multiplierLimb The operand to multiply.
   * @return Any overflow that cannot be stored in
   * {@code augend[0, ..., multiplicand.length - 1]}. This should be treated as
   * an unsigned integer.
   */
  public static final int multiplyAdd(
    final int[] augend,
    final int[] multiplicand,
    final int multiplierLimb)
  {
    assert augend != null;
    assert multiplicand != null;
    assert multiplicand.length <= augend.length;
    long overflow = 0L;
    final long multiplier = multiplierLimb & MASK32;
    for(int i = 0; i < multiplicand.length; ++i) {
      final long augendLimb = augend[i] & MASK32;
      final long multiplicandLimb = multiplicand[i] & MASK32;
      overflow += augendLimb + multiplicandLimb * multiplier;
      augend[i] = (int)overflow;
      overflow >>>= 32;
    }
    return (int)overflow;
  }
  
  public static final int multiplyAdd(
    final int length,
    final int[] augend,
    final int[] multiplicand,
    final int multiplierLimb)
  {
    assert augend != null;
    assert multiplicand != null;
    assert length >= 0;
    assert length <= augend.length;
    assert length <= multiplicand.length;
    long overflow = 0L;
    final long multiplier = multiplierLimb & MASK32;
    for(int i = 0; i < length; ++i) {
      final long augendLimb = augend[i] & MASK32;
      final long multiplicandLimb = multiplicand[i] & MASK32;
      overflow += augendLimb + multiplicandLimb * multiplier;
      augend[i] = (int)overflow;
      overflow >>>= 32;
    }
    return (int)overflow;
  }
  
  public static final int multiplyAdd(
    final int length,
    final int[] augend,
    final int augendOffset,
    final int[] multiplicand,
    final int multiplicandOffset,
    final int multiplierLimb)
  {
    assert augend != null;
    assert multiplicand != null;
    assert length >= 0;
    assert augendOffset >= 0;
    assert multiplicandOffset >= 0;
    assert augendOffset + length <= augend.length;
    assert multiplicandOffset + length <= multiplicand.length;
    long overflow = 0L;
    final long multiplier = multiplierLimb & MASK32;
    for(int i = 0; i < length; ++i) {
      final long augendLimb = augend[augendOffset + i] & MASK32;
      final long multiplicandLimb = multiplicand[multiplicandOffset + i] & MASK32;
      overflow += augendLimb + multiplicandLimb * multiplier;
      augend[augendOffset + i] = (int)overflow;
      overflow >>>= 32;
    }
    return (int)overflow;
  }
  
  public static final long multiplySubtract(
    final int[] minuend,
    final int[] multiplicand,
    final int multiplierLimb)
  {
    assert minuend != null;
    assert multiplicand != null;
    assert multiplicand.length <= minuend.length;
    long value = 0L;
    long overflow = 0L;
    final long multiplier = multiplierLimb & MASK32;
    for(int i = 0; i < multiplicand.length; ++i) {
      final long minuendLimb = minuend[i] & MASK32;
      final long multiplicandLimb = multiplicand[i] & MASK32;
      value += multiplicandLimb * multiplier;
      overflow += minuendLimb - (value & MASK32);
      minuend[i] = (int)overflow;
      value >>>= 32;
      overflow >>= 32;
    }
    overflow -= value;
    return overflow;
  }
  
  public static final long multiplySubtract(
    final int length,
    final int[] minuend,
    final int[] multiplicand,
    final int multiplierLimb)
  {
    assert minuend != null;
    assert multiplicand != null;
    assert length >= 0;
    assert length <= minuend.length;
    assert length <= multiplicand.length;
    long value = 0L;
    long overflow = 0L;
    final long multiplier = multiplierLimb & MASK32;
    for(int i = 0; i < length; ++i) {
      final long minuendLimb = minuend[i] & MASK32;
      final long multiplicandLimb = multiplicand[i] & MASK32;
      value += multiplicandLimb * multiplier;
      overflow += minuendLimb - (value & MASK32);
      minuend[i] = (int)overflow;
      value >>>= 32;
      overflow >>= 32;
    }
    overflow -= value;
    return overflow;
  }
  
  public static final long multiplySubtract(
    final int length,
    final int[] minuend,
    final int minuendOffset,
    final int[] multiplicand,
    final int multiplicandOffset,
    final int multiplierLimb)
  {
    assert minuend != null;
    assert multiplicand != null;
    assert length >= 0;
    assert minuendOffset >= 0;
    assert multiplicandOffset >= 0;
    assert minuendOffset + length <= minuend.length;
    assert multiplicandOffset + length <= multiplicand.length;
    long value = 0L;
    long overflow = 0L;
    final long multiplier = multiplierLimb & MASK32;
    for(int i = 0; i < length; ++i) {
      final long minuendLimb = minuend[minuendOffset + i] & MASK32;
      final long multiplicandLimb = multiplicand[multiplicandOffset + i] & MASK32;
      value += multiplicandLimb * multiplier;
      overflow += minuendLimb - (value & MASK32);
      minuend[minuendOffset + i] = (int)overflow;
      value >>>= 32;
      overflow >>= 32;
    }
    overflow -= value;
    return overflow;
  }
  
  public static final void mod(
    final int[] dividend,
    final int[] divisor,
    final int[] scratch)
  {
    assert dividend != null;
    assert divisor != null;
    assert scratch != null;
    assert (divisor[divisor.length-1] & 0x80000000) != 0;
    assert scratch.length > divisor.length;
    
    if(dividend.length < divisor.length)
      return;
    
    final int[] n = dividend;
    final int nn = n.length;
    
    final int[] d = divisor;
    final int dn = d.length;
    final int dTop = d[dn-1];
    final long dInv = (((((~dTop) & MASK32) << 32) | MASK32) / (dTop & MASK32)) & MASK32;
    
    // dshift = d << 16
    final int[] dshift = scratch;
    dshift[0] = d[0] << 16;
    for(int i = 1; i < dn; ++i)
      dshift[i] = (d[i-1] >>> 16) | (d[i] << 16);
    dshift[dn] = d[dn-1] >>> 16;
    
    int q;
    long nTop = 0;
    for(int i = nn - dn - 1; i >= 0; --i) {
      // Quotient high half-limb
      nTop = (nTop << 16) | ((n[i+dn] & MASK32) >>> 16);
      q = (int)(((nTop * dInv) >>> 32) + nTop); // Approx nTop / dTop
      multiplySubtract(dn+1, n, i, dshift, 0, q);
      
      // Quotient low half-limb
      nTop = n[i+dn] & MASK32;
      q = (int)(((nTop * dInv) >>> 32) + nTop); // Approx nTop / dTop
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
}
