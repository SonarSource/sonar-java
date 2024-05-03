package checks.S2638_ChangeMethodContractCheck.nonNullApi;

class ChangeMethodContractCheck {
  void argAnnotatedCheckForNull(@javax.annotation.CheckForNull Object a) { }

  void argAnnotatedNonNullViaPackageAnnotation(Object a) { }

  String nonNullViaPackageAnnotation(Object a) { return null; }
}

class ChangeMethodContractCheck_Child extends ChangeMethodContractCheck {
  @Override
  void argAnnotatedCheckForNull(Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @NonNullApi at package level to honor @CheckForNull of the overridden method.}}

  @Override
  void argAnnotatedNonNullViaPackageAnnotation(@javax.annotation.CheckForNull Object a) { } // Nonnull to CheckForNull is compliant anyway.

  @Override
  // True positive, the package annotation is correctly detected on the parent.
  @javax.annotation.CheckForNull
  String nonNullViaPackageAnnotation(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @CheckForNull to honor @NonNullApi at package level of the overridden method.}}
}

  @org.eclipse.jdt.annotation.NonNullByDefault
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^>
class ChangeMethodContractCheckAtClassLevel {
  void argAnnotatedNonNullViaClassAnnotation(Object a) { }
}

class ChangeMethodContractCheckAtClassLevel_Child extends ChangeMethodContractCheckAtClassLevel {
  @javax.annotation.Nullable
//^^^^^^^^^^^^^^^^^^^^^^^^^^>
  @Override
  void argAnnotatedNonNullViaClassAnnotation(Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nullable to honor @NonNullByDefault at class level of the overridden method.}}
//^^^^
}
