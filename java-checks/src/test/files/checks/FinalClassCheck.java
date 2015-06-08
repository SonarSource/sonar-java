public final class PrivateConstructorFinalClass {  // Compliant, declared final

  private PrivateConstructorClass() {
    // ...
  }

  public static int magic(){
    return 42;
  }
}


public class PrivateConstructorNotFinalClass {  // Noncompliant {{Make this class "final" or add a public constructor.}}

  private PrivateConstructorClass() {
    // ...
  }

  public static int magic(){
    return 42;
  }
}

public class PrivateAndPublicConstructorClass { // Compliant, one public constructor

  private PrivateConstructorClass() {
    // ...
  }

  public PrivateConstructorClass(int i) {
    // ...
  }

  public static int magic(){
    return 42;
  }
}

public class ProtectedConstructorClass {  // Compliant, all constructors are not private

  protected PrivateConstructorClass() {
    // ...
  }

  public static int magic(){
    return 42;
  }
}
