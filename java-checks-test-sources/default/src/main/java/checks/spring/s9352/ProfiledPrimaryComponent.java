package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Profile("test")
@Component
public class ProfiledPrimaryComponent implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
  }
}
