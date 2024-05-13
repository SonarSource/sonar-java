package checks.serialization;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

class S2060_A implements Externalizable { // Noncompliant {{Add a no-arg constructor to this class.}}
//    ^^^^^^^
  public S2060_A(String color, int weight) {}
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class S2060_B implements Externalizable { // Compliant
  public S2060_B() {}
  public S2060_B(String color, int weight) {}
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class S2060_C implements Externalizable { // Compliant - default constructor
  void foo() {
    Externalizable e = new Externalizable() { // Compliant
      @Override public void writeExternal(ObjectOutput out) throws IOException { }
      @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
    };
  }

  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

interface S2060_I extends Externalizable {}

class S2060_D implements S2060_I { // Compliant
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class S2060_E implements S2060_I { // Noncompliant {{Add a no-arg constructor to this class.}}
//    ^^^^^^^
  public S2060_E(String color, int weight) {}
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class S2060_F  {  // Compliant
  public S2060_F(String color, int weight) {}
}

class CtorVisibility_A implements Externalizable {
  public CtorVisibility_A() {} // Compliant
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class CtorVisibility_B implements Externalizable { // Compliant
  protected CtorVisibility_B() {} // Noncompliant {{Declare this no-arg constructor public.}}
//          ^^^^^^^^^^^^^^^^
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class CtorVisibility_C implements Externalizable { // Compliant
  CtorVisibility_C() {} // Noncompliant
//^^^^^^^^^^^^^^^^
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class CtorVisibility_D implements Externalizable { // Compliant
  private CtorVisibility_D() {} // Noncompliant
//        ^^^^^^^^^^^^^^^^
  @Override public void writeExternal(ObjectOutput out) throws IOException { }
  @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
}

class OuterClass {

  private static class CtorVisibility_E implements Externalizable { // Compliant, no-arg constructor is private, but implicit
    @Override public void writeExternal(ObjectOutput out) throws IOException { }
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
  }

  public static class CtorVisibilita_F implements Externalizable { // Compliant, implicit no-arg constructor is public
    @Override public void writeExternal(ObjectOutput out) throws IOException { }
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
  }
}
