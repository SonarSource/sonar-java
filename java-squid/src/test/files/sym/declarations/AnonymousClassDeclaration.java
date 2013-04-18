package declarations;

/**
 * JLS7 15.9.5. Anonymous Class Declarations
 */
@SuppressWarnings("all")
class AnonymousClassDeclaration {

  class Superclass {
  }

  void method() {
    new Superclass() {
      void methodInAnonymousClass() {
      }
    };
  }

}
