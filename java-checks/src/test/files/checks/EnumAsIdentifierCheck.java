class A {
  int foo = 0;
  int enum = 0; // Non-Compliant

  enum mynum {RED, GREEN, YELLOW}; // Compliant

  public void f(
      int a,
      int enum) { // Non-Compliant

  }

  public void g(){
    int a;
    int enum;
  }
}

enum B { // Compliant
  ;
}
