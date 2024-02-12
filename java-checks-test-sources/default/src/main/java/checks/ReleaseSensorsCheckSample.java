package checks;

public class ReleaseSensorsCheckSample {

  public class WifiManager {

    public void test() {
      MulticastLock multicastLock = new MulticastLock();
      multicastLock.acquire();// Noncompliant {{Make sure to release this sensor.}}
    }

    public class MulticastLock {
      public void acquire() {
        String str = "acquire Called";
      }

      public void release() {
        String str = "release Called";
      }
    }
  }
}
