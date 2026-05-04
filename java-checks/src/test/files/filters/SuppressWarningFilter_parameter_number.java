// ParameterNumber suppresses S107

@SuppressWarnings("ParameterNumber")
class ParameterNumberClassLevel {
  ParameterNumberClassLevel(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // NoIssue
  }

  void f(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // NoIssue
  }
}

class ParameterNumberMethodLevel {
  @SuppressWarnings("ParameterNumber")
  ParameterNumberMethodLevel(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // NoIssue
  }

  @SuppressWarnings("ParameterNumber")
  void f(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // NoIssue
  }

  void g(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // WithIssue
  }
}

class ParameterNumberArrayForm {
  @SuppressWarnings({"ParameterNumber", "unchecked"})
  void f(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // NoIssue
  }
}
