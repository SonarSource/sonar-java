package checks;

import java.security.MessageDigest;

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

  void update(byte[] payload) {
    requestDigest.update(payload);
    sharedDigest.reset();
    this.configuredDigest.digest(payload);
    customDigest.update(payload);
  }

  byte[] hash(byte[] payload) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Compliant
    digest.update(payload);
    return digest.digest();
  }

  void process(byte[][] payloads) throws Exception {
    PayloadConsumer consumer = payload -> {
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
//                        ^^^^^^

    byte[] hash() {
      return digest.digest();
    }
  }

  static class PrototypeHolder {
    private final MessageDigest prototype; // Compliant, only used as a prototype for cloning
    private MessageDigest unused; // Compliant

    PrototypeHolder() throws Exception {
      this.prototype = MessageDigest.getInstance("SHA-256");
    }

    MessageDigest newDigest() throws Exception {
      if (prototype.getDigestLength() > 0) {
        return (MessageDigest) this.prototype.clone();
      }
      return MessageDigest.getInstance(prototype.getAlgorithm());
    }
  }

  static class NonPrivatePrototypeHolder {
    final MessageDigest prototype = null; // Noncompliant

    Object newDigest() throws Exception {
      return prototype.clone();
    }
  }

  static class SynchronizedHolder {
    private static MessageDigest lockedDigest; // Compliant, every usage is synchronized
    private MessageDigest methodDigest; // Compliant, every usage is synchronized
    private MessageDigest lambdaDigest; // Noncompliant
    private MessageDigest partiallyLockedDigest; // Noncompliant
    private static final Object LOCK = new Object();

    byte[] hash(byte[] payload) throws Exception {
      synchronized (LOCK) {
        if (lockedDigest == null) {
          lockedDigest = MessageDigest.getInstance("MD5");
        }
        lockedDigest.reset();
        lockedDigest.update(payload);
        partiallyLockedDigest.update(payload);
        return lockedDigest.digest();
      }
    }

    synchronized byte[] hashInMethod(byte[] payload) {
      methodDigest.update(payload);
      return methodDigest.digest();
    }

    synchronized Runnable deferred(byte[] payload) {
      return () -> lambdaDigest.update(payload);
    }

    byte[] unlocked() {
      return partiallyLockedDigest.digest();
    }
  }

  interface PayloadConsumer {
    void accept(byte[] payload) throws Exception;
  }

  abstract static class CustomDigest extends MessageDigest {
    protected CustomDigest(String algorithm) {
      super(algorithm);
    }
  }
}

interface DigestRegistry {
  MessageDigest DEFAULT_DIGEST = null; // Noncompliant {{Make this MessageDigest a local variable.}}
}

record HashHolder(MessageDigest digest) { // Noncompliant {{Make this MessageDigest a local variable.}}
}
