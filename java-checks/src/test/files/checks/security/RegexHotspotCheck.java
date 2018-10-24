import java.util.regex.Pattern;

class A {
  String regex; // a regular expression
  String input; // a user input

  void fun() {
    input.matches(regex);  // Noncompliant {{Make sure that using a regular expression is safe here.}}
    Pattern pattern = Pattern.compile(regex);  // Noncompliant
    pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);  // Noncompliant

    String replacement = "test";
    input.replaceAll(regex, replacement);  // Noncompliant [[sc=22;ec=27]]
    input.replaceFirst(regex, replacement);  // Noncompliant
    input.split(regex);  // compliant
    input.split(regex, 3);  // compliant

    pattern.split("foo"); //compliant, excluded
    pattern.split("foo", 12); //compliant, excluded
    java.util.regex.Matcher matcher = pattern.matcher("someinput"); //compliant, excluded
    matcher.find(12); //compliant, excluded

    String htmlString = "some input";
    !Pattern.matches(".*<script>.*", htmlString); // Noncompliant, even if the pattern is hard-coded
  }

  void methodRef() {
    java.util.function.BiFunction<String, String, String> replaceAll = input::replaceAll; // Noncompliant [[sc=79;ec=89]] {{Make sure that using a regular expression is safe here.}}
    java.util.function.BiFunction<String, String, String> replaceFirst = input::replaceFirst; // Noncompliant
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

  @org.hibernate.validator.constraints.URL(regexp = ".*") // Noncompliant
  private String url;

  @org.hibernate.validator.constraints.URL(message = "hello") // compliant no regexp
  private String url2;
  // ...
}
