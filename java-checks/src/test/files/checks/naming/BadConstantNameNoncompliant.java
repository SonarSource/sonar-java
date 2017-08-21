class BadConstantName {

  static final long serialVersionUID = 42L;

  public static final int GOOD_CONSTANT = 0;
  public static final int bad_constant = 0; // Noncompliant [[sc=27;ec=39]] {{Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.}}
  public static int static_field;
  public final int final_field = 0;
  public static final Object object = 0;
  enum Enum {
    GOOD_CONSTANT,
    bad_constant; // Noncompliant

    int SHOULD_NOT_BE_CHECKED;
  }

  interface Interface {
    int GOOD_CONSTANT = 1,
        bad_constant = 2; // Noncompliant

    void foo();
  }

  @interface AnnotationType {
    int GOOD_CONSTANT = 1,
        bad_constant = 2; // Noncompliant

    long serialVersionUID = 42L;
  }

  public static final String my_string = 0; // Noncompliant
  public static final String MY_STRING = 0; 
  public static final Double my_double = 0; // Noncompliant

}
