import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

class A {
  public static void main(String[] arguments) { // Compliant - arguments not used
    String b = "arguments";
  }

  public static void main(String a) { // Compliant - not a valid main method
    String b = a;
  }
}

class B {
  public void main(String[] a) { // Compliant - not a valid main method
    String[] b = a;
  }
}

class C {
  public static void main(String[] arguments) { // Noncompliant [[sc=27;ec=45;secondary=22,24]] {{Make sure that command line arguments are used safely here.}}
    String[] b = arguments;
    // ...
    doStuff(arguments);
  }

  private static void doStuff(String[] args) { }
}

class D {
  interface Foo {} // for coverage

  @Argument(name="-name",usage="Sets a name")
  public String name;

  @Option(name="-lastName")
  public String lastName;

  private String nonAnnotatedMember;

  @Option(name="-file")
  public void setFile(File f, String other) { }

  // Compliant - we don't raise issues on annotated parameters as it has no impact
  public void setOtherArgs(@org.kohsuke.args4j.Option(name="-another1") String other1, @Option(name="-another2") String other2) { }

  public void setName() { }

  public void run() { } // Noncompliant [[sc=15;ec=18;secondary=33,36,41]] {{Make sure that command line arguments are used safely here.}}
}

class E {
  // Compliant - here as there is no 'run' method inside the class

  @Argument(name="-name",usage="Sets a name")
  public String name;

  @Option(name="-file")
  public void setFile(File f, String other) { }

  public void setOtherArgs(@org.kohsuke.args4j.Option(name="-another1") String other1, @Option(name="-another2") String other2) { }
}

class F {
  public void setName() { }

  public void run() { }
}
