/**
 * some documentation
 */
class UndocumentedApi { // Compliant - documented
  public String p; // Non-Compliant
  private String key; // Compliant - private

  public UndocumentedApi() { // Compliant - empty constructor
  }

  public UndocumentedApi(String key) { // Non-Compliant
    this.key = key;
  }

  public void run() { // Non-Compliant
  }

  public interface InnerUndocumentedInterface { // Non-Compliant
  }

  /**
   * no violation, because documented
   */
  public void run2() {
  }

  public void setKey(String key) { // Compliant - setter
    this.key = key;
  }

  public String getKey() { // Compliant - getter
    return key;
  }

  @Override
  public String toString() { // Compliant - method with override annotation
    return key;
  }

  public static final int FOO = 0; // Non-Compliant
  private static final int BAR = 0; // Compliant - private
  int a = 0; // Compliant

}

public enum Foo { // Non-Compliant
}

public interface A { // Non-Compliant
}

public @interface Foo { // Non-Compliant
}

public class A { // Non-Compliant

  public int a; // Non-Compliant

  public A() { // Non-Compliant
    System.out.println();
  }

}
