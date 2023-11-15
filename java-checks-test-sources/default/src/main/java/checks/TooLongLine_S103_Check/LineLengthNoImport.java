package checks.TooLongLine_S103_Check;

class LineLengthNoImport {
  void method() {
    // Noncompliant {{Split this 97 characters long line (which is greater than 40 authorized).}}
    // Noncompliant {{Split this 97 characters long line (which is greater than 40 authorized).}}
  }
}
