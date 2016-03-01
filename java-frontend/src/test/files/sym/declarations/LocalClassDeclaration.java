package declarations;

/**
 * JLS7 14.3. Local Class Declarations
 */
@SuppressWarnings("all")
class LocalClassDeclaration {

  class Superclass {
  }

  void method() {
    {
      class Declaration extends Superclass { // no forward reference here
      }
      class Superclass {
      }
    }
    {
      class Superclass {
      }
      class Declaration extends Superclass {
      }
    }
    class Declaration extends Superclass {
    }
  }

}
