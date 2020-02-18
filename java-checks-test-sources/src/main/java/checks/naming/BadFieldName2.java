package checks.naming;

class BadFieldName2 {
  public int BAD_FIELD_NAME;
  public int goodFieldName;
  public static int STATIC;

  enum Enum {
    CONSTANT;

    int BAD_FIELD_NAME;
    int goodFieldName;
  }

  interface Interface {
    int SHOULD_NOT_BE_CHECKED = 1;
  }

  @interface Annotation {
    int SHOULD_NOT_BE_CHECKED = 1;
  }
}
