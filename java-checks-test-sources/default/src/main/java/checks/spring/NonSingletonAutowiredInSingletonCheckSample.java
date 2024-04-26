package checks.spring;

import checks.spring.NonSingletonAutowiredInSingletonCheckSampleNonSingletonBeansDefinition.PrototypeBean1;
import checks.spring.NonSingletonAutowiredInSingletonCheckSampleNonSingletonBeansDefinition.PrototypeBean2;
import checks.spring.NonSingletonAutowiredInSingletonCheckSampleNonSingletonBeansDefinition.PrototypeBean3;
import checks.spring.NonSingletonAutowiredInSingletonCheckSampleNonSingletonBeansDefinition.RequestBean1;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

public class NonSingletonAutowiredInSingletonCheckSample {

  private static final String SINGLETON_SCOPE = "singleton";

  public class SingletonBean {
    @Autowired
    private RequestBean1 requestBean1; // Noncompliant {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired field).}}
//          ^^^^^^^^^^^^
    @Autowired
    private PrototypeBean1 prototypeBean1; // Noncompliant
    @Autowired
    private PrototypeBean2 prototypeBean2; // Noncompliant
    @Autowired
    private PrototypeBean3 prototypeBean3; // Noncompliant

    @Autowired // Noncompliant@+1
    public SingletonBean(RequestBean1 requestBean1) { // Noncompliant {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired constructor).}}
//                       ^^^^^^^^^^^^
    }

    @Autowired // Noncompliant@+1
    public SingletonBean(PrototypeBean2 prototypeBean2) { // Noncompliant
    }

    @Autowired
    public SingletonBean(RequestBean1 requestBean1, // Noncompliant
      PrototypeBean1 prototypeBean1, // Noncompliant
      PrototypeBean2 prototypeBean2, // Noncompliant
      PrototypeBean3 prototypeBean3) { // Noncompliant
    }

    @Autowired
    public void setRequestBean1(RequestBean1 requestBean1) { // Noncompliant {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired setter method).}}
//                              ^^^^^^^^^^^^
      this.requestBean1 = requestBean1;
    }

    public void setPrototypeBean1(@Autowired PrototypeBean1 prototypeBean1) { // Noncompliant {{Don't auto-wire this non-Singleton bean into a Singleton bean (autowired parameter).}}
//                                           ^^^^^^^^^^^^^^
      this.prototypeBean1 = prototypeBean1;
    }

    @Autowired
    private void setPrototypeBean2(@Autowired PrototypeBean2 prototypeBean2) { // Compliant
      this.prototypeBean2 = prototypeBean2;
    }

    public void method(@Autowired RequestBean1 requestBean1) { // Compliant, not a setter or constructor
      // ...
    }
  }

  @Scope(value = "singleton")
  public class SingletonBean2 {
    @Autowired
    private SingletonBean singletonBean; // Compliant, since scope is non-Singleton
    @Autowired
    private RequestBean1 requestBean1; // Noncompliant
    @Autowired
    private PrototypeBean1 prototypeBean1; // Noncompliant

    @Autowired
    private SingletonBean2(@Autowired SingletonBean singletonBean) {// Compliant, since scope is non-Singleton
      this.singletonBean = singletonBean;
    }

    @Autowired // Noncompliant@+1
    public SingletonBean2(RequestBean1 requestBean1) { // Noncompliant
      this.requestBean1 = requestBean1;
    }
  }

  @Scope(scopeName = "singleton")
  public class SingletonBean3 {
    @Autowired
    private SingletonBean singletonBean; // Compliant, since scope is non-Singleton
    @Autowired
    private RequestBean1 requestBean1; // Noncompliant
    @Autowired
    private PrototypeBean1 prototypeBean1; // Noncompliant

 // Noncompliant@+2
    @Autowired // Noncompliant@+1
    public SingletonBean3(@Autowired RequestBean1 requestBean1) { // Noncompliant
    }
  }

  @Scope(proxyMode = TARGET_CLASS)
  public class SingletonBean4 {
    @Autowired
    private SingletonBean singletonBean; // Compliant, since scope is non-Singleton
    @Autowired
    private RequestBean1 requestBean1; // Noncompliant
    @Autowired
    private PrototypeBean1 prototypeBean1; // Noncompliant
  }

  @Scope(value = SINGLETON_SCOPE, scopeName = "singleton")
  public class SingletonBean5 {
    @Autowired
    private SingletonBean singletonBean; // Compliant, since scope is non-Singleton
    @Autowired
    private RequestBean1 requestBean1; // Noncompliant
    @Autowired
    private PrototypeBean1 prototypeBean1; // Noncompliant
  }

  @Scope("singleton")
  public class SingletonBean6 {
    @Inject
    private SingletonBean5 singletonBean5; // Compliant, since scope is non-Singleton
    @Inject
    private RequestBean1 requestBean1; // Noncompliant
    private PrototypeBean1 prototypeBean1;
    private PrototypeBean2 prototypeBean2;

    public SingletonBean6(PrototypeBean1 prototypeBean1) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
    }

    @Inject
    public SingletonBean6(PrototypeBean1 prototypeBean1, // Noncompliant
      PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
      this.prototypeBean2 = prototypeBean2;
    }

    @Inject
    public void setPrototypeBean2(PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean2 = prototypeBean2;
    }
  }

  public class SingletonBean7 {
    @jakarta.inject.Inject
    private SingletonBean5 singletonBean5; // Compliant, since scope is non-Singleton
    @jakarta.inject.Inject
    private RequestBean1 requestBean1; // Noncompliant
    private PrototypeBean1 prototypeBean1;
    private PrototypeBean2 prototypeBean2;

    public SingletonBean7(PrototypeBean1 prototypeBean1) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
    }

    @jakarta.inject.Inject
    public SingletonBean7(PrototypeBean1 prototypeBean1, // Noncompliant
      PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean1 = prototypeBean1;
      this.prototypeBean2 = prototypeBean2;
    }

    @jakarta.inject.Inject
    public void setPrototypeBean2(PrototypeBean2 prototypeBean2) { // Noncompliant
      this.prototypeBean2 = prototypeBean2;
    }
  }

  @Scope(value = "custom", scopeName = "custom", proxyMode = TARGET_CLASS)
  public class CustomBean {
    @Autowired
    private SingletonBean singletonBean; // Compliant, since scope is non-Singleton
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

    @Autowired
    public void method(CustomBean customBean) { // Compliant, not a setter or constructor
      // ...
    }

    public void method2(@Autowired CustomBean customBean) { // Compliant, not a setter or constructor
      // ...
    }
  }

  @Autowired
  public @interface customAutowired {
  }
}
