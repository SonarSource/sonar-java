package declarations;

/**
 * JLS7 8.9. Enums
 */
@SuppressWarnings("all")
class EnumDeclaration {

  private enum Declaration implements FirstInterface, SecondInterface {
    FIRST_CONSTANT,
    SECOND_CONSTANT;
  }

  private interface FirstInterface {
  }

  private interface SecondInterface {
  }

}
