class A {
  private volatile int vInts0;
  private volatile int [] vInts;  // Noncompliant [[sc=11;ec=26]] {{Use an "AtomicIntegerArray" instead.}}
  private volatile long [] vLongs;  // Noncompliant [[sc=11;ec=27]] {{Use an "AtomicLongArray" instead.}}
  private volatile Object [] vObjects;  // Noncompliant [[sc=11;ec=29]] {{Use an "AtomicReferenceArray" instead.}}
  private volatile MyObj myObj;  // Noncompliant [[sc=11;ec=25]] {{Remove the "volatile" keyword from this field.}}
  private AtomicIntegerArray vInts2;
  private MyObj myObj2;

  void foo(){}
}
enum MyEnum {
  FOO;
  private volatile int vInts0;
  private volatile int [] vInts;  // Noncompliant [[sc=11;ec=26]] {{Use an "AtomicIntegerArray" instead.}}
  private volatile MyObj myObj;  // Noncompliant [[sc=11;ec=25]] {{Remove the "volatile" keyword from this field.}}
  private AtomicIntegerArray vInts2;
  private MyObj myObj2;

  void foo(){}
}
