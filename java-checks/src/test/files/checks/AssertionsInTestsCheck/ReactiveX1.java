import java.util.Arrays;
import org.junit.Test;

import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

public class ReactiveX1 {

  @Test
  public void noAssert() { // Noncompliant
    Observable<String> observable = Observable.just("string");
    observable.test();
    observable.test(10L);
  }

  @Test
  public void assertWithTestMethod() {
    Observable<String> observable = Observable.just("string");
    observable
      .test()
      .assertCompleted();
  }

  @Test
  public void assertWithTestObserver() {
    TestObserver<String> observer = new TestObserver<>();
    Observable<String> observable = Observable.just("string");

    observable.subscribe(observer);

    observer.assertTerminalEvent();
    observer.assertReceivedOnNext(Arrays.asList("string"));
  }

  @Test
  public void assertWithTestSubscriber() {
    TestSubscriber<String> subscriber = new TestSubscriber<>();
    Observable<String> observable = Observable.just("string");

    observable.subscribe(subscriber);

    subscriber.assertCompleted();
    subscriber.assertValue("string");
  }

}
