class A {
  int foo = 0;
  int enum = 0; // Noncompliant [[sc=7;ec=11]] {{Use a different name than "enum".}}

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

class Underscore {

  void f() {
    // underscore will become a keyword in Java 9
    String _ = ""; // Noncompliant [[sc=12;ec=13]] {{Use a different name than "_".}}
  }

  void lambda() {
    // usage as lambda identifier is a compilation error already in Java 8
    IntStream.range(0, 10).forEach(_ -> System.out.println(_)); // Noncompliant [[sc=36;ec=37]] {{Use a different name than "_".}}
  }

}
