class Foo {
  public static void main(String[] args) {  // Compliant
  }

  static public void main(String[] args) {  // Non-Compliant
  }

  public int a;
}

interface Bar{
  default void fun(){}
}
