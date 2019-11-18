public class EclispeI18NFiltered {

  /**
   * The issues from this classes related to the following rules will be filtered:
   * - java:S1444
   * - java:ClassVariableVisibilityCheck
   */
  static class A extends org.eclipse.osgi.util.NLS {
    public static Integer foo; // raise java:S1444 + java:ClassVariableVisibilityCheck
    public String bar; // raise java:ClassVariableVisibilityCheck
  }

  /**
   * The issues from this classes won't be filtered
   */
  static class B {
    public static Integer foo; // raise java:S1444 + java:ClassVariableVisibilityCheck
  }

}
