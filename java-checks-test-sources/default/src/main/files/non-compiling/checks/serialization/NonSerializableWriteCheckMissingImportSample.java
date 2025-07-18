package checks.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;

class NonSerializableWriteCheckMissingImportSample {
  public record R(String foo, Boolean bar) implements Serializable {}

  public void writeOut(ObjectOutputStream oos) throws IOException {
    R r = new R("foo", true);
    oos.writeObject(r);
  }
}
