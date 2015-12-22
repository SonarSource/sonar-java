
class A {
  int a = 0;
  int i = 0;

  private void noncompliant() {
    if ( !(a == 2)) { }  // Noncompliant {{Use the opposite operator ("!=") instead.}} [[sc=10;ec=19]]
    boolean b = !(i < 10);  // Noncompliant {{Use the opposite operator (">=") instead.}}
    b = !(i > 10);  // Noncompliant {{Use the opposite operator ("<=") instead.}}
    b = !(i != 10);  // Noncompliant {{Use the opposite operator ("==") instead.}} [[sc=9;ec=19]]
    b = !(i <= 10);  // Noncompliant {{Use the opposite operator (">") instead.}}
    b = !(i >= 10);  // Noncompliant {{Use the opposite operator ("<") instead.}}
  }

  private void compliant() {
    if (a != 2) { }
    boolean b = (i >= 10);
    b = !(a + i);
  }

}
