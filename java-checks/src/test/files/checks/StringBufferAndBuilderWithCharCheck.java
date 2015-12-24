class A {

  public void f() {
    StringBuffer foo = new java.lang.StringBuffer('x');  // Noncompliant [[sc=51;ec=54]] {{Replace the constructor character parameter 'x' with string parameter "x".}}
    StringBuilder foo = new StringBuilder('x');          // Noncompliant {{Replace the constructor character parameter 'x' with string parameter "x".}}

    StringBuffer foo = new StringBuffer("x");            // OK
    StringBuilder foo = new StringBuilder("x");          // OK
  }
}
