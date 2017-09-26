class MyClass extends Class3 {
  @Override
  protected void finalize() throws Throwable {  // Compliant
    System.out.println("foo");
    super.finalize();
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();                           // Noncompliant [[sc=5;ec=21]] {{Move this super.finalize() call to the end of this Object.finalize() implementation.}}
    System.out.println("foo");
  }

  @Override
  protected void finalize() throws Throwable {  // Noncompliant [[sc=18;ec=26]] {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
    new Object().finalize();
    System.out.println("foo");
  }

  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
    Object object = new Object();
    object.finalize();
    System.out.println("foo");
  }

  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
    finalize();
    System.out.println("foo");
  }

  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
  }

  @Override
  protected void finalize() throws Throwable {  // Noncompliant
    System.out.println("foo");
    super.foo();
  }

  @Override
  protected void foo() throws Throwable {       // Compliant
  }

  void finalize() {
    if (0) {
      super.finalize();
    } else {
      super.finalize();                         // Noncompliant
    }
  }

  void finalize() {
    try {
      // ...
    } finally {
      super.finalize();                         // Compliant
    }

    int a;
  }

  void finalize() {
    try {
      // ...
    } finally {
      super.finalize();                         // Noncompliant
      System.out.println();
    }
  }

  void finalize() {
    try {
      // ...
    } catch (Exception e) {
      super.finalize();                         // Noncompliant
    }
  }
  public void finalize(Object pf, int mode) {

  }
}

class Class3 extends Class1 {
  public void finalize(Object object) {
  }
}

class Class2 extends Class1 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
  }
}

class Class1 {
  @Override
  protected void finalize() throws Throwable {  // Compliant, superclass is java.lang.Object
  }
}
