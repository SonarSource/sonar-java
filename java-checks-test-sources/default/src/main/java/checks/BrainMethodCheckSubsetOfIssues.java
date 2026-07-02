package checks;

// With numberOfFoundIssuesThreshold=5, issuesToReportPercentage=10, numberOfIssuesPerModuleThreshold=20:
// 10 issues found, numberOfFoundIssuesThreshold(5) < 10 < numberOfIssuesPerModuleThreshold(20).
// Issues to report = min(10 * 10 / 100, 20) = min(1, 20) = 1.
// The most complex method (highest brain score) is reported.
class BrainMethodCheckSubsetOfIssues {

  void method1(String a, String b) { // Compliant: this will be skipped since the following method is more complex
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
