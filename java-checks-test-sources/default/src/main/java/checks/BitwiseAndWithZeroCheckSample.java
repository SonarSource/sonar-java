package checks;

class BitwiseAndWithZeroCheckSample {

  private static final int READ_PERMISSION = 0x04;

  int getFlags() {
    return 42;
  }

  void noncompliantPatterns() {
    int flags = getFlags();
    int result;

    // Basic cases
    result = flags & 0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    result = 0 & flags; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Hex zero
    result = flags & 0x0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    result = flags & 0x00; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    result = flags & 0X0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Long zero
    result = (int) (flags & 0L); // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    result = (int) (flags & 0x00L); // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Binary zero
    result = flags & 0b0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    result = flags & 0B0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Octal zero (leading zero)
    result = flags & 00; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Compound assignment
    flags &= 0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    flags &= 0x0; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    flags &= 0L; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Parenthesized zero
    result = flags & (0); // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    result = (0) & flags; // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    flags &= (0); // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}

    // Nested in comparison (issue on the & expression)
    if ((flags & 0) == 0) { } // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
    if ((flags & 0) != 0) { } // Noncompliant {{Remove this bitwise AND with zero; the result is always zero.}}
  }

  void compliantPatterns() {
    int flags = getFlags();
    int mask = 0xFF;
    int result;

    // Non-zero bitmask
    result = flags & 0x01;
    result = flags & 0xFF;
    result = flags & 1;

    // Variable operands
    result = flags & mask;
    result = flags & READ_PERMISSION;

    // Two variables
    int a = 1, b = 2;
    result = a & b;

    // Non-zero compound assignment
    flags &= 0x0F;

    // Different operators (covered by S2437)
    result = flags | 0;
    result = flags ^ 0;

    // Parenthesized non-zero
    result = flags & (0x0F);

    // Long non-zero bitmask
    long longResult = flags & 0xFFL;
    longResult = flags & 0x0FL;
  }
}
