package checks.spring.innovation;

import org.springframework.stereotype.Component;

@Component
public class B implements Named {
  @Override
  public String getName() {
    return "B";
  }
}
