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
  }

  void testCharacterLiteral() {
    char c1 = '\12'; // Compliant
    char c2 = '\1'; // Compliant
  }
}
