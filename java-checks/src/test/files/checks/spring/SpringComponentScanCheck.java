import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ComponentScan(basePackages = "com.myapp") // Noncompliant [[sc=2;ec=15]] {{Consider replacing "@ComponentScan" by a list of beans imported with @Import to speed-up the start-up of the application.}}
public class MyFirstApp {
}

@SpringBootApplication(scanBasePackages = "com.myapp") // Noncompliant [[sc=2;ec=23]] {{Consider replacing "@SpringBootApplication" by a list of beans imported with @Import to speed-up the start-up of the application.}}
class MySecondApp {
}

@SpringBootApplication // Noncompliant
class MyThirdApp {
}

@Configuration
@Import({
  MyController.class
})
class MyFinalApp {
}

@Controller
class MyController {
}
