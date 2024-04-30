package checks;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.WebService;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import lombok.Builder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

class WithoutConstructor { // Noncompliant {{Add a constructor to the class, or provide default values.}}
//    ^^^^^^^^^^^^^^^^^^
  private int field;
//^^^^^^^^^^^^^^^^^^<
}

class WitConstructor {
  private int field;

  WitConstructor() {
    field = 0;
  }
}

class CreateObject {
  public int field;
  private static int field2;
  void foo() {
    Object o = new Object() {
      private int field;
    };
  }
}

enum Enum { // Noncompliant {{Add a constructor to the enum, or provide default values.}}
//   ^^^^
  A;
  private int field;
//^^^^^^^^^^^^^^^^^^<
}

abstract class AbstractD {
  private int field;
}

class Initialized {
  private int field = 5;
}

class myEJB  {
  @EJB
  private Object foo; // injection via EJB
  @Resource
  private Object foo2; // injection via EJB
}

class MyService {}

class Inject1 {
  @Inject
  private MyService myService;
}
class Inject2 { // Noncompliant
//    ^^^^^^^
  @Inject
  private MyService myService;
  private MyService myService2;
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
}

class ABuilder { // Compliant, Builder pattern are excluded
  private int field;

  public ABuilder withField(int field) {
    this.field = field;
    return this;
  }
}

abstract class AbstractMojo {}

@Mojo(name = "myMojo")
class MyMojo extends AbstractMojo { // Compliant, Mojo don't requires specific constructor
  @Parameter(property = "project", readonly = true, required = true)
  private Object project;
  private int field;
}

class MyMojo2 extends AbstractMojo { // Compilant, skip autowired fileds
  @Parameter(property = "project", readonly = true, required = true)
  private Object project;
  @org.apache.maven.plugins.annotations.Component( role = Object.class)
  private Object component;
}

interface MyComponent {}
@org.codehaus.plexus.component.annotations.Component( role = Object.class, hint = "hint-value" )
class MyComponentImplementation implements MyComponent { // Compliant, Component  don't requires specific constructor
  @Requirement
  private boolean InjectedComponent;
  private int field;
}

class MyComponentImplementation2 implements MyComponent { // Compliant, skip autowired fileds
  @Requirement
  private boolean InjectedComponent;
  @Configuration("test")
  private String config;
}

@ManagedBean
class MyManagedBean { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@MessageDriven
class MyMessageDriven { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Singleton
class MySingleton { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Stateful
class MyStateful { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Stateless
class MyStateless { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@WebService
class MyWebService { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@WebFilter
class MyWebFilter { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@WebServlet
class MyWebServlet { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Builder
class MyLombok { // Compliant, Builder pattern are excluded (Lombok builder)
  private int field;

  public int getField(){return field;}
}
