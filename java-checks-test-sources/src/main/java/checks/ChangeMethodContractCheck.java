package checks;

class ChangeMethodContractCheck {

  @interface MyAnnotation {}

  void foo1(@javax.annotation.Nullable Object a) { }
  void foo2(@javax.annotation.CheckForNull Object a) { }
  void foo3(@javax.annotation.Nonnull Object a) { }
  @javax.annotation.CheckForNull
  void foo4(Object a) { }
  @javax.annotation.Nullable
  void foo5(Object a) { }
  @javax.annotation.Nonnull
  void foo6(Object a) { }
}

class ChangeMethodContractCheck_B extends ChangeMethodContractCheck {
  @Override
  void foo1(@javax.annotation.CheckForNull Object a) { }

  @Override
  void foo2(@javax.annotation.Nullable Object a) { }

  @Override
  void foo3(@javax.annotation.CheckForNull Object a) { }

  @javax.annotation.Nullable
  void foo4(Object a) { }
  @javax.annotation.CheckForNull
  void foo5(Object a) { }
  @javax.annotation.CheckForNull // Noncompliant
  void foo6(Object a) { }

  public boolean equals(Object o) { return false; } // compliant
}

class ChangeMethodContractCheck_C extends ChangeMethodContractCheck {
  @Override
  void foo1(@javax.annotation.Nonnull @MyAnnotation Object a) { } // Noncompliant {{Remove this "Nonnull" annotation to honor the overridden method's contract.}}
  @Override
  void foo2(@javax.annotation.Nonnull Object a) { } // Noncompliant
  @Override
  void foo3(@javax.annotation.Nullable Object a) { }

  @javax.annotation.Nonnull
  @Deprecated
  void foo4(Object a) { }
  @javax.annotation.Nonnull
  void foo5(Object a) { }
  @Deprecated
  @javax.annotation.Nullable // Noncompliant [[sc=3;ec=29]] {{Remove this "Nullable" annotation to honor the overridden method's contract.}}
  void foo6(Object a) { }

  public boolean equals(@javax.annotation.Nonnull Object o) { return false; } // Noncompliant {{Equals method should accept null parameters and return false.}}
}
