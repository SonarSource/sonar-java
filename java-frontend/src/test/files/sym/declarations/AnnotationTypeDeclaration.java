package declarations;

/**
 * JLS7 9.6. Annotation Types
 */
@SuppressWarnings("all")
class AnnotationTypeDeclaration {

  // JLS7 9.6: Unless explicitly modified, all of the rules that apply to ordinary interface declarations apply to annotation type declarations.
  // JLS7 6.6.1: All members of interfaces are implicitly public.
  private @interface Declaration {
    int FIRST_CONSTANT = 1,
        SECOND_CONSTANT = 2;

    int value() default 42;

    class NestedClass {
    }

    interface NestedInterface {
    }

    enum NestedEnum {
    }

    @interface NestedAnnotationType {
    }
  }

}
