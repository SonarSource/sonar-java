package checks.spring;

import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

public class NonSingletonAutowiredInSingletonCheckSample {

  private static final String SINGLETON_SCOPE = "singleton";
  private static final String PROTOTYPE_SCOPE = "prototype";
  private static final String CUSTOM_SCOPE = "custom";

  @Component
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = TARGET_CLASS)
  public class RequestBean1 {
    //...
  }

  public class SingletonBean {
    @Autowired
    private final RequestBean1 requestBean1; // Noncompliant, [[sc=19;ec=31]] {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired field/parameter).}}

    public SingletonBean() {
      requestBean1 = null;
    }
  }

  @Scope("prototype")
  public class PrototypeBean1 {
  }

  public class SingletonBean1 {
    @Autowired
    private final PrototypeBean1 prototypeBean1; // Noncompliant
    public SingletonBean1() {
      prototypeBean1 = null;
    }
  }

  @Scope(value = "prototype")
  public class PrototypeBean2 {
  }

  @Scope(value = "singleton")
  public class SingletonBean2 {
    private final PrototypeBean2 prototypeBean2;

    @Autowired // Noncompliant@+1
    public SingletonBean2(PrototypeBean2 prototypeBean2) { // Noncompliant, [[sc=27;ec=41]] {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired method/constructor).}}
      this.prototypeBean2 = prototypeBean2;
    }
  }

  @Scope(value = PROTOTYPE_SCOPE, scopeName = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
  public class PrototypeBean3 {
  }

  @Scope(scopeName = "singleton")
  public class SingletonBean3 {
    private final PrototypeBean3 prototypeBean3;
    public SingletonBean3(PrototypeBean3 prototypeBean3) { // Noncompliant, [[sc=27;ec=41]] {{Don't auto-wire this non-Singleton bean into a Singleton bean (single argument constructor).}}
      this.prototypeBean3 = prototypeBean3;
    }
  }

  @Scope(proxyMode = TARGET_CLASS)
  public class SingletonBean4 {
    private final SingletonBean1 singletonBean1;
    private final RequestBean1 requestBean1;

    @Autowired
    public SingletonBean4 (SingletonBean1 singletonBean1, RequestBean1 requestBean1) { // Noncompliant
      this.singletonBean1 = singletonBean1;
      this.requestBean1 = requestBean1;
    }
  }

  @Scope(value = SINGLETON_SCOPE, scopeName = "singleton")
  public class SingletonBean5 {
    private final SingletonBean1 singletonBean1;
    private final RequestBean1 requestBean1;
    private final PrototypeBean1 prototypeBean1;

    @Autowired
    public SingletonBean5 (
      SingletonBean1 singletonBean1,
      RequestBean1 requestBean1, // Noncompliant
      PrototypeBean1 prototypeBean1) { // Noncompliant
      this.singletonBean1 = singletonBean1;
      this.requestBean1 = requestBean1;
      this.prototypeBean1 = prototypeBean1;
    }

    public void method(PrototypeBean2 prototypeBean2) { // Compliant, method is not annotated with @Autowired
      // ...
    }
  }

  public class SingletonBean6 {
    private final SingletonBean1 singletonBean1;
    private final RequestBean1 requestBean1;
    private final PrototypeBean1 prototypeBean1;

    public SingletonBean6 (
      SingletonBean1 singletonBean1,
      @Autowired RequestBean1 requestBean1, // Noncompliant
      @Autowired PrototypeBean1 prototypeBean1) { // Noncompliant
      this.singletonBean1 = singletonBean1;
      this.requestBean1 = requestBean1;
      this.prototypeBean1 = prototypeBean1;
    }
  }

  @Scope("singleton")
  public class SingletonBean7 {
    private SingletonBean1 singletonBean2;
    private RequestBean1 requestBean1;
    private PrototypeBean1 prototypeBean1;

    public SingletonBean7 (
      SingletonBean1 singletonBean2, RequestBean1 requestBean1, PrototypeBean1 prototypeBean1) { // Compliant, constructor is not annotated with @Autowired
      this.singletonBean2 = singletonBean2;
      this.requestBean1 = requestBean1;
      this.prototypeBean1 = prototypeBean1;
    }
    @Autowired
    public void setSingletonBean1(SingletonBean1 singletonBean1) { // Compliant
      this.singletonBean2 = singletonBean2;
    }

    @Autowired
    public void setRequestBean1(RequestBean1 requestBean1) { // Noncompliant, [[sc=33;ec=45]] {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired method/constructor).}}
      this.requestBean1 = requestBean1;
    }

    @Autowired // Noncompliant@+1
    public void setPrototypeBean1(@Autowired PrototypeBean1 prototypeBean1){ // Noncompliant,
      this.prototypeBean1 = prototypeBean1;
    }
  }

  public class SingletonBean9 {
    @Inject
    private RequestBean1 requestBean1; // Noncompliant
    private PrototypeBean1 prototypeBean1;
    private PrototypeBean2 prototypeBean2;

    public SingletonBean9(PrototypeBean1 prototypeBean1) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
    }

    @Inject
    public SingletonBean9(PrototypeBean1 prototypeBean1, // Noncompliant
      PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
      this.prototypeBean2 = prototypeBean2;
    }

    @Inject
    public void setPrototypeBean2(PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean2 = prototypeBean2;
    }
  }

  public class SingletonBean10 {
    @jakarta.inject.Inject
    private RequestBean1 requestBean1; // Noncompliant
    private PrototypeBean1 prototypeBean1;
    private PrototypeBean2 prototypeBean2;

    public SingletonBean10(PrototypeBean1 prototypeBean1) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
    }

    @jakarta.inject.Inject
    public SingletonBean10(PrototypeBean1 prototypeBean1, // Noncompliant
      PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
      this.prototypeBean2 = prototypeBean2;
    }

    @jakarta.inject.Inject
    public void setPrototypeBean2(PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean2 = prototypeBean2;
    }
  }

  @Scope(value = CUSTOM_SCOPE, scopeName = "custom", proxyMode = TARGET_CLASS)
  public class CustomBean {
    @Autowired
    private SingletonBean1 singletonBean1; // Compliant, since scope is non-Singleton
    @Autowired
    private SingletonBean5 singletonBean5; // Compliant, since scope is non-Singleton
    @Autowired
    public PrototypeBean1 prototypeBean1; // Compliant, since scope is non-Singleton
  }

  public class SingletonBean11 {
    @Autowired
    private CustomBean customBean; // Noncompliant

    @Autowired // Noncompliant@+1
    public SingletonBean11(CustomBean customBean) { // Noncompliant
     this.customBean = customBean;
    }

    @Autowired
    public void setCustomBean(CustomBean customBean) { // Noncompliant
      this.customBean = customBean;
    }
  }

  @Autowired
  public @interface customAutowired {
  }
}
