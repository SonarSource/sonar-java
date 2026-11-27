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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sonarsource.semsitter.wire.Document;

/**
 * What the SemSitter frontend asks of a running SemSitter server. {@link SemSitterClient} answers it over a process.
 */
public interface SemSitterApi extends AutoCloseable {

  void setSettings(Map<String, List<String>> settings) throws IOException;

  int fullGen(Collection<String> files) throws IOException;

  Document getFile(String path, int ahead) throws IOException;

  /**
   * @return the JSON object of declarations, as {@code types.schema.json} describes it
   */
  byte[] getTypes(Collection<String> names) throws IOException;

  Map<String, Boolean> hasPackage(Collection<String> names) throws IOException;

  @Override
  void close();
}
