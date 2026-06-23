package checks;

import io.micronaut.http.annotation.Get;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.mongodb.repository.Query;

class RecordInsteadOfClassCheckPackagePrefixSample {

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
  final class ClassWithSpringDataMongoDocumentAnnotation { // Noncompliant {{Refactor this class declaration to use 'record ClassWithSpringDataMongoDocumentAnnotation(int sum)'.}}
    private final int sum;

    ClassWithSpringDataMongoDocumentAnnotation(int sum) { this.sum = sum; }
    int getSum() { return sum; }
  }

  @Document
  final class ClassWithSpringDataElasticsearchDocumentAnnotation { // Noncompliant {{Refactor this class declaration to use 'record ClassWithSpringDataElasticsearchDocumentAnnotation(int sum)'.}}
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
