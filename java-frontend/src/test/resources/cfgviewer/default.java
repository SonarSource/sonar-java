import java.io.IOException;

abstract class A {
  int foo(boolean a, boolean b) {
    try {
      doSomething(a);
    } catch (IOException e) {
      return -1;
    }
    if (a && b) {
      return 1;
    } else {
      return 0;
    }
  }

  abstract void doSomething(boolean a) throws IOException;
}
