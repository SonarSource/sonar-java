package checks;

class CloneableImplementingCloneCheckSample implements Cloneable { // Noncompliant {{Add a "clone()" method to this class.}}
  
  private String clone;
  
  public Object notClone() {
    return this;
  }

  public Object clone(String parameter) {
    return this;
  }

}

class CloneableWithCloneMethod implements Cloneable {
  
  public Object clone() {
    return this;
  }
  
}

class NotCloneableWithoutCloneMethod implements Runnable {
  public void run() {}
}

abstract class AbstractClass implements Cloneable {
  
}

interface Cloneable2 extends Cloneable {
  
}

class CloneableImplementingCloneCheckSampleA {
  void foo() {
    Object a = new Cloneable() { //False negative

    };
  }
}
