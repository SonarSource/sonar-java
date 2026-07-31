package checks;

/**
 * Verifies that S1192 respects the excludePatterns rule property.
 * Strings matching the configured pattern are excluded from duplicate detection.
 */
class StringLiteralDuplicatedCheckExcludePatternsSample {
  void test() {
    System.out.println("excluded string"); // Compliant — matches excludePatterns
    System.out.println("excluded string");
    System.out.println("excluded string");
    System.out.println("included string"); // Noncompliant {{Define a constant instead of duplicating this literal "included string" 3 times.}}
//                     ^^^^^^^^^^^^^^^^^
    System.out.println("included string");
//                     ^^^^^^^^^^^^^^^^^<
    System.out.println("included string");
//                     ^^^^^^^^^^^^^^^^^<
  }
}