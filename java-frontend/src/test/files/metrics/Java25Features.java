// import module
import module java.base;
import java.logging.Logger;

// compact source file
void main() {}

  class Parent {}
  class Child extends Parent{
    // flexible constructor body
    Child() {
      IO.println("Hello from Child constructor");
      super();
    }

    Child(int i) {
      IO.println("Hello from Child constructor");
      IO.println("Hello from Child constructor");
    }
  }
