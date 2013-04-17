package declarations;

/**
 * JLS7 8.1. Class Declarations
 */
@SuppressWarnings("all")
class ClassDeclaration {

  private class Declaration extends Superclass implements FirstInterface, SecondInterface {
  }

  private class Superclass {
  }

  private interface FirstInterface {
  }

  private interface SecondInterface {
  }

}
