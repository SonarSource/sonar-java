import java.io.FileNotFoundException;
import java.io.IOException;

class MultiCatch {

  public void example() {
    try {
      Class.forName("org.example.Foo").newInstance();
    } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

}
