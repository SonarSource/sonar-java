package test;

import java.io.ObjectInputStream;

public class ObjectDeserialization {

  public void foo() {
    InputStream untrusted = new WhatEverInputStream();
    ObjectInputStream ois = new ObjectInputStream(untrusted);
    Foo deserialized = (Foo) ois.readObject();  // Noncompliant [[sc=30;ec=46]] {{Verify there is no risk to deserialize from this source}}
    deserialized = (Foo) ois.readUnshared();  // Noncompliant {{Verify there is no risk to deserialize from this source}}
  }

}

class CustomOIS extends ObjectInputStream {

  public CustomOIS(InputStream in) throws IOException {
    super(in);
  }
}

class Test {
  public void m() throws IOException {
    CustomOIS ois = new CustomOIS(new FileInputStream("test"));
    ois.readObject(); // Noncompliant
  }
}
