import java.io.Serializable;

class A implements Serializable {
  transient String x;
  String y;
}

class B {
  transient String x; // Noncompliant {{Remove the "transient" modifier from this field.}}
  String y;
  void myMethod() {}
}

class C extends Unknown {
  transient String x;
  String y;  
}
