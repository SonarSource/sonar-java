package checks;

class PrimitiveWrappersInTernaryOperatorCheckSample {
  void foo() {
    long l = 123456789123456789L;
    Integer i1 = 123456789;
    int i2 = 123456789;
    Float f1 = 1.0f;
    Object o1 = new Object();
    A a2 = new A();
    Number n = true ? i1 : f1; // Noncompliant [[sc=21;ec=22]] {{Add an explicit cast to match types of operands.}}
    o1 = true ? o1 : a2; // Compliant
    o1 = true ? f1 : a2; // Compliant
    n = true ? (Number) i1 : f1; // Compliant
    l = true ? i2 : l; // Compliant
    float f2 = 0.2f;
    l = (long) (true ? i2 : f2); // Compliant
    n = true ? Long.valueOf(i1) : (Long) l; // Compliant
  }

  private static class A { }
}
