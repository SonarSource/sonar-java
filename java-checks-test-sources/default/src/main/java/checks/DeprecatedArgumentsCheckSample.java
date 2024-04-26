package checks;

class DeprecatedArgumentsCheckSample {

  @Deprecated // Noncompliant {{Add 'since' and/or 'forRemoval' arguments to the @Deprecated annotation.}}
//^^^^^^^^^^^
  public int lim;

  /**
   * @deprecated
   */
  @Deprecated // Noncompliant {{Add 'since' and/or 'forRemoval' arguments to the @Deprecated annotation.}}
//^^^^^^^^^^^
  public int foo;

  /**
   * @deprecated
   */
  @Deprecated(since = "4.2") // Compliant
  public int bar;

  /**
   * @deprecated
   */
  @Deprecated(forRemoval = false) // Compliant
  public int gul;

  /**
   * @deprecated
   */
  @Deprecated(since = "4.2", forRemoval = true) // Compliant
  public int qix;

}
