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
package org.sonar.java.serialization;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.sonar.java.serialization.JsonUtils.END_CHARACTER;
import static org.sonar.java.serialization.JsonUtils.END_LINE;
import static org.sonar.java.serialization.JsonUtils.START_CHARACTER;
import static org.sonar.java.serialization.JsonUtils.START_LINE;
import static org.sonar.java.serialization.JsonUtils.readInt;
import static org.sonar.java.serialization.JsonUtils.required;

/**
 * JSON representation of the source location of a cached element.
 */
public final class TextSpanTypeAdapter extends TypeAdapter<AnalyzerMessage.TextSpan> {

  private static final TextSpanTypeAdapter INSTANCE = new TextSpanTypeAdapter();

  private TextSpanTypeAdapter() {
  }

  public static TextSpanTypeAdapter getInstance() {
    return INSTANCE;
  }

  @Override
  public void write(JsonWriter out, AnalyzerMessage.TextSpan span) throws IOException {
    out.beginObject();
    out.name(START_LINE).value(span.startLine);
    out.name(START_CHARACTER).value(span.startCharacter);
    out.name(END_LINE).value(span.endLine);
    out.name(END_CHARACTER).value(span.endCharacter);
    out.endObject();
  }

  /**
   * Rejects spans that cannot come from a real source range, so that a corrupted cache entry is discarded instead of
   * producing issues reported at meaningless locations.
   */
  @Override
  public AnalyzerMessage.TextSpan read(JsonReader in) throws IOException {
    Integer startLine = null;
    Integer startCharacter = null;
    Integer endLine = null;
    Integer endCharacter = null;
    in.beginObject();
    while (in.hasNext()) {
      switch (in.nextName()) {
        case START_LINE -> startLine = readInt(in);
        case START_CHARACTER -> startCharacter = readInt(in);
        case END_LINE -> endLine = readInt(in);
        case END_CHARACTER -> endCharacter = readInt(in);
        default -> in.skipValue();
      }
    }
    in.endObject();
    return newTextSpan(
      required(startLine, START_LINE),
      required(startCharacter, START_CHARACTER),
      required(endLine, END_LINE),
      required(endCharacter, END_CHARACTER)
    );
  }

  private static AnalyzerMessage.TextSpan newTextSpan(int startLine, int startCharacter, int endLine, int endCharacter) {
    if (startLine < 1 || startCharacter < 0 || endLine < startLine || endCharacter < 0 || (startLine == endLine && endCharacter < startCharacter)) {
      throw new IllegalArgumentException("Invalid source span: " + startLine + ":" + startCharacter + " to " + endLine + ":" + endCharacter);
    }
    return new AnalyzerMessage.TextSpan(startLine, startCharacter, endLine, endCharacter);
  }
}
