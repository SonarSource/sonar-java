package checks.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class ValueAnnotationShouldInjectPropertyOrSpELCheckSample {
  private static final String COMPLIANT_CONSTANT = "${catalog.name}";
  private static final String NON_COMPLIANT_CONSTANT = "catalog.name";

  @Value("catalog.name") // Noncompliant {{Either replace the "@Value" annotation with a standard field initialization, use "${propertyName}" to inject a property or use "#{expression}" to evaluate a SpEL expression.}}
//^^^^^^^^^^^^^^^^^^^^^^
  String catalogA;

  @Value("${catalog.name}") // Compliant
  String catalogB;

  @Value(value = "catalog.name") // Noncompliant {{Either replace the "@Value" annotation with a standard field initialization, use "${propertyName}" to inject a property or use "#{expression}" to evaluate a SpEL expression.}}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  String catalogWithNamedArgumentA;

  @Value(value = "${catalog.name}") // Compliant
  String catalogWithNamedArgumentB;

  @Value(value = NON_COMPLIANT_CONSTANT) // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  String catalogWithNamedArgumentC;

  @Value(value = COMPLIANT_CONSTANT) // Compliant
  String catalogWithNamedArgumentD;


  @Value("book.topics[0]") // Noncompliant
  String topicA;

  @Value("#{book.topics[0]}") // Compliant
  String topicB;

  @Value("Hello, world!") // Noncompliant
  String greetingA;


  String greetingB = "Hello, world!"; // Compliant

  @Value("") // Noncompliant
  String empty;

  public void setValue(@Value("xxx") String x){ // Compliant
  }

  @Value("xxx")
  public void setValueA(String x){ // Compliant
  }

  @Value("${a") // Noncompliant
  String a;

  @Value("#{a") // Noncompliant
  String b;

  @Autowired
  String c;

  @Value("classpath:some.xml") // Compliant
  String classpath;

  @Value("file:aPath") // Compliant
  String file;

  @Value("url:anUrl") // Compliant
  String url;

  @Value("invlalidPrefix:xxxx") // Noncompliant
  String notAResource;
}

@Value("${myValue.ok}") // Compliant
@interface MyValueOk {}
@Value("myValue.not.ok") // Noncompliant
@interface MyValueNotOk {}
