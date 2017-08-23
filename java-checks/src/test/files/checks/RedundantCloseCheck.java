abstract class A {
  MyCloseable mc3;

  void foo(MyCloseable mc2) {
    MyCloseable mc4 = new MyCloseable();

    try(
      // java 7
      MyCloseable mc1 = new MyCloseable();
      // java 9
      mc2; this.mc3; unknownVariable) {

      mc1.close(); // Noncompliant [[sc=11;ec=18]] {{Remove this "close" call; closing the resource is handled automatically by the try-with-resources.}}
      mc2.close(); // Noncompliant
      mc3.close(); // Noncompliant

      mc4.close(); // Compliant - not part of auto-closed resources
      getCloseable().close(); // Compliant

      getCloseable();
    }

    try {
      mc4.close();
    } catch (Exception e) {
      // do something
    }
  }

  void foo(java.io.FileOutputStream fos, java.io.File file) {

    try (java.io.PrintWriter writer = new java.io.PrintWriter(fos)) {
      String contents = file.getName();
      writer.write(contents);
      writer.flush();
      writer.close(); // Noncompliant
    }
  }

  abstract AutoCloseable getCloseable();

  static class MyCloseable implements AutoCloseable {
    @Override
    public void close() throws Exception {
      // do something
    }

    void foo() {
      try(MyCloseable mc = new MyCloseable()) {
        close(); // Compliant
      } catch (Exception e) {
        // do something
      }
    }
  }
}
