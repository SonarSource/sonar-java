import java.io.BufferedReader;
import java.io.FileReader;

class A {
  void foo(String fileName) {
    FileReader fr = null;
    BufferedReader br = null;
    try { // Noncompliant [[sc=5;ec=8;secondary=9,10]] {{Change this "try" to a try-with-resources.}}
      fr = new FileReader(fileName);
      br = new BufferedReader(fr);
      return br.readLine();
    } catch (Exception e) {
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }
      if (fr != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }
    }
    try { // compliant, no finally block so let's rely on unclosed resource rule
      fr = new FileReader(fileName);
    } catch (Exception e){

    }
    try (
      FileReader fr = new FileReader(fileName);
      BufferedReader br = new BufferedReader(fr)) { // compliant
      return br.readLine();
    } catch (Exception e) {
    }

  }
}
