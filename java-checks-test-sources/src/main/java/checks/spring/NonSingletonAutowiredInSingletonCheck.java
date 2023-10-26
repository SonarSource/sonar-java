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

  public class SingletonBean2 {
    private final PrototypeBean2 prototypeBean2;

    @Autowired
    public SingletonBean2(PrototypeBean2 prototypeBean2) { // Noncompliant, [[sc=27;ec=41]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.prototypeBean2 = prototypeBean2;
    }
  }

  @Scope(scopeName = "prototype")
  public class PrototypeBean3 {
  }

  public class SingletonBean3 {
    private final PrototypeBean3 prototypeBean3;
    public SingletonBean3(PrototypeBean3 prototypeBean3) { // Noncompliant, [[sc=27;ec=41]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.prototypeBean3 = prototypeBean3;
    }
  }

  public class SingletonBean4 {
    private final SingletonBean1 singletonBean1;
    private final RequestBean1 requestBean1;

    @Autowired
    public SingletonBean4 (SingletonBean1 singletonBean1, RequestBean1 requestBean1) { // Noncompliant, [[sc=59;ec=71]] {{Singleton beans should not auto-wire non-Singleton beans.}}
      this.singletonBean1 = singletonBean1;
      this.requestBean1 = requestBean1;
    }
  }

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
  }
}
