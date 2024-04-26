import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

class SpringScanDefaultPackageCheckSample {
  @Configuration
  @ComponentScan // Noncompliant {{Remove the annotation "@ComponentScan" or move the annotated class out of the default package.}}
// ^^^^^^^^^^^^^
  @SpringBootApplication // Noncompliant {{Remove the annotation "@SpringBootApplication" or move the annotated class out of the default package.}}
// ^^^^^^^^^^^^^^^^^^^^^
  @ServletComponentScan // Noncompliant {{Remove the annotation "@ServletComponentScan" or move the annotated class out of the default package.}}
// ^^^^^^^^^^^^^^^^^^^^
  class SomeApp {}

  @ComponentScan("") // Noncompliant {{Define packages to scan. Don't rely on the default package.}}
//               ^^
  @ComponentScan("org.company")
  @ComponentScan({
    "", // Noncompliant {{Define packages to scan. Don't rely on the default package.}}
//  ^^
    "org.company",
    "" // Noncompliant
  })
  @ComponentScan(value = "") // Noncompliant {{Define packages to scan. Don't rely on the default package.}}
//                       ^^
  @ComponentScan(value = {""}) // Noncompliant {{Define packages to scan. Don't rely on the default package.}}
//                        ^^
  @ComponentScan(lazyInit = true, basePackages = "") // Noncompliant
//                                               ^^
  @ComponentScan(basePackages = {""}) // Noncompliant
  @ComponentScan(basePackageClasses = {Map.class, java.util.Map.class})
  @ComponentScan(basePackageClasses = App.class) // Noncompliant {{Remove the annotation "@ComponentScan" or move the "App" class out of the default package.}}
//                                    ^^^
  @ComponentScan(basePackageClasses = {
    App.class, // Noncompliant {{Remove the annotation "@ComponentScan" or move the "App" class out of the default package.}}
//  ^^^
    Map.class,
    Controller.class, // Noncompliant
  })

  @SpringBootApplication(scanBasePackages = "") // Noncompliant
  class SomeApp2 {}
  @SpringBootApplication(scanBasePackageClasses = App.class) // Noncompliant
  class SomeApp3 {}

  @ServletComponentScan("") // Noncompliant
  class SomeApp4 {}
  @ServletComponentScan(value = "") // Noncompliant
  class SomeApp5 {}
  @ServletComponentScan(basePackages = {""}) // Noncompliant
  class SomeApp6 {}
  @ServletComponentScan(basePackageClasses = App.class) // Noncompliant
  class SomeApp7 {}

  public class App {

    public static final Class CLS = App.class;

    @ComponentScan() // Noncompliant
    @ComponentScan(basePackageClasses = ChildClass.class) // Noncompliant
    public static class ChildClass {
    }

  }

  class Controller {
  }
}
