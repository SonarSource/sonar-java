import lombok.experimental.UtilityClass;

// FP happening without semantics.
@UtilityClass
public class Utility { // WithIssue
  public static int triple(int in) {
    return in * 3;
  }
}
