import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

class SpringScanDefaultPackageCheckSample {
  @Configuration
  @ComponentScan  // Noncompliant [[sc=4;ec=17]] {{Remove the annotation "@ComponentScan" or move the annotated class out of the default package.}}
  @SpringBootApplication  // Noncompliant [[sc=4;ec=25]] {{Remove the annotation "@SpringBootApplication" or move the annotated class out of the default package.}}
  @ServletComponentScan  // Noncompliant [[sc=4;ec=24]] {{Remove the annotation "@ServletComponentScan" or move the annotated class out of the default package.}}
  class SomeApp {}

  @ComponentScan("") // Noncompliant [[sc=18;ec=20]] {{Define packages to scan. Don't rely on the default package.}}
  @ComponentScan("org.company")
  @ComponentScan({
    "", // Noncompliant [[sc=5;ec=7]] {{Define packages to scan. Don't rely on the default package.}}
    "org.company",
    "" // Noncompliant
  })
  @ComponentScan(value = "")  // Noncompliant [[sc=26;ec=28]] {{Define packages to scan. Don't rely on the default package.}}
  @ComponentScan(value = {""})  // Noncompliant [[sc=27;ec=29]] {{Define packages to scan. Don't rely on the default package.}}
  @ComponentScan(lazyInit = true, basePackages = "") // Noncompliant [[sc=50;ec=52]]
  @ComponentScan(basePackages = {""}) // Noncompliant
  @ComponentScan(basePackageClasses = {Map.class, java.util.Map.class})
  @ComponentScan(basePackageClasses = App.class) // Noncompliant [[sc=39;ec=42]] {{Remove the annotation "@ComponentScan" or move the "App" class out of the default package.}}
  @ComponentScan(basePackageClasses = {
    App.class,  // Noncompliant [[sc=5;ec=8]] {{Remove the annotation "@ComponentScan" or move the "App" class out of the default package.}}
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
