package checks;

import io.micronaut.http.annotation.Get;
import io.micronaut.core.annotation.Introspected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;

class RecordInsteadOfClassCheckPackagePrefixSample {

  final class ClassWithSpringValueAnnotation { // Compliant, beans factory annotations are in scope
    @Value("${record.sum}")
    private final int sum;

    ClassWithSpringValueAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @ConfigurationProperties("record")
  final class ClassWithConfigurationPropertiesAnnotation { // Compliant, boot context properties annotations are in scope
    private final int sum;

    ClassWithConfigurationPropertiesAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithSpringDataAnnotationField { // Compliant, spring data annotations are in scope
    @Id
    private final int sum;

    ClassWithSpringDataAnnotationField(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @Introspected
  final class ClassWithMicronautCoreAnnotation { // Compliant, Micronaut core annotations are in scope
    private final int sum;

    ClassWithMicronautCoreAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @Configuration
  final class ClassWithSpringConfigurationAnnotation { // Noncompliant {{Refactor this class declaration to use 'record ClassWithSpringConfigurationAnnotation(int sum)'.}}
    private final int sum;

    ClassWithSpringConfigurationAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithMicronautHttpGetter { // Noncompliant {{Refactor this class declaration to use 'record ClassWithMicronautHttpGetter(int sum)'.}}
    private final int sum;

    ClassWithMicronautHttpGetter(int sum) { this.sum = sum; }

    @Get
    int getSum() { return sum; }
  }
}
