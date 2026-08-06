package checks.serialization;

import java.io.ObjectStreamException;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

class NonSerializableWithoutConstructorWS {}

class NonSerializableWithAccessibleNoArgConstructorWS {
  public NonSerializableWithAccessibleNoArgConstructorWS(String arg1) {}
  public NonSerializableWithAccessibleNoArgConstructorWS() {}
}

class NonSerializableWithoutAccessibleNoArgConstructorWS {
  int field;

  public NonSerializableWithoutAccessibleNoArgConstructorWS(String arg1) {}
  private NonSerializableWithoutAccessibleNoArgConstructorWS() {}
}

class S2055_AWS extends NonSerializableWithoutConstructorWS implements Serializable {}
class S2055_BWS extends NonSerializableWithAccessibleNoArgConstructorWS implements Serializable {}
class S2055_C1WS extends NonSerializableWithoutAccessibleNoArgConstructorWS implements Serializable { // Noncompliant

  S2055_C1WS(String arg1) { super(arg1); }
}
class S2055_C2WS extends NonSerializableWithoutAccessibleNoArgConstructorWS implements Serializable { // Compliant
  S2055_C2WS(String arg1) { super(arg1); }
  Object writeReplace() throws ObjectStreamException { return null; }
}
class S2055_DWS implements Serializable {}
class S2055_EWS extends NonSerializableWithoutAccessibleNoArgConstructorWS { S2055_EWS(String arg1) { super(arg1); } }
class S2055_FWS extends S2055_AWS {}
class S2055_GWS {
  S2055_C1WS c1 = new S2055_C1WS("") {
    @Override
    public String toString() { return ""; }
    Object writeReplace() throws ObjectStreamException { return null; }
  };
}

class S2055_AzWS<T> implements Serializable {
  public S2055_AzWS(String arg1) {}
  private S2055_AzWS() {}
}

class S2055_Bz2WS extends S2055_AzWS<String> implements Serializable {
  S2055_Bz2WS(String arg1) { super(arg1); }
}

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class NonSerializableWithLombokPrivateNoArgsConstructorWS {
  int field;

  public NonSerializableWithLombokPrivateNoArgsConstructorWS(int field) {
    this.field = field;
  }
}

@NoArgsConstructor
class NonSerializableWithLombokNoArgsConstructorWS {
  int field;

  public NonSerializableWithLombokNoArgsConstructorWS(int field) {
    this.field = field;
  }
}

class S2055_LombokPublicWS extends NonSerializableWithLombokNoArgsConstructorWS implements Serializable { // Compliant
  S2055_LombokPublicWS(int field) {
    super(field);
  }
}
