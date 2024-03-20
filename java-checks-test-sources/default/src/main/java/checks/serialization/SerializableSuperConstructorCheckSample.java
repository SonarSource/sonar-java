package checks.serialization;

import java.io.ObjectStreamException;
import java.io.Serializable;

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

class S2055_A extends NonSerializableWithoutConstructor implements Serializable {}
class S2055_B extends NonSerializableWithAccessibleNoArgConstructor implements Serializable {}
class S2055_C1 extends NonSerializableWithoutAccessibleNoArgConstructor implements Serializable { // Noncompliant [[sc=24;ec=72]] {{Add a no-arg constructor to "NonSerializableWithoutAccessibleNoArgConstructor".}}
  S2055_C1(String arg1) { super(arg1); }
}
class S2055_C2 extends NonSerializableWithoutAccessibleNoArgConstructor implements Serializable { // Compliant
  S2055_C2(String arg1) { super(arg1); }
  Object writeReplace() throws ObjectStreamException { return null; }
}
class S2055_D implements Serializable {}
class S2055_E extends NonSerializableWithoutAccessibleNoArgConstructor { S2055_E(String arg1) { super(arg1); } }
class S2055_F extends S2055_A {}
class S2055_G {
  S2055_C1 c1 = new S2055_C1("") {
    @Override
    public String toString() { return ""; }
    Object writeReplace() throws ObjectStreamException { return null; }
  };
}

class S2055_Az<T> implements Serializable {
  public S2055_Az(String arg1) {}
  private S2055_Az() {}
}

class S2055_Bz2 extends S2055_Az<String> implements Serializable {
  S2055_Bz2(String arg1) { super(arg1); }
}
