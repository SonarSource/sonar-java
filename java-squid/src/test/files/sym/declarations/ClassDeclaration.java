package declarations;

/**
 * JLS7 8.1. Class Declarations
 */
@SuppressWarnings("all")
class ClassDeclaration {

  private class Declaration extends @TypeAnnotation Superclass implements FirstInterface, SecondInterface {
  }

  private class Superclass {
  }

  private interface FirstInterface {
  }

  private interface SecondInterface {
  }

  class Example {
    static class Foo extends Bar.Baz {} // Bar.Baz cannot be resolved if hierarchy of Bar is incomplete
    static class Bar extends Base {}
    static class Base {
      static class Baz extends Declaration{
      }
    }
  }

}
