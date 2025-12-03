class A {
  void specExample() {
    for (float x = 16_000_000; x < 17_000_000; x++) { // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//                                             ^^^
      // ...
    }
  }
  void floatIsNotCompliant() {
    float y = 0.1f;
    y++; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
    ++y; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
    y--; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
    --y; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
  }

  void doubleIsNotCompliant() {
    double y = 0.1;
    y++; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
    ++y; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
    y--; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
    --y; // Noncompliant {{Increment and decrement operators (++/--) should not be used with floating point variables}}
//  ^^^
  }

  void intIsCompliant() {
    int z = 0;
    z++; // Compliant
    ++z; // Compliant
    z--; // Compliant
    --z; // Compliant
  }

  void longIsCompliant() {
    long w = 0L;
    w++; // Compliant
    ++w; // Compliant
    w--; // Compliant
    --w; // Compliant
  }

  void charIsCompliant() {
    char c = 'a';
    c++; // Compliant
    ++c; // Compliant
    c--; // Compliant
    --c; // Compliant
  }

  void otherOperatorsNotAffected() {
    float f = 1f;
    // test unary operator -
    float g = -f; // compliant
    // test binary operators just in case
    float h = f + 1f; // compliant
  }
}
