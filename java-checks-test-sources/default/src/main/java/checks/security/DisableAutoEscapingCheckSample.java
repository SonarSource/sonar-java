package checks.security;

import com.samskivert.mustache.Escapers;
import com.samskivert.mustache.Mustache;
import java.util.Locale;

import static com.samskivert.mustache.Escapers.HTML;
import static com.samskivert.mustache.Escapers.NONE;
import static com.samskivert.mustache.Escapers.simple;
import static freemarker.template.Configuration.DISABLE_AUTO_ESCAPING_POLICY;
import static freemarker.template.Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY;
import static freemarker.template.Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY;

public class DisableAutoEscapingCheckSample {
  private String template = "{{foo}}";
  private Object context = new Object() {
    String foo = "<bar>";
  };

  /**
   * https://github.com/samskivert/jmustache
   */
  public void jMustache(boolean arg) {
    Mustache.compiler()
      .escapeHTML(false) // Noncompliant {{Make sure disabling auto-escaping feature is safe here.}}
//                ^^^^^
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .escapeHTML(true) // Compliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .escapeHTML(arg) // Compliant, unknown value
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(Escapers.NONE) // Noncompliant {{Make sure disabling auto-escaping feature is safe here.}}
//                 ^^^^^^^^^^^^^
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(NONE) // Noncompliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(com.samskivert.mustache.Escapers.NONE) // Noncompliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(MyConstant.NONE) // Compliant, user defined
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(Escapers.HTML) // Compliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(HTML) // Compliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(com.samskivert.mustache.Escapers.HTML) // Compliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(Escapers.simple()) // Noncompliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(simple()) // Noncompliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(com.samskivert.mustache.Escapers.simple()) // Noncompliant
      .compile(template)
      .execute(context);

    String[][] escapes = {{"[", "[["}, {"]", "]]"}};
    Mustache.compiler()
      .withEscaper(Escapers.simple(escapes))  // Compliant, as soon as "Escapers.simple" has an argument, as it means that the dev
                                              // is explicitly listing what he wants to escape or not.
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(new MyEscaper()) // Compliant, user defined escaper
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(MyConstant.mySimple()) // Compliant
      .compile(template)
      .execute(context);

    Mustache.compiler()
      .withEscaper(MyConstant.mySimple("a")) // Compliant
      .compile(template)
      .execute(context);

    Mustache.compiler() // Compliant, escaped by default.
      .compile(template)
      .execute(context);
  }

  /**
   * https://freemarker.apache.org/
   */
  public void freemarker(freemarker.template.Configuration config) {
    config.setAutoEscapingPolicy(DISABLE_AUTO_ESCAPING_POLICY); // Noncompliant {{Make sure disabling auto-escaping feature is safe here.}}
//                               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    config.setAutoEscapingPolicy(freemarker.template.Configuration.DISABLE_AUTO_ESCAPING_POLICY); // Noncompliant
    config.setAutoEscapingPolicy(MyConstant.DISABLE_AUTO_ESCAPING_POLICY); // Compliant, user defined constant
    config.setAutoEscapingPolicy(ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY); // Compliant
    config.setAutoEscapingPolicy(ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY); // Compliant
    config.setAutoEscapingPolicy(42); // Compliant
    config.setAutoEscapingPolicy(20); // Compliant, even if it's the value for "Disable"
    config.setLocale(Locale.ENGLISH);
  }

  static class MyConstant {
    static final Mustache.Escaper NONE = null;
    static final int DISABLE_AUTO_ESCAPING_POLICY = 20;

    static Mustache.Escaper mySimple() {
      return new MyEscaper();
    }

    static Mustache.Escaper mySimple(String s) {
      return new MyEscaper();
    }

  }

  static class MyEscaper implements Mustache.Escaper {

    @Override
    public String escape(String s) {
      return s;
    }
  }
}
