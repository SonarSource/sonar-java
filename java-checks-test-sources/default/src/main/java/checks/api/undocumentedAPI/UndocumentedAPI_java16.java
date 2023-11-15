package checks.api.undocumentedAPI;

/**
 * This is documented
 */
public class UndocumentedAPI_java16 {

  /**
   * This is documented
   */
  public sealed interface Shape permits Unicorn, Lemniscate {

    /**
     * This is documented
     * @return looks fun to be there
     */
    double area();
  }

  public record Unicorn(String name, double area) implements Shape { } // Noncompliant {{Document this public record by adding an explicit description.}}

  /**
   * This is documented
   */
  public record Lemniscate(double area) implements Shape { }
}
