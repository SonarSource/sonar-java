public class Greeter {

  public static Foo foo = new Foo(); // Noncompliant {{Make this "public static foo" field final}}
//                  ^^^

  public static final Bar bar = new Bar();  // Compliant
  public final Bar bar2 = new Bar();         // Compliant
  public Bar bar3 = new Bar();               // Compliant

  private static final Fun fun = new Fun(); // Compliant
  private final Fun fun2 = new Fun();        // Compliant
  private  Fun fun3 = new Fun();             // Compliant

  public static method() {
    Foo foo = new Foo ();
  }

  class InnerClass {
    public static Foo foo; // Noncompliant {{Make this "public static foo" field final}}
  }

}
