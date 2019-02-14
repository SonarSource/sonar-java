import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

public class ReactiveX2 {

  @Test
  public void noAssert() { // Noncompliant
    Observable<String> observable = Observable.just("string");
    observable.test();
    observable.test(true);
  }

  @Test
  public void assertWithTestMethod() {
    Observable<String> observable = Observable.just("string");
    observable
      .test()
      .assertSubscribed();
  }

  @Test
  public void assertWithTestObserver() {
    TestObserver<String> observer = new TestObserver<>();
    Observable<String> observable = Observable.just("string");

    observable.subscribe(observer);

    observer.assertResult("string");
  }

  @Test
  public void assertWithTestSubscriber() {
    TestSubscriber<String> subscriber = new TestSubscriber<>();
    Flowable<String> flowable = Flowable.just("string");

    flowable.subscribe(subscriber);

    subscriber.assertSubscribed();
    subscriber.assertValue("string");
  }

}
