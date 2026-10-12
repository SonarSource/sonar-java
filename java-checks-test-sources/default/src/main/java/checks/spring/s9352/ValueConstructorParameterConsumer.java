package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Two beans of type ApplicationContextAware exist (ComponentOne, ComponentTwo), but this constructor
// parameter is resolved from a SpEL expression rather than by type: not an injection point, no issue expected.
@Service
public class ValueConstructorParameterConsumer {

  private final ApplicationContextAware contextAware;
  private final String label;

  public ValueConstructorParameterConsumer(
    @Value("#{componentOne}") ApplicationContextAware contextAware,
    @Value("${consumer.label}") String label) {
    this.contextAware = contextAware;
    this.label = label;
  }
}
