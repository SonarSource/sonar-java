package checks;

class IncompatibleBitMaskCheckSample {

  // When semantic info is unavailable, the rule uses a heuristic based on literal kinds
  // to decide if the operation is int or long.

  void noSemanticWithIntLiterals(UnknownType obj) {
    // No long literals present: heuristic treats as int operation
    if ((obj.getValue() & 0x0F) == 0x10) {} // Noncompliant {{This comparison is always false.}}
    if ((obj.getValue() | 3) == 2) {} // Noncompliant {{This comparison is always false.}}
    if ((obj.getValue() & 0x0F) == 0x04) {} // Compliant
    if ((obj.getValue() & 1) != 2) {} // Noncompliant {{This comparison is always true.}}
  }

  void noSemanticWithLongLiterals(UnknownType obj) {
    // Long literal in mask: heuristic treats as long operation
    if ((obj.getValue() & 0xFFL) == 0x100L) {} // Noncompliant {{This comparison is always false.}}
    if ((obj.getValue() & 0xFFL) == 0x80L) {} // Compliant
  }

  void noSemanticWithNegativeLongLiteral(UnknownType obj) {
    // Negative long literal (unary minus on LONG_LITERAL) exercises hasLongLiteral unary path
    if ((obj.getValue() & -1L) == 0x100L) {} // Compliant - mask is -1L (all bits), any value is reachable
    if ((obj.getValue() | -1L) == 0L) {} // Noncompliant {{This comparison is always false.}}
  }

  void noSemanticWithPositiveLongLiteral(UnknownType obj) {
    // Positive unary plus on long literal exercises hasLongLiteral unary path
    if ((obj.getValue() & +1L) == 2L) {} // Noncompliant {{This comparison is always false.}}
  }

  void noSemanticLongLiteralInComparisonValue(UnknownType obj) {
    // Long literal only in the comparison value, not in the mask
    if ((obj.getValue() & 0x0F) == 0x100L) {} // Noncompliant {{This comparison is always false.}}
  }

  void noSemanticLongLiteralAsLeftOperand(UnknownType obj) {
    // Long literal as left operand of bitwise operation: hasLongLiteral(leftOperand) returns true
    if ((0xFFL & obj.getValue()) == 0x100L) {} // Noncompliant {{This comparison is always false.}}
    if ((0xFFL & obj.getValue()) == 0x80L) {} // Compliant
  }
}
