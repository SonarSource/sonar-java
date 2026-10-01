package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// The anonymous Runnable triggers visitNode with a null simpleName — should be skipped gracefully.
@SpringBootApplication
class SpringBootAppWithAnonymousClass {
  Runnable r = new Runnable() {
    @Override
    public void run() {}
  };
}
