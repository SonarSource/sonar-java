package checks.S2638_ChangeMethodContractCheck.nonNullApi;

class ChangeMethodContractCheck {
  void argAnnotatedCheckForNull(@javax.annotation.CheckForNull Object a) { }

  void argAnnotatedNonNullViaPackageAnnotation(Object a) { }

  String nonNullViaPackageAnnotation(Object a) { return null; }
}

class ChangeMethodContractCheck_Child extends ChangeMethodContractCheck {
  @Override
  void argAnnotatedCheckForNull(Object a) { } // FN, package annotated with NonNull, should raise an issue.

  @Override
  void argAnnotatedNonNullViaPackageAnnotation(@javax.annotation.CheckForNull Object a) { } // Nonnull to CheckForNull is compliant anyway.

  @Override
  // True positive, the package annotation is correctly detected on the parent.
  @javax.annotation.CheckForNull // Noncompliant {{Remove this "CheckForNull" annotation to honor the overridden method's contract.}}
  String nonNullViaPackageAnnotation(Object a) { return null; }
}
