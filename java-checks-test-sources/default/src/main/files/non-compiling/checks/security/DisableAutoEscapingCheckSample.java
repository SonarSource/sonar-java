package checks.security;

import com.samskivert.mustache.Mustache;

public class DisableAutoEscapingCheckSample {
  private String template = "{{foo}}";
  private Object context = new Object() {
    String foo = "<bar>";
  };

  /**
   * https://github.com/samskivert/jmustache
   */
  public void jMustache() {
    Mustache.compiler()
      .escapeHTML(false) // Noncompliant [[sc=19;ec=24]] {{Make sure disabling auto-escaping feature is safe here.}}
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(Unknown.NONE) // Compliant, unknown Type
      .compile(template)
      .execute(context);
  }
}
