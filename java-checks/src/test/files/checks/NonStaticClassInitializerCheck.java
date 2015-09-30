class A {
  static {
  }

  { // Noncompliant {{Move the contents of this initializer to a standard constructor or to field initializers.}}
    System.out.println();
  }

  public A() {
    System.out.println();

    new Runnable() {

      { // Noncompliant
        System.out.println();
      }

      @Override
      public void run() {
      }
    };
  }
}
