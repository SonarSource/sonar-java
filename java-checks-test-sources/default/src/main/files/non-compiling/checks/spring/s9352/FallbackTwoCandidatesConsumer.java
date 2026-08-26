package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Service;

// See FallbackTwoCandidatesComponentA for context: still ambiguous between the two non-fallback candidates,
// issue expected once @Fallback support is added.
@Service
public class FallbackTwoCandidatesConsumer {

  @Autowired
  private MessageSourceAware contextAware;
}
