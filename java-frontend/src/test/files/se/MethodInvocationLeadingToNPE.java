import javax.annotation.Nullable;

class A {

  void foo1(Object o) {
    if (o == null) {
      npeIfNull(o); // Noncompliant [[sc=7;ec=16]] {{NullPointerException will be thrown when invoking method npeIfNull().}}
    }

    o.toString(); // Compliant - can not be reached with 'o' being NULL, as the NPE is triggered insde bar()
  }

  void foo2(Object o) {
    if (o == null) { // flow@foo2 [[order=1]] {{Implies 'o' is null.}}
      A.npeIfNull(o); // Noncompliant [[flows=foo2]] [[sc=9;ec=18]] {{NullPointerException will be thrown when invoking method npeIfNull().}}  flow@foo2 [[order=2]] {{'npeIfNull()' is invoked.}}
    }

    o.toString(); // Compliant - can not be reached with 'o 'being NULL, as the NPE is triggered insde bar()
  }

  void foo3(Object o) {
    npeIfNull(o); // Compliant - nothing is known about 'o' yet, but after the call 'o' cannot be null anymore

    if (o == null) {
      // do nothing
    }

    o.toString(); // Compliant
  }

  void foo4(Object o) {
    nullable(o); // Compliant - nothing is known about 'o' yet, but after the call 'o' cannot be null anymore
    o.toString();
  }

  void foo5(Object o) {
    if (o == null) { // flow@foo5 [[order=1]] {{Implies 'o' is null.}}
      nullable(o); // Noncompliant [[flows=foo5]] [[sc=7;ec=15]] {{NullPointerException will be thrown when invoking method nullable().}} flow@foo5 [[order=2]] {{'nullable()' is invoked.}}
    }
    o.toString(); // Compliant - can not be reached with 'o 'being NULL, as the NPE is triggered insde bar()
  }

  void foo6(Object o, boolean b1, boolean b2) {
    if (!b1 && b2) { // flow@foo6 [[order=1]] {{Implies 'b1' is true.}}
      npeIfArg0IsTrueAndArg1IsNull(b1, o, b2); // Compliant
    }
    if (o == null && b1) { // flow@foo6 [[order=2]] {{Implies 'o' is null.}}
      npeIfArg0IsTrueAndArg1IsNull(b1, o, b2); // Noncompliant [[flows=foo6]] [[sc=7;ec=35]] {{NullPointerException will be thrown when invoking method npeIfArg0IsTrueAndArg1IsNull().}}  flow@foo6 [[order=3]] {{'npeIfArg0IsTrueAndArg1IsNull()' is invoked.}}
    }
  }

  static void npeIfNull(Object o) {
    o.toString(); // flow@foo2 [[order=3]] {{...}} flow@foo2 [[order=4]] {{'NullPointerException' is thrown here.}}
  }

  static void npeIfArg0IsTrueAndArg1IsNull(boolean arg0, Object arg1, boolean arg2) {
    if (arg1 != null) { // flow@foo6  [[order=4]] {{Implies 'arg1' is null.}}
      return;
    }
    if (arg0) { // only b2 has consequences on NPE
      arg1.toString(); // Noncompliant {{NullPointerException might be thrown as 'arg1' is nullable here}} flow@foo6  [[order=5]] {{'NullPointerException' is thrown here.}}
    }
  }

  static void nullable(@Nullable Object o) {
    o.toString(); // Noncompliant {{NullPointerException might be thrown as 'o' is nullable here}} flow@foo5 [[order=3]] {{'NullPointerException' is thrown here.}}
  }
}

class B {
  void bar1(@Nullable Object o) {
    npeIfNull(o); // Noncompliant {{NullPointerException will be thrown when invoking method npeIfNull().}}
  }

  static void npeIfNull(Object o) {
    try {
      String s1 = o.toString(); // Compliant
    } catch (Exception e) {
      // Ignore
    }
    String s2 = o.toString(); // Compliant
  }
}

class C_varArgs {
  static final String NONE = "none";

  void coa1(Object o1, Object o2) {
    format("helloworld", o1, o2); // Compliant
  }

  void coa2() {
    Object[] args = null;
    format("helloworld", args); // Noncompliant {{NullPointerException will be thrown when invoking method format().}}
  }

  void coa3() {
    format("helloworld"); // Compliant
  }

  void coa4(Object o) {
    format("helloworld", o); // Compliant
  }

  void coa5(Object o) {
    format("helloworld", C.NONE); // Compliant
  }

  void coa6(Object o) {
    format("helloworld", null); // Noncompliant {{NullPointerException will be thrown when invoking method format().}}
  }

  static int format(String template, @Nullable Object ... args) {
    return args.length; // Noncompliant {{NullPointerException might be thrown as 'args' is nullable here}}
  }
}

class D {
  void catch_super_type_of_NPE(@Nullable Object o) {
    Object o2 = new Object();
    try {
      npeIfNull(o); // Compliant - NPE catched
    } catch (Exception e) {
      o2 = o;
    }
    o2.toString(); // Noncompliant
  }

  void catch_NPE(@Nullable Object o) {
    Object o2 = new Object();
    try {
      npeIfNull(o); // Compliant - NPE catched
    } catch (NullPointerException e) {
      o2 = o;
    }
    o2.toString(); // Noncompliant
  }

  void catch_runtime_exception(@Nullable Object o) {
    Object o2 = new Object();
    try {
      npeIfNull(o); // Compliant - NPE catched
    } catch (RuntimeException e) {
      o2 = o;
    }
    o2.toString(); // Noncompliant
  }

  void catch_not_related(@Nullable Object o) {
    Object o2 = new Object();
    try {
      npeIfNull(o); // Noncompliant {{NullPointerException will be thrown when invoking method npeIfNull().}}
    } catch (MyCheckedException e) {
      o2 = o;
    }
    o2.toString();
  }

  static void npeIfNull(Object o) {
    o.toString();
  }

  static class MyCheckedException extends Exception { }
}
