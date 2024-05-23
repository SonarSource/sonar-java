class A {

  boolean cond = Math.random() < 0.5d;
  private void f() {
    Object a = null;
    if (cond) a.toString(); // Noncompliant
    nested();
    a.toString(); // Noncompliant
  }

  private void nested() {
    Object a = null;
    if (cond) a.toString(); // Noncompliant
  }

}
