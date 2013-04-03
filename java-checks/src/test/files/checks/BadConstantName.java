class BadConstantName {

  public static final int GOOD_CONSTANT = 0;
  public static final int bad_constant = 0;
  public static int static_field;
  public final int final_field = 0;

  enum Enum {
    GOOD_CONSTANT,
    bad_constant;
  }

}
