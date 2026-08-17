package checks;

class IncompatibleBitMaskCheckSample {

  void bitwiseAndNoncompliant(int x, long status) {
    // AND mask 1 (0b01) cannot produce 2 (0b10)
    if ((x & 1) == 2) {} // Noncompliant {{This comparison is always false.}}
    //         ^^

    // AND mask 0x0F cannot produce 0x10
    if ((x & 0x0F) == 0x10) {} // Noncompliant {{This comparison is always false.}}

    // AND mask 3 (0b11) cannot produce 4 (0b100)
    if ((x & 3) == 4) {} // Noncompliant {{This comparison is always false.}}

    // != with incompatible values is always true
    if ((x & 1) != 2) {} // Noncompliant {{This comparison is always true.}}

    // Long variant: AND mask 0xFF cannot produce 0x100
    if ((status & 0xFFL) == 0x100L) {} // Noncompliant {{This comparison is always false.}}
  }

  void bitwiseOrNoncompliant(int x, long data) {
    // OR with 1 always sets bit 0, result can never be 0
    if ((x | 1) == 0) {} // Noncompliant {{This comparison is always false.}}

    // OR with 3 always sets bits 0 and 1, result can never be 2 (missing bit 0)
    if ((x | 3) == 2) {} // Noncompliant {{This comparison is always false.}}

    // != with incompatible OR is always true
    if ((x | 2) != 1) {} // Noncompliant {{This comparison is always true.}}

    // Long variant: OR with 0xFF always sets low 8 bits
    if ((data | 0xFFL) == 0L) {} // Noncompliant {{This comparison is always false.}}
  }

  void bitwiseAndCompliant(int x, long status) {
    // Mask 2 can produce 2
    if ((x & 2) == 2) {} // Compliant

    // 0x04 is within mask 0x0F
    if ((x & 0x0F) == 0x04) {} // Compliant

    // Comparing AND result to 0 is always valid
    if ((x & 1) == 0) {} // Compliant

    // 3 is within mask 7
    if ((x & 7) == 3) {} // Compliant

    // 2 is reachable by AND with 3
    if ((x & 3) != 2) {} // Compliant

    // Long: 0x80 is within mask 0xFF
    if ((status & 0xFFL) == 0x80L) {} // Compliant
  }

  void bitwiseOrCompliant(int x, long data) {
    // 1 includes all mask bits
    if ((x | 1) == 1) {} // Compliant

    // 3 includes all mask bits
    if ((x | 3) == 3) {} // Compliant

    // 0xFF includes all mask bits 0x0F
    if ((x | 0x0F) == 0xFF) {} // Compliant

    // 3 includes mask bit 2, comparison is meaningful
    if ((x | 2) != 3) {} // Compliant

    // Value includes all mask bits
    if ((data | 0xFFL) == 0xFFL) {} // Compliant
  }

  void maskOnLeftSide(int x) {
    // Mask on left side of bitwise operation
    if ((1 & x) == 2) {} // Noncompliant {{This comparison is always false.}}

    if ((1 & x) == 0) {} // Compliant
  }

  void constantOnLeftSideOfComparison(int x) {
    // Constant on left side of comparison
    if (2 == (x & 1)) {} // Noncompliant {{This comparison is always false.}}

    if (0 == (x & 1)) {} // Compliant
  }

  void hexAndBinaryLiterals(int x) {
    // Hex literals
    if ((x & 0xFF) == 0x100) {} // Noncompliant {{This comparison is always false.}}

    // Binary literals
    if ((x & 0b1111) == 0b10000) {} // Noncompliant {{This comparison is always false.}}

    if ((x & 0b1111) == 0b1010) {} // Compliant
  }

  void edgeCases(int x) {
    // Mask of 0: AND with 0 always produces 0
    if ((x & 0) == 1) {} // Noncompliant {{This comparison is always false.}}
    if ((x & 0) == 0) {} // Compliant

    // OR with 0: result is x, any comparison is meaningful
    if ((x | 0) == 5) {} // Compliant
  }

  void noConstantOperands(int x, int y, int z) {
    // No constant mask - no issue
    if ((x & y) == 2) {} // Compliant

    // No constant comparison value - no issue
    if ((x & 1) == y) {} // Compliant

    // No constant at all
    if ((x & y) == z) {} // Compliant
  }

  void notBitwiseOperations(int x) {
    // XOR is not covered
    if ((x ^ 1) == 2) {} // Compliant

    // Regular comparison without bitwise
    if (x == 2) {} // Compliant

    // Addition, not bitwise
    if ((x + 1) == 2) {} // Compliant
  }

  void parenthesizedExpressions(int x) {
    // Extra parentheses around bitwise operation
    if (((x & 1)) == 2) {} // Noncompliant {{This comparison is always false.}}

    if (((x & 2)) == 2) {} // Compliant
  }
}
