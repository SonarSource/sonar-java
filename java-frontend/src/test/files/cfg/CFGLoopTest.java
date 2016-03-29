package trials;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CFGLoopTest {

  private Object a;
  private List<Foo> foos;

  void simpleWhileLoop() {
    int i = 0;
    while (i < 10) {
      i++;
      System.out.println(i);
    }
  }

  void simpleWhileLoopWithBreak() {
    int i = 0;
    while (i < 10) {
      i++;
      if (i == 5)
        break;
    }
  }

  void simpleForLoop() {
    for (int i = 0; i < 10; i++) {
      doSomething();
    }
  }

  void embeddedMixedLoops() {
    for (int i = 0; i < 10; i++) {
      int n = 0;
      while (n++ < 10) {
        if (n < 5) {
          doSomething();
        } else {
          doSomeOtherThing();
        }
        n += 1;
      }
      if (i < 5) {
        doSomething();
      } else {
        doSomeOtherThing();
      }
    }
  }

  public void mixedWithForEach() {
    Lock lock = new ReentrantLock();
    for (int i = 0; i < 10; i++) {
      lock.tryLock();
      lock = new ReentrantLock();
    }
    lock.unlock();
    for (Foo foo : foos) {
      lock.tryLock();
      lock = new ReentrantLock();
    }
    lock.unlock();
  }

  void doWhile() {
    int i = 0;
    do {
      i++;
      System.out.println(i);
    } while (i < 10);
  }

  void minimalForLoop() {
    a = null;
    for (; a != null;) {
      a.toString();
      break;
    }
  }

  void emptyFor() {
    for (;;) {
    }
  }

  void forWithOnlyInitializer() {
    for (int i = 1;;) {
    }
  }

  void emptyConditionFor() {
    for (;;) {
      doIt();
    }
  }

  void almostEmptyConditionFor() {
    for (int i = 1;;) {
      doIt();
    }
  }

  void embeddedLoops() {
    int j = 0;
    while (true) {
      j++;
      if (canExit()) {
        return;
      }
      while (true) {
        j++;
        if (canExit()) {
          return;
        }
      }
    }
  }

  void embeddedLoopsReturnInInnermost() {
    int j = 0;
    while (true) {
      j++;
      while (true) {
        j++;
        if (canExit()) {
          return;
        }
      }
    }
  }

  void doubleReturnWhileLoop() {
    int j = 0;
    while (true) {
      j++;
      if (canExit()) {
        return;
      }
      for (;;) {
        j++;
        if (canExit()) {
          return;
        }
      }
    }
  }

  private void doIt() {
  }

  private void doSomething() {
  }

  private void doSomeOtherThing() {
  }

  private boolean canExit() {
    return false;
  }

  private static class Foo {
  }

}
