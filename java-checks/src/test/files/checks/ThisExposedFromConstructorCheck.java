import java.util.List;

class A{
  private static final List<A> list;
  private String location;
  private static String loc;
  private static A tempVal;
  public A Instance;
  private B Instance2;
  
  public A(String location) {
    list.add(this);  // Noncompliant [sc=16;ec=20] {{Make sure the use of "this" doesn't expose partially-constructed instances of this class in multi-threaded environments.}}
    foo(this);  // Compliant
    this.location = location; // Compliant
    this.tempVal = this;  // Compliant
    Instance = this;  // Noncompliant
    this.Instance = this; // Noncompliant
    tempVal = this;  // Compliant
    this.loc = "loc"; // Compliant
    foo(Instance2.foo3(this)); // Noncompliant  
    this.Instance2.foo(this); // Noncompliant
    this.Instance.foo3(this.Instance2.foo(this)); // Noncompliant
    this.tempVal = Instance.foo(this);  // Compliant
    this.Instance2 = this;  // Noncompliant
  }

  public A() {
    this.location = ""; // Compliant
    foo2(this);  // Noncompliant
    foo2(); // Compliant
    foo3(); //  Compliant
    foo1(new A());  // Compliant
    B.foo2();  // Compliant
    B.foo1(this);  // Noncompliant
    B.field = this;  // Noncompliant
    B.field3 = this; // Noncompliant
    this.Instance2.foo3(this); // Noncompliant
    this.foo3(this);  // Compliant
    this.Instance = (this); // Noncompliant
    this.tempVal = (this); // Compliant
  }
  
  public void foo(A a) {}
  
  private void foo2() {
    list.add(this);
  }
  
  private void foo3(A a) {
  }
  
}

class B {
  public static final List<A> list1;
  public static A field;
  public A field3;
  public B field2;
  public B[] arr1;
    
  public B() {
    this.field2 = this;  // Noncompliant
    this.field3 = foo2(this);  // Compliant
    new A() {
    };
    new Thread() {
      public void run() {
        A a = new A();
        a.Instance2 = this;  // Compliant
      }
    }.start();
   arr1[0] = this;  // Noncompliant
  }
  
  void foo(A a) {
  }
  
  A foo2(B b) {}
  
  public void foo3(A a) {
  }
}
