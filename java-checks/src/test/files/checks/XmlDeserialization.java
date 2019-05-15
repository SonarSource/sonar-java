
import java.beans.XMLDecoder;
import java.io.*;

class A {
  public void decode(InputStream in) {
    XMLDecoder xmlDecoder = new XMLDecoder(in); // Noncompliant [[secondary=8]] {{Make sure deserializing with XMLDecoder is safe here.}}
    Object result = xmlDecoder.readObject();
    xmlDecoder.close();
  }
}

class UsedAsField {

  XMLDecoder xmlDecoder = new XMLDecoder(new FileInputStream("file")); // Noncompliant [[secondary=21,26]]

  UsedAsField() throws FileNotFoundException {
  }

  public void decode() {
    xmlDecoder.readObject();
  }

  public void decode(InputStream in) {
    XMLDecoder xmlDecoder = new XMLDecoder(in); // Noncompliant [[secondary=26]]
    Object result = xmlDecoder.readObject();  // this will be highlighted as secondary for both issues, because we don't consider scope
    xmlDecoder.close();
  }
}


enum C {
  foo(new XMLDecoder(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))); // Noncompliant

  C(XMLDecoder xmlDecoder) { }
}
