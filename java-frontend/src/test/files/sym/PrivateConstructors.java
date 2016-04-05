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

class Outer {
  class Inner {
    Inner(int a){}
  }
}

class Foo {
  void foo(Outer outer) {
    Inner inner = outer.new Inner(2);
  }
}

class Outer2<T> {
  class Inner2 {
    Inner2(T a){}
  }
}

class Foo2 {
  void foo(Outer2<String> outer) {
    Inner2 inner = outer.new Inner2("");
  }
}

