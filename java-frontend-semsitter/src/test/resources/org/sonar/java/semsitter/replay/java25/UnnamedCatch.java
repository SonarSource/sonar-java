import java.io.FileNotFoundException;
import java.io.IOException;

class UnnamedCatch {
  void read() {
    try {
      throw new FileNotFoundException();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException _) {
    }
  }
}
