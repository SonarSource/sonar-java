class A {
  
  void myMethod() {
    float a = 16777216.0f;
    float b = 1.0f;
    float c = a + b; // Noncompliant; yields 1.6777216E7 not 1.6777217E7

    double d1 = a + b; // Noncompliant; addition is still between 2 floats
    double d2 = a - b; // Noncompliant
    double d3 = a * b; // Noncompliant
    double d4 = a / b; // Noncompliant
    double d5 = a / b + b; // Noncompliant, only one issue should be reported
    
    double d6 = a + d1;
    
    int i = 16777216;
    int j = 1;
    int k = i + j;
  }
  
}
