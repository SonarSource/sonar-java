public class Greeter {

  public static Foo foo = new Foo();        // NOK

  public static final Bar bar = new Bar();  // OK
  public final Bar bar = new Bar();         // OK
  public Bar bar = new Bar();               // OK

  private static final Fun fun = new Fun(); // OK
  private final Fun fun = new Fun();        // OK
  private  Fun fun = new Fun();             // OK

  public static method() {
    Foo foo = new Foo ();
  }

  class InnerClass {
    public static Foo foo; // NOK
  }

}
