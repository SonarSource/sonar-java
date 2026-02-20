// import module
import module java.base;
// compact source file
void main() {}

  class Parent {}
  class Child extends Parent{
  // flexible constructor body
    Child() {
      IO.println("Hello from Child constructor");
      super();
    }
  }
