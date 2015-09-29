class A {

  public void f() {
    StringBuffer foo = new java.lang.StringBuffer('x');  // Noncompliant {{Replace the constructor character parameter 'x' with string parameter "x".}}
    StringBuilder foo = new StringBuilder('x');          // Noncompliant {{Replace the constructor character parameter 'x' with string parameter "x".}}

    StringBuffer foo = new StringBuffer("x");            // OK
    StringBuilder foo = new StringBuilder("x");          // OK
  }
}
