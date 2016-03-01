package declarations;

/**
 * JLS7 8.9. Enums
 */
@SuppressWarnings("all")
class EnumDeclaration {

  private enum Declaration implements FirstInterface, SecondInterface {
    FIRST_CONSTANT {
      int method() {
        return 1;
      }
    },
    SECOND_CONSTANT {
      int method() {
        return 2;
      }
    };

    abstract int method();
  }

  private interface FirstInterface {
  }

  private interface SecondInterface {
  }

  public static void main(String[] args) {
    System.out.println(Declaration.FIRST_CONSTANT.method());
    System.out.println(Declaration.SECOND_CONSTANT.method());
  }

  private enum ConstructorEnum {
    ID(1),
    IDA("");
    ConstructorEnum(int i){}
    ConstructorEnum(String i){}
  }

  Enum<Declaration> parameterizedDeclaration;

}
