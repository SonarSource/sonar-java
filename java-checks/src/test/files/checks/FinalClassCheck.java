public final class PrivateConstructorFinalClass {  // Compliant, declared final

  private PrivateConstructorFinalClass() {
    // ...
  }

  public static int magic(){
    return 42;
  }
}


public class PrivateConstructorNotFinalClass {  // Noncompliant {{Make this class "final" or add a public constructor.}}

  private PrivateConstructorNotFinalClass() {
    // ...
  }

  public static int magic(){
    return 42;
  }
}

public class PrivateAndPublicConstructorClass { // Compliant, one public constructor

  private PrivateAndPublicConstructorClass() {
    // ...
  }

  public PrivateAndPublicConstructorClass(int i) {
    // ...
  }

  public static int magic(){
    return 42;
  }
}

public class ProtectedConstructorClass {  // Compliant, all constructors are not private

  protected ProtectedConstructorClass() {
    // ...
  }

  public static int magic(){
    return 42;
  }
}

public class NoConstructorClass { // Compliant, implicit constructor has package visibility
}

public abstract class AbstractClass {  // Compliant, constructor is used in

  private AbstractClass(String value) {

  }

  final class ConcreteNested extends AbstractClass {

    private ConcreteNested() {
      super("Concrete");
    }
  }

}

private class Test { // Noncompliant
  Class c = Test.class;
  private Test() {}
}
