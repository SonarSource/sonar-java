import java.io.FileInputStream;
import java.io.FileOutputStream;

class TryWithResources {

  public void example() throws Exception {
    // single resource
    try (FileInputStream in = new FileInputStream("foo.txt")) {
      int k;
      while ((k = in.read()) != -1) {
        System.out.write(k);
      }
    }

    try (FileInputStream in = new FileInputStream("foo.txt");) {}

    // multiple resources
    try (FileInputStream in = new FileInputStream("foo.txt"); FileOutputStream out = new FileOutputStream("bar.txt")) {}

    try (FileInputStream in = new FileInputStream("foo.txt"); FileOutputStream out = new FileOutputStream("bar.txt");) {}
  }

}
