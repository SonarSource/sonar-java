public class EclispeI18NFiltered {

  /**
   * The issues from this classes related to the following rules will be filtered:
   * - squid:S1444
   * - squid:ClassVariableVisibilityCheck
   */
  static class A extends org.eclipse.osgi.util.NLS {
    public static Integer foo; // raise squid:S1444 + squid:ClassVariableVisibilityCheck
    public String bar; // raise squid:ClassVariableVisibilityCheck
  }

  /**
   * The issues from this classes won't be filtered
   */
  static class B {
    public static Integer foo; // raise squid:S1444 + squid:ClassVariableVisibilityCheck
  }

}
