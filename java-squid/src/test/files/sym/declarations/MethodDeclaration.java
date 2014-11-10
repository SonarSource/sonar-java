package declarations;

/**
 * JLS7 8.4. Method Declarations
 */
@SuppressWarnings("all")
class MethodDeclaration {

  class FirstExceptionType extends Throwable {
  }

  class SecondExceptionType extends Throwable {
  }

  class ReturnType {
  }

  class ParameterType {
  }

  protected <T> ReturnType declaration(ParameterType param) throws FirstExceptionType, SecondExceptionType {
    return null;
  }

}
