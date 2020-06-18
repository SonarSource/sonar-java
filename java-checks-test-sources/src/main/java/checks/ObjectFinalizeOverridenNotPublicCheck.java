package checks;

class ObjectFinalizeOverridenNotPublicCheck_Noncompliant {
  @Override
  public void finalize() throws Throwable {    // Noncompliant [[sc=15;ec=23]] {{Make this finalize() method protected.}}
  }
}

class ObjectFinalizeOverridenNotPublicCheck_Compliant_1 {

  @Override
  protected void finalize() throws Throwable {  // Compliant
  }

  public void foo() {                           // Compliant
  }

}

abstract class ClassWithFinalize {
  public abstract void finalize(String arg) throws Throwable; // Compliant
}

class ObjectFinalizeOverridenNotPublicCheck_Extends_ClassWithFinalize extends ClassWithFinalize {

  @Override
  public void finalize(String arg) throws Throwable { // Compliant
  }

  @Override
  public void finalize() throws Throwable { // Noncompliant
  }

}
