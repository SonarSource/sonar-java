package declarations;

/**
 * JLS7 8.8. Constructor Declarations
 */
@SuppressWarnings("all")
class ConstructorDeclaration {

  class FirstExceptionType extends Throwable {
  }

  class SecondExceptionType extends Throwable {
  }

  class ParameterType {
  }

  ConstructorDeclaration(ParameterType param) throws  FirstExceptionType, SecondExceptionType {
  }

  ConstructorDeclaration cd = new ConstructorDeclaration(null);

  ParameterType pt = new ParameterType();

  abstract class Foo {}
  class Bar extends Foo {
    Bar(int i) {
      super();
    }
  }
}
