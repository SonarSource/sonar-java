package references;

@SuppressWarnings("all")
class FieldAccess {

  static FirstStaticNestedClass field;

  private void method() {
    field = null;
    this.field = null;
    FieldAccess.field = null;
    references.FieldAccess.field = null;

    FirstStaticNestedClass.field_in_FirstStaticNestedClass = 1;
    FirstStaticNestedClass.SecondStaticNestedClass.field_in_SecondStaticNestedClass = 1;
    field.field_in_FirstStaticNestedClass = 1;
    field.field_in_Superclass = 1;
  }

  static class FirstStaticNestedClass extends Superclass {
    static int field_in_FirstStaticNestedClass;

    static class SecondStaticNestedClass {
      static int field_in_SecondStaticNestedClass;
    }
  }

  static class Superclass {
    int field_in_Superclass;
  }

}
