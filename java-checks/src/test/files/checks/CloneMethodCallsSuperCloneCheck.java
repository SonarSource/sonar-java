class A {
  @Override
  protected A clone() throws CloneNotSupportedException { // Noncompliant [[sc=15;ec=20]] {{Use super.clone() to create and seed the cloned instance to be returned.}}
    return new A();
  }

  private Object foo() { // Compliant
  }
}

class B {
  @Override
  public B clone() throws CloneNotSupportedException { // Compliant
    return (B)super.clone();
  }
}

class C {
  @Override
  protected Object clone() { // Compliant
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }
}

class D {
  @Override
  public Object clone() throws CloneNotSupportedException { // Compliant
    super.clone();
    return new D();
  }
}

class E {
  @Override
  protected Object clone(Object a) throws CloneNotSupportedException { // Compliant - does not override Object.clone()
    return a;
  }
}

class F {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Noncompliant {{Use super.clone() to create and seed the cloned instance to be returned.}}
    return super.toString();
  }
}
class G0 {
  int clone;
}
class G1 extends G0 {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Noncompliant {{Use super.clone() to create and seed the cloned instance to be returned.}}
    super();
    int c = super.clone;
    return super.clone("foo");
  }

  private int clone;
}

class H {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Compliant - limitation
    class H2 {
      @Override
      protected Object clone() throws CloneNotSupportedException { // Compliant
        return super.clone();
      }
    }
  }
}
