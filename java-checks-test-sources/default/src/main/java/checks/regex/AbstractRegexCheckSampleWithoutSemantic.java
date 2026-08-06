package checks.regex;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.validation.constraints.Pattern.Flag;
import org.apache.commons.lang3.RegExUtils;

public abstract class AbstractRegexCheckSampleWithoutSemantic {

  public static final String CONST = "const";

  @javax.validation.constraints.Pattern( // Noncompliant
    regexp = "[^@]+" + CONST + "@")
  private String field1;

  @javax.validation.constraints.Pattern(regexp = "a+", flags = Flag.CASE_INSENSITIVE) // Noncompliant
  private String field2;

  @javax.validation.constraints.Pattern(regexp = "a+", flags = { Flag.CASE_INSENSITIVE, Flag.DOTALL }) // Noncompliant
  private String field3;

  @javax.validation.constraints.Email(flags = Flag.CASE_INSENSITIVE)
  private String field4;

  @javax.validation.constraints.Email(regexp = "[^@]+@[^@]+") // Noncompliant
  private String field5;

  @javax.validation.constraints.Email(regexp = "a+", flags = Flag.CASE_INSENSITIVE) // Noncompliant
  private String field6;

  @javax.validation.constraints.Email(regexp = "a+", flags = { Flag.CASE_INSENSITIVE, Flag.DOTALL }) // Noncompliant
  private String field7;

  @org.hibernate.validator.constraints.URL
  private String field8;

  @org.hibernate.validator.constraints.URL(regexp = "[^@]+@[^@]+") // Noncompliant
  private String field9;

  @org.hibernate.validator.constraints.URL(regexp = "a+", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) // Noncompliant
  @Nullable
  private String field10;

  @org.hibernate.validator.constraints.URL(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.DOTALL }) // Noncompliant
  @SuppressWarnings("coverage")
  private String field11;

  @org.hibernate.validator.constraints.Email(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.UNICODE_CASE, jakarta.validation.constraints.Pattern.Flag.CANON_EQ }) // Noncompliant
  @SuppressWarnings(value = "coverage")
  private String field12;

  void test(String regex) {
    f("".matches("a|bc")); // Noncompliant
    f("".replace("text1", "text2"));
    f("".replaceFirst("xy", "text")); // Noncompliant
    String a = "a" + "b";
    f("".replaceAll(a, "text")); // Noncompliant
    f(Pattern.compile("\\d")); // Noncompliant
    f(Pattern.compile("\\w", Pattern.CASE_INSENSITIVE)); // Noncompliant
    f(Pattern.matches("\\d+", "text")); // Noncompliant
    f("text".split(",")[0]); // Noncompliant
    f("text".split(";", -1)[0]); // Noncompliant

    Pattern p = Pattern.compile("abc", Pattern.CASE_INSENSITIVE); // Noncompliant
    f(Pattern.compile(p + "d")); // Noncompliant

    f(Pattern.compile(id("abc"))); // Not detected because we don't track regex patterns through methods

    // org.apache.commons.lang3.RegExUtils
    Pattern pattern = Pattern.compile(regex);
    f(RegExUtils.removeAll("text", pattern));
    f(RegExUtils.removeFirst("text", pattern));
    f(RegExUtils.replaceAll("text", pattern, "text"));
    f(RegExUtils.replaceFirst("text", pattern, "text"));
  }

  abstract void f(boolean x);
  abstract void f(String x);
  abstract void f(Pattern x);

  private String id(String s) {
    return s;
  }

  @jakarta.validation.constraints.Pattern( // Noncompliant
    regexp = "[^@]+" + CONST + "@")
  private String jakartaField1;

  @jakarta.validation.constraints.Pattern(regexp = "a+", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) // Noncompliant
  private String jakartaField2;

  @jakarta.validation.constraints.Pattern(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.DOTALL }) // Noncompliant
  private String jakartaField3;

  @jakarta.validation.constraints.Email(flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE)
  private String jakartaField4;

  @jakarta.validation.constraints.Email(regexp = "[^@]+@[^@]+") // Noncompliant
  private String jakartaField5;

  @jakarta.validation.constraints.Email(regexp = "a+", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) // Noncompliant
  private String jakartaField6;

  @jakarta.validation.constraints.Email(regexp = "a+", flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.DOTALL }) // Noncompliant
  private String jakartaField7;
}
