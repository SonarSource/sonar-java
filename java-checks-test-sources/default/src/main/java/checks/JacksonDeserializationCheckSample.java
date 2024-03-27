package checks;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Nullable;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.MINIMAL_CLASS;

class JacksonDeserializationCheckSample {

  public void enableDefaultTyping() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enableDefaultTyping(); // Noncompliant [[sc=5;ec=33]] {{Make sure using this Jackson deserialization configuration is safe here.}}
  }

}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) // Noncompliant [[sc=21;ec=42]] {{Make sure using this Jackson deserialization configuration is safe here.}}
abstract class JacksonDeserializationPhoneNumber {

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) // Noncompliant {{Make sure using this Jackson deserialization configuration is safe here.}}
  String field;
}

@JsonTypeInfo(use = MINIMAL_CLASS) // Noncompliant
abstract class JacksonDeserializationPhoneNumber2 {

  @JsonTypeInfo(use = MINIMAL_CLASS)   // not applied on method (probably not even legal)
  String method() {
    return "";
  }

}

@JsonTypeInfo(use = MINIMAL_CLASS) // Noncompliant
interface JacksonDeserializationPhoneNumber3 {
}

// test below is testing older versions of Jackson with different package name

class JacksonDeserializationJacksonCodehaus {
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
class JacksonDeserializationCompleteCoverage {
  public void method(@Nullable Object arg) {
    method(null);
  }
}
