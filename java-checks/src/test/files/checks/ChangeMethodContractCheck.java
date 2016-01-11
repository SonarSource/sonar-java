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
    void foo3(@javax.annotation.CheckForNull Object a) { } // Noncompliant [[sc=15;ec=54]] {{The "a" parameter nullability is different in the superclass method, and that should not be changed.}}
    @javax.annotation.Nullable
    void foo4(Object a) { }
    @javax.annotation.CheckForNull
    void foo5(Object a) { }
    @javax.annotation.CheckForNull
    void foo6(Object a) { } // Noncompliant
    public boolean equals(Object o) { } // compliant
}

class C extends A {
    @Override
    void foo1(@javax.annotation.Nonnull Object a) { } // Noncompliant
    @Override
    void foo2(@javax.annotation.Nonnull Object a) { } // Noncompliant
    @Override
    void foo3(@javax.annotation.Nullable Object a) { } // Noncompliant

    @javax.annotation.Nonnull
    void foo4(Object a) { } // Noncompliant
    @javax.annotation.Nonnull
    void foo5(Object a) { } // Noncompliant
    @javax.annotation.Nullable
    void foo6(Object a) { } // Noncompliant [[sc=5;ec=9]] {{The return value nullability of this method is different in the superclass, and that should not be changed.}}

    public boolean equals(@javax.annotation.Nonnull Object o) { } // Noncompliant {{Equals method should accept null parameters and return false.}}
}

class D extends Unknown {
    @Override
    void foo(@javax.annotation.Nonnull Object a) { } // compliant : we cannot check the overriden method
}
