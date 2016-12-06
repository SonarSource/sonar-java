import java.nio.file.AccessMode;

public enum Foo {
  A, B, @Deprecated C;
}

class MyClass {
  void fromFile() {
    useValues(Foo.values());
    useValueOf(Foo.valueOf("A"));
  }

  void useValues(Foo... values) {}
  void useValueOf(Foo f) {}

  void fromByteCode() {
    useValues(AccessMode.values());
    useValueOf(AccessMode.valueOf("READ"));
  }

  void useValues(AccessMode... values) {}
  void useValueOf(AccessMode am) {}
}
