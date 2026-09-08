package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Service;

// Three beans of type MessageSourceAware exist; this field's name matches none of them, but
// UnprofiledPrimaryComponent is the only unambiguous @Primary once ProfiledPrimaryComponent (profiled) is
// excluded: no issue expected.
@Service
public class MessageSourceConsumer {

  @Autowired
  private MessageSourceAware contextAware;
}
