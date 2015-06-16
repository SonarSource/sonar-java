import java.lang.RuntimeException;
import org.junit.Test;
import org.junit.Ignore;

public class MyTest {
  @Test
  public void success() throws Exception {
    Thread.sleep(1000);
  }

  @Ignore
  @Test
  public void ignored() {
  }
}
