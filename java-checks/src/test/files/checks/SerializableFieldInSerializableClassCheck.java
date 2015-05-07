import java.io.Serializable;
import java.util.List;

class Address {
}
class Person implements Serializable {
  Address address; // Noncompliant {{Make "address" transient or serializable.}}
  static Address address;//Compliant : static field
  transient Address address;
}
enum A {
  B;
  Address address;// Noncompliant {{Make "address" transient or serializable.}}
  Address[][] addressArray;// Noncompliant {{Make "addressArray" transient or serializable.}}
}

class Person2 implements Serializable {
  Address address; //Compliant: read/write methods are implemented
  transient Address address;
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {}
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {}
}
interface MyCustomInterface extends Serializable {}
class Person3 implements MyCustomInterface {
  Address address; // Noncompliant {{Make "address" transient or serializable.}}
}
class Person4<T extends Serializable, S extends Address> implements MyCustomInterface {
  T t; //Compliant
  S s; // Noncompliant {{Make "s" transient or serializable.}}
}
class Person5 implements Serializable {
  int[][] matrix; //Compliant
  Integer integer; //Compliant
}

class Person6<E, F extends Serializable> implements Serializable {
  List<Person6> persons; // Compliant
  List things; // Noncompliant {{Make "things" transient or serializable.}}
  List<MyObject> objects; // Noncompliant {{Make "objects" transient or serializable.}}
  List<? extends MyObject> otherObjects; // Noncompliant {{Make "otherObjects" transient or serializable.}}
  List<? extends Person6> otherPersons; // Compliant
  List<? extends E> otherThings; // Noncompliant {{Make "otherThings" transient or serializable.}}
  List<? extends F> otherSerializableThings; // Compliant
  List<?> otherUnknown; // Noncompliant {{Make "otherUnknown" transient or serializable.}}
}

class MyObject {

}
