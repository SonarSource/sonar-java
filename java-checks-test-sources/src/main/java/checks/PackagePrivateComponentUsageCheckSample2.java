package checks;

class NonIssueClass2{}
class NonIssueClass3{}

class NonIssueClass {
  String issueVar; // Noncompliant {{This variable is package private but is never used within the package}}
  static String nonIssueVar;

  void issueMethod() { // Noncompliant {{This method is package private but is never used within the package}}
  }
  static void nonIssueMethod() { }
  void nonIssueMethod2() {}
  String nonIssueMethod3() {return "";} // Noncompliant TODO: False Positive, not catching stream.map(NonIssueClass::nonIssueMethod3)
}
