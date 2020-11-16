package checks;

import java.util.*;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableList;
import java.awt.*;
import java.nio.charset.*;
import java.nio.*;

class WildCard {

  void notWildcardImport() {
    ImmutableList list;
    ImmutableList.Builder<Object> builder =
      ImmutableList.builder();
    System.out.println(ImmutableList.class);
    ImmutableList.builder();
    ImmutableList anotherList;
  }

  void wildcardImport() {
    java.util.List<String> myList =      // If we remove java.util.List, the code won't compile, because of the ambiguity with java.awt.List.
      new ArrayList<String>();

    ImmutableMap map;

    java.awt.image.ImageProducer x; // OK
    Charset.defaultCharset().name();
  }
}
