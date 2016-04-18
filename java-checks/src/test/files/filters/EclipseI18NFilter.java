public class EclispeI18NFiltered {

  static class A extends org.eclipse.osgi.util.NLS {
    public static Integer foo; // NoIssue
    public String bar; // NoIssue

    class C {
      // C does not extends NLS, so issues are valid
      public static Integer foo; // WithIssue
      public String bar; // WithIssue
    }

    public String gul; // NoIssue

  }

  static class B {
    public static Integer foo;  // WithIssue
  }

}
