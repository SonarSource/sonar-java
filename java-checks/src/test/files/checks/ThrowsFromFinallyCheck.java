class A {
  public void f() {
    throw new IllegalAccessError();

    try {
      throw new IllegalAccessError();
    } catch (Exception e) {
      throw new IllegalAccessError();
    } finally {
      throw new IllegalAccessError();   // Non-Compliant

      if (false) {
        throw new IllegalAccessError(); // Non-Compliant
      }

      new A() {
        public void f() {
          throw new IllegalAccessError();
        }
      };
      try{

      }catch (Exception e){

      }
      throw new IllegalAccessError();
    }
  }

  {
    throw new IllegalAccessError();
  }

  public A() {
    throw new IllegalAccessError();
  }
}

public interface B {
  public default void f() {
    throw new IllegalAccessError();
  }
}
