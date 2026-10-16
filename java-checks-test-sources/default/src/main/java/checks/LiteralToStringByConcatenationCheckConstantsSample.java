package checks;

class LiteralToStringByConcatenationCheckConstantsSample {

  static final char LETTER = 'a';
  static final int NUMBER = 97;

  static final String LEADING = LETTER + ("" + 1); // Noncompliant {{Replace this concatenation with the string literal "a1".}}
//                              ^^^^^^^^^^^^^^^^^
  static final String TRAILING = "" + 1 + LETTER; // Noncompliant {{Replace this concatenation with the string literal "1a".}}
//                               ^^^^^^^^^^^^^^^
  static final String MEMBER = "" + 1 + Constants.LETTER; // Noncompliant {{Replace this concatenation with the string literal "1b".}}
//                             ^^^^^^^^^^^^^^^^^^^^^^^^^
  static final String MINIMUM = "" + 1 + Character.MIN_VALUE; // Noncompliant {{Replace this concatenation with the string literal "1\u0000".}}
//                              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  static final String NUMERIC = NUMBER + ("" + 1); // Noncompliant {{Replace this concatenation with the string literal "971".}}
//                              ^^^^^^^^^^^^^^^^^
  static final String ARITHMETIC = (LETTER + 1) + ("" + 1); // Noncompliant {{Replace this concatenation with the string literal "981".}}
//                                 ^^^^^^^^^^^^^^^^^^^^^^^

  interface Constants {
    char LETTER = 'b';
  }
}
