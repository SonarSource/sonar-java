class DumpElement {
  private final String filename;
  private final Parser<MSG> parser;

  private DumpElement(String filename, Parser<MSG> parser) { // Compliant this is a constructor
    this.filename = filename;
    this.parser = parser;
  }

  public String filename() {
    return filename;
  }

  public Parser<MSG> parser() {
    return parser;
  }

  public static class IssueDumpElement extends DumpElement<ProjectDump.Issue> {
    public final int NO_LINE = 0;
    public final double NO_GAP = -1;
    public final long NO_EFFORT = -1;

    public IssueDumpElement() {
      super("issues.pb", ProjectDump.Issue.parser());
    }
  }
}

class UnknownInvocation {
  private void invoke(int a) {}
  private void invoke2(int a, int b) {} // Noncompliant
  private void invoke3(String a, String b, int... c) {}
  private void invoke4(String a, String b, int... c) {} // Noncompliant
  private void invoke5(int a) {}

  private class InnerOne {
    void fun(){
      invoke(1);
      invoke2(1, 2);
      invoke3("", "");
      invoke4("", "");
      invoke5(1);
    }
  }
  private class InnerTwo {
    void fun(){
      invoke(unknown);
      invoke2(1); // unknown
      invoke3("", unknown);
      invoke4("");
      invoke5(1);
    }
  }
}
