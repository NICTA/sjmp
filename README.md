Secure Java Multiple-Precision (SJMP) Library
=============================================

SJMP aims to provide a suite of multiple-precision arithmetic operations
suitable for implementing cryptographic software. To this end, special attention
has been paid to ensuring that the operations are "constant-time" in the sense
that the execution time is not dependent on the values of the operands (although
it might be dependent on the lengths of the operands). As a secondary
consideration, some effort has been expended to make the operations as efficient
as reasonably possible.

Integer representation
----------------------

The library represents non-negative integers as little-endian arrays of Java
`int`s. Elements of these arrays are interpreted and operated on as unsigned
integers by using a number of techniques to bypass Java's lack of a native
unsigned integer type. The value then of such an array `int[] x` is given by:

    x[0] + x[1] * 2^32 + ... + x[x.length - 1] * 2^(32*(length-1))

Various operations may also return/set an additional `int` or `long` that
typically represents some form of "overflow" that does not fit in the result
array. This value is often, but not always, interpreted as being signed.

Because java makes it difficult to obtain references to sub-arrays (i.e. without
copy content), we typically have to explicitly denote the ranges we wish to
operate on. Most arithmetic operations thus have several variants which obey
the following conventions:

* If just the operand arrays are provided they are considered and used in their
  entirety.
* If the operand arrays and one or more length parameters are provided,
  operations will be performed on the sub-arrays `x[0], ..., x[xLength - 1]` for
  each operand `x` and each associated length `xLength`.
* If the operand arrays are joined by one or more length parameters and one or
  more offset parameters then the operations will be performed on the sub-arrays
  `x[xOffset], ..., x[xOffset + xLength - 1]` for each operand `x` and
  associated length `xLength` and offset `xOffset`.

Constant-time arithmetic
------------------------

Due to the layers below the code (JVM intepreter, JIT Compile, OS, CPU, etc.) we
can never be certain that our implementation will satisfy the desired
properties. That being said, a best-effort attempt has been made to implement
constant-time arithmetic by avoiding data-dependent branching and memory access.
Algorithm selection and appropriate masking of parameters plays a key role in
achieving this.

Error handling
--------------

Error handling/detection in the primitive operations is implemented primarily
via assertions rather than exceptions. This decision was made in the light of
the desire for high-performance. There is nothing to stop the operations from
being wrapped at a higher level that also provides a catalouge of exceptions for
specific failure cases.

Implemented operations
----------------------

The `Modulus` class
-------------------

The modulus class represent an odd, normalised modulus. That is to say, the
least significant bit of the least significant limb of the modulus must be set
as must the most significant bit of the most significant limb. These constraints
make the underlying algorithms much easier to implement and do not limit any of
the current cryptographic uses of this class.

Modular exponentiation
----------------------

Exponentiation is performed using windowed Montgomery multiplication. Currently
the window size is set to a constant 4 bits of exponent per iteration. To
calculate `b^e mod m` we calculate the first 16 powers of `b`:
`b^0, b^1, ..., b^15` in the Montgomery domain. We then iterate, consuming 4
bits of exponent at a time, from the most to least significant bits. Each
iteration then involves squaring the current value 4 times, indexing the table
of pre-computed Montgomery powers by the exponent bits and performing a
Montgomery multiplication between the two. Once we have exhausted all exponent
bits we transform the value out of the Montgomery domain.
