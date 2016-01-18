import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;

class NonSerializableWithoutConstructor {
  
}

class NonSerializableWithAccessibleNoArgConstructor {
  
  public NonSerializableWithAccessibleNoArgConstructor(String arg1) {}
  public NonSerializableWithAccessibleNoArgConstructor() {}
  
}

class NonSerializableWithoutAccessibleNoArgConstructor {
  
  public NonSerializableWithoutAccessibleNoArgConstructor(String arg1) {}
  private NonSerializableWithoutAccessibleNoArgConstructor() {}
  
}

class A extends NonSerializableWithoutConstructor implements Serializable {

}

class B extends NonSerializableWithAccessibleNoArgConstructor implements Serializable {

}

class C extends NonSerializableWithoutAccessibleNoArgConstructor implements Serializable { // Noncompliant {{Add a no-arg constructor to "NonSerializableWithoutAccessibleNoArgConstructor".}}
  
}

class D implements Serializable {
  
}

class E extends NonSerializableWithoutAccessibleNoArgConstructor {
  
}

class F extends A {
  
}
