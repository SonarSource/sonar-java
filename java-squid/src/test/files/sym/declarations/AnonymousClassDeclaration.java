package declarations;

/**
 * JLS7 15.9.5. Anonymous Class Declarations
 */
@SuppressWarnings("all")
class AnonymousClassDeclaration {

  void method() {
    new Object() {
      void methodInAnonymousClass() {
      }
    };
  }

}
