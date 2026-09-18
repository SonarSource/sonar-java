package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.stereotype.Service;

// Three beans of type EnvironmentAware exist. Excluding the candidate profiled with an operator expression
// (ExpressionProfiledEnvironmentComponent) still leaves two ambiguous candidates: issue expected.
@Service
public class EnvironmentConsumer {

  @Autowired
  private EnvironmentAware environmentAware;
}
