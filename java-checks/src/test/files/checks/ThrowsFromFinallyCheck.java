class A {
  public void f() {
    throw new IllegalAccessError();

    try {
      throw new IllegalAccessError();
    } catch (Exception e) {
      throw new IllegalAccessError();
    } finally {
      throw new IllegalAccessError(); // Noncompliant {{Refactor this code to not throw exceptions in finally blocks.}} [[sc=7;ec=38]]

      if (false) {
        throw new IllegalAccessError(); // Noncompliant
      }

      new A() {
        public void f() {
          throw new IllegalAccessError();
        }
      };
      try{

      }catch (Exception e){

      }
      throw new IllegalAccessError(); // Noncompliant
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
