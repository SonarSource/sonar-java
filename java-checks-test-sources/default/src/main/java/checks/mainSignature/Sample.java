package checks.mainSignature;

public class Sample {
  public static class Traditional {
    public static void main(String[] args) { // Compliant
      System.out.println("traditional");
    }
  }

  public static class NoArg {
    public static void main() { // Compliant
      System.out.println("no arg");
    }
  }

  public static class Instance {
    void main(String[] args) { // Compliant
      System.out.println("instance");
    }
  }

  public static class Wrong {
    public static void main(String[] args) {
      System.out.println(new Wrong().main(42));
    }

    int main(int x) { // Noncompliant
      return x * x;
    }

    String main(String x, String y) { // Noncompliant
      return "";
    }
  }

  public static class WrongReturn {
    public static int main(String[] args) { // Noncompliant
      return 1;
    }
  }

  @interface MyAnnotation {
    String main() default ""; // Noncompliant
  }

  enum MyEnum {
    A, B;

    void main(int x) { // Noncompliant
    }
  }

  record MyRecord(int value) {
    void main(int x) { // Noncompliant
    }

    static void main(String[] args) {
    }
  }
}
