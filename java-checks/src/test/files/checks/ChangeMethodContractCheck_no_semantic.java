package java.lang;

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
    @javax.annotation.CheckForNull
    void foo6(Object a) { } 
    public boolean equals(Object o) { } // compliant
}

class C extends A {
    @Override
    void foo1(@javax.annotation.Nonnull Object a) { } 
    @Override
    void foo2(@javax.annotation.Nonnull Object a) { } 
    @Override
    void foo3(@javax.annotation.Nullable Object a) { } 

    @javax.annotation.Nonnull
    void foo4(Object a) { } 
    @javax.annotation.Nonnull
    void foo5(Object a) { } 
    @javax.annotation.Nullable
    void foo6(Object a) { }

    public boolean equals(@javax.annotation.Nonnull Object o) { }
}

class D extends Unknown {
    @Override
    void foo(@javax.annotation.Nonnull Object a) { } // compliant : we cannot check the overriden method
}
