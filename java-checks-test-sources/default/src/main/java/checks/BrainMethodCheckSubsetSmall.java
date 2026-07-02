package checks;

// With numberOfFoundIssuesThreshold=5, 3 issues found is below the threshold, so all are reported.
class BrainMethodCheckSubsetSmall {

  void method1(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method2(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method3(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

}
