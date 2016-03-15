package javax.annotation;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}


class A {
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

class B extends A {
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
    public boolean equals(Object o) { } // compliant
}

class C extends A {
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
    @javax.annotation.Nullable // Noncompliant [[sc=5;ec=31]] {{Remove this "Nullable" annotation to honor the overridden method's contract.}}
    void foo6(Object a) { }

    public boolean equals(@javax.annotation.Nonnull Object o) { } // Noncompliant {{Equals method should accept null parameters and return false.}}
}

class D extends Unknown {
    @Override
    void foo(@javax.annotation.Nonnull Object a) { } // compliant : we cannot check the overriden method
}

@interface MyAnnotation {}
