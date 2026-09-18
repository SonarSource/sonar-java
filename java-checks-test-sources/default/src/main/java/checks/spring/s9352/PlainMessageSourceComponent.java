package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Component;

@Component
public class PlainMessageSourceComponent implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
  }
}
