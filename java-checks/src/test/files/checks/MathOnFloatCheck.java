class A {
  
  void myMethod() {
    float a = 16777216.0f;
    float b = 1.0f;
    float c = a + b; // Noncompliant {{Use a "double" or "BigDecimal" instead.}} yields 1.6777216E7 not 1.6777217E7 [[sc=15;ec=20]]

    double d1 = a + b; // Noncompliant ; addition is still between 2 floats
    double d2 = a - b; // Noncompliant
    double d3 = a * b; // Noncompliant
    double d4 = a / b; // Noncompliant
    double d5 = a / b + b; // Noncompliant, only one issue should be reported [[sc=17;ec=26]]
    
    double d6 = a + d1;
    
    int i = 16777216;
    int j = 1;
    int k = i + j;
    System.out.println("[Max time to retrieve connection:"+(a/1000f/1000f)+" ms.");
    System.out.println("[Max time to retrieve connection:"+a/1000f/1000f);
    float foo = 12 + a/1000f/1000f; // Noncompliant
  }

}
