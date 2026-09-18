package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class UnprofiledPrimaryComponent implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
  }
}
