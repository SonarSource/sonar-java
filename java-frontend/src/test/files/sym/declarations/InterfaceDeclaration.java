package declarations;

/**
 * JLS7 9.1. Interface Declarations
 */
@SuppressWarnings("all")
class InterfaceDeclaration {

  // JLS7 6.6.1: All members of interfaces lacking access modifiers are implicitly public.
  private interface Declaration extends FirstInterface, SecondInterface {
    int FIRST_CONSTANT = 1,
        SECOND_CONSTANT = 2;

    void method();

    static void staticMethod() {
    }

    default void defaultMethod() {
    }

    class NestedClass {
    }

    interface NestedInterface {
    }

    enum NestedEnum {
    }

    @interface NestedAnnotationType {
    }
  }

  private interface FirstInterface {
  }

  private interface SecondInterface {
  }

  interface IfaceWithPrivateMethods {
    private void thisIsPrivate() {

    }

    private static void staticPrivate() {

    }
  }

}
