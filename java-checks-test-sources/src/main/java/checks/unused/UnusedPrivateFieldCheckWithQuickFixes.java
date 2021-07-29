package checks.unused;

public class UnusedPrivateFieldCheckWithQuickFixes {
  private int a,b; // Noncompliant {{Remove this unused "a" private field.}}

  void foo(int t) {
    foo(b);
  }

  private Object field; // Noncompliant {{Remove this unused "field" private field.}}

  /**
   * Some javadoc
   */
  private final Object javadocField = null; // Noncompliant {{Remove this unused "javadocField" private field.}}
}
