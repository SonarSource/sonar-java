
class A {
  int a = 0;
  int i = 0;

  private void noncompliant() {
    if ( !(a == 2)) { } // Noncompliant {{Use the opposite operator ("!=") instead.}}
//       ^^^^^^^^^
    boolean b = !(i < 10); // Noncompliant {{Use the opposite operator (">=") instead.}}
    b = !(i > 10); // Noncompliant {{Use the opposite operator ("<=") instead.}}
    b = !(i != 10); // Noncompliant {{Use the opposite operator ("==") instead.}}
//      ^^^^^^^^^^
    b = !(i <= 10); // Noncompliant {{Use the opposite operator (">") instead.}}
    b = !(i >= 10); // Noncompliant {{Use the opposite operator ("<") instead.}}
  }

  private void compliant() {
    if (a != 2) { }
    boolean b = (i >= 10);
    b = !(a + i);
  }

}
