package checks;

import java.io.IOException;
import java.io.ObjectOutputStream;

class RubberBall {

  public void bounce(float angle, float velocity) {
  }

  private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Noncompliant {{Remove this "synchronized" keyword.}}
  }

  static class Color {}
}

class RubberBall2 {

  public void bounce(float angle, float velocity) {
  }

  private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Compliant, maybe FN because we include nested classes,
                                                                                        // which may or may not be synchronizing something relevant
  }

  class Nested {

    public void bounce(float angle, float velocity) {
    }

    private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Noncompliant {{Remove this "synchronized" keyword.}}
    }
  }

  class NestedCompliant {

    public NestedCompliant(RubberBall.Color color, int diameter) {
      synchronized (this) {
        System.out.println("This is OK");
      }
    }

    private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Compliant
    }
  }

  class NestedCompliant2 {

    public NestedCompliant2(RubberBall.Color color, int diameter) {
      synchronized (this) {
        System.out.println("This is OK");
      }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException { // Compliant
    }
  }
}

class ClassWithSynchronizedStmtIsOK {

  public void bounce(float angle, float velocity) {
    synchronized (this) {
    }
  }

  private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Compliant
  }
}

class TwoSynchrozniedMethodIsOK {

  public TwoSynchrozniedMethodIsOK(RubberBall.Color color, int diameter) {
  }

  public synchronized void bounce(float angle, float velocity) {

  }

  private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Compliant
  }
}

class Nested3 {

  private synchronized void writeObject(ObjectOutputStream stream) throws IOException { // Compliant
  }

  class Nested {
    Nested() {
      synchronized (this) {

      }
    }
  }
}
