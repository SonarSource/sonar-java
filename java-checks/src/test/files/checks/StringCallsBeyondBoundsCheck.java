class A {
  void f(String aString) {
    String str = "Hello, world!";
    char[] array = new char[str.length()];
    int zero = 0, minusOne = -1, any = System.currentTimeMillis();

    "".charAt("".length());
    "".charAt("".length() - 1);
    "".charAt(0);               // Noncompliant
    "".charAt(-1);              // Noncompliant
    "".charAt(minusOne);

    str.charAt(str.hashCode());
    str.charAt(str.length());   // Noncompliant
    str.charAt(str.length() - 1);
    str.charAt(0);
    str.charAt(-1);             // Noncompliant
    str.charAt(minusOne);
    
    aString.charAt(aString.length()); // Noncompliant

    str.codePointAt(str.length());   // Noncompliant
    str.codePointAt(str.length() - 1);
    str.codePointAt(0);
    "str".codePointAt(0);
    str.codePointAt(-1);             // Noncompliant
    str.codePointAt(minusOne);

    aString.codePointAt(aString.length()); // Noncompliant

    str.codePointCount(0, str.length());
    str.codePointCount(0, str.length() - 1);
    str.codePointCount(-1, str.length());        // Noncompliant
    str.codePointCount(minusOne, str.length());
    str.codePointCount(5, 0);                    // Noncompliant
    str.codePointCount(0, 25);
    "str".codePointCount(0, 25);                 // Noncompliant

    str.codePointBefore(str.length());
    str.codePointBefore(25);
    "str".codePointBefore(25);          // Noncompliant
    str.codePointBefore(1);
    "str".codePointBefore(1);
    str.codePointBefore(0);             // Noncompliant
    str.codePointBefore(zero);

    str.getChars(0, str.length(), array, 0);
    str.getChars(-1, str.length(), array, 0);       // Noncompliant
    str.getChars(minusOne, str.length(), array, 0);
    str.getChars(5, 0, array, 0);                   // Noncompliant
    str.getChars(0, 25, array, 0);
    "str".getChars(0, 25, array, 0);                // Noncompliant
    "str".getChars(0, 2, array, 0);
    str.getChars(0, str.length(), array, -1);       // Noncompliant
    str.getChars(0, str.length(), array, minusOne);

    str.offsetByCodePoints(str.length(), any);
    str.offsetByCodePoints(25, any);
    str.offsetByCodePoints(0, any);
    "str".offsetByCodePoints(any, any + 5);
    "str".offsetByCodePoints(0, any);
    "str".offsetByCodePoints(25, any);          // Noncompliant
    str.offsetByCodePoints(-1, any);            // Noncompliant
    str.offsetByCodePoints(minusOne, any);

    str.subSequence(0, str.length());
    str.subSequence(-1, str.length());         // Noncompliant
    str.subSequence(minusOne, str.length());
    str.subSequence(0, -1);                    // Noncompliant
    str.subSequence(5, 0);                     // Noncompliant
    str.subSequence(0, 25);
    "str".subSequence(0, 25);                  // Noncompliant
    "str".subSequence(0, 2);
    "str".subSequence(minusOne, str.length());

    str.substring(str.length());
    str.substring(25);
    "str".substring(25);          // Noncompliant
    "str".substring(1);
    str.substring(0);
    str.substring(-1);            // Noncompliant
    str.substring(minusOne);

    str.substring(0, str.length());
    str.substring(-1, str.length());        // Noncompliant
    str.substring(minusOne, str.length());
    str.substring(0, -1);                   // Noncompliant
    str.substring(5, 0);                    // Noncompliant
    str.substring(0, 25);
    "str".substring(0, 25);                 // Noncompliant
  }
}
