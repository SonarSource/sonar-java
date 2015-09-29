class A {
  int foo = 0;
  int enum = 0; // Noncompliant {{Use a different name than "enum".}}

  enum mynum {RED, GREEN, YELLOW};

  public void f(
      int a,
      int enum) { // Noncompliant

  }

  public void g(){
    int a;
    int enum; // Noncompliant
  }
}

enum B { // Compliant
  ;
}
