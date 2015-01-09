import java.io.Serializable;

class A implements Serializable {
  transient String x;
  String y;
}

class B {
  transient String x; // Noncompliant
  String y;
  void myMethod() {}
}

class C extends Unknown {
  transient String x;
  String y;  
}
