import java.util.Vector;
import java.net.URI;

class BadConstantName {

  static final long serialVersionUID = 42L;

  public static final int GOOD_CONSTANT = 0;
  public static final int bad_constant = 0;
  public static int static_field;
  public final int final_field = 0;
  public static final Vector vector = new Vector();
  public static final java.io.File file = new File("test");
  public static final URI uri = null;

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

}
