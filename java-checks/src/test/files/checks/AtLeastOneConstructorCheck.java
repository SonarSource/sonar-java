import org.springframework.beans.factory.annotation.Autowired;
import java.lang.Object;
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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;

class A { // Noncompliant [[sc=7;ec=8;secondary=21]] {{Add a constructor to the class, or provide default values.}}
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

enum Enum { // Noncompliant [[secondary=44]] {{Add a constructor to the enum, or provide default values.}}
  A;
  private int field;
}

abstract class D {
  private int field;
}

class E {
  private int field = 5;
}

class myEJB  {
  @EJB
  private MyObject foo; // injection via EJB
  @Resource
  private MyObject foo2; // injection via EJB
}

class MyService {}
class Spring1 {
  @Autowired
  private MyService myService;
}
class Spring2 { // Noncompliant [[secondary=70]]
  @Autowired
  private MyService myService;
  private MyService myService2;
}

class Inject1 {
  @Inject
  private MyService myService;
}
class Inject2 { // Noncompliant [[secondary=80]]
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
  private int field;
}

public class MyMojo2 extends AbstractMojo { // Compilant, skip autowired fileds
  @Parameter(property = "project", readonly = true, required = true)
  private MavenProject project;
  @org.apache.maven.plugins.annotations.Component( role = MyComponentExtension.class)
  private MyComponent component;
}

@org.codehaus.plexus.component.annotations.Component( role = MyComponent.class, hint = "hint-value" )
public class MyComponentImplementation implements MyComponent { // Compliant, Component  don't requires specific constructor
  @Requirement
  private boolean InjectedComponent;
  private int field;
}

public class MyComponentImplementation2 implements MyComponent { // Compliant, skip autowired fileds
  @Requirement
  private boolean InjectedComponent;
  @Configuration("test")
  private String config;
}

@ManagedBean
public class MyManagedBean { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@MessageDriven
public class MyMessageDriven { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Singleton
public class MySingleton { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Stateful
public class MyStateful { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@Stateless
public class MyStateless { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@WebService
public class MyWebService { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@WebFilter
public class MyWebFilter { // Compliant, Java EE Bean managed by application server
  private Object field;
}

@WebServlet
public class MyWebServlet { // Compliant, Java EE Bean managed by application server
  private Object field;
}
