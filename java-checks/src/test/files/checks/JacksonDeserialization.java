import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.MINIMAL_CLASS;

class JacksonDeserialization {

  public void enableDefaultTyping() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enableDefaultTyping(); // Noncompliant [[sc=5;ec=33]] {{Consider using @JsonTypeInfo instead of enabling polymorphic type handling globally}}
  }

}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) // Noncompliant [[sc=21;ec=42]] {{Use @JsonTypeInfo(use = Id.NAME) instead}}
abstract class PhoneNumber {

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) // Noncompliant {{Use @JsonTypeInfo(use = Id.NAME) instead}}
  String field;
}

@JsonTypeInfo(use = MINIMAL_CLASS) // Noncompliant
abstract class PhoneNumber2 {

  @JsonTypeInfo(use = MINIMAL_CLASS)   // not applied on method (probably not even legal)
  String method() {

  }

}

// test below is testing older versions of Jackson with different package name

class JacksonCodehaus {
  public void enableDefaultTyping() {
    org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
    mapper.enableDefaultTyping(); // Noncompliant
  }


  @org.codehaus.jackson.annotate.JsonTypeInfo(use = org.codehaus.jackson.annotate.JsonTypeInfo.Id.CLASS) // Noncompliant
  abstract class PhoneNumber {

  }

  @org.codehaus.jackson.annotate.JsonTypeInfo(use = org.codehaus.jackson.annotate.JsonTypeInfo.Id.MINIMAL_CLASS) // Noncompliant
  abstract class PhoneNumber2 {

  }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
class CompleteCoverage {
  public void method(@Nullable Object arg) {
    method(null);
  }
}
