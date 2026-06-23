package checks;

import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.mongodb.repository.Query;

class RecordInsteadOfClassCheckPackagePrefixSample {

  @Value("${record.sum}")
  final class ClassWithSpringValueAnnotation { // Compliant, beans factory annotations are in scope
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

  @Serdeable
  final class ClassWithMicronautSerdeAnnotation { // Compliant, Micronaut serde annotations are in scope
    private final int sum;

    ClassWithMicronautSerdeAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @Configuration
  final class ClassWithSpringConfigurationAnnotation { // Noncompliant {{Refactor this class declaration to use 'record ClassWithSpringConfigurationAnnotation(int sum)'.}}
    private final int sum;

    ClassWithSpringConfigurationAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithSpringDataQueryGetter { // Noncompliant {{Refactor this class declaration to use 'record ClassWithSpringDataQueryGetter(int sum)'.}}
    private final int sum;

    ClassWithSpringDataQueryGetter(int sum) { this.sum = sum; }

    @Query("{}")
    int getSum() { return sum; }
  }

  @org.springframework.data.mongodb.core.mapping.Document
  final class ClassWithSpringDataMongoDocumentAnnotation { // Compliant, MongoDB document annotations are in scope
    private final int sum;

    ClassWithSpringDataMongoDocumentAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @Document
  final class ClassWithSpringDataElasticsearchDocumentAnnotation { // Compliant, Elasticsearch document annotations are in scope
    private final int sum;

    ClassWithSpringDataElasticsearchDocumentAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  final class ClassWithMicronautHttpGetter { // Noncompliant {{Refactor this class declaration to use 'record ClassWithMicronautHttpGetter(int sum)'.}}
    private final int sum;

    ClassWithMicronautHttpGetter(int sum) { this.sum = sum; }

    @Get
    int getSum() { return sum; }
  }
}
