package checks.spring.context;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

interface Repository<T> {
}

class User {
}

class Order {
}

@Component
class UserRepository implements Repository<User> {
}

@Configuration
class GenericBeans {

  @Bean
  Map<Integer, User> usersById() {
    return null;
  }

  @Bean
  List<Order> orders() {
    return null;
  }
}
