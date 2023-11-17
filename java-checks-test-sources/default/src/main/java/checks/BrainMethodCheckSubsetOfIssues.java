package checks;

//The test unit is set to report the first brain method issue + the 10% of the total brain method issues found,
//ordered by general complexity.
//In this file there are 10 issues found, so a total of 1 + (10% of 10 = 1) = 2 issues will be raised
class BrainMethodCheckSubsetOfIssues {

  void method1(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  
  void method2(String a, String b) { // Compliant: this will be skipped since the following method is more complex
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method3(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println("This method will be reported over the previous one");
      System.out.println("Because it is more complex");
      System.out.println(a + b);
    }
  }

  void method4(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method5(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }
  
  void method6(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }
  
  void method7(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }
  
  void method8(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }
  
  void method9(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }
  
  void method10(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }
  
  void compliantMethod() {
    System.out.println("Hello world");
  }

}
