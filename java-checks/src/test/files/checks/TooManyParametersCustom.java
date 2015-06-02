class TooManyParameters {
  TooManyParameters(int p1, int p2, int p3, int p4, int p5, int p6) { // Noncompliant {{Constructor has 6 parameters, which is greater than 5 authorized.}}
  }

  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) { // Noncompliant {{Method has 8 parameters, which is greater than 8 authorized.}}
  }
}
