class A {
  int foo = 0;
  int _ = 0; // Noncompliant {{Use a different name than "_".}}
//    ^
  int enum = 0; // Noncompliant {{Use a different name than "enum".}}
//    ^^^^

  public void f(
      int a,
      int _, // Noncompliant
      int enum) { // Noncompliant
  }

  public void g(){
    int a;
    int _; // Noncompliant
    int enum; // Noncompliant
    _ = enum; // should be reported only for declarations
  }
}
