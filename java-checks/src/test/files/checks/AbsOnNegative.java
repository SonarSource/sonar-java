public class TestClass implements Comparable<TestClass> {

  public void method() {
    intmethod(0); // Compliant
    Math.abs(0); // Compliant
    Math.abs(intmethod(0)); // Compliant
    Math.abs(hashCode()); // Noncompliant {{Use the original value instead.}}
    Math.abs(((int) super.hashCode())); // Noncompliant {{Use the original value instead.}}
    Math.abs(this.hashCode()); // Noncompliant {{Use the original value instead.}}
    Math.abs(new java.util.Random().nextInt()); // Noncompliant {{Use the original value instead.}}
    Math.abs(new java.util.Random().nextLong()); // Noncompliant {{Use the original value instead.}}
    Math.abs(this.compareTo(this)); // Noncompliant {{Use the original value instead.}}
    
    -((int)0); // Compliant
    -intmethod(0); // Compliant
    -this.compareTo(this); // Noncompliant {{Use the original value instead.}}
  }

  public int intmethod(int arg) {
    return arg;
  }

}
