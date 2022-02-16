package checks;

public class ReadObjectSynchronized {

  private synchronized void readObject(java.io.ObjectInputStream is) { // Compliant, not in a Serializable class
  }
}

class ReadObjectSynchronized2 implements java.io.Serializable {

  private void readObject(java.io.ObjectInputStream is) { // Compliant, not a synchronized method
  }
}

class ReadObjectSynchronized3 implements java.io.Serializable {

  Object myField;

  private synchronized void readObject(java.io.InputStream is) { // Compliant, wrong signature
  }

  private synchronized void readObject() { // Compliant, wrong signature
  }

  private synchronized void read(java.io.ObjectInputStream is) { // Compliant, wrong signature
  }

  private synchronized void readObject(java.io.ObjectInputStream is) { // Noncompliant {{Remove the "synchronized" keyword from this method.}}
  }
}
