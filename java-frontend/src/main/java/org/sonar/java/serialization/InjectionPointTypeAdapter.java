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
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.sonar.java.serialization.JsonUtils.MULTIPLE;
import static org.sonar.java.serialization.JsonUtils.NAME;
import static org.sonar.java.serialization.JsonUtils.SPAN;
import static org.sonar.java.serialization.JsonUtils.readString;
import static org.sonar.java.serialization.JsonUtils.required;

/**
 * JSON representation of an {@link InjectionPoint.InputFileData} within a cached Spring bean definition.
 */
public final class InjectionPointTypeAdapter extends TypeAdapter<InjectionPoint.InputFileData> {

  private static final InjectionPointTypeAdapter INSTANCE = new InjectionPointTypeAdapter();

  private InjectionPointTypeAdapter() {
  }

  public static InjectionPointTypeAdapter getInstance() {
    return INSTANCE;
  }

  @Override
  public void write(JsonWriter out, InjectionPoint.InputFileData injectionPoint) throws IOException {
    out.beginObject();
    out.name(NAME).value(injectionPoint.name());
    out.name(SPAN);
    TextSpanTypeAdapter.getInstance().write(out, injectionPoint.span());
    out.name(MULTIPLE).value(injectionPoint.multiple());
    out.endObject();
  }

  @Override
  public InjectionPoint.InputFileData read(JsonReader in) throws IOException {
    String name = null;
    AnalyzerMessage.TextSpan span = null;
    Boolean multiple = null;
    in.beginObject();
    while (in.hasNext()) {
      switch (in.nextName()) {
        case NAME -> name = readString(in);
        case SPAN -> span = TextSpanTypeAdapter.getInstance().read(in);
        case MULTIPLE -> multiple = in.nextBoolean();
        default -> in.skipValue();
      }
    }
    in.endObject();
    return new InjectionPoint.InputFileData(required(name, NAME), required(span, SPAN), required(multiple, MULTIPLE));
  }
}
