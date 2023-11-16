package checks;

class MissingDeprecatedCheckJava9 {

  @Deprecated
  public int lim; // Noncompliant [[sc=14;ec=17]] {{Add the missing @deprecated Javadoc tag.}}

  /**
   * @deprecated
   */
  @Deprecated // Compliant: will be reported by S6355
  public int foo;

  /**
   * @deprecated
   */
  @Deprecated(since = "4.2") // Compliant: even for S6355
  public int bar;

  /**
   * @deprecated
   */
  @Deprecated(forRemoval = false) // Compliant: even for S6355
  public int gul;

  /**
   * @deprecated
   */
  @Deprecated(since = "4.2", forRemoval = true) // Compliant: even for S6355
  public int qix;

}
