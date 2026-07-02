package checks;

// With numberOfFoundIssuesThreshold=5, issuesToReportPercentage=10, numberOfIssuesPerModuleThreshold=20:
// 21 issues found > numberOfIssuesPerModuleThreshold=20.
// Issues to report = min(21 * 10 / 100, 20) = min(2, 20) = 2.
// The 2 most complex methods (highest brain score) are reported.
class BrainMethodCheckSubsetLarge {

  void method1(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println("This is the most complex method");
      System.out.println("It has the most lines");
      System.out.println("To ensure it has the highest brain score");
      System.out.println("And will be the first to be reported");
      System.out.println(a + b);
    }
  }

  void method2(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println("This method is reported second");
      System.out.println("It has more lines than the simple methods below");
      System.out.println("But fewer than method1");
      System.out.println(a + b);
    }
  }

  void method3(String a, String b) { // Compliant
    if (a != null) {
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

  void method11(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method12(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method13(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method14(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method15(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method16(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method17(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method18(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method19(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method20(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method21(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

}