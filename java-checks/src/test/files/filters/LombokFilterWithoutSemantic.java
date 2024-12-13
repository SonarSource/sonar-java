import lombok.experimental.UtilityClass;

class Utility { // WithIssue
  public static int triple(int in) {
    return in * 3;
  }
}

@UtilityClass
class UtilityAnnotated { // NoIssue
  public static int triple(int in) {
    return in * 3;
  }
}

@lombok.experimental.UtilityClass
class UtilityFullyQualified { // NoIssue
  public static int triple(int in) {
    return in * 3;
  }
}
