package checks;

// With numberOfFoundIssuesThreshold=5, issuesToReportPercentage=10, numberOfAdditionalIssuesThreshold=5:
// 60 issues found, 10% = 6 > numberOfAdditionalIssuesThreshold=5.
// Issues to report = numberOfFoundIssuesThreshold + min(60 * 10 / 100, 5) = 5 + min(6, 5) = 10.
// The 10 most complex methods (highest brain score) are reported.
class BrainMethodCheckSubsetCapped {

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
      System.out.println("Second most complex");
      System.out.println("Has four extra lines");
      System.out.println("More than simple methods");
      System.out.println(a + b);
    }
  }

  void method3(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println("Third most complex");
      System.out.println("Has three extra lines");
      System.out.println(a + b);
    }
  }

  void method4(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println("Fourth most complex");
      System.out.println(a + b);
    }
  }

  void method5(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println("Fifth most complex - last to be reported");
      System.out.println(a + b);
    }
  }

  void method6(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method7(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method8(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method9(String a, String b) { // Noncompliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method10(String a, String b) { // Noncompliant
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

  void method22(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method23(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method24(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method25(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method26(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method27(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method28(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method29(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method30(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method31(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method32(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method33(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method34(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method35(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method36(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method37(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method38(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method39(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method40(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method41(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method42(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method43(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method44(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method45(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method46(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method47(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method48(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method49(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method50(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method51(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method52(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method53(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method54(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method55(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method56(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method57(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method58(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method59(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

  void method60(String a, String b) { // Compliant
    if (a != null) {
      System.out.println(a + b);
    }
  }

}