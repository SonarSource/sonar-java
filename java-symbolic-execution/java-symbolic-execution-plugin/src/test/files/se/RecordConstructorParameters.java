package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.List;

public record RecordConstructorParameters(List<Message> messages) {

  public RecordConstructorParameters {
    requireNonNull(messages == null && messages.isEmpty()); // Noncompliant {{A "NullPointerException" could be thrown; "messages" is nullable here.}}
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

  public static void requireNonNull(boolean condition) {
  }

  record Message(String text) {
  }

}
