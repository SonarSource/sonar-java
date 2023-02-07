package checks;

class S1114_Case_1 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {  // Compliant
    System.out.println("foo");
    super.finalize();
  }
}

class S1114_Case_2 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {
    super.finalize();                           // Noncompliant [[sc=5;ec=21]] {{Move this super.finalize() call to the end of this Object.finalize() implementation.}}
    System.out.println("foo");
  }
}

class S1114_Case_3 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant [[sc=18;ec=26]] {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
    new S1114_MyObject().finalize();
    System.out.println("foo");
  }
}

class S1114_Case_4 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
    S1114_MyObject object = new S1114_MyObject();
    object.finalize();
    System.out.println("foo");
  }
}

class S1114_Case_5 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
    finalize();
    System.out.println("foo");
  }
}

class S1114_Case_6 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
  }
}

class S1114_Case_7 extends S1114_Class3 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant
    System.out.println("foo");
    super.foo();
  }

  @Override
  protected void foo() throws Throwable {       // Compliant
  }
}

class S1114_Case_8 extends S1114_Class3 {
  boolean test;

  protected void finalize() throws Throwable {
    if (test) {
      super.finalize();
    } else {
      super.finalize();                         // Noncompliant
    }
  }
}

class S1114_Case_9 extends S1114_Class3 {
  protected void finalize() throws Throwable {
    try {
      // ...
    } finally {
      super.finalize();                         // Compliant
    }

    int a;
  }
}

class S1114_Case_10 extends S1114_Class3 {
  protected void finalize() throws Throwable {
    try {
      // ...
    } finally {
      super.finalize();                         // Noncompliant
      System.out.println();
    }
  }
}

class S1114_Case_11 extends S1114_Class3 {
  protected void finalize() throws Throwable {
    try {
      // ...
    } catch (Exception e) {
      super.finalize();                         // Noncompliant
    }
  }

  public void finalize(Object pf, int mode) {
  }
}

class S1114_Class3 extends S1114_Class1 {
  public void finalize(Object object) {
  }
}

class S1114_Class2 extends S1114_Class1 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Add a call to super.finalize() at the end of this Object.finalize() implementation.}}
  }
}

abstract class S1114_Class4 extends S1114_Class1 {
  @Override
  abstract protected void finalize() throws Throwable;
}

class S1114_Class1 {
  @Override
  protected void finalize() throws Throwable {  // Compliant, superclass is java.lang.Object
  }

  protected void foo() throws Throwable {
  }
}

class S1114_MyObject {

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }
}

class CutControlFlowInnerClass extends S1114_Class1 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant, there is no call to finalize here
    class InnerClass {
      void foo() throws Throwable {
        super.finalize(); // Compliant, this call is not inside the control flow of a finalize method
      }
    }
  }
}

class CutControlFlowLambda extends S1114_Class1 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant, there is no call to finalize here

    ThrowingRunnable r = () -> super.finalize(); // Compliant, this call is not inside the control flow of a finalize method
    foo();
  }

  @FunctionalInterface
  private static interface ThrowingRunnable {
      public void run() throws Throwable;
  }
}

class ScanAlsoInnerClasses extends S1114_Class1 {
  @Override
  protected void finalize() throws Throwable {  // Noncompliant, there is no call to finalize here
    class InnerClassCompliant {
      @Override
      public void finalize() throws Throwable {
        super.finalize(); // Compliant, this call is not inside the control flow of a finalize method
      }
    }

    class InnerClassNonCompliant {
      @Override
      public void finalize() throws Throwable {
        foo(); // Noncompliant, there is no call to finalize here
      }
    }
  }

  protected void bar() throws Throwable {  // Compliant, this is not a finalize method.
    class InnerClassCompliant {
      @Override
      public void finalize() throws Throwable {
        super.finalize(); // Compliant, this call is not inside the control flow of a finalize method
      }
    }

    class InnerClassNonCompliant {
      @Override
      public void finalize() throws Throwable {
        foo(); // Noncompliant, there is no call to finalize here
      }
    }
  }
  
  class InnerClassCompliant {
    @Override
    public void finalize() throws Throwable {
      super.finalize(); // Compliant, this call is not inside the control flow of a finalize method
    }
  }

  class InnerClassNonCompliant {
    @Override
    public void finalize() throws Throwable {
      foo(); // Noncompliant, there is no call to finalize here
    }
  }
}
