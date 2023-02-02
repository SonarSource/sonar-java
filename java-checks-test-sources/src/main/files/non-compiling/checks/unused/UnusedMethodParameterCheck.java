class UnusedMethodParameterCheck {

  public void unknownAnnotatedParameter(@Unknown Object agr1, int arg2) { // Compliant, unknown annotation on parameter
    System.out.println(arg2);
  }

  abstract class KnownParent {
    public abstract void method(Object arg1, int arg2);
    public abstract void methodWithUnknowParameterType(Unknown arg1, int arg2);
  }

  class ChildWithKnownHierarchy extends KnownParent {

    public void nonOverringMethod(Object arg1, int arg2) { // Noncompliant
      System.out.println(arg2);
    }

    public void method(Object arg1, int arg2) { // Compliant, overriding
      System.out.println(arg2);
    }

    public void methodWithUnknowParameterType(Unknown arg1, int arg2) { // Compliant, overriding
      System.out.println(arg2);
    }

    public void method(Unknown arg1, int arg2) { // Compliant, false-negative due to the Unknown type
      System.out.println(arg2);
    }

    public void methodWithUnknowParameterType(Exception arg1, int arg2) { // Noncompliant
      System.out.println(arg2);
    }
  }

  class ChildWithUnknownHierarchy extends UnkownParent {
    public void method(Object arg1, int arg2) { // Compliant, !private and !static, so may override
      System.out.println(arg2);
    }
    private void privateMethod(Object arg1, int arg2) { // Noncompliant
      System.out.println(arg2);
    }
    public static void staticMethod(Object arg1, int arg2) { // Noncompliant
      System.out.println(arg2);
    }
  }

}
