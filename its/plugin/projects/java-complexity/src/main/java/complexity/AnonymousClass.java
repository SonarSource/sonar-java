package complexity;

import java.io.Serializable;
import java.lang.Runnable;

// class complexity: 3+1
public class AnonymousClass {

  // method complexity: 1
  public void hasComplexAnonymousClass() {
    Runnable runnable = new Runnable() {
      public void run() {
        if (true) {
          System.out.println("true");
        }
      }
    };
  }

  // method complexity: 1
  public void hasEmptyAnonymousClass() {
    Serializable serializable = new Serializable() {

    };
  }
}
