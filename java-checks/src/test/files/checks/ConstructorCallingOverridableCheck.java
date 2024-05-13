class A {
  
  A(A otherA, B b) {
    privateMethod();
    finalMethod();
    this.privateMethod();
    this.finalMethod();
    otherA.privateMethod();
    otherA.finalMethod();
    b.nonFinalPublicMethod();
    staticMethod();
  }
  
  private void privateMethod() {}
  public final void finalMethod() {}
  public static void staticMethod() {}
  
}

class B extends A {
  
  B(B otherB) {
    nonFinalPublicMethod(); // Noncompliant {{Remove this call from a constructor to the overridable "nonFinalPublicMethod" method.}}
    this.nonFinalPublicMethod(); // Noncompliant
    otherB.nonFinalPublicMethod();
    unknownMethod().nonFinalPublicMethod();
  }
  
  public void nonFinalPublicMethod() {}
  
}

class SuperClass {
  
  public final void finalMethod() {}
  public void nonFinalPublicMethod() {}
  
}

class SubClass extends SuperClass {
  
  SuperClass() {
    super.finalMethod();
    super.nonFinalPublicMethod(); // Compliant
  }
  
}

final class FinalClass {
  
  FinalClass() {
    nonFinalPublicMethod();
  }
  
  public void nonFinalPublicMethod() {}
  
}

class OuterClass {
  
  public void nonFinalPublicMethod() {}
  
  class InnerClass {
    InnerClass() {
      nonFinalPublicMethod();
    }
  }
  
}

class D {
  public D(String s) {
  }
  
  public class C extends D {
    C() {
      super(null); // Compliant
    }
  }
}

public class Parent {

  public Parent () {
    doSomething(); // Noncompliant
  }

  public void doSomething () {  // not final; can be overridden
  }
}

public class Child extends Parent {

  private String foo;

  public Child(String foo) {
    super(); // leads to call doSomething() in Parent constructor which triggers a NullPointerException as foo has not yet been initialized
    this.foo = foo;
  }

  public void doSomething () {
    System.out.println(this.foo.length());
  }
}

public class Foo {
  public Foo() {
    register(Foo.class); // Noncompliant
    Class<?> type = null;
    register(type); // Noncompliant
    this.<A>register(B.class); // Noncompliant
  }

  public <T> void register(Class<? extends T> type) {
  }
}

class Extra {
  class Easy {
  }

  class A<K, V> {
    A(B b) {
      foo(b.bar().qix()); // Noncompliant
    }

    void foo(Easy easy) {
    }
  }

  class B {
    I<? extends Easy> bar() {
      return null;
    }
  }

  interface I<T> {
    T qix();
  }
}

abstract class E {
  private final MyInterface myInterface1;
  private final MyInterface myInterface2;

  public E() {
    myInterface1 = new MyInterface() {
      @Override
      public String foo() throws Exception {
        return doInBackground(); // Compliant
      }
    };
    myInterface2 = () -> doInBackground(); // Compliant
  }

  protected abstract String doInBackground() throws Exception;

  interface MyInterface {
    String foo() throws Exception;
  }
}

class F {
  public F(){
    yaml = memoize(() -> loadYamlConfig("_config.yml")); // Compliant

    data = memoize(() -> getResourceList() // Compliant
      .stream()
      .filter(path -> path.startsWith("_data/"))
      .collect(toMap(Site::nameWithoutExtension, this::readYaml))
    );
  }

  private Map<String, Object> loadYamlConfig(String configFile) {
    return emptyMap();
  }

  public Set<String> getResourceList() {
    return resourceList.get();
  }
}

class G{
  G(int value) {
    this.profileActivator = new ProfileActivator() {
      public void activate() throws Exception {
        activateDeferredProfile();  // Compliant
      }
    };
    ProfileDeferralMgr.registerDeferral(this.profileActivator);
  }
  void activateDeferredProfile()  throws Exception{

  }
}
