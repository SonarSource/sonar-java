class A {
  void f() {
    boolean a = true, b = false;
    int d = 0, e = 0;

    if (a)
      d++;

    if (a)
      d++;
    else        // Compliant, not nested
      e++;

    if (a)
      d++;
    else if (b) // Compliant, else if construct
      e++;
    else
      d = e;

    if (a)
      if (b) {
        d++;
      } else {  // Compliant, curly braces
        e++;
      }

    if (a)
      if (b)
        d++;
    else        // Noncompliant {{Add explicit curly braces to avoid dangling else.}}
      e++;
  }
}