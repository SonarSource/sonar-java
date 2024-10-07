class A {
  int foo = 0;
  int _ = 0; // Noncompliant {{Use a different name than "_".}}
//    ^

  public void f(
      int a,
      int _) { // Noncompliant
  }

  public void g(){
    int a;
    int _; // Noncompliant
  }
}
