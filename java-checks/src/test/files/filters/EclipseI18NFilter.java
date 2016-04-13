public class EclispeI18NFiltered {

  static class A extends org.eclipse.osgi.util.NLS {
    public static Integer foo; // NoIssue
    public String bar; // NoIssue
  }

  static class B {
    public static Integer foo;
  }

}
