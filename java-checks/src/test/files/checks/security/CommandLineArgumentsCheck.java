import org.kohsuke.args4j.Option;

class A {
  public static void main(String[] arguments) { // Compliant - arguments no used
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
  public static void main(String[] arguments) {
    String[] b = arguments; // Noncompliant [[sc=18;ec=27]] {{Make sure that command line arguments are used safely here.}}
    // ...
    doStuff(arguments); // Noncompliant [[sc=13;ec=22]] {{Make sure that command line arguments are used safely here.}}
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
