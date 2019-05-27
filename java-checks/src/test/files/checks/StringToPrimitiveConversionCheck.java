import static java.lang.Integer.MAX_VALUE;

class A {
  void ints(Integer integerParam) {
    int i1 = new Integer("42").intValue(); // Noncompliant [[sc=14;ec=31]] {{Use "Integer.parseInt" for this string-to-int conversion.}}
    new Integer("42").intValue(); // Noncompliant
    int i2 = new Integer("42"); // Noncompliant
    int i3 = Integer.valueOf("42").intValue(); // Noncompliant
    int i4 = Integer.valueOf("42"); // Noncompliant
    Integer boxedInt = new Integer("42");
    int i5 = boxedInt.intValue(); // Noncompliant
    Integer reusedBoxedInt = new Integer("42");
    int i6 = reusedBoxedInt.intValue();
    reusedBoxedInt.hashCode();
    int i7 = Integer.parseInt("42");
    int i8 = new Integer(42).intValue();
    int i9 = valueOf("42").intValue();
    int i10 = 3 + Integer.valueOf("42");
    int i11 = unknown;
    int i12 = integerParam;
    int i13;
    int i14 = MAX_VALUE;
  }
  
  Integer valueOf(String s) {
    return null;
  }
  
  void others() {
    boolean bool1 = new Boolean("true").booleanValue(); // Noncompliant
    byte byte1 = new Byte("0").byteValue(); // Noncompliant
    double d1 = new Double("42.0").doubleValue(); // Noncompliant
    float f1 = new Float("42.0").floatValue(); // Noncompliant
    long l1 = new Long("42").longValue(); // Noncompliant
    short s1 = new Short("42").shortValue(); // Noncompliant
    char c1 = new Character('c').charValue();
  }

}

abstract class sonarjava3090 {

  void foo() {
    var hs = new java.util.HashSet<>();
    bar(new java.util.ArrayList<>(hs)); // analysis was failing here due to incorrect semanting resolution of type of hs
  }

  abstract void bar(Object o);
}
