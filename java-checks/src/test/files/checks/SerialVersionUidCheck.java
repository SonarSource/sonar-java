import java.io.Serializable;

class A implements Cloneable {}
class B implements Serializable {
  private static final long serialVersionUID = 1L;
}
class C implements Serializable {} // Noncompliant
class D extends C {} // Noncompliant
class E implements Serializable {
  private final long serialVersionUID = 1L; // Noncompliant
}
class F implements Serializable {
  private static long serialVersionUID = 1L; // Noncompliant
}
class G implements Serializable {
  private static final int serialVersionUID = 1; // Noncompliant
}
class H implements Serializable {
  void serialVersionUID() {}
  private static final long serialVersionUID = 1L;
}
abstract class I implements Serializable {}
