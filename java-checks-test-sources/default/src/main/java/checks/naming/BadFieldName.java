package checks.naming;

class BadFieldName {
  public int BAD_FIELD_NAME; // Noncompliant [[sc=14;ec=28]] {{Rename this field "BAD_FIELD_NAME" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
  public static int STATIC;
  public int goodFieldName;

  enum Enum {
    CONSTANT;

    int BAD_FIELD_NAME; // Noncompliant {{Rename this field "BAD_FIELD_NAME" to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
    int goodFieldName;
  }

  interface Interface {
    int SHOULD_NOT_BE_CHECKED = 1;
  }

  @interface Annotation {
    int SHOULD_NOT_BE_CHECKED = 1;
  }
}
