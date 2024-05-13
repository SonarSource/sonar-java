package checks;

class NonStaticClassInitializerCheckSample {
  static {
  }

  { // Noncompliant {{Move the contents of this initializer to a standard constructor or to field initializers.}}
//^
    System.out.println();
  }

  public NonStaticClassInitializerCheckSample() {
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
