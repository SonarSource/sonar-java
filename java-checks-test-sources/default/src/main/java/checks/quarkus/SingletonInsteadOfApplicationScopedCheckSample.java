package checks.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Singleton;

@Singleton // Noncompliant [[sc=1;ec=11]]
class NoncompliantService {
  public String foo() {
    return "foo";
  }
}

class NoncompliantProducerClass {
  @Singleton // Noncompliant
//^^^^^^^^^^
  public NoncompliantService produceService() {
    return new NoncompliantService();
  }
}

// Using @Singleton intentionally: low-level config holder, singleton scope required for eager init.
@Singleton
class CompliantWithComment {
  private static final String VERSION = "1.0";

  public String getVersion() {
    return VERSION;
  }
}

class CompliantProducerWithComment {
  // Singleton scope required: external library expects a non-proxied instance.
  @Singleton
  public CompliantWithComment produceConfig() {
    return new CompliantWithComment();
  }
}

/**
 * Using @Singleton intentionally: this is a low-level infrastructure bean with no interceptor requirements.
 * Singleton scope required to avoid proxy overhead measured on this critical path.
 */
@Singleton
class CompliantWithJavadocComment {
  public String getConfig() {
    return "config";
  }
}

@Singleton // singleton scope required: this bean is a lightweight config holder with no interception needs
class CompliantWithInlineComment {
  public String getConfig() {
    return "config";
  }
}

class CompliantProducerWithInlineComment {
  @Singleton // singleton scope required: non-proxied instance expected by external library
  public CompliantWithInlineComment produceConfig() {
    return new CompliantWithInlineComment();
  }
}

@Singleton
// singleton scope required: lightweight config holder with no interception needs
class CompliantWithFollowingLineComment {
  public String getConfig() {
    return "config";
  }
}

class CompliantProducerWithFollowingLineComment {
  @Singleton
  // singleton scope required: non-proxied instance expected by external library
  public CompliantWithFollowingLineComment produceConfig() {
    return new CompliantWithFollowingLineComment();
  }
}

@ApplicationScoped
class SingletonCheckCompliantApplicationScoped {
  public String hello() {
    return "hello";
  }
}

@Dependent
class SingletonCheckCompliantDependent {
}

class CompliantNoScope {
  public String hello() {
    return "hello";
  }
}
