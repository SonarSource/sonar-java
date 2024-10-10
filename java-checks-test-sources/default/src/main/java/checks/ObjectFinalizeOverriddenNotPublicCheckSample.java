package checks;

class ObjectFinalizeOverriddenNotPublicCheckSample_Noncompliant {
  @Override
  public void finalize() throws Throwable { // Noncompliant {{Make this finalize() method protected.}}
//            ^^^^^^^^
  }
}

class ObjectFinalizeOverriddenNotPublicCheckSample_Compliant_1 {

  @Override
  protected void finalize() throws Throwable {  // Compliant
  }

  public void foo() {                           // Compliant
  }

}

abstract class ClassWithFinalize {
  public abstract void finalize(String arg) throws Throwable; // Compliant
}

class ObjectFinalizeOverriddenNotPublicCheckSample_Extends_ClassWithFinalize extends ClassWithFinalize {

  @Override
  public void finalize(String arg) throws Throwable { // Compliant
  }

  @Override
  public void finalize() throws Throwable { // Noncompliant
  }

}
