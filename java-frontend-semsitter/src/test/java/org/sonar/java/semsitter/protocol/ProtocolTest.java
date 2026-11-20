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
package org.sonar.java.semsitter.protocol;

import com.google.protobuf.MessageLite;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolTest {

  @Test
  void requests_round_trip_length_delimited() throws IOException {
    Request settings = Request.newBuilder()
      .setId(1)
      .setSetSettings(SetSettingsRequest.newBuilder()
        .putSettings("jvm.libraries", StringList.newBuilder().addValues("/libs/a.jar").addValues("/libs/b.jar").build()))
      .build();
    Request getFile = Request.newBuilder()
      .setId(2)
      .setGetFile(GetFileRequest.newBuilder().setPath("p/A.java").setAhead(1))
      .build();

    InputStream in = roundTrip(settings, getFile);

    assertThat(Request.parseDelimitedFrom(in)).isEqualTo(settings);
    Request read = Request.parseDelimitedFrom(in);
    assertThat(read.getCommandCase()).isEqualTo(Request.CommandCase.GET_FILE);
    assertThat(read.getGetFile().getPath()).isEqualTo("p/A.java");
    assertThat(Request.parseDelimitedFrom(in)).isNull();
  }

  @Test
  void document_round_trips_with_symbols_references_and_facts() throws IOException {
    TypeRef listOfString = TypeRef.newBuilder()
      .setId("java.util.List")
      .addArguments(TypeRef.newBuilder().setId("java.lang.String"))
      .build();
    Node identifier = Node.newBuilder()
      .setKind("identifier")
      .setNamed(true)
      .setStartLine(2)
      .setStartColumn(4)
      .setText("xs")
      .addDeclarations("p.A#xs")
      .addReferences(Reference.newBuilder()
        .setOutcome("resolved")
        .setTarget("java.util.List")
        .putFacts("type", FactValue.newBuilder().setType(listOfString).build()))
      .build();
    Response response = Response.newBuilder()
      .setId(3)
      .setDocument(Document.newBuilder()
        .setPath("p/A.java")
        .setSemantic(true)
        .setTree(Node.newBuilder().setKind("program").addChildren(identifier))
        .addSymbols(Symbol.newBuilder()
          .setId("p.A#xs")
          .setKind("field")
          .setName("xs")
          .setPlace(Place.newBuilder().setStartByte(30).setEndByte(32).setLine(2).setColumn(4))
          .putFacts("final", FactValue.newBuilder().setFlag(true).build()))
        .putFacts("p.A", Fact.newBuilder()
          .setKind("class")
          .putProps("modifiers", StringList.newBuilder().addValues("public").build())
          .build()))
      .build();

    Response read = Response.parseDelimitedFrom(roundTrip(response));

    assertThat(read).isEqualTo(response);
    assertThat(read.getResultCase()).isEqualTo(Response.ResultCase.DOCUMENT);
    Reference reference = read.getDocument().getTree().getChildren(0).getReferences(0);
    assertThat(reference.getFactsOrThrow("type").getType().getArguments(0).getId()).isEqualTo("java.lang.String");
  }

  @Test
  void type_declarations_round_trip() throws IOException {
    Member get = Member.newBuilder()
      .setKind(Member.Kind.METHOD)
      .setName("get")
      .setJvmDescriptor("(I)Ljava/lang/Object;")
      .setIsAbstract(true)
      .setReturnType(TypeRef.newBuilder().setKind(TypeRef.Kind.TYPE_VARIABLE).setId("E"))
      .addParameterTypes(TypeRef.newBuilder().setId("int"))
      .addAnnotations(Annotation.newBuilder()
        .setName("java.lang.Deprecated")
        .addValues(ElementValue.newBuilder().setName("forRemoval").setBooleanValue(true)))
      .build();
    Response response = Response.newBuilder()
      .setId(4)
      .addWarnings("unresolved import x.Y")
      .setTypes(TypesResponse.newBuilder()
        .putDeclarations("java.util.List", TypeDeclaration.newBuilder()
          .setOrigin("jdk")
          .setKind("interface")
          .addInterfaces("java.util.Collection")
          .setTypeParameterCount(1)
          .addTypeVariables("E")
          .addMembers(get)
          .build())
        .addMissing("x.Missing"))
      .build();

    Response read = Response.parseDelimitedFrom(roundTrip(response));

    assertThat(read).isEqualTo(response);
    TypeDeclaration list = read.getTypes().getDeclarationsOrThrow("java.util.List");
    assertThat(list.hasSuperclass()).isFalse();
    assertThat(list.getMembers(0).hasReturnParameter()).isFalse();
    assertThat(list.getMembers(0).getAnnotations(0).getValues(0).getValueCase()).isEqualTo(ElementValue.ValueCase.BOOLEAN_VALUE);
  }

  @Test
  void error_and_empty_results_are_told_apart() throws IOException {
    Response error = Response.newBuilder().setId(5).setError("no such file p/Nope.java").build();
    Response quit = Response.newBuilder().setId(6).build();

    InputStream in = roundTrip(error, quit);

    assertThat(Response.parseDelimitedFrom(in).getResultCase()).isEqualTo(Response.ResultCase.ERROR);
    assertThat(Response.parseDelimitedFrom(in).getResultCase()).isEqualTo(Response.ResultCase.RESULT_NOT_SET);
  }

  private static InputStream roundTrip(MessageLite... messages) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (MessageLite message : messages) {
      message.writeDelimitedTo(out);
    }
    return new ByteArrayInputStream(out.toByteArray());
  }
}
