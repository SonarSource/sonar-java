class BadFieldName {
  public int BAD_FIELD_NAME;
  public int goodFieldName;
  public static int BAD_FIELD_NAME_STATIC_NON_FINAL; // Noncompliant {{Rename this field "BAD_FIELD_NAME_STATIC_NON_FINAL" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
//                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  public static int goodFieldNameStaticNonFinal;
  public final static int STATIC; // Compliant, final modifier

  enum Enum {
    CONSTANT;

    int BAD_FIELD_NAME;
    int goodFieldName;
    static int BAD_FIELD_NAME_STATIC_NON_FINAL; // Noncompliant {{Rename this field "BAD_FIELD_NAME_STATIC_NON_FINAL" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
    static int goodFieldNameStaticNonFinal;
  }

  interface Interface {
    int SHOULD_NOT_BE_CHECKED = 1;
  }

  @interface Annotation {
    int SHOULD_NOT_BE_CHECKED = 1;
  }
}
