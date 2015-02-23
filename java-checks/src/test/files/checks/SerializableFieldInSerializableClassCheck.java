import java.io.Serializable;

class Address {
}
class Person implements Serializable {
  Address address; //Non compliant
  static Address address;//Compliant : static field
  transient Address address;
}
enum A {
  B;
  Address address;//Non-Compliant
  Address[][] addressArray;//Non-Compliant
}

class Person2 implements Serializable {
  Address address; //Compliant: read/write methods are implemented
  transient Address address;
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {}
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {}
}
interface MyCustomInterface extends Serializable {}
class Person3 implements MyCustomInterface {
  Address address; //Non compliant
}
class Person4<T extends Serializable, S extends Address> implements MyCustomInterface {
  T t; //Compliant
  S s; //NonCompliant
}
class Person5 implements Serializable {
  int[][] matrix; //Compliant
  Integer integer; //Compliant
}