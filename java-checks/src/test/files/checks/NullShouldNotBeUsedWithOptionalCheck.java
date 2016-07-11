import java.util.Optional;
import javax.annotation.Nullable;

interface A {

  @Nullable                     // Noncompliant [[sc=3;ec=12]] {{Methods with an "Optional" return type should not be "@Nullable".}}
  public Optional<String> getOptionalKo();

}

class ClassA {
  
  @Nullable                     // Noncompliant [[sc=3;ec=12]] {{Methods with an "Optional" return type should not be "@Nullable".}}
  public Optional<String> getOptionalKo() {
    return null;                // Noncompliant [[sc=12;ec=16]] {{Methods with an "Optional" return type should never return null.}}
  }
  
  public Optional<String> getOptionalOk() {
    return Optional.of("hello");
  }
  
  public Object doSomething1() {
    return null;
  }
  
  public Optional<String> doSomething2() {
    Worker x = new Worker() {
      public String work() {
        return null;
      }
    };
    return Optional.of("hello");
  }
  
  public void doSomething3() {
    Optional<String> optional = getOptionalOk();
    if (optional == null) {           // Noncompliant [[sc=9;ec=25]] {{Remove this null-check of an "Optional".}}
      return;
    } else if (null != optional) {    // Noncompliant [[sc=16;ec=32]] {{Remove this null-check of an "Optional".}}
      return;
    }
    
    Optional<String> optional2 = getOptionalOk();
    if (optional == optional2) {
      return;
    } else if (null == null) {
      return;
    }
  }
  
  interface Worker {
    String work();
  }
  
}
