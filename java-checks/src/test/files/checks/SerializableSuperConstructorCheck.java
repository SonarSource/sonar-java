import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;

class NonSerializableWithoutConstructor {
  
}

class NonSerializableWithVoidConstructor {
  
  public NonSerializableWithVoidConstructor() {}
  
}

class NonSerializableWithNonVoidConstructor {
  
  public NonSerializableWithNonVoidConstructor(String arg1) {}
  
}

class A extends NonSerializableWithoutConstructor implements Serializable {

}

class B extends NonSerializableWithVoidConstructor implements Serializable {

}

class C extends NonSerializableWithNonVoidConstructor implements Serializable { // Noncompliant
  
}

class D implements Serializable {
  
}

class E extends NonSerializableWithNonVoidConstructor {
  
}

class F extends A {
  
}

class G1 extends NonSerializableWithNonVoidConstructor implements Serializable {
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException;
}

class G2 extends NonSerializableWithNonVoidConstructor implements Serializable { // Noncompliant
  private boolean readObject;
  private void readObject() throws IOException, ClassNotFoundException;
  private void readObject(String s) throws IOException, ClassNotFoundException;
  private void readObject(ObjectInputStream in) throws ClassNotFoundException;
  private void readObject(ObjectInputStream in) throws UnknownException;
}
