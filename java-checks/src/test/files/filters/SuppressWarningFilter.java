// place itself in the same package as the filter to access the constant
package org.sonar.java.filters;

import java.io.Serializable;

/**
 * Extra rules running:
 * - TodoTagPresenceCheck
 * - BadConstantNameCheck
 * - SuppressWarningCheck - Raise an issue on all the @SuppressWarning annotation, can not be suppressed
 * - UnusedPrivateFieldCheck
 * - ObjectFinalizeCheck
 * - SwitchCaseWithoutBreakCheck
 * - RedundantTypeCastCheck
 * - CallToDeprecatedMethodCheck
 * - CallToDeprecatedCodeMarkedForRemovalCheck
 * - MissingDeprecatedCheck
 * - DivisionByZeroCheck
 * - EmptyBlockCheck
 * - EmptyStatementUsageCheck
 * - ReturnInFinallyCheck
 * - EqualsOverridenWithHashCodeCheck
 * - StaticMembersAccessCheck
 * - SerialVersionUidCheck
 * - RawTypeCheck
 */
class Test {
  class UnusedPrivateFieldCheck {
    private String s; // WithIssue
  }

  @SuppressWarnings("all") // WithIssue
  class A {
    private static final int bad_constant_name = 42; // NoIssue

    private String s; // NoIssue

    int foo() {
      return bad_constant_name;
    }
  }

  // Syntax not supported - repository is required
  @SuppressWarnings("S1068") // WithIssue
  class B {
    private static final int bad_constant_name = 42; // WithIssue

    private String s; // WithIssue

    int foo() {
      return bad_constant_name;
    }
  }

  @SuppressWarnings({"java:S1068", "java:S115"}) // WithIssue
  class C {
    private static final int bad_constant_name = 42; // NoIssue

    private String s; // NoIssue

    int foo() {
      return bad_constant_name;
    }
  }

  @SuppressWarnings("java:S115") // WithIssue
  class D {
    private static final int bad_constant_name = 42; // NoIssue

    @SuppressWarnings("unused") // WithIssue
    private String s; // WithIssue

    int foo() {
      return bad_constant_name;
    }
  }

  @SuppressWarnings // WithIssue
  class E {
  }

  class F {
    @SuppressWarnings("java:S115") // WithIssue
    private static final int bad_constant_name = 42; // NoIssue

    @SuppressWarnings("java:S1068") // WithIssue
    private String s; // NoIssue

    int foo() {
      return bad_constant_name;
    }

    @SuppressWarnings(org.sonar.java.filters.SuppressWarningFilterTest.CONSTANT_RULE_KEY) // WithIssue
    private static final int bad_constant_name2 = 42; // NoIssue

    @SuppressWarnings(someUnresolvedConstant) // WithIssue
    private static final int bad_constant_name3 = 42; // WithIssue
  }

  @SuppressWarnings("squid:S1068") // WithIssue
  class WithDeprecatedRuleKey {

    @SuppressWarnings("squid:S00115") // WithIssue
    private static final int bad_constant_name = 42; // NoIssue
    @SuppressWarnings("squid:S115") // WithIssue
    private static final int bad_constant_name2 = 42; // NoIssue
    @SuppressWarning("S115") // WithIssue
    private static final int bad_constant_name3 = 42; // WithIssue

    @SuppressWarnings({"squid:ObjectFinalizeCheck", "java:S1874", "java:S5738"}) // WithIssue
    void a() {
      Object object = new Object();
      object.finalize(); // NoIssue
    }

    @SuppressWarnings({"squid:S1111", "java:S1874", "java:S5738"}) // WithIssue
    void b() {
      Object object = new Object();
      object.finalize(); // NoIssue
    }

    @SuppressWarnings({"java:S1111", "java:S1874", "java:S5738"}) // WithIssue
    void c() {
      Object object = new Object();
      object.finalize(); // NoIssue
    }

    void d() {
      Object object = new Object();
      object.finalize(); // WithIssue
    }
  }
}

/**
 * This is trivia with issue
 * TODO  NoIssue
 */
// TODO another one NoIssue
@SuppressWarnings("java:S1135") // WithIssue
class JavadocSuppressed {


}

/**
 * This is trivia with issue
 * TODO  WithIssue
 */
// TODO another one WithIssue
class Javadoc {

  /**
   * Works on method too
   * TODO  NoIssue
   */
  @SuppressWarnings("java:S1135") // WithIssue
  void m() {

  }

}

// cast suppresses S1905
@SuppressWarnings("cast") // WithIssue
class Cast {
  void f() {
    String s1 = "";
    String s2 = (String) s1; // NoIssue
  }
}

// deprecation suppresses S1874 (but not S5738)
@SuppressWarnings("deprecation") // WithIssue
class Deprecation1 {

  @Deprecated
  private String deprecated; // WithIssue

  @Deprecated(forRemoval=true)
  private String deprecatedForRemoval; // WithIssue

  void f() {
    String s = deprecated  // NoIssue
      + deprecatedForRemoval; // WithIssue
  }
}

// removal suppresses S5738 (but not S1874)
@SuppressWarnings("removal") // WithIssue
class Deprecation2 {

  @Deprecated
  private String deprecated; // WithIssue

  @Deprecated(forRemoval=true)
  private String deprecatedForRemoval; // WithIssue

  void f() {
    String s = deprecated  // WithIssue
      + deprecatedForRemoval; // NoIssue
  }
}

// dep-ann suppresses S1123
@SuppressWarnings("dep-ann") // WithIssue
class Deprecation3 {
  @Deprecated
  public void foo1() { // NoIssue
  }

  /**
   * @deprecated
   */
  public void foo2() { // NoIssue
  }

}

// divzero suppresses S3518
@SuppressWarnings("divzero") // WithIssue
class Divzero {
  void f() {
    int j = 1 / 0; // NoIssue
  }
}

// empty suppresses S108, S1116
@SuppressWarnings("empty") // WithIssue
class Empty {
  void f(boolean b) {
    if (b) { } // NoIssue
    ; // NoIssue
  }
}

// fallthrough suppresses S128
@SuppressWarnings("fallthrough") // WithIssue
class FallThough {
  private String s; // WithIssue

  void f() {
    switch (myVariable) {
      case 2: // NoIssue
        doSomething();
      default:
        doSomethingElse();
        break;
    }
  }
}

// finally suppresses S1143
@SuppressWarnings("finally") // WithIssue
class Finally {
  void f() {
    try {
      // Not empty
    } finally {
      return; // NoIssue
    }
  }
}

// overrides suppresses S1206
@SuppressWarnings("overrides") // WithIssue
class Overrides {
  @Override
  public boolean equals(Object obj) { // NoIssue
    return super.equals(obj);
  }
}

// serial suppresses S2209
@SuppressWarnings("serial") // WithIssue
class Serial implements Serializable { // NoIssue
}

// static suppresses S2209
@SuppressWarnings("static") // WithIssue
class Static {
  public static int counter = 0;
  void f() {
    this.counter ++; // NoIssue
  }
}

class RawTypes {
  // rawtypes suppresses S3740
  @SuppressWarnings("rawtypes") // WithIssue
  void f(java.util.List myList) { } // NoIssue

  //rawtypes suppresses S3740
  @SuppressWarnings("java:S3740") // WithIssue
  void g(java.util.List myList) { } // NoIssue

  void h(java.util.List myList) { } // WithIssue
}
