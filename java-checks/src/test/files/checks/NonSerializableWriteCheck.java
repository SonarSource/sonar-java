import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.Serializable;

class A {
  void myMethod(ObjectOutputStream out, Object x, byte[] array) {
    out.writeObject(new Object());
    out.writeObject("x");
    out.writeObject(1);
    MySerializable mySerializable = new MySerializable();
    out.writeObject(mySerializable);
    MyNonSerializable myNonSerializable1 = new MyNonSerializable();
    MyNonSerializable myNonSerializable2 = new MyNonSerializable();
    if (myNonSerializable2 instanceof Runnable) {
      out.writeObject(myNonSerializable1); // Noncompliant {{Make the "MyNonSerializable" class "Serializable" or don't write it.}}
    }
    if (myNonSerializable2 instanceof Serializable) {
      out.writeObject(myNonSerializable2);
    }
    if (x.toString() instanceof Serializable) {
      out.writeObject(new MyNonSerializable()); // Noncompliant [[sc=23;ec=46]] {{Make the "MyNonSerializable" class "Serializable" or don't write it.}}
    }
    out.writeObject(array);
  }
}

class MySerializable implements Serializable {
}

class MyNonSerializable implements Runnable {
}

class ParameterizedSerializable<T> implements Serializable {
  
  T t;
  Unknown u;
  
  private void writeObject(ObjectOutputStream s) throws java.io.IOException {
    s.writeObject(t);
    s.writeObject(u);
  }
  
}

class TypeOfAssignedExpressions {
  final java.util.Map<Integer, List<Object>> cacheProp = new HashMap<>();
  final java.util.Map<Integer, List<Object>> cacheVar;
  java.util.Map<Integer, List<Object>> cacheVar2 = new HashMap<>();

  void foo() {
    try (FileOutputStream fos = new FileOutputStream(""); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(cacheProp); // compliant, real type is hashmap
      oos.writeObject(cacheVar); // Noncompliant : no initializer
      oos.writeObject(cacheVar2); // Noncompliant not final, so we are unsure of concrete type
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
