package checks.serialization;

import java.io.Serializable;

class NonSerializableWithoutAccessibleNoArgConstructor {
  int field;

  public NonSerializableWithoutAccessibleNoArgConstructor(String arg1) {}
  private NonSerializableWithoutAccessibleNoArgConstructor() {}
}

class S2055_C1 extends NonSerializableWithoutAccessibleNoArgConstructor implements Serializable { // Noncompliant
  S2055_C1(String arg1) { super(arg1); }
}

class S2055_Az<T> implements Serializable {
  public S2055_Az(String arg1) {}
  private S2055_Az() {}
}

class S2055_Bz extends S2055_Az<Unknown> implements Serializable {}

class S2055_B2 extends Unknown implements Serializable {}

