package checks;

class TooManyParametersExtended extends Unknown {


  TooManyParametersExtended(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Compliant, unknown in hierarchy

    super(p1, p2, p3, p4, p5, p6, p7, p8);
  }
  // Compliant, the method isOverriding should return null in this case, because we have no idea if this method is an override or not.
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {}
}

class MethodsUsingSpringGetMapping {
  @Unknown
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant, Unknown annotation
}
