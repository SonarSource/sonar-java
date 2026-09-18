package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Service;

@Service
public class MessageSourceConsumer {

  @Autowired
  private MessageSourceAware contextAware;
}
