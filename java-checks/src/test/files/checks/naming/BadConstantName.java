class BadConstantName {

  static final long serialVersionUID = 42L;

  public static final int GOOD_CONSTANT = 0;
  public static final int bad_constant = 0;
  public static int static_field;
  public final int final_field = 0;
  public static final Object object = 0;
  enum Enum {
    GOOD_CONSTANT,
    bad_constant;

    int SHOULD_NOT_BE_CHECKED;
  }

  interface Interface {
    int GOOD_CONSTANT = 1,
        bad_constant = 2;
  }

  @interface AnnotationType {
    int GOOD_CONSTANT = 1,
        bad_constant = 2;

    long serialVersionUID = 42L;
  }

  public static final String my_string = 0;
  public static final String MY_STRING = 0; 
  public static final Double my_double = 0;

}
