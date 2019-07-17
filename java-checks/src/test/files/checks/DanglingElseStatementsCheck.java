class A {
  void f() {
    boolean a = true, b = false;
    int d = 0, e = 0;

    if (a)
      d++;        // Compliant, missing else clause

    if (a)
      d++;
    else          // Compliant, not nested
      e++;

    if (a)
      d++;
    else if (b)   // Compliant, else if construct
      e++;
    else
      d = e;

    if (a)
      d++;
    else if (b)   // Compliant, else if constructs
      e++;
    else if (d > e)
      e = d;
    else
      d = e;

    if (a) {
      if (b)
        d++;
      else        // Compliant, expected fix
        e++;
    }

    if (a) {
      if (b) {
        d++;
      } else {    // Compliant, perfect style
        e++;
      }
    }

    if (a)
      if (b)
        d++;
    else          // Noncompliant {{Add explicit curly braces to avoid dangling else.}}
      e++;

    if (a || b)
      if (a)
        d++;
      else if (b) // Noncompliant
        e++;
      else if (d > e)
        e = d;
      else
        d = e;

    if (a || b) {
      if (a)
        d++;
      else if (b) // Compliant
        e++;
      else if (d > e)
        e = d;
      else
        d = e;
    }
  }
}
