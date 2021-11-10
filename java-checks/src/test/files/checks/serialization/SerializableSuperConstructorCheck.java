import java.io.Serializable;
import java.io.ObjectStreamException;

class NonSerializableWithoutConstructor {}

class NonSerializableWithAccessibleNoArgConstructor {
  public NonSerializableWithAccessibleNoArgConstructor(String arg1) {}
  public NonSerializableWithAccessibleNoArgConstructor() {}
}

class NonSerializableWithoutAccessibleNoArgConstructor {
  int field;

  public NonSerializableWithoutAccessibleNoArgConstructor(String arg1) {}
  private NonSerializableWithoutAccessibleNoArgConstructor() {}
}

class A extends NonSerializableWithoutConstructor implements Serializable {}
class B extends NonSerializableWithAccessibleNoArgConstructor implements Serializable {}
class C1 extends NonSerializableWithoutAccessibleNoArgConstructor implements Serializable {} // Noncompliant [[sc=18;ec=66]] {{Add a no-arg constructor to "NonSerializableWithoutAccessibleNoArgConstructor".}}
class C2 extends NonSerializableWithoutAccessibleNoArgConstructor implements Serializable { // Compliant
  Object writeReplace() throws ObjectStreamException { return null; }
}
class D implements Serializable {}
class E extends NonSerializableWithoutAccessibleNoArgConstructor {}
class F extends A {}
class G {
  C1 c1 = new C1() {
    @Override
    public String toString() { return ""; }
    Object writeReplace() throws ObjectStreamException { return null; }
  };
}

class Az<T> implements Serializable {
  public Az(String arg1) {}
  private Az() {}
}

class Bz extends Az<Unknown> implements Serializable {}
class Bz2 extends Az<String> implements Serializable {}
