import java.lang.Object;

class My {
  My() {
  }

  int My() { // Noncompliant {{Rename this method to prevent any misunderstanding or make it a constructor.}}
//    ^^
  }

  void foo() {
    new Object() {
    };
  }
}
