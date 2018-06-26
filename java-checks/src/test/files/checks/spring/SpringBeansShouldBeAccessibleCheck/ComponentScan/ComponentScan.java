package foo;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import java.util.function.Consumer;

@Configuration
@ComponentScan({"src.test.files.checks.spring.A", "src.test.files.checks.spring.B"})
class Foo1 {
}

@ComponentScan("src.test.files.checks.spring.X")
class Foo2 {
  Consumer<Foo1> foo = new Consumer<Foo1>() { // anonymous classes are ignored
    @Override public void accept(Foo1 foo1) { }
  };
}

@ComponentScan(basePackageClasses = Bar4.class, basePackages = {"src.test.files.checks.spring.Y1"})
class Foo3 {

}

@ComponentScan(basePackages = "src.Y2")
class Foo4 {

}

@ComponentScan(value = "src.test.files.checks.spring.Z1")
class Foo5 {

}

@ComponentScan(value = {"src.test.files.checks.spring.Z2", "src.test.files.checks.spring.Z3"})
class Foo6 {
}

@ComponentScan(Foo7.PACKAGE) // this is not taken in consideration, see FalsePositive.java
class Foo7 {
  static final String PACKAGE = "falsey.positive";
}

@Component
class Bar1 { } // Noncompliant [[sc=7;ec=11]] {{'Bar1' is not reachable by @ComponentsScan or @SpringBootApplication. Either move it to a package configured in @ComponentsScan or update your @ComponentsScan configuration.}}

@Service
class Bar2 { } // Noncompliant

@Controller
class Bar3 { } // Noncompliant

@RestController
class Bar4 { } // Noncompliant, we ignore basePackageClasses

@Configuration
class Bar5 { } // ignored annotation
