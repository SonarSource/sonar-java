package checks;

public class UseLombokCheckSample {
  String name;

  String getName() { // Noncompliant {{Consider using @Getter and @Setter from Lombok to reduce boilerplate.}}
    return name;
  }
}
