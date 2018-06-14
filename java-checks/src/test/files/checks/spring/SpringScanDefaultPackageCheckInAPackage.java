package files.checks.spring;

import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@SpringBootApplication
@ServletComponentScan

@ComponentScan(basePackages = "") // Noncompliant
@ComponentScan(basePackageClasses = App.class)

@SpringBootApplication(scanBasePackages = "") // Noncompliant
@SpringBootApplication(scanBasePackageClasses = App.class)

@ServletComponentScan(basePackages = {""}) // Noncompliant
@ServletComponentScan(basePackageClasses = App.class)

public class App {
}
