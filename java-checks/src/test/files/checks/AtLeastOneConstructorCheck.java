import org.springframework.beans.factory.annotation.Autowired;
import java.lang.Object;
import javax.inject.Inject;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

class A { // Noncompliant [[sc=7;ec=8;secondary=12]] {{Add a constructor to the class, or provide default values.}}
  private int field;
}

class B {
  private int field;

  B() {
    field = 0;
  }
}

class C {
  public int field;
  private static int field2;
  void foo() {
    Object o = new Object() {
      private int field;
    };
  }
}

enum Enum { // Noncompliant [[secondary=35]] {{Add a constructor to the enum, or provide default values.}}
  A;
  private int field;
}

abstract class D {
  private int field;
}

class E {
  private int field = 5;
}

class EJB  {
  @javax.ejb.EJB
  private MyObject foo; // injection via EJB
}

@javax.ejb.EJB // injection via EJB
class EJB2 {
  private Object someObject;
}

class MyService {}
class Spring1 {
  @Autowired
  private MyService myService;
}
class Spring2 { // Noncompliant [[secondary=64]]
  @Autowired
  private MyService myService;
  private MyService myService2;
}

class Inject1 {
  @Inject
  private MyService myService;
}
class Inject2 { // Noncompliant [[secondary=74]]
  @Inject
  private MyService myService;
  private MyService myService2;
}

class ABuilder { // Compliant, Builder pattern are excluded
  private int field;

  public ABuilder withField(int field) {
    this.field = field;
    return this;
  }
}

@Mojo(name = "myMojo")
public class MyMojo extends AbstractMojo { // Compliant, Mojo don't requires specific constructor
  @Parameter(property = "project", readonly = true, required = true)
  private MavenProject project;
}

@Component( role = MyComponent.class, hint = "hint-value" )
public class MyComponentImplementation implements MyComponent { // Compliant, Component  don't requires specific constructor
  @Requirement
  private boolean InjectedComponent;
}

