package checks.spring.s4605.componentScan;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import java.util.function.Consumer;

@Configuration
@ComponentScan({"checks.spring.s4605.componentScan.packageA", "checks.spring.s4605.componentScan.packageB"})
class Foo1 { }

@ComponentScan("checks.spring.s4605.componentScan.packageX")
class Foo2 {
  Consumer<Foo1> foo = new Consumer<Foo1>() { // anonymous classes are ignored
    @Override public void accept(Foo1 foo1) { }
  };
}

@ComponentScan(basePackageClasses = Bar4.class, basePackages = {"checks.spring.s4605.componentScan.packageY"})
class Foo3 { }

@ComponentScan(basePackages = "checks.spring.s4605.componentScan.packageZ")
class Foo4 { }

@ComponentScan(value = "checks.spring.s4605.componentScan.unknownPackage1")
class Foo5 { }

@ComponentScan(value = {"checks.spring.s4605.componentScan.unknownPackage2", "checks.spring.s4605.componentScan.unknownPackage3"})
class Foo6 { }

@ComponentScan(Foo7.PACKAGE) // this is not taken in consideration, see FalsePositive.java
class Foo7 {
  static final String PACKAGE = "checks.spring.s4605.componentScan.packageFP";
}

@Component
class Bar1 { } // Noncompliant {{'Bar1' is not reachable by @ComponentScan or @SpringBootApplication. Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.}}
//    ^^^^

@Service
class Bar2 { } // Noncompliant

@Controller
class Bar3 { } // Noncompliant

@RestController
class Bar4 { } // Noncompliant

@Configuration
class Bar5 { } // ignored annotation
