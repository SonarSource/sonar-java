package checks.quarkus;

import io.quarkus.cache.CacheKeyGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.lang.reflect.Method;

class NoncompliantBasic implements CacheKeyGenerator { // Noncompliant {{Make this class a CDI bean by adding a scope annotation, or add a public no-args constructor.}}
//    ^^^^^^^^^^^^^^^^^
  private final ConfigService configService;

  public NoncompliantBasic(ConfigService configService) {
    this.configService = configService;
  }

  @Override
  public Object generate(Method method, Object... methodParams) {
    return configService.getPrefix() + methodParams[0];
  }
}

class NoncompliantMultipleDependencies implements CacheKeyGenerator { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  private final DatabaseService dbService;
  private final CacheConfig config;

  public NoncompliantMultipleDependencies(DatabaseService dbService, CacheConfig config) {
    this.dbService = dbService;
    this.config = config;
  }

  @Override
  public Object generate(Method method, Object... methodParams) {
    return dbService.format(methodParams[0]);
  }
}

class NoncompliantMultipleConstructors implements CacheKeyGenerator { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  private final String prefix;

  public NoncompliantMultipleConstructors(String prefix) {
    this.prefix = prefix;
  }

  public NoncompliantMultipleConstructors(String prefix, int timeout) {
    this.prefix = prefix;
  }

  @Override
  public Object generate(Method method, Object... methodParams) {
    return prefix + methodParams[0];
  }
}

class NoncompliantPrivateConstructor implements CacheKeyGenerator { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  private NoncompliantPrivateConstructor() {}

  @Override
  public Object generate(Method method, Object... methodParams) {
    return methodParams[0];
  }
}

@ApplicationScoped
class CompliantApplicationScoped implements CacheKeyGenerator {
  @Inject
  ConfigService configService;

  @Override
  public Object generate(Method method, Object... methodParams) {
    String prefix = configService.getPrefix();
    return prefix + methodParams[0];
  }
}

@Dependent
class CompliantDependent implements CacheKeyGenerator {
  @Inject
  ConfigService configService;

  @Override
  public Object generate(Method method, Object... methodParams) {
    return configService.getPrefix() + methodParams[0];
  }
}

@RequestScoped
class CompliantRequestScoped implements CacheKeyGenerator {
  @Inject
  ConfigService configService;

  @Override
  public Object generate(Method method, Object... methodParams) {
    return configService.getPrefix() + methodParams[0];
  }
}

class NoncompliantPackagePrivateImplicitConstructor implements CacheKeyGenerator { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  @Override
  public Object generate(Method method, Object... methodParams) {
    return methodParams[0];
  }
}

class CompliantExplicitNoArgsConstructor implements CacheKeyGenerator {
  public CompliantExplicitNoArgsConstructor() {}

  @Override
  public Object generate(Method method, Object... methodParams) {
    return methodParams[0];
  }
}

class CompliantMultipleConstructorsWithNoArgs implements CacheKeyGenerator {
  private final String prefix;

  public CompliantMultipleConstructorsWithNoArgs() {
    this.prefix = "default";
  }

  public CompliantMultipleConstructorsWithNoArgs(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public Object generate(Method method, Object... methodParams) {
    return prefix + methodParams[0];
  }
}

abstract class CompliantAbstractClass implements CacheKeyGenerator {
}

interface CompliantInterface extends CacheKeyGenerator {
}

class NotACacheKeyGenerator {
  public NotACacheKeyGenerator(String param) {}
}

class ConfigService {
  public String getPrefix() {
    return "prefix";
  }
}

class DatabaseService {
  public String format(Object obj) {
    return obj.toString();
  }
}

class CacheConfig {
}
