class A {
  void specNonCompliantExamples() {
    for (float i = 16_000_000; i < 17_000_000; i++) { // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
//                                             ^^^
      // ...
    }


    float x = 0f;
    double y = 1.0;

    x++; // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
//  ^^^
    y--; // Noncompliant {{Decrement operator (--) should not be used with floating point variables}}
//  ^^^
  }

  void specCompliantExamples() {
    for (int i = 16_000_000; i < 17_000_000; i++) { // Compliant
      // ...
    }

    float x = 0f;
    double y = 1.0;

    x += 1.0; // Compliant
    y -= 1.0; // Compliant
  }

  void floatIsNotCompliant() {
    float y = 0.1f;
    y++; // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
//  ^^^
    ++y; // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
//  ^^^
    y--; // Noncompliant {{Decrement operator (--) should not be used with floating point variables}}
//  ^^^
    --y; // Noncompliant {{Decrement operator (--) should not be used with floating point variables}}
//  ^^^
  }

  void doubleIsNotCompliant() {
    double y = 0.1;
    y++; // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
//  ^^^
    ++y; // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
//  ^^^
    y--; // Noncompliant {{Decrement operator (--) should not be used with floating point variables}}
//  ^^^
    --y; // Noncompliant {{Decrement operator (--) should not be used with floating point variables}}
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

  // variable shadowing test
  private double d = 3.4;

  void variableShadowingTestCompliant() {
    for (int d = 0; d < 10; d++) { // Compliant
    }
  }
  // variable shadowing test
  private int i = 3;

  void variableShadowingTestCompliant() {
    for (float i = 0; i < 10; i++) { // Noncompliant {{Increment operator (++) should not be used with floating point variables}}
    }
  }
}
