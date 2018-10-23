import org.kohsuke.args4j.Option;

class A {
  interface Foo {} // for coverage

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
  public static void main(String[] arguments) { // Noncompliant [sc=27;ec=45;secondary=21,23] {{Make sure that command line arguments are used safely here.}}
    String[] b = arguments;
    // ...
    doStuff(arguments);
  }

  private static void doStuff(String[] args) { }
}

class D {
  @Option(name="-name",usage="Sets a name") // Noncompliant [[sc=3;ec=44]] {{Make sure that command line arguments are used safely here.}}
  public String name;

  @Option(name="-file") // Noncompliant
  public void setFile(File f, @Option(name="-other") String other) { } // Noncompliant [[sc=31;ec=53]]

  public void setOtherArgs(@org.kohsuke.args4j.Option(name="-another1") String other1, // Noncompliant
                           @Option(name="-another2") String other2) // Noncompliant
  { }
}
