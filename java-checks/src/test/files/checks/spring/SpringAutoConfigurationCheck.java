import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;

@SpringBootApplication // Noncompliant
public class Foo1 {
}

@Configuration
@EnableAutoConfiguration // Noncompliant
public class Foo2 {
}

@Configuration
@EnableAutoConfiguration(excludeName = {}) // Noncompliant
public class Foo3 {
}

@SpringBootApplication(exclude = {}) // Noncompliant
public class Foo4 {
}

@SpringBootApplication(exclude = {
    MultipartAutoConfiguration.class,
    JmxAutoConfiguration.class,
})
public class Bar1 {
}

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration"
})
public class Bar2 {
}

@Configuration
@Import({
    HttpMessageConvertersAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    ServerPropertiesAutoConfiguration.class,
    WebMvcAutoConfiguration.class
})
public class Bar3 {
}

@Configuration
@EnableAutoConfiguration(exclude = {
    MultipartAutoConfiguration.class,
    JmxAutoConfiguration.class,
})
public class Bar4 {
}

@Configuration
@EnableAutoConfiguration(excludeName = {
    "org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration",
    "org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration"
})
public class Bar5 {
}

@SpringBootApplication(foo = "foo") // Noncompliant
public class Bar6 {
}
