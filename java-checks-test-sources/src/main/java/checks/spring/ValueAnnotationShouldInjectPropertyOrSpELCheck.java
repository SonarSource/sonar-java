package checks.spring;

import org.springframework.beans.factory.annotation.Value;

class ValueAnnotationShouldInjectPropertyOrSpELCheck {

  @Value("catalog.name") // Noncompliant [[sc=3;ec=25]] {{Only a simple value is injected, replace the "@Value" annotation with a standard field initialization.}}
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

}
