package checks;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Head;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
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

  @javax.inject.Inject
  public void foo5(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
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
 * Exceptions to the rule : RECORD, ANNOTATION_TYPE (annotations cannot have method params nor constructors)
 */
record Record1(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant 

 