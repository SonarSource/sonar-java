import java.util.*;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableList;
import java.awt.*;
import java.nio.charset.*;
import java.nio.*;

class A {

  void notWildcardImport() {
    com.google.common.collect.ImmutableList list;
    com.google.common.collect.ImmutableList.Builder<Object> builder =
      com.google.common.collect.ImmutableList.builder();
    System.out.println(com.google.common.collect.ImmutableList.class);
    ImmutableList.builder();
    ImmutableList anotherList;
  }

  void wildcardImport() {
    java.util.List<String> myList =      // If we remove java.util.List, the code won't compile, because of the ambiguity with java.awt.List.
      new java.util.ArrayList<String>();

    com.google.common.collect.ImmutableMap map;

    java.awt.image.ImageProducer x; // OK
    java.nio.charset.Charset.defaultCharset().name();
  }
}
