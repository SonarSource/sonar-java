import java.util.regex.Pattern;

class A {

  String input; // a user input

  void fun() {
    input.matches("(a+)*");  // Noncompliant {{Make sure that using a regular expression is safe here.}}
    Pattern pattern = Pattern.compile("(a+)*");  // Noncompliant
    pattern = Pattern.compile("(a+)*", Pattern.CASE_INSENSITIVE);  // Noncompliant

    String replacement = "test";
    input.replaceAll("(a+)*", replacement);  // Noncompliant [[sc=22;ec=29]]
    input.replaceFirst("(a+)*", replacement);  // Noncompliant
    input.split("(a+)*");  // compliant
    input.split("(a+)*", 3);  // compliant

    pattern.split("foo"); //compliant, excluded
    pattern.split("foo", 12); //compliant, excluded
    java.util.regex.Matcher matcher = pattern.matcher("someinput"); //compliant, excluded
    matcher.find(12); //compliant, excluded

    String htmlString = "some input";
    boolean a = !Pattern.matches(".*<script>.*", htmlString); // Noncompliant
  }

  void methodRef() {
    java.util.function.BiFunction<String, String, String> replaceAll = input::replaceAll; // compliant
    java.util.function.BiFunction<String, String, String> replaceFirst = input::replaceFirst; // compliant
  }
}

public class Test implements Serializable {
  @javax.validation.constraints.Pattern(regexp = ".+@.+") // Noncompliant [[sc=41;ec=57]] {{Make sure that using a regular expression is safe here.}}
  private String email;

  @javax.validation.constraints.Pattern
  @javax.validation.constraints.Pattern("arg")
  private String emailNoPattern; // compliant no regexp param : non compilable example for coverage

  @javax.validation.constraints.Email(regexp = ".+@.+") // Noncompliant
  private String email2;

  @javax.validation.constraints.Email(messge = "yabadabadoo") // compliant no regexp
  private String email3;

  @org.hibernate.validator.constraints.URL(regexp = "(a+)*") // Noncompliant
  private String url;

  @org.hibernate.validator.constraints.URL(message = "hello") // compliant no regexp
  private String url2;

  @org.hibernate.validator.constraints.URL(regexp = ".") // compliant, safeRegex
  private String url3;

}

private class SafeRegex {
  void foo(String input) {
    String replacement = "test";
    input.replaceAll("", replacement);
    input.replaceAll("a", replacement);
    input.replaceAll(".", replacement);
    input.replaceAll("asdklj44_", replacement);
    input.replaceAll("a+", replacement);
    input.replaceAll("a*", replacement);
    input.replaceAll("a{1}", replacement);
    input.replaceAll("(a{1})*", replacement); // Noncompliant
    input.replaceAll("(a+)*", replacement); // Noncompliant
    input.replaceAll("(a{1})+", replacement); // Noncompliant
    input.replaceAll("(a+)+", replacement); // Noncompliant
    input.replaceAll("(a{1}){2}", replacement); // Noncompliant
  }
}
