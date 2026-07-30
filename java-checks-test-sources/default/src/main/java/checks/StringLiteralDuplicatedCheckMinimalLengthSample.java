package checks;

/**
 * Verifies that S1192 respects the minimalLength rule property.
 * With minimalLength=3, strings of 3+ chars are considered; shorter ones are ignored.
 */
class StringLiteralDuplicatedCheckMinimalLengthSample {
  void test() {
    System.out.println("abc"); // Noncompliant {{Define a constant instead of duplicating this literal "abc" 3 times.}}
//                     ^^^^^
    System.out.println("abc");
//                     ^^^^^<
    System.out.println("abc");
//                     ^^^^^<
    System.out.println("xy"); // Compliant — 2 chars, below minimalLength of 3
    System.out.println("xy");
    System.out.println("xy");
  }
}
