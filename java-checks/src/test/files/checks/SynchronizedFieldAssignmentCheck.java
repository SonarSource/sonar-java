class A {
  private String color = "red";
  private Object lockObj = new Object();
  private B b = new B();

  private void doSomething(){
    synchronized(color) { // Noncompliant
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      String[] array = new String[4];
      array[0] = color;
    }

    synchronized (b) { // Noncompliant because of line 22
      b.val = 1;
      b = new B();
    }

    synchronized (Integer.valueOf(42)) { // Compliant
      color = "green";
    }
  }
}

class B {
  public int val = 0;
}