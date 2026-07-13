package checks.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Configuration
public final class BasicFinalConfig { // Noncompliant {{Remove the "final" modifier from this "@Configuration" class.}}
//     ^^^^^

  @Bean
  public String dataSource() {
    return "dataSource";
  }
}

@Configuration
public final class FinalConfigMultipleBeans { // Noncompliant

  @Bean
  public String foo() {
    return "foo";
  }

  @Bean
  public String bar() {
    return foo();
  }
}

@Configuration
@EnableScheduling
public final class FinalConfigOtherAnnotations { // Noncompliant

  @Bean
  public String scheduler() {
    return "scheduler";
  }
}

class OuterClass {
  @Configuration
  public static final class NestedFinalConfig { // Noncompliant

    @Bean
    public String taskScheduler() {
      return "taskScheduler";
    }
  }
}

@Configuration(proxyBeanMethods = true)
public final class FinalConfigProxyEnabled { // Noncompliant

  @Bean
  public String dataSource() {
    return "dataSource";
  }
}

@Configuration
public class NonFinalConfig {

  @Bean
  public String dataSource() {
    return "dataSource";
  }

  @Bean
  public String jdbcTemplate() {
    return dataSource();
  }
}

@Configuration(proxyBeanMethods = false)
public final class FinalConfigProxyDisabled {

  @Bean
  public String dataSource() {
    return "dataSource";
  }
}

@Component
public final class FinalComponentNotConfiguration {

  public String createDataSource() {
    return "dataSource";
  }
}

@Configuration
public abstract class AbstractConfig {

  @Bean
  public String dataSource() {
    return "dataSource";
  }
}

class NoAnnotation {
  public final class NotAConfigurationClass {
    public String method() {
      return "value";
    }
  }
}

class OuterClassCompliant {
  @Configuration
  public static class NestedNonFinalConfig {

    @Bean
    public String taskScheduler() {
      return "taskScheduler";
    }
  }
}
