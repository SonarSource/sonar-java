package checks;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.Head;
import io.micronaut.http.annotation.Trace;




public class TooManyParametersCheck {
  TooManyParametersCheck(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Noncompliant {{Constructor has 8 parameters, which is greater than 7 authorized.}}
  }

  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Noncompliant [[sc=8;ec=14]] {{Method has 8 parameters, which is greater than 7 authorized.}}
  }

  void otherMethod(int p1) {}

  static void staticMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
}

class TooManyParametersExtended extends TooManyParametersCheck {
  TooManyParametersExtended(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    super(p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {}

  static void staticMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
}

class MethodsUsingSpringRequestMapping {
  @RequestMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @RequestMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringGetMapping {
  @GetMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @GetMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringPostMapping {
  @PostMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @PostMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringPutMapping {
  @PutMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @PutMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringDeleteMapping {
  @DeleteMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @DeleteMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringPatchMapping {
  @PatchMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @PatchMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingJsonCreator {
  @JsonCreator
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingAnnotations {
  @javax.ws.rs.GET
  public void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.ws.rs.POST
  public void foo1(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.ws.rs.PUT
  public void foo2(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.ws.rs.PATCH
  public void foo3(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @org.springframework.beans.factory.annotation.Autowired
  public void foo4(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @javax.inject.Inject
  public void foo5(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

@Component
class SpringComponent{
  public SpringComponent(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

@Component
class SpringComponentMoreThanOneConstructor{
  public SpringComponentMoreThanOneConstructor(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Noncompliant
  public SpringComponentMoreThanOneConstructor(String p1, String p2, String p3) {} // Compliant
}

@Configuration
class SpringConfiguration{
  public SpringConfiguration(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

@Configuration
class SpringConfigurationMoreThanOneConstructor{
  public SpringConfigurationMoreThanOneConstructor(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Noncompliant
  public SpringConfigurationMoreThanOneConstructor(String p1, String p2, String p3) {}; // Compliant
  
  @Bean
  public void testBean(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  
}

@Repository
class SpringRepository{
  public SpringRepository(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

@Repository
class SpringRepositoryMoreThanOneConstructor{
  public SpringRepositoryMoreThanOneConstructor(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Noncompliant
  public SpringRepositoryMoreThanOneConstructor(String p1, String p2, String p3) {}; // Compliant
}

@Service
class SpringService{
  public SpringService(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

@Service
class SpringServiceMoreThanOneConstructor{
  public SpringServiceMoreThanOneConstructor(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Noncompliant
  public SpringServiceMoreThanOneConstructor(String p1, String p2, String p3) {}; // Compliant
}

class MicronautHttpAnnotations{
  
  @Get
  public void get(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Post
  public void post(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Put
  public void put(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Delete
  public void delete(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Options
  public void options(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Patch
  public void patch(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Head
  public void head(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  @Trace
  public void trace(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
  
}

/* 
 * We can maybe have a discussion on records' constructors, if they should be checked against the rule,
 * but since they are used to store articulate data, it might not be a good idea
 * The one note to keep in mind is that it seems like a record could be used as a Spring @Component
 */
record Record1(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant 
 