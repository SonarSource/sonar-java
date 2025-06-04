package checks.spring.innovation;

import org.springframework.stereotype.Component;

@Component
public class A implements Named {
  @Override
  public String getName() {
    return "A";
  }
}
