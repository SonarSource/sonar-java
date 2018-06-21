package src.test.files.checks.spring.A;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;


@Component
class A1 { // Compliant
  A1() {}
}

@Service
class A2 { // Compliant
  A2();
}

@Controller
class A3 { // Compliant
  A3();
}

@RestController
class A4 { // Compliant
  A4();
}

@Repository
class A5 { // Compliant
  class A5Inner {
  }
}
