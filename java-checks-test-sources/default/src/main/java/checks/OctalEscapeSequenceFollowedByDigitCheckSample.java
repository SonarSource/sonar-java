package checks;

class OctalEscapeSequenceFollowedByDigitCheckSample {
  void testNoncompliant() {
    String s1 = "\128"; // Noncompliant {{Remove this octal escape sequence or separate it from the following digit.}}
    String s2 = "\09"; // Noncompliant
    String s3 = "\7778"; // Noncompliant
    String s4 = "\1234"; // Noncompliant
    String s5 = "\789"; // Noncompliant
    String s6 = "\0000"; // Noncompliant
    String s7 = "\7777"; // Noncompliant
    String s8 = "a\128b"; // Noncompliant
    String s9 = "\12\3456"; // Noncompliant
    String s10 = "\456"; // Noncompliant
  }

  void testCompliant() {
    String s1 = "\12"; // Compliant
    String s2 = "\12a"; // Compliant
    String s3 = "\\128"; // Compliant
    String s4 = "128"; // Compliant
    String s5 = "\u0041"; // Compliant
    String s6 = "\n"; // Compliant
    String s7 = "\\08"; // Compliant
    String s8 = "\12" + "8"; // Compliant
    String s9 = "\1\2"; // Compliant
    String s10 = ""; // Compliant - empty string
    String s11 = "\377"; // Compliant - max octal at end of string
    String s12 = "\377a"; // Compliant - max octal followed by non-digit
    String s13 = "\t9"; // Compliant - non-octal escape followed by digit
    String s14 = "\n0"; // Compliant - non-octal escape followed by digit
    String s15 = "\\\\8"; // Compliant - double escaped backslash followed by digit
    String s16 = "\1"; // Compliant - single octal at end
    String s17 = "abc"; // Compliant - no escapes
    String s18 = "\45"; // Compliant
    String s19 = "\45a"; // Compliant
  }

  void testNoncompliantTextBlock() {
    String tb1 = """
      \128"""; // Noncompliant@-1
    String tb2 = """
      \09"""; // Noncompliant@-1
  }

  void testCompliantTextBlock() {
    String tb1 = """
      \12""";
    String tb2 = """
      \12a""";
    String tb3 = """
      \\128""";
    String tb4 = """
      \n0""";
  }

  void testCharacterLiteral() {
    char c1 = '\12'; // Compliant
    char c2 = '\1'; // Compliant
  }

  void testNoncompliantMaxOctal() {
    String s1 = "\3778"; // Noncompliant
  }
}
