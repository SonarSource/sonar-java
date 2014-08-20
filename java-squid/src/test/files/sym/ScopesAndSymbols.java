@SuppressWarnings("all")
class ScopesAndSymbols {

  static interface T0 {
    int CONSTANT = 1;

    void method(int param) throws Exception;
  }

  static class T1 {
    static final int CONSTANT = 1;
    int field, anotherField;

    static {
      System.out.println("static initializer 1");
    }

    static {
      System.out.println("static initializer 2");
    }

    {
      System.out.println("instance initializer 1");
    }

    {
      System.out.println("instance initializer 2");
    }

    T1() {
       System.out.println("class constructor");
    }

    void method(int... varArg) throws Exception {
      int localVariable;
      System.out.println("class method");
    }

    void method(String a, int @Foo @Bar ... varArg) throws Exception {
      int localVariable;
      System.out.println("class method");
    }
  }

  static enum T2 {
    CONSTANT(1);

    T2(int param) {
      System.out.println("enum constructor");
    }

    void method() {
      System.out.println("enum method");
    }
  }

  @interface T3 {
    int CONSTANT = 1;

    int method();
  }

  public static void main(String[] args) throws Exception {
    new T1().method();
    T2.CONSTANT.method();
  }

}
