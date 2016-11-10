import java.io.IOException;

/**
 * Only displays CFG for the first method of the class
 */
abstract class A {
  int foo(boolean a, boolean b) {
    try {
      System.out.println("Exception?");
      this.doSomething(a);
    } catch (IOException e) {
      return -1;
    }
    if (a && b) {
      return 1;
    } else {
      return 0;
    }
  }

  // Only used to test exception flow
  abstract void doSomething(boolean a) throws IOException;
}
