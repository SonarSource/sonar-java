package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
class QualifierOrderService {

  private final ApplicationContext applicationContext;

  @Autowired
  QualifierOrderService(@Qualifier("namedBean") ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }
}
