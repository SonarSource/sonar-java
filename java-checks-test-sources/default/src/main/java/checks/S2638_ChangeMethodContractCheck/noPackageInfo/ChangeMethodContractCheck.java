package checks.S2638_ChangeMethodContractCheck.noPackageInfo;

import javax.annotation.meta.When;

/**
 * For parameters:
 * Weak/Strong Nullable to Weak/Strong Nullable  -> OK
 * NonNull to Weak/Strong Nullable               -> OK
 * Weak/Strong Nullable to NonNull               -> NOT OK
 * NonNull to NonNull                            -> OK
 *
 * For return values:
 * Weak/Strong Nullable to Weak/Strong Nullable  -> OK
 * NonNull to Weak/Strong Nullable               -> NOT OK
 * Weak/Strong Nullable to NonNull               -> OK
 * NonNull to NonNull                            -> OK
 */
class ChangeMethodContractCheck {

  @interface MyAnnotation {}

  void argAnnotatedWeakNullable(@javax.annotation.Nullable Object a) { }
  void argAnnotatedStrongNullable(@javax.annotation.CheckForNull Object a) { }
  void argAnnotatedNonNull(@javax.annotation.Nonnull Object a, @javax.annotation.Nonnull Object b) { }

  @javax.annotation.Nullable
  String annotatedWeakNullable(Object a) { return null; }
  @javax.annotation.CheckForNull
  String annotatedStrongNullable(Object a) { return null; }
  @javax.annotation.Nonnull
//  ^^^<
  String annotatedNonNull(Object a) { return ""; }
}

class ChangeMethodContractCheck_B extends ChangeMethodContractCheck {
  @Override
  void argAnnotatedWeakNullable(@javax.annotation.CheckForNull Object a) { } // Compliant: Strong instead of Weak Nullable is accepted.

  @Override
  void argAnnotatedStrongNullable(@javax.annotation.Nullable Object a) { } // Compliant: Weak instead of Strong Nullable is accepted.

  // For arguments: if you call the the method from the parent but the child is actually used, the caller will be force to give non-null argument
  // despite the fact that the implementation would accept null. It is not armful, therefore, NonNull to Strong/Weak Nullable is compliant.
  @Override
  void argAnnotatedNonNull(@javax.annotation.CheckForNull Object a, @javax.annotation.Nullable Object b) { } // Compliant

  @javax.annotation.CheckForNull
  String annotatedWeakNullable(Object a) { return null; } // Compliant: Strong instead of Weak Nullable is accepted.
  @javax.annotation.Nullable
  String annotatedStrongNullable(Object a) { return null; } // Compliant: Weak instead of Strong Nullable is accepted.
  // Annotations on methods is the opposite of arguments: if the method from the parent claim to never return null, the method from the child
  // that can actually be executed at runtime should not return null.
  @javax.annotation.CheckForNull
  String annotatedNonNull(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @CheckForNull to honor @Nonnull of the overridden method.}}

  @javax.annotation.CheckForNull // Compliant: unrelated method.
  void unrelatedMethod(Object a) { }

  public boolean equals(Object o) { return false; } // Compliant: no nullable annotation
}

class ChangeMethodContractCheck_C extends ChangeMethodContractCheck {
  @Override
  void argAnnotatedWeakNullable(@javax.annotation.Nonnull @MyAnnotation Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull to honor @Nullable of the overridden method.}}
  @Override
  void argAnnotatedStrongNullable(@javax.annotation.Nonnull Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull to honor @CheckForNull of the overridden method.}}
  @Override
  void argAnnotatedNonNull(@javax.annotation.Nonnull Object a, @javax.validation.constraints.NotNull Object b) { } // Compliant: Nonnull to Nonnull is fine (even if different annotations).

  // If the method from the parent claims that it can return null, the caller will have to deal with this possibility.
  // If the actual method called (from a child) can never return null, it can not lead to bugs. Nullable to Nonnull is therefore fine.
  @javax.annotation.Nonnull
  @Deprecated
  String annotatedStrongNullable(Object a) { return ""; } // Compliant: Strong Nullable to Nonnull
  @javax.annotation.Nonnull
  String annotatedWeakNullable(Object a) { return ""; } // Compliant: Weak Nullable to Nonnull
  @Deprecated
  @javax.annotation.Nullable
//  ^^^<
  String annotatedNonNull(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @Nullable to honor @Nonnull of the overridden method.}}
//^^^^^^

  public boolean equals(@javax.annotation.Nonnull Object o) { return false; } // Compliant, handled by by S4454.
}

/**
 * Meta-annotations are inconsistently supported. See SONARJAVA-3795.
 */
class ChangeMethodContractCheck_WithMetaAnnotations {

  @javax.annotation.Nonnull
  public @interface MyNonnullMetaAnnotation {
  }

  @javax.annotation.CheckForNull
//  ^^^<
  public @interface MyCheckFroNullMetaAnnotation {
  }

  class Parent {
    void argAnnotatedNullableViaMetaAnnotation(@MyCheckFroNullMetaAnnotation Object a) { }
    void argAnnotatedNullableViaMetaAnnotation2(@MyCheckFroNullMetaAnnotation Object a) { }
    void argAnnotatedDirectlyNullable(@javax.annotation.CheckForNull Object a) { }

    @MyNonnullMetaAnnotation
    String annotatedNonnullViaMetaAnnotation(Object a) { return "null"; }

    @MyNonnullMetaAnnotation
    String annotatedNonnullViaMetaAnnotation2(Object a) { return "null"; }

    @javax.annotation.Nonnull
//  ^^^<
    String annotatedNonnullDirectly(Object a) { return "null"; }
  }

