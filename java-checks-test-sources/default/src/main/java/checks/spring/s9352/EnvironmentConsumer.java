package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.stereotype.Service;

// Three beans of type EnvironmentAware exist. With no profile active, the candidate profiled with an operator
// expression (ExpressionProfiledEnvironmentComponent) is out and the two others compete: issue expected.
@Service
public class EnvironmentConsumer {

  @Autowired
  private EnvironmentAware environmentAware;
}
