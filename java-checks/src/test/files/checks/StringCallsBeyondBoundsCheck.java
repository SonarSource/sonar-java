class A {
  void f(String aString) {
    String str = "Hello, world!";
    char[] array = new char[str.length()];
    int zero = 0, minusOne = -1, any = System.currentTimeMillis();

    "".charAt("".length());     // Noncompliant {{Refactor this "charAt" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    "".charAt("".length() - 1);
    "".charAt(0);               // Noncompliant
    "".charAt(-1);              // Noncompliant
    "".charAt(minusOne);        // Noncompliant

    str.charAt(str.hashCode());
    str.charAt(str.length());   // Noncompliant
    str.charAt(str.length() - 1);
    str.charAt(0);
    str.charAt(-1);             // Noncompliant
    str.charAt(minusOne);       // Noncompliant
    
    aString.charAt(aString.length()); // Noncompliant

    str.codePointAt(str.length());   // Noncompliant {{Refactor this "codePointAt" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.codePointAt(str.length() - 1);
    str.codePointAt(0);
    str.codePointAt(-1);             // Noncompliant
    str.codePointAt(minusOne);       // Noncompliant

    aString.codePointAt(aString.length()); // Noncompliant

    str.codePointCount(0, str.length());
    str.codePointCount(0, str.length() - 1);
    str.codePointCount(-1, str.length());        // Noncompliant {{Refactor this "codePointCount" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.codePointCount(minusOne, str.length());  // Noncompliant
    str.codePointCount(5, 0);                    // Noncompliant
    str.codePointCount(0, 25);                   // Noncompliant

    str.codePointBefore(str.length());
    str.codePointBefore(25);            // Noncompliant {{Refactor this "codePointBefore" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.codePointBefore(1);
    str.codePointBefore(0);             // Noncompliant
    str.codePointBefore(zero);          // Noncompliant

    str.getChars(0, str.length(), array, 0);
    str.getChars(-1, str.length(), array, 0);       // Noncompliant {{Refactor this "getChars" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.getChars(minusOne, str.length(), array, 0); // Noncompliant
    str.getChars(5, 0, array, 0);                   // Noncompliant
    str.getChars(0, 25, array, 0);                  // Noncompliant
    str.getChars(0, str.length(), array, -1);       // Noncompliant
    str.getChars(0, str.length(), array, minusOne); // Noncompliant

    str.offsetByCodePoints(str.length(), any);
    str.offsetByCodePoints(25, any);            // Noncompliant {{Refactor this "offsetByCodePoints" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.offsetByCodePoints(0, any);
    str.offsetByCodePoints(-1, any);            // Noncompliant
    str.offsetByCodePoints(minusOne, any);      // Noncompliant

    str.subSequence(0, str.length());
    str.subSequence(-1, str.length());        // Noncompliant {{Refactor this "subSequence" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.subSequence(minusOne, str.length());  // Noncompliant
    str.subSequence(0, -1);                   // Noncompliant
    str.subSequence(5, 0);                    // Noncompliant
    str.subSequence(0, 25);                   // Noncompliant

    str.substring(str.length());
    str.substring(25);            // Noncompliant {{Refactor this "substring" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.substring(0);
    str.substring(-1);            // Noncompliant
    str.substring(minusOne);      // Noncompliant

    str.substring(0, str.length());
    str.substring(-1, str.length());        // Noncompliant {{Refactor this "substring" call; it will result in an "StringIndexOutOfBounds" exception at runtime.}}
    str.substring(minusOne, str.length());  // Noncompliant
    str.substring(0, -1);                   // Noncompliant
    str.substring(5, 0);                    // Noncompliant
    str.substring(0, 25);                   // Noncompliant
  }
}
