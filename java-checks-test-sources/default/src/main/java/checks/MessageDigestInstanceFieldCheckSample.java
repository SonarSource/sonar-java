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

  static class MonitorHolder {
    private static final Object LOCK = new Object();
    private static Object mutableLock = new Object();
    private final Object lockA = new Object();
    private final Object lockB = new Object();
    private static MessageDigest staticInMethodDigest; // Noncompliant
    private static MessageDigest staticSynchronizedDigest; // Compliant, every usage holds the class monitor
    private static MessageDigest classLiteralDigest; // Compliant, every usage holds the class monitor
    private MessageDigest mixedLockDigest; // Noncompliant
    private MessageDigest twoLocksDigest; // Noncompliant
    private MessageDigest instanceLockDigest; // Compliant, every usage holds lockA
    private static MessageDigest staticInstanceLockDigest; // Noncompliant
    private MessageDigest thisLockDigest; // Compliant, every usage holds this
    private MessageDigest innerClassDigest; // Noncompliant
    private MessageDigest mutableLockDigest; // Noncompliant
    private MessageDigest localLockDigest; // Noncompliant
    private MessageDigest otherInstanceLockDigest; // Noncompliant
    private MessageDigest clonedOutsideLockDigest; // Noncompliant
    private MessageDigest clonedInsideLockDigest; // Compliant, every usage holds LOCK
    private MessageDigest unsynchronizedDigest; // Noncompliant

    synchronized void staticInMethod(byte[] payload) {
      staticInMethodDigest.update(payload);
    }

    static synchronized void staticSynchronized(byte[] payload) {
      staticSynchronizedDigest.update(payload);
    }

    static void classLiteral(byte[] payload) {
      synchronized (MonitorHolder.class) {
        classLiteralDigest.update(payload);
      }
    }

    synchronized void classLiteralInMethod(byte[] payload) {
      synchronized (MonitorHolder.class) {
        classLiteralDigest.update(payload);
      }
    }

    void mixedLock(byte[] payload) {
      synchronized (LOCK) {
        mixedLockDigest.update(payload);
      }
    }

    synchronized byte[] mixedLockInMethod() {
      return mixedLockDigest.digest();
    }

    void twoLocks(byte[] payload) {
      synchronized (lockA) {
        twoLocksDigest.update(payload);
        instanceLockDigest.update(payload);
        staticInstanceLockDigest.update(payload);
      }
      synchronized (this.lockB) {
        twoLocksDigest.update(payload);
      }
      synchronized (this.lockA) {
        instanceLockDigest.update(payload);
      }
    }

    void thisLock(byte[] payload) {
      synchronized (this) {
        thisLockDigest.update(payload);
      }
    }

    synchronized byte[] thisLockInMethod() {
      return thisLockDigest.digest();
    }

    void mutableLock(byte[] payload) {
      synchronized (mutableLock) {
        mutableLockDigest.update(payload);
      }
    }

    void localLock(byte[] payload) {
      Object lock = new Object();
      synchronized (lock) {
        localLockDigest.update(payload);
      }
    }

    void otherInstanceLock(MonitorHolder other, byte[] payload) {
      synchronized (other.lockA) {
        otherInstanceLockDigest.update(payload);
      }
    }

    MessageDigest clonedOutsideLock(byte[] payload) throws Exception {
      synchronized (LOCK) {
        clonedOutsideLockDigest.update(payload);
      }
      return (MessageDigest) clonedOutsideLockDigest.clone();
    }

    MessageDigest clonedInsideLock(byte[] payload) throws Exception {
      synchronized ((LOCK)) {
        clonedInsideLockDigest = MessageDigest.getInstance("SHA-256");
        clonedInsideLockDigest.update(payload);
        return (MessageDigest) clonedInsideLockDigest.clone();
      }
    }

    MonitorHolder() {
      unsynchronizedDigest.reset();
    }

    class Inner {
      synchronized void update(byte[] payload) {
        innerClassDigest.update(payload);
      }
    }
  }

  static class QualifiedAccessHolder {
    private final Object lockA = new Object();
    private MessageDigest otherMethodDigest; // Noncompliant {{Make this MessageDigest a local variable.}}
//                        ^^^^^^^^^^^^^^^^^
    private MessageDigest otherLockDigest; // Noncompliant {{Make this MessageDigest a local variable.}}
//                        ^^^^^^^^^^^^^^^
    private MessageDigest thisDigest; // Compliant, every usage is on this and holds the instance monitor
    private static MessageDigest staticDigest; // Compliant, every usage holds the class monitor

    synchronized void merge(QualifiedAccessHolder other, byte[] payload) {
      otherMethodDigest.update(payload);
      other.otherMethodDigest.update(payload);
    }

    void mergeWithLock(QualifiedAccessHolder other, byte[] payload) {
      synchronized (lockA) {
        otherLockDigest.update(payload);
        other.otherLockDigest.update(payload);
      }
    }

    synchronized void updateThis(byte[] payload) {
      this.thisDigest.update(payload);
      thisDigest.reset();
    }

    static void updateStatic(byte[] payload) {
      synchronized (QualifiedAccessHolder.class) {
        QualifiedAccessHolder.staticDigest.update(payload);
        staticDigest.reset();
      }
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
