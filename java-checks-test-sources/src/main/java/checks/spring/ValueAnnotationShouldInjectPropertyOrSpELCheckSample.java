package checks.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class ValueAnnotationShouldInjectPropertyOrSpELCheckSample {

  @Value("catalog.name") // Noncompliant [[sc=3;ec=25]] {{Either replace the "@Value" annotation with a standard field initialization, use "${propertyName}" to inject a property or use "#{expression}" to evaluate a SpEL expression.}}
  String catalogA;

  @Value("${catalog.name}") // Compliant
  String catalogB;

  @Value("book.topics[0]") // Noncompliant, this will not evaluate the expression
  String topicA;

  @Value("#{book.topics[0]}") // Compliant
  String topicB;

  @Value("Hello, world!") // Noncompliant, this use of @Value is redundant
  String greetingA;

  String greetingB = "Hello, world!"; // Compliant

  @Value("") // Noncompliant
  String empty;

  public void setValue(@Value("xxx") String x){ // compliant
  }

  @Value("xxx")
  public void setValueA(String x){ // compliant
  }

  @Value("${a") // Noncompliant
  String a;

  @Value("#{a") // Noncompliant
  String b;

  @Autowired
  String c;
}

@Value("${myValue.ok}") // Compliant
@interface MyValueOk {}
@Value("myValue.not.ok") // Noncompliant
@interface MyValueNotOk {}
