package checks.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

public class NonSingletonAutowiredInSingletonCheck {

  @Component
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public class RequestBean1 {
    //...
  }

  public class SingletonBean {
    @Autowired
    private final RequestBean1 requestBean1; // Noncompliant, [[sc=19;ec=31]] {{Singleton beans should not auto-wire non-Singleton beans.}}

    public SingletonBean() {
      requestBean1 = null;
    }
  }

  @Scope("prototype")
  public class PrototypeBean1 {
  }

  public class SingletonBean1 {
    @Autowired
    private final PrototypeBean1 prototypeBean1; // Noncompliant, [[sc=19;ec=33]] {{Singleton beans should not auto-wire non-Singleton beans.}}
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

    @Autowired
    public SingletonBean2(PrototypeBean2 prototypeBean2) { // Noncompliant, [[sc=27;ec=41]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.prototypeBean2 = prototypeBean2;
    }
  }

  @Scope(value = "prototype", scopeName = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
  public class PrototypeBean3 {
  }

  @Scope(scopeName = "singleton")
  public class SingletonBean3 {
    private final PrototypeBean3 prototypeBean3;
    public SingletonBean3(PrototypeBean3 prototypeBean3) { // Noncompliant, [[sc=27;ec=41]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.prototypeBean3 = prototypeBean3;
    }
  }

  @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
  public class SingletonBean4 {
    private final SingletonBean1 singletonBean1;
    private final RequestBean1 requestBean1;

    @Autowired
    public SingletonBean4 (SingletonBean1 singletonBean1, RequestBean1 requestBean1) { // Noncompliant, [[sc=59;ec=71]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.singletonBean1 = singletonBean1;
      this.requestBean1 = requestBean1;
    }
  }

  @Scope(value = "singleton", scopeName = "singleton")
  public class SingletonBean5 {
    private final SingletonBean1 singletonBean1;
    private final RequestBean1 requestBean1;
    private final PrototypeBean1 prototypeBean1;

    @Autowired
    public SingletonBean5 (
      SingletonBean1 singletonBean1,
      RequestBean1 requestBean1, // Noncompliant, [[sc=7;ec=19]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      PrototypeBean1 prototypeBean1) { // Noncompliant, [[sc=7;ec=21]] {{Singleton beans should not auto-wire non-Singleton beans.}}
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
      @Autowired RequestBean1 requestBean1, // Noncompliant, [[sc=18;ec=30]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      @Autowired PrototypeBean1 prototypeBean1) { // Noncompliant, [[sc=18;ec=32]] {{Singleton beans should not auto-wire non-Singleton beans.}}
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
    public void setRequestBean1(RequestBean1 requestBean1) { // Noncompliant, [[sc=33;ec=45]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.requestBean1 = requestBean1;
    }

    @Autowired
    public void setPrototypeBean1(@Autowired PrototypeBean1 prototypeBean1){ // Noncompliant, [[sc=46;ec=60]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.prototypeBean1 = prototypeBean1;
    }
  }
}
