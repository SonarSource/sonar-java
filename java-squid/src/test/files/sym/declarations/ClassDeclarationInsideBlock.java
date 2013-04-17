package declarations;

@SuppressWarnings("UnusedDeclaration")
class ClassDeclarationInsideBlock {

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
