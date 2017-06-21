class Foo {

  @Deprecated
  public int lim; // Noncompliant [[sc=14;ec=17]] {{Add the missing @deprecated Javadoc tag.}}

  /**
   * @deprecated
   */
  @Deprecated // Noncompliant [[sc=3;ec=14]] {{Add 'since' and/or 'forRemoval' arguments to the @Deprecated annotation.}}
  public int foo;

  /**
   * @deprecated
   */
  @Deprecated(since = "4.2") // Compliant - at least one of the argument
  public int bar;

  /**
   * @deprecated
   */
  @Deprecated(forRemoval = false) // Compliant - at least one of the argument
  public int gul;

  /**
   * @deprecated
   */
  @Deprecated(since = "4.2", forRemoval = true) // Compliant - at least one of the argument
  public int qix;

}
