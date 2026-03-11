class Foo {

  @Deprecated(forRemoval = true)
  public String getName; // Noncompliant

  @Deprecated(forRemoval = false)
  public String getName; // Compliant

  @Deprecated
  public int foo; // Noncompliant {{Do not forget to remove this deprecated code someday.}}
//           ^^^

  public void foo1() { // Compliant
  }

  @Deprecated
  public void foo2() { // Noncompliant
  }

  /**
   * @deprecated
   */
  public void foo3() { // Noncompliant

  }

  /**
   * @deprecated
   */
  @Ignore
  @Deprecated
  public void foo4() { // Noncompliant
//            ^^^^
  }

  @Deprecated
  /**
   * @deprecated
   */
  public void foo5() { // Noncompliant
  }

  /*
   * @deprecated
   */
  @Deprecated
  public int foo7() { // Noncompliant
  }

  /**
   *
   */
  @Deprecated
  public void foo8() { // Noncompliant
  }

  @java.lang.Deprecated // Compliant - no one does this
  public void foo9() {

    @Deprecated
    int local1 = 0; // Noncompliant

  }

}

interface Bar {

  @Deprecated
  int foo(); // Noncompliant

}

// Test cases for SONARJAVA-6070: Legitimate deprecation documentation should NOT be flagged

class LegitimateDeprecation {

  /**
   * @deprecated Use the new DynEnum instead
   */
  public void oldMethod1() { // Compliant
  }

  /**
   * @deprecated Will be removed in Tomcat 10.
   */
  public int getPollerThreadCount() { // Compliant
    return 1;
  }

  /**
   * @deprecated Please use {@link NewClass} instead
   */
  public void oldMethod2() { // Compliant
  }

  /**
   * @deprecated Replaced by newMethod()
   */
  public void oldMethod3() { // Compliant
  }

  /**
   * @deprecated Scheduled for removal in version 2.0
   */
  public void oldMethod4() { // Compliant
  }

  /**
   * @deprecated Use newApi() instead.
   */
  public void oldMethod5() { // Compliant
  }

  /**
   * @deprecated See {@link NewApi#betterMethod}
   */
  public void oldMethod6() { // Compliant
  }

  /**
   * @deprecated Prefer using modernMethod()
   */
  public void oldMethod7() { // Compliant
  }

  /**
   * @deprecated Migrate to the new API
   */
  public void oldMethod8() { // Compliant
  }

  /**
   * @deprecated Removed in version 3.0
   */
  public void oldMethod9() { // Compliant
  }

  /**
   * @deprecated To be removed in future releases
   */
  public void oldMethod10() { // Compliant
  }

  /**
   * @deprecated deprecated since version 1.5, use newMethod() instead
   */
  public void oldMethod11() { // Compliant
  }

  /**
   * @deprecated This is old and not useful
   */
  public void oldMethod12() { // Noncompliant
  }

  /**
   * @deprecated
   */
  public void oldMethod13() { // Noncompliant
  }

  /**
   * Some javadoc
   * @deprecated This method is outdated
   */
  public void oldMethod14() { // Noncompliant
  }

  // Use multiline comments

  /**
   * Returns the empty iterator.
   *
   * @deprecated Use
   *     newApi() instead.
   * @since 1.0
   */
  public static void oldMethod15() { // Compliant when having new tag after comment
  }

  /**
   * Returns the empty iterator.
   *
   * @deprecated Use
   *     newApi() instead.
   */
  public static void oldMethod16() { // Compliant when having endline after comment
  }
}
