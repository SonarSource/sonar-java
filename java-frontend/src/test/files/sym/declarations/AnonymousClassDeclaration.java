package declarations;

/**
 * JLS7 15.9.5. Anonymous Class Declarations
 */
@SuppressWarnings("all")
class AnonymousClassDeclaration {

  class Superclass {
  }


  interface SuperInterface {
  }

  void method() {
    new Superclass() {
      void methodInAnonymousClass() {
      }
    };
    new SuperInterface() {
      void methodInAnonymousClassInterface() {
      }
    };
  }



}
