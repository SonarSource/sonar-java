class Foo {

  public void f() {
    i++; // Compliant
    ++i; // Compliant
    i--; // Compliant
    --i; // Compliant

    foo[i]++; // Compliant

    foo[i++] = 0; // Noncompliant
    foo[i--] = 0; // Noncompliant
    foo[++i] = 0; // Noncompliant
    foo[--i] = 0; // Noncompliant

    foo[~i] = 0; // Compliant

    return i++; // Compliant
    return ++i; // Compliant
    return foo[++i]; // NonCompliant
    return;
  }

}
