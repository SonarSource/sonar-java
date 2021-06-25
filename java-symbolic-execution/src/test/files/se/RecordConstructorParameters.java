package org.sonar.java.checks;

public record RecordConstructorParameters(List<Message> messages) {

  public RecordConstructorParameters {
    requireNonNull(messages == null && messages.isEmtpy()); // Noncompliant {{A "NullPointerException" could be thrown; "messages" is nullable here.}}
  }

  public RecordConstructorParameters() {
    this(new ArrayList<>());
  }

  public static RecordConstructorParameters create(List<Message> messages) {
    if (messages == null) {
      return new RecordConstructorParameters();
    }
    return new RecordConstructorParameters(messages);
  }

}
