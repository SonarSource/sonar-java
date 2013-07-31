class A {
  static { // Compliant
  }

  { // Non-Compliant
    System.out.println();
  }

  public A() { // Compliant
    System.out.println();

    new Runnable() {

      { // Non-Compliant
        System.out.println();
      }

      @Override
      public void run() {
      }
    };
  }
}
