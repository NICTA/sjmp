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

import static com.n1analytics.sjmp.Constants.MASK32;
import static com.n1analytics.sjmp.TestUtil.*;
import static org.junit.Assert.assertEquals;

// TODO tests on proper sub-arrays
public class OperationsTest {
  private static final Random random = new Random();
  private static final int FUZZ_SIZE = 64;
  private static final int FUZZ_ITERATIONS = 10000;

  @Test
  public void fuzzTestAdd() {
    int overflow;
    int[] temp = new int[FUZZ_SIZE];
    int[] augend = new int[FUZZ_SIZE];
    int[] addend = new int[FUZZ_SIZE];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      BigInteger augendBig = toBigInteger(randomInteger(augend));
      BigInteger addendBig = toBigInteger(TestUtil.randomInteger(addend));
      BigInteger resultBig = augendBig.add(addendBig);
      
      copy(augend, temp);
      overflow = Operations.add(temp, addend);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
      copy(augend, temp);
      overflow = Operations.add(FUZZ_SIZE, temp, addend);
      assertEquals(resultBig, toBigInteger(temp, overflow));        
      
      copy(augend, temp);
      overflow = Operations.add(FUZZ_SIZE, temp, 0, addend, 0);
      assertEquals(resultBig, toBigInteger(temp, overflow));
    }
  }
  
  @Test
  public void fuzzTestMaskedAdd() {
    int overflow;
    int[] temp = new int[FUZZ_SIZE];
    int[] augend = new int[FUZZ_SIZE];
    int[] addend = new int[FUZZ_SIZE];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      BigInteger augendBig = toBigInteger(randomInteger(augend));
      BigInteger addendBig = toBigInteger(TestUtil.randomInteger(addend));
      BigInteger resultBig = augendBig.add(addendBig);
      
      copy(augend, temp);
      overflow = Operations.maskedAdd(temp, addend, 0);
      assertEquals(augendBig, toBigInteger(temp, overflow));
      
      copy(augend, temp);
      overflow = Operations.maskedAdd(temp, addend, 0xFFFFFFFF);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
      copy(augend, temp);
      overflow = Operations.maskedAdd(FUZZ_SIZE, temp, addend, 0);
      assertEquals(augendBig, toBigInteger(temp, overflow));
      
      copy(augend, temp);
      overflow = Operations.maskedAdd(FUZZ_SIZE, temp, addend, 0xFFFFFFFF);
      assertEquals(resultBig, toBigInteger(temp, overflow));      
      
      copy(augend, temp);
      overflow = Operations.maskedAdd(FUZZ_SIZE, temp, 0, addend, 0, 0);
      assertEquals(augendBig, toBigInteger(temp, overflow));      
      
      copy(augend, temp);
      overflow = Operations.maskedAdd(FUZZ_SIZE, temp, 0, addend, 0, 0xFFFFFFFF);
      assertEquals(resultBig, toBigInteger(temp, overflow));
    }
  }

  @Test
  public void fuzzTestSubtract() {
    int overflow;
    int[] temp = new int[FUZZ_SIZE];
    int[] minuend = new int[FUZZ_SIZE];
    int[] subtrahend = new int[FUZZ_SIZE];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      BigInteger minuedBig = toBigInteger(randomInteger(minuend));
      BigInteger subtrahendBig = toBigInteger(TestUtil.randomInteger(subtrahend));
      BigInteger resultBig = minuedBig.subtract(subtrahendBig);
      
      copy(minuend, temp);
      overflow = Operations.subtract(temp, subtrahend);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
      copy(minuend, temp);
      overflow = Operations.subtract(FUZZ_SIZE, temp, subtrahend);
      assertEquals(resultBig, toBigInteger(temp, overflow));        
      
      copy(minuend, temp);
      overflow = Operations.subtract(FUZZ_SIZE, temp, 0, subtrahend, 0);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
    }
  }
  
  @Test
  public void fuzzTestMaskedSubtract() {
    int overflow;
    int[] temp = new int[FUZZ_SIZE];
    int[] minuend = new int[FUZZ_SIZE];
    int[] subtrahend = new int[FUZZ_SIZE];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      BigInteger minuendBig = toBigInteger(randomInteger(minuend));
      BigInteger subtrahendBig = toBigInteger(TestUtil.randomInteger(subtrahend));
      BigInteger resultBig = minuendBig.subtract(subtrahendBig);
      
      copy(minuend, temp);
      overflow = Operations.maskedSubtract(temp, subtrahend, 0);
      assertEquals(minuendBig, toBigInteger(temp, overflow));
      
      copy(minuend, temp);
      overflow = Operations.maskedSubtract(temp, subtrahend, 0xFFFFFFFF);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
      copy(minuend, temp);
      overflow = Operations.maskedSubtract(FUZZ_SIZE, temp, subtrahend, 0);
      assertEquals(minuendBig, toBigInteger(temp, overflow));
      
      copy(minuend, temp);
      overflow = Operations.maskedSubtract(FUZZ_SIZE, temp, subtrahend, 0xFFFFFFFF);
      assertEquals(resultBig, toBigInteger(temp, overflow));      
      
      copy(minuend, temp);
      overflow = Operations.maskedSubtract(FUZZ_SIZE, temp, 0, subtrahend, 0, 0);
      assertEquals(minuendBig, toBigInteger(temp, overflow));      
      
      copy(minuend, temp);
      overflow = Operations.maskedSubtract(FUZZ_SIZE, temp, 0, subtrahend, 0, 0xFFFFFFFF);
      assertEquals(resultBig, toBigInteger(temp, overflow));
    }
  }

  @Test
  public void fuzzTestMultiply() {
    int[] multiplicand = new int[FUZZ_SIZE];
    int[] multiplier = new int[FUZZ_SIZE];
    int[] result = new int[2 * FUZZ_SIZE];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      BigInteger multiplicandBig = toBigInteger(randomInteger(multiplicand));
      BigInteger multiplierBig = toBigInteger(randomInteger(multiplier));
      BigInteger resultBig = multiplicandBig.multiply(multiplierBig);
      
      Operations.multiply(multiplicand, multiplier, result);
      assertEquals(resultBig, toBigInteger(result));
      
      Operations.multiply(multiplicand, FUZZ_SIZE, multiplier, FUZZ_SIZE, result);
      assertEquals(resultBig, toBigInteger(result));
      
      Operations.multiply(multiplicand, 0, FUZZ_SIZE, multiplier, 0, FUZZ_SIZE, result, 0);
      assertEquals(resultBig, toBigInteger(result));
    }    
  }
  
  @Test
  public void fuzzTestMultiplyAdd() {
    int overflow;
    int multiplierLimb;
    int[] temp = new int[FUZZ_SIZE];
    int[] augend = new int[FUZZ_SIZE];
    int[] multiplicand = new int[FUZZ_SIZE];
    int[] result = new int[FUZZ_SIZE+1];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      multiplierLimb = random.nextInt();
      BigInteger augendBig = toBigInteger(randomInteger(augend));
      BigInteger multiplicandBig = toBigInteger(randomInteger(multiplicand));
      BigInteger resultBig = multiplicandBig
        .multiply(BigInteger.valueOf(multiplierLimb & MASK32))
        .add(augendBig);
      
      copy(augend, temp);
      overflow = Operations.multiplyAdd(temp, multiplicand, multiplierLimb);
      copy(temp, result);
      result[result.length - 1] = overflow;
      assertEquals(resultBig, toBigInteger(result));
      
      copy(augend, temp);
      overflow = Operations.multiplyAdd(FUZZ_SIZE, temp, multiplicand, multiplierLimb);
      copy(temp, result);
      result[result.length - 1] = overflow;
      assertEquals(resultBig, toBigInteger(result));
      
      copy(augend, temp);
      overflow = Operations.multiplyAdd(FUZZ_SIZE, temp, 0, multiplicand, 0, multiplierLimb);
      copy(temp, result);
      result[result.length - 1] = overflow;
      assertEquals(resultBig, toBigInteger(result));
    }
  }
  
  @Test
  public void fuzzTestMultiplySubtract() {
    long overflow;
    int multiplierLimb;
    int[] temp = new int[FUZZ_SIZE];
    int[] minuend = new int[FUZZ_SIZE];
    int[] multiplicand = new int[FUZZ_SIZE];
    for(int i = 0; i < FUZZ_ITERATIONS; ++i) {
      multiplierLimb = random.nextInt();
      BigInteger minuendBig = toBigInteger(randomInteger(minuend));
      BigInteger multiplicandBig = toBigInteger(randomInteger(multiplicand));
      BigInteger resultBig = minuendBig.subtract(
          multiplicandBig.multiply(BigInteger.valueOf(multiplierLimb & MASK32)));
      
      copy(minuend, temp);
      overflow = Operations.multiplySubtract(temp, multiplicand, multiplierLimb);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
      copy(minuend, temp);
      overflow = Operations.multiplySubtract(FUZZ_SIZE, temp, multiplicand, multiplierLimb);
      assertEquals(resultBig, toBigInteger(temp, overflow));
      
      copy(minuend, temp);
      overflow = Operations.multiplySubtract(FUZZ_SIZE, temp, 0, multiplicand, 0, multiplierLimb);
      assertEquals(resultBig, toBigInteger(temp, overflow));
    }
  }
}
