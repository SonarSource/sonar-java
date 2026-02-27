package checks;

public class MinimizeBoundScopedValuesCheckSample {

  private static final ScopedValue<String> USER_NAME = ScopedValue.newInstance();
  private static final ScopedValue<String> USER_ID = ScopedValue.newInstance();
  private static final ScopedValue<String> USER_EMAIL = ScopedValue.newInstance();
  private static final ScopedValue<String> USER_ROLE = ScopedValue.newInstance();

  public void processFour() {
    ScopedValue.where(USER_NAME, "John") // Noncompliant {{Consider grouping the 4 scoped values bound in this chain of method calls into a record class to maintain good performance.}}
      .where(USER_ID, "123")
      .where(USER_EMAIL, "john@example.com")
      .where(USER_ROLE, "admin")
      .run(this::doWork);
  }

  public void processThree() {
    ScopedValue.where(USER_NAME, "John") // Noncompliant {{Consider grouping the 3 scoped values bound in this chain of method calls into a record class to maintain good performance.}}
      .where(USER_ID, "123")
      .where(USER_EMAIL, "john@example.com")
      .run(this::doWork2);
  }

  public void processThreeSeparated() {
    ScopedValue<ScopedValue.Carrier> carrier = ScopedValue.newInstance();
    ScopedValue.where(carrier, ScopedValue.where(USER_NAME, "John")).run(() ->
      ScopedValue.where(USER_ID, "123") // Noncompliant {{Consider grouping the 3 scoped values bound in this chain of method calls into a record class to maintain good performance.}}
        .call(carrier::get)
        .where(USER_EMAIL, "john@example.com")
        .where(USER_ROLE, "admin")
        .run(this::doWork));
  }

  public void processThreeFromCarrier() {
    ScopedValue<ScopedValue.Carrier> carrier = ScopedValue.newInstance();
    ScopedValue.where(carrier, ScopedValue.where(USER_NAME, "John")).run(() ->
      carrier.get() // Noncompliant {{Consider grouping the 3 scoped values bound in this chain of method calls into a record class to maintain good performance.}}
        .where(USER_ID, "123")
        .where(USER_NAME, "John")
        .where(USER_EMAIL, "john@mail.com")
        .run(this::doWork)
    );
  }

  public void processTwoOrLess() {
    ScopedValue.where(USER_NAME, "John") // Compliant
      .where(USER_ID, "123")
      .run(this::doWork2);

    ScopedValue.where(USER_EMAIL, "john@email.org").run(() -> IO.print(USER_EMAIL.get())); // Compliant
  }

  private void doWork() {
    String importantMessage = String.format("User %s, ID %s, email %s, role %s", USER_NAME.get(), USER_ID.get(), USER_EMAIL.get(), USER_ROLE.get());
    IO.println(importantMessage);
  }

  private void doWork2() {
    String importantMessage = String.format("User %s, ID %s", USER_NAME.get(), USER_ID.get());
    IO.println(importantMessage);
  }

}
