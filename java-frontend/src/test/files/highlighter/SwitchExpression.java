package org.foo;

public class A {
  public boolean block() {
    return switch (Bool.random()) {
      case TRUE -> {
        System.out.println("Bool true");
        yield true;
      }
      case FALSE -> {
        System.out.println("Bool false");
        yield false;
      }
      case FILE_NOT_FOUND -> {
        var ex = new IllegalStateException("Ridiculous");
        throw ex;
      }
      default -> false;
    };
  }

  public enum Bool {
    TRUE, FALSE, FILE_NOT_FOUND;

    public static Bool random() {
      return TRUE;
    }
  }
}
