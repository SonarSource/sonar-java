package checks.naming;

class BadConstantNameNoIssueWithoutSemantic {

  // Without semantic analysis, isConstantType() returns false for non-primitive types,
  // so no issues are raised for static final Object fields.
  public static final Object bad_constant = null;

}
