class A {

  public void foo() {}
  private void bar() {}
  private void qix() { // Noncompliant [[sc=16;ec=19]] {{Move this method into "inner".}}
    bar();
  }
  private void baz(){} // Noncompliant [[sc=16;ec=19]] {{Move this method into "inner".}}
  private void bax(){} // Noncompliant [[sc=16;ec=19]] {{Move this method into the anonymous class declared at line 25.}}

  class inner {
    void plop() {
      bar();
      qix();
      foo();
      baz();
      baz();
      baz();
      baz();
      innerFun();
    }
    private void innerFun() {}
  }

  Object foo = new Object() {
    void plop() {
      bax();
    }
  };

}
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
  private void invoke4(String a, String b, int... c) {} // Noncompliant invoke4 in InnerTwo can't be this method
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
