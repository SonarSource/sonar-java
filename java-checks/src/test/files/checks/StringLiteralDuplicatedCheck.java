class A {
  @SupressWarnings("allall")
  @SupressWarnings("allall")
  @SupressWarnings("allall")
  @SupressWarnings("aaaaa")
  public void f() {
    System.out.println("aaaaa");
    System.out.println("bbbbb");
    System.out.println("bbbbb");
    System.out.println("ccccc"); // Noncompliant {{Define a constant instead of duplicating this literal "ccccc" 3 times.}}
    System.out.println("ccccc");
    System.out.println("ccccc");
    System.out.println("dddd");
    System.out.println("dddd");
  }

  interface F {
    default void foo() {
      System.out.println("aaaaa");
      System.out.println("aaaaa");
      System.out.println("aaaaa");

    }
  }
}
class AllConstants {
  private static final String Constant1 = "allConstant";
  private static final String Constant2 = "allConstant";
}
class ConstantAlreadyDefined {

  private static final String A = "constant";
  private static final String B = "constant";
  private static final String C = "constant";

  private static final String REPORT_WITHOUT_THRESHOLD = "blabla";

  void test() {
    System.out.println("constant"); // Noncompliant [[secondary=40]] {{Use already-defined constant 'A' instead of duplicating its value here.}}
    System.out.println("constant");
    System.out.println("blabla"); // Noncompliant {{Use already-defined constant 'REPORT_WITHOUT_THRESHOLD' instead of duplicating its value here.}}
  }

  public IssueChangeNotification setProject(Component project) {
    setFieldValue("projectName", project.longName());
    setFieldValue("projectKey", project.key());
    return this;
  }

  public IssueChangeNotification setProject(String projectKey, String projectName) {
    setFieldValue("projectName", projectName);
    setFieldValue("projectKey", projectKey);
    return this;
  }
}

class CompleteCoverage {
  private final String notConstant = "blablah";  // Noncompliant {{Define a constant instead of duplicating this literal "blablah" 3 times.}}
  private final String notConstant2 = "blablah";
  static String notConstant3 = "blablah";
  String notConstant4;
  int notString = 42;

  private static final String NOT_USED = "this constant is not used anywhere";
}
