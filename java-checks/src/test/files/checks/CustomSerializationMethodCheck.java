import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.io.ObjectStreamException;

class A implements Serializable { // all compliant
  private void writeObject(ObjectOutputStream out) throws IOException {}
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {}
  private void readObjectNoData() throws ObjectStreamException {}
  Object writeReplace() throws ObjectStreamException {}
  Object readResolve() throws ObjectStreamException {}
}

class B implements Serializable { // non private methods
  void writeObject(ObjectOutputStream out) throws IOException {} // Noncompliant {{Make "writeObject" "private".}}
//     ^^^^^^^^^^^
  void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {} // Noncompliant
  void readObjectNoData() throws ObjectStreamException {} // Noncompliant
}

class C implements Serializable { // static methods
  private static void writeObject(ObjectOutputStream out) throws IOException {} // Noncompliant {{The "static" modifier should not be applied to "writeObject".}}
//                    ^^^^^^^^^^^
  private static void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {} // Noncompliant
  private static void readObjectNoData() throws ObjectStreamException {} // Noncompliant
  static Object writeReplace() throws ObjectStreamException {} // Noncompliant
  static Object readResolve() throws ObjectStreamException {} // Noncompliant
}

class D implements Serializable { // wrong exceptions: compliant
  private void writeObject(ObjectOutputStream out) throws ClassNotFoundException {}
  private void readObject(ObjectInputStream in) {}
  private void readObjectNoData() {}
  Object writeReplace() throws FileNotFoundException {}
  Object readResolve() {}
}

class E implements Serializable { // invalid return type
  Unknown writeReplace() throws ObjectStreamException {} // Noncompliant {{"writeReplace" should return "java.lang.Object".}}
//        ^^^^^^^^^^^^
  String readResolve() throws ObjectStreamException {} // Noncompliant
}

class F implements Serializable { // compliant, method parameters do not match
  void writeObject(String s) {}
  void readObject() {}
  void readObjectNoData(String s) {}
  Unknown writeReplace(String s) {}
  String readResolve(String s) {}
}

class G { // compliant: static methods but not Serializable class
  private static void writeObject(ObjectOutputStream out) throws IOException {}
  private static void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {}
  private static void readObjectNoData() throws ObjectStreamException {}
  static Object writeReplace() throws ObjectStreamException {}
  static Object readResolve() throws ObjectStreamException {}
}
