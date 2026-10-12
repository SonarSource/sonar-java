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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;

import static org.sonar.java.serialization.JsonUtils.BEANS;
import static org.sonar.java.serialization.JsonUtils.FILES_DATA;
import static org.sonar.java.serialization.JsonUtils.PACKAGES;
import static org.sonar.java.serialization.JsonUtils.missingProperty;
import static org.sonar.java.serialization.JsonUtils.readStrings;
import static org.sonar.java.serialization.JsonUtils.required;
import static org.sonar.java.serialization.JsonUtils.writeStrings;

/**
 * JSON representation of the Spring context data collected by module and file. Null properties in bean definitions
 * remain explicit so the bean adapter can read them back with Gson's default settings.
 */
public final class SpringContextGatheringModelTypeAdapter extends TypeAdapter<SpringContextGatheringModel> {

  private static final SpringContextGatheringModelTypeAdapter INSTANCE = new SpringContextGatheringModelTypeAdapter();

  private SpringContextGatheringModelTypeAdapter() {
  }

  public static SpringContextGatheringModelTypeAdapter getInstance() {
    return INSTANCE;
  }

  @Override
  public void write(JsonWriter out, SpringContextGatheringModel model) throws IOException {
    boolean serializeNulls = out.getSerializeNulls();
    out.setSerializeNulls(true);
    try {
      out.beginObject();
      out.name(FILES_DATA);
      out.beginObject();
      for (Map.Entry<String, Map<String, SpringContextGatheringModel.InputFileData>> module : model.filesData().entrySet()) {
        out.name(module.getKey());
        out.beginObject();
        for (Map.Entry<String, SpringContextGatheringModel.InputFileData> file : module.getValue().entrySet()) {
          out.name(file.getKey());
          writeInputFileData(out, file.getValue());
        }
        out.endObject();
      }
      out.endObject();
      out.endObject();
    } finally {
      out.setSerializeNulls(serializeNulls);
    }
  }

  @Override
  public SpringContextGatheringModel read(JsonReader in) throws IOException {
    var model = new SpringContextGatheringModel();
    boolean filesDataRead = false;
    in.beginObject();
    while (in.hasNext()) {
      if (FILES_DATA.equals(in.nextName())) {
        readFilesData(in, model);
        filesDataRead = true;
      } else {
        in.skipValue();
      }
    }
    in.endObject();
    if (!filesDataRead) {
      throw missingProperty(FILES_DATA);
    }
    return model;
  }

  private static void writeInputFileData(JsonWriter out, SpringContextGatheringModel.InputFileData fileData) throws IOException {
    out.beginObject();
    out.name(BEANS);
    out.beginArray();
    for (BeanDefinitionHolder.InputFileData bean : fileData.beans()) {
      BeanDefinitionHolderTypeAdapter.getInstance().write(out, bean);
    }
    out.endArray();
    out.name(PACKAGES);
    writeStrings(out, fileData.packages());
    out.endObject();
  }

  private static void readFilesData(JsonReader in, SpringContextGatheringModel model) throws IOException {
    in.beginObject();
    while (in.hasNext()) {
      String moduleKey = in.nextName();
      model.ensureModule(moduleKey);
      in.beginObject();
      while (in.hasNext()) {
        readInputFileData(in, model, moduleKey, in.nextName());
      }
      in.endObject();
    }
    in.endObject();
  }

  private static void readInputFileData(JsonReader in, SpringContextGatheringModel model, String moduleKey, String fileKey) throws IOException {
    List<BeanDefinitionHolder.InputFileData> beans = null;
    Set<String> packages = null;
    in.beginObject();
    while (in.hasNext()) {
      switch (in.nextName()) {
        case BEANS -> beans = readBeans(in);
        case PACKAGES -> packages = readStrings(in);
        default -> in.skipValue();
      }
    }
    in.endObject();
    model.collectBeans(moduleKey, fileKey, null, required(beans, BEANS));
    model.collectPackages(moduleKey, fileKey, null, required(packages, PACKAGES));
  }

  private static List<BeanDefinitionHolder.InputFileData> readBeans(JsonReader in) throws IOException {
    List<BeanDefinitionHolder.InputFileData> beans = new ArrayList<>();
    in.beginArray();
    while (in.hasNext()) {
      beans.add(BeanDefinitionHolderTypeAdapter.getInstance().read(in));
    }
    in.endArray();
    return beans;
  }
}
