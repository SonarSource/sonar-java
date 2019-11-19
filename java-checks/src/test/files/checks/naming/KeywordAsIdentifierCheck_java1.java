class A {
  int a = 0;
  int assert = 0; // Noncompliant [[sc=7;ec=13]] {{Use a different name than "assert".}}

  public void f(
    int a,
    int assert) { // Noncompliant
  }

  public void g(){
    int a;
    int assert;  // Noncompliant
    a = assert; // should be reported only for declarations
  }
}
