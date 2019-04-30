public abstract class TestClass implements Comparable<TestClass> {

  public static final int MIN_VALUE = 0;
  public static final int SOME_VALUE = 0;

  public void method() {
    intmethod(0); // Compliant
    Math.abs(0); // Compliant
    Math.abs(intmethod(0)); // Compliant
    Math.abs(hashCode()); // Noncompliant [[sc=14;ec=24]] {{Use the original value instead.}}
    Math.abs(((int) super.hashCode())); // Noncompliant {{Use the original value instead.}}
    Math.abs(this.hashCode()); // Noncompliant {{Use the original value instead.}}
    Math.abs(new java.util.Random().nextInt()); // Noncompliant {{Use the original value instead.}}
    Math.abs(new java.util.Random().nextLong()); // Noncompliant {{Use the original value instead.}}
    Math.abs(this.compareTo(this)); // Noncompliant {{Use the original value instead.}}
    Math.abs((long) Integer.MIN_VALUE); // Compliant
    Math.abs(Integer.MIN_VALUE); // Noncompliant {{Use the original value instead.}}
    Math.abs(Long.MIN_VALUE); // Noncompliant {{Use the original value instead.}}
    Math.abs(TestClass.MIN_VALUE); // Compliant
    Math.abs(TestClass.SOME_VALUE); // Compliant

    long x;
    x = -((int) 0); // Compliant
    x = -intmethod(0); // Compliant
    x = -this.compareTo(this); // Noncompliant {{Use the original value instead.}}
    x = -Integer.MIN_VALUE; // Noncompliant [[sc=10;ec=27]] {{Use the original value instead.}}
    x = -Long.MIN_VALUE; // Noncompliant {{Use the original value instead.}}
    x = -TestClass.MIN_VALUE; // Compliant
    x = -TestClass.SOME_VALUE; // Compliant
  }

  public int intmethod(int arg) {
    return arg;
  }

}
