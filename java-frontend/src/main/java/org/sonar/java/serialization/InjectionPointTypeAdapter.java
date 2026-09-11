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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.sonar.java.serialization.JsonUtils.NAME;
import static org.sonar.java.serialization.JsonUtils.SPAN;
import static org.sonar.java.serialization.JsonUtils.readString;
import static org.sonar.java.serialization.JsonUtils.required;

/**
 * JSON representation of an {@link InjectionPoint} within a cached Spring bean definition.
 */
public final class InjectionPointTypeAdapter extends TypeAdapter<InjectionPoint> {

  private final InputFile inputFile;

  /**
   * @param inputFile The file the cache entry belongs to, against which locations are restored when reading. The
   *                  file is not part of the serialized form, since a cache entry only ever holds one file's data.
   */
  public InjectionPointTypeAdapter(InputFile inputFile) {
    this.inputFile = inputFile;
  }

  @Override
  public void write(JsonWriter out, InjectionPoint injectionPoint) throws IOException {
    out.beginObject();
    out.name(NAME).value(injectionPoint.name());
    out.name(SPAN);
    TextSpanTypeAdapter.getInstance().write(out, injectionPoint.location().mainLocation());
    out.endObject();
  }

  @Override
  public InjectionPoint read(JsonReader in) throws IOException {
    String name = null;
    AnalyzerMessage.TextSpan span = null;
    in.beginObject();
    while (in.hasNext()) {
      switch (in.nextName()) {
        case NAME -> name = readString(in);
        case SPAN -> span = TextSpanTypeAdapter.getInstance().read(in);
        default -> in.skipValue();
      }
    }
    in.endObject();
    return new InjectionPoint(required(name, NAME), new BeanLocation(inputFile, required(span, SPAN)));
  }
}
