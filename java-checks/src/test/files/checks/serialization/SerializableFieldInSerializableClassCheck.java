package javax.inject;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@interface Inject {}

class Address {
}
class Person implements Serializable {
  Address address; // Noncompliant [[sc=11;ec=18]] {{Make "address" transient or serializable.}}
  A a;
  UnknownField unknownField;
  static Address address2;//Compliant : static field
  transient Address address3;
}
enum A {
  B;
  Address address;
  Address[][] addressArray;
}

class Person2 implements Serializable {
  Address address; //Compliant: read/write methods are implemented
  transient Address address2;
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
  List<? super F> super1;
  List<? super E> super2; // Noncompliant
}

class Person7 implements Serializable {
  Map<Object, Object> both; // Noncompliant {{Make "both" transient or serializable.}}
  Map<String, Object> right; // Noncompliant {{Make "right" transient or serializable.}}
  Map<Object, String> left; // Noncompliant {{Make "left" transient or serializable.}}
  Map<String, String> ok; // Compliant
}

class Person8 implements Serializable {
  @Inject Address address; // Compliant field is injected
}

class MyObject {

}
