class A {

  public void f() {
    StringBuffer foo = new java.lang.StringBuffer('x');  // NOK
    StringBuilder foo = new StringBuilder('x');          // NOK

    StringBuffer foo = new StringBuffer("x");            // OK
    StringBuilder foo = new StringBuilder("x");          // OK
  }
}
