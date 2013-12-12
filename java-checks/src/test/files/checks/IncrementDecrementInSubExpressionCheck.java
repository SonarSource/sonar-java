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
  }

}
