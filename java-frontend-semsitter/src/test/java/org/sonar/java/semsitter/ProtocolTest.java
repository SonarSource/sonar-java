/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import com.google.protobuf.MessageLite;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactEntry;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.Node;
import org.sonarsource.semsitter.wire.Place;
import org.sonarsource.semsitter.wire.Ref;
import org.sonarsource.semsitter.wire.StringList;
import org.sonarsource.semsitter.wire.Symbol;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolTest {

  @Test
  void document_round_trips() throws IOException {
    TypeRef listOfString = TypeRef.newBuilder()
      .setId("java.util.List")
      .addArguments(TypeRef.newBuilder().setId("java.lang.String"))
      .build();
    Document document = Document.newBuilder()
      .setPath("p/A.java")
      .setSemantic(true)
      .setTree(Node.newBuilder()
        .setKind("program")
        .addChildren(Node.newBuilder()
          .setKind("identifier")
          .setNamed(true)
          .setStartLine(2)
          .setText("xs")
          .addDecls("p.A#xs")
          .addRefs(Ref.newBuilder()
            .setOutcome("resolved")
            .setTarget("java.util.List")
            .putFacts("type", FactValue.newBuilder().setType(listOfString).build()))))
      .addSymbols(Symbol.newBuilder()
        .setId("p.A#xs")
        .setKind("field")
        .setName("xs")
        .setPlace(Place.newBuilder().setStartByte(30).setEndByte(32).setLine(2).setColumn(4))
        .putFacts("final", FactValue.newBuilder().setFlag(true).build()))
      .addFacts(FactEntry.newBuilder()
        .setKey("p.A")
        .setValue(Fact.newBuilder()
          .setKind("class")
          .addProps(org.sonarsource.semsitter.wire.Prop.newBuilder()
            .setName("modifiers")
            .addValues("public")
            .build())
          .build()))
      .build();

    Document read = Document.parseDelimitedFrom(roundTrip(document));

    assertThat(read).isEqualTo(document);
    assertThat(read.getTree().getChildren(0).getRefs(0).getFactsOrThrow("type").getType().getArguments(0).getId())
      .isEqualTo("java.lang.String");
    assertThat(read.getSymbols(0).getFactsOrThrow("final").getFlag()).isTrue();
  }

  @Test
  void string_lists_round_trip() throws IOException {
    StringList libraries = StringList.newBuilder().addValues("a.jar").addValues("b.jar").build();

    StringList read = StringList.parseDelimitedFrom(roundTrip(libraries));

    assertThat(read).isEqualTo(libraries);
  }

  private static InputStream roundTrip(MessageLite message) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    message.writeDelimitedTo(out);
    return new ByteArrayInputStream(out.toByteArray());
  }
}
