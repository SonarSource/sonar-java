import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan // Noncompliant [[sc=2;ec=15]] {{Remove the annotation "@ComponentScan" or move the annotated class out of the default package.}}
@SpringBootApplication // Noncompliant [[sc=2;ec=23]] {{Remove the annotation "@SpringBootApplication" or move the annotated class out of the default package.}}
@ServletComponentScan // Noncompliant [[sc=2;ec=22]] {{Remove the annotation "@ServletComponentScan" or move the annotated class out of the default package.}}

@ComponentScan("") // Noncompliant [[sc=16;ec=18]] {{Define packages to scan. Don't rely on the default package.}}
@ComponentScan("org.company")
@ComponentScan({
  "", // Noncompliant [[sc=3;ec=5]] {{Define packages to scan. Don't rely on the default package.}}
  "org.company",
  "" // Noncompliant
})
@ComponentScan(value = "") // Noncompliant [[sc=24;ec=26]] {{Define packages to scan. Don't rely on the default package.}}
@ComponentScan(value = {""}) // Noncompliant [[sc=25;ec=27]] {{Define packages to scan. Don't rely on the default package.}}
@ComponentScan(lazyInit = true, basePackages = "") // Noncompliant [[sc=48;ec=50]]
@ComponentScan(basePackages = {""}) // Noncompliant
@ComponentScan(basePackageClasses = {Map.class, java.util.Map.class})
@ComponentScan(basePackageClasses = App.class) // Noncompliant [[sc=37;ec=40]] {{Remove the annotation "@ComponentScan" or move the "App" class out of the default package.}}
@ComponentScan(basePackageClasses = {
  App.class,  // Noncompliant [[sc=3;ec=6]] {{Remove the annotation "@ComponentScan" or move the "App" class out of the default package.}}
  Map.class,
  Controller.class, // Noncompliant
  Unknown.class
})

@SpringBootApplication(scanBasePackages = "") // Noncompliant
@SpringBootApplication(scanBasePackageClasses = App.class) // Noncompliant

@ServletComponentScan("") // Noncompliant
@ServletComponentScan(value = "") // Noncompliant
@ServletComponentScan(basePackages = {""}) // Noncompliant
@ServletComponentScan(basePackageClasses = App.class) // Noncompliant

public class App {

  public static final Class CLS = App.class;

  @ComponentScan // Noncompliant
  @ComponentScan(basePackageClasses = ChildClass.class) // Noncompliant
  @ComponentScan(basePackageClasses = CLS) // false-negative, no constant resolution for classes
  @ComponentScan(basePackageClasses = App.CLS) // false-negative
  public static class ChildClass {
  }

}

class Controller {
}
