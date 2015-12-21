import java.lang.Object;

class My {
  My() {
  }

  int My() { // Noncompliant [[sc=7;ec=9]] {{Rename this method to prevent any misunderstanding or make it a constructor.}}
  }

  void foo() {
    new Object() {
    };
  }
}
