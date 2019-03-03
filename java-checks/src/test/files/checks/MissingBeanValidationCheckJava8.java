import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

class User {
  @NotNull
  private String name;
}

class NonCompliantUserSelection {
  private List<User> contents; // Noncompliant
}

class CompliantUserSelection {
  private List<@Valid User> contents; // Compliant
}

