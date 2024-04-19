package checks;

class StringBufferAndBuilderWithCharCheckSample {

  public void f() {
    StringBuffer foo = new java.lang.StringBuffer('x');  // Noncompliant [[sc=51;ec=54]] {{Replace the constructor character parameter 'x' with string parameter "x".}}
    StringBuilder foo2 = new StringBuilder('x');          // Noncompliant {{Replace the constructor character parameter 'x' with string parameter "x".}}

    StringBuffer foo3 = new StringBuffer("x");            // OK
    StringBuilder foo4 = new StringBuilder("x");          // OK
  }
}
