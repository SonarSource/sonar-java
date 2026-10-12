package checks;

import java.security.MessageDigest;
import java.util.function.Consumer;

class MessageDigestInstanceFieldCheckSample {

  private MessageDigest requestDigest; // Noncompliant {{Make this MessageDigest a local variable.}}
  private static MessageDigest sharedDigest; // Noncompliant {{Make this MessageDigest a local variable.}}
  private final MessageDigest configuredDigest; // Noncompliant {{Make this MessageDigest a local variable.}}
  private CustomDigest customDigest; // Noncompliant {{Make this MessageDigest a local variable.}}
  private String digest = "SHA-256"; // Compliant
  private ThreadLocal<MessageDigest> threadDigest = new ThreadLocal<>(); // Compliant
  private Object deferredDigest; // Compliant

  MessageDigestInstanceFieldCheckSample() throws Exception {
    configuredDigest = MessageDigest.getInstance("SHA-256");
    MessageDigest constructorDigest = MessageDigest.getInstance("SHA-256"); // Compliant
  }

  byte[] hash(byte[] payload) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Compliant
    digest.update(payload);
    return digest.digest();
  }

  void process(byte[][] payloads) throws Exception {
    Consumer<byte[]> consumer = payload -> {
      MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Compliant
      digest.update(payload);
    };
    for (byte[] payload : payloads) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Compliant
      consumer.accept(digest.digest(payload));
    }
  }

  void recover() {
    try {
      throw new IllegalArgumentException();
    } catch (IllegalArgumentException exception) {
      MessageDigest digest = null; // Compliant
    }
  }

  class NestedHasher {
    private MessageDigest digest; // Noncompliant {{Make this MessageDigest a local variable.}}
  }

  abstract static class CustomDigest extends MessageDigest {
  }
}

interface DigestRegistry {
  MessageDigest DEFAULT_DIGEST = null; // Noncompliant {{Make this MessageDigest a local variable.}}
}

record HashHolder(MessageDigest digest) { // Noncompliant {{Make this MessageDigest a local variable.}}
}