  class Child extends Parent {
    // Parent and Child with meta-annotation.
    void argAnnotatedNullableViaMetaAnnotation(@MyNonnullMetaAnnotation Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull via meta-annotation to honor @CheckForNull via meta-annotation of the overridden method.}}
    // Parent with meta-annotation, then Child annotated directly works
    void argAnnotatedNullableViaMetaAnnotation2(@javax.annotation.Nonnull Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull to honor @CheckForNull via meta-annotation of the overridden method.}}
    // Parent directly annotated but Child with meta-annotations.
    void argAnnotatedDirectlyNullable(@MyNonnullMetaAnnotation Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull via meta-annotation to honor @CheckForNull of the overridden method.}}

    @Override
    // Parent and Child with meta-annotation.
    @MyCheckFroNullMetaAnnotation
    String annotatedNonnullViaMetaAnnotation(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @CheckForNull via meta-annotation to honor @Nonnull via meta-annotation of the overridden method.}}

    // Parent with meta-annotation, then child annotated directly works
    @javax.annotation.CheckForNull
    String annotatedNonnullViaMetaAnnotation2(Object a) { return "null"; } // Noncompliant {{Fix the incompatibility of the annotation @CheckForNull to honor @Nonnull via meta-annotation of the overridden method.}}

    @Override
    // Parent directly annotated but Child with meta-annotations.
    @MyCheckFroNullMetaAnnotation
    String annotatedNonnullDirectly(Object a) { return "null"; } // Noncompliant {{Fix the incompatibility of the annotation @CheckForNull via meta-annotation to honor @Nonnull of the overridden method.}}
  }
}

/**
 * Not null with arguments is inconsistently supported. See SONARJAVA-3803.
 */
class ChangeMethodContractCheck_NonnullWithArguments {

  class Parent {
    @javax.validation.constraints.NotNull(groups = { ChangeMethodContractCheck.class })
    String annotatedNotNullWithArg(Object a) { return "null"; }

    @javax.validation.constraints.NotNull
    String annotatedNotNullWithoutArg(Object a) { return "null"; }

    void argAnnotatedNoNullWithArg(@javax.validation.constraints.NotNull(groups = { ChangeMethodContractCheck.class }) Object a) { }
    void argAnnotatedNoNullWithoutArg(@javax.validation.constraints.NotNull Object a) { }
  }

  class Child extends Parent {
    // Parent is not strictly not null (NotNull with arguments).
    @Override
    @javax.annotation.CheckForNull
    String annotatedNotNullWithArg(Object a) { return null; }

    @Override
    // This one is a TP though.
    @javax.annotation.CheckForNull
    String annotatedNotNullWithoutArg(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @CheckForNull to honor @NotNull of the overridden method.}}

    // It works correctly for arguments though.
    void argAnnotatedNoNullWithArg(@javax.annotation.CheckForNull Object a) { }
    void argAnnotatedNoNullWithoutArg(@javax.annotation.CheckForNull Object a) { }
  }
}

/**
 * javax.annotation.Nonnull with argument when=When.MAYBE or when=When.UNKNOWN is actually Nullable.
 */
class ChangeMethodContractCheck_NullableViaNonnull {

  class Parent {
    @javax.annotation.Nonnull
    String annotatedNonnull(Object a) { return "null"; }
    @javax.annotation.Nonnull
    String annotatedNonnull2(Object a) { return "null"; }
    @javax.annotation.Nonnull(when= When.MAYBE)
    String annotatedNullableViaNonNull(Object a) { return "null"; }

    void argNullableViaNonnull1(@javax.annotation.Nonnull(when=When.MAYBE) Object a) { }
    void argNullableViaNonnull2(@javax.annotation.Nonnull(when=When.UNKNOWN) Object a) { }
    void argNonnullViaNonnull(@javax.annotation.Nonnull Object a) { }
  }

  class Child extends Parent {
    @Override
    @javax.annotation.Nonnull(when=When.MAYBE) // Nonnull to Nullable should be reported
    String annotatedNonnull(Object a) { return "null"; } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull(when=MAYBE) to honor @Nonnull of the overridden method.}}
    @Override
    @javax.annotation.Nonnull(when=When.UNKNOWN) // Nonnull to Nullable should be reported
    String annotatedNonnull2(Object a) { return "null"; } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull(when=UNKNOWN) to honor @Nonnull of the overridden method.}}
    @Override
    @javax.annotation.CheckForNull // Compliant, Nullable to Nullable is compliant for return value, annotation with argument is correctly taken into account in the parent.
    String annotatedNullableViaNonNull(Object a) { return "null"; }

    // For parameters, it works correctly and lead to TP
    void argNullableViaNonnull1(@javax.annotation.Nonnull Object a) { } // Noncompliant
    void argNullableViaNonnull2(@javax.annotation.Nonnull Object a) { } // Noncompliant
    void argNonnullViaNonnull(@javax.annotation.Nonnull(when=When.MAYBE) Object a) { } // Compliant, Nonnull to CheckForNull
  }
}

class ChangeMethodContractCheck_FromExternalDependency {

  class ImplementsFunction implements com.google.common.base.Function<String, String> {

    @Override
    @javax.annotation.Nonnull
    public String apply(@lombok.NonNull String s) { // Noncompliant {{Fix the incompatibility of the annotation @NonNull to honor @Nonnull(when=UNKNOWN) via meta-annotation of the overridden method.}}
//                                             ^
      return null;
    }

    @Override
    public boolean equals(Object o) {
      return false;
    }
  }
}
