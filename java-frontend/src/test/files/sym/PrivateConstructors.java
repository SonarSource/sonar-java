public class PrivateConstructors {

  static class PrivateConstructorClass {
    public PrivateConstructorClass(Object o) {
    }

    private PrivateConstructorClass(String s) {
    }

    public PrivateConstructorClass(String s, int i) {
      this(s); // call PrivateConstructorClass(String)
    }
  }

  static class PublicConstructorClass extends PrivateConstructorClass {
    public PublicConstructorClass(String s) {
      super(s); // call PrivateConstructorClass(String)
    }
  }
}

class ExternalClass extends PrivateConstructors.PrivateConstructorClass {
  public ExternalClass(String s) {
    super(s); // call PrivateConstructorClass(Object)
  }
}
