package checks.regex;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.validation.constraints.Pattern.Flag;
import org.apache.commons.lang3.RegExUtils;

public abstract class AbstractRegexCheck {

  public static final String CONST = "const";

  @javax.validation.constraints.Pattern( // Noncompliant {{[^@]+const@}}
    regexp = "[^@]+" + CONST + "@")
  private String field1;

  @javax.validation.constraints.Pattern(regexp = "a+", flags = Flag.CASE_INSENSITIVE) // Noncompliant {{a+,initialFlags=2}}
  private String field2;

  @javax.validation.constraints.Pattern(regexp = "a+", flags = { Flag.CASE_INSENSITIVE, Flag.DOTALL }) // Noncompliant {{a+,initialFlags=34}}
  private String field3;

  @javax.validation.constraints.Email(flags = Flag.CASE_INSENSITIVE)
  private String field4;

  @javax.validation.constraints.Email(regexp = "[^@]+@[^@]+") // Noncompliant {{[^@]+@[^@]+}}
  private String field5;

  @javax.validation.constraints.Email(regexp = "a+", flags = Flag.CASE_INSENSITIVE) // Noncompliant {{a+,initialFlags=2}}
  private String field6;

  @javax.validation.constraints.Email(regexp = "a+", flags = { Flag.CASE_INSENSITIVE, Flag.DOTALL }) // Noncompliant {{a+,initialFlags=34}}
  private String field7;

  @org.hibernate.validator.constraints.URL
  private String field8;

  @org.hibernate.validator.constraints.URL(regexp = "[^@]+@[^@]+") // Noncompliant {{[^@]+@[^@]+}}
  private String field9;

  @org.hibernate.validator.constraints.URL(regexp = "a+", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) // Noncompliant {{a+,initialFlags=2}}
  @Nullable
  private String field10;

  @org.hibernate.validator.constraints.URL(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.DOTALL }) // Noncompliant {{a+,initialFlags=34}}
  @SuppressWarnings("coverage")
  private String field11;

  @org.hibernate.validator.constraints.Email(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.UNICODE_CASE, jakarta.validation.constraints.Pattern.Flag.CANON_EQ }) // Noncompliant {{a+,initialFlags=192}}
  @SuppressWarnings(value = "coverage")
  private String field12;

  void test(String regex) {
    f("".matches("a|bc")); // Noncompliant {{a|bc}}
    f("".replace("text1", "text2"));
    f("".replaceFirst("xy", "text")); // Noncompliant {{xy}}
    String a = "a" + "b";
    f("".replaceAll(a, "text")); // Noncompliant {{ab}}
    f(Pattern.compile("\\d")); // Noncompliant {{\\d}}
    f(Pattern.compile("\\w", Pattern.CASE_INSENSITIVE)); // Noncompliant {{\\w,initialFlags=2}}
    f(Pattern.matches("\\d+", "text")); // Noncompliant {{\\d+}}
    f("text".split(",")[0]); // Noncompliant {{,}}
    f("text".split(";", -1)[0]); // Noncompliant {{;}}

    Pattern p = Pattern.compile("abc", Pattern.CASE_INSENSITIVE); // Noncompliant {{abc,initialFlags=2}}
    f(Pattern.compile(p + "d")); // Noncompliant {{abcd}}

    f(Pattern.compile(id("abc"))); // Not detected because we don't track regex patterns through methods

    // org.apache.commons.lang3.RegExUtils
    Pattern pattern = Pattern.compile(regex);
    f(RegExUtils.removeAll("text", pattern));
    f(RegExUtils.removeAll("text", "regex"));  // Noncompliant {{regex}}
    f(RegExUtils.removeFirst("text", pattern));
    f(RegExUtils.removeFirst("text", "regex"));  // Noncompliant {{regex}}
    f(RegExUtils.replaceAll("text", pattern, "text"));
    f(RegExUtils.replaceAll("text", "regex", "text"));  // Noncompliant {{regex}}
    f(RegExUtils.replaceFirst("text", pattern, "text"));
    f(RegExUtils.replaceFirst("text", "regex", "text"));  // Noncompliant {{regex}}
    f(RegExUtils.removePattern("text", "regex"));  // Noncompliant {{regex,initialFlags=32}}
    f(RegExUtils.replacePattern("text", "regex", "text"));  // Noncompliant {{regex,initialFlags=32}}
  }

  abstract void f(boolean x);
  abstract void f(String x);
  abstract void f(Pattern x);

  private String id(String s) {
    return s;
  }

  @jakarta.validation.constraints.Pattern( // Noncompliant {{[^@]+const@}}
    regexp = "[^@]+" + CONST + "@")
  private String jakartaField1;

  @jakarta.validation.constraints.Pattern(regexp = "a+", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) // Noncompliant {{a+,initialFlags=2}}
  private String jakartaField2;

  @jakarta.validation.constraints.Pattern(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.DOTALL }) // Noncompliant {{a+,initialFlags=34}}
  private String jakartaField3;

  @jakarta.validation.constraints.Email(flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE)
  private String jakartaField4;

  @jakarta.validation.constraints.Email(regexp = "[^@]+@[^@]+") // Noncompliant {{[^@]+@[^@]+}}
  private String jakartaField5;

  @jakarta.validation.constraints.Email(regexp = "a+", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) // Noncompliant {{a+,initialFlags=2}}
  private String jakartaField6;

  @jakarta.validation.constraints.Email(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.DOTALL }) // Noncompliant {{a+,initialFlags=34}}
  private String jakartaField7;
}
