class A {
  public static void main(String[] args) {
    Runnable runnable = null;

    Thread myThread = new Thread(runnable);
    myThread.run(); // Noncompliant

    Thread myThread = new Thread(runnable);
    myThread.start(); // Compliant
  }

  public String run() {
    A run = new A();
    run.run().toString(); // Compliant
  }
}
