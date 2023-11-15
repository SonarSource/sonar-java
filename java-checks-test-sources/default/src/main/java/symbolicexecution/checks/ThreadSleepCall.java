package symbolicexecution.checks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class ThreadSleepCall {
  private boolean okToPrint = true;

  public void echoFile(PrintStream ps, File f) throws IOException, InterruptedException {
    BufferedReader br = new BufferedReader(new FileReader(f));
    try {
      while (okToPrint) {
        Thread.sleep(1000);
        ps.println(br.readLine());
      }
    } catch (IOException ex) {
      if (okToPrint) { // compliant
        throw new RuntimeException("Couldn't read a line", ex);
      }
      throw new RuntimeException("Got an error, but it's not OK to print", ex);
    }
  }

  public void stopPrinting() {
    okToPrint = false;
  }

}
