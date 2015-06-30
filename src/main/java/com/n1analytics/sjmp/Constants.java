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

public class Constants {
  /**
   * Used to cast ints to longs in an unsigned manner. For example, the integer
   * -1 has twos-complement representation 0xFFFFFFFF, performing the type cast
   * {@code (long)-1} will result in a value of {@code -1L} whereas performing
   * a bitwise and with {@code LONG_MASK} will result in a value of 0xFFFFFFFFL.
   */
  public static final long MASK32 = 0xFFFFFFFFL;
}
