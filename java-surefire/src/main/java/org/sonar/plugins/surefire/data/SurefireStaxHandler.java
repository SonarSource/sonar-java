/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.surefire.data;

import java.text.ParseException;
import java.util.Locale;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.ElementFilter;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.ParsingUtils;

public class SurefireStaxHandler {

  private final UnitTestIndex index;

  public SurefireStaxHandler(UnitTestIndex index) {
    this.index = index;
  }

  public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
    SMInputCursor testSuite = rootCursor.constructDescendantCursor(new ElementFilter("testsuite"));
    SMEvent testSuiteEvent;
    for (testSuiteEvent = testSuite.getNext(); testSuiteEvent != null; testSuiteEvent = testSuite.getNext()) {
      if (testSuiteEvent.compareTo(SMEvent.START_ELEMENT) == 0) {
        String testSuiteClassName = testSuite.getAttrValue("name");
        if (StringUtils.contains(testSuiteClassName, "$")) {
          // test suites for inner classes are ignored
          return;
        }
        parseTestCase(testSuiteClassName, testSuite.childCursor(new ElementFilter("testcase")));
      }
    }
  }

  private void parseTestCase(String testSuiteClassName, SMInputCursor testCase) throws XMLStreamException {
    for (SMEvent event = testCase.getNext(); event != null; event = testCase.getNext()) {
      if (event.compareTo(SMEvent.START_ELEMENT) == 0) {
        String testClassName = getClassname(testCase, testSuiteClassName);
        UnitTestClassReport classReport = index.index(testClassName);
        parseTestCase(testCase, testSuiteClassName, classReport);
      }
    }
  }

  private static String getClassname(SMInputCursor testCaseCursor, String defaultClassname) throws XMLStreamException {
    String testClassName = testCaseCursor.getAttrValue("classname");
    if (StringUtils.isNotBlank(testClassName) && testClassName.endsWith(")")) {
      int openParenthesisIndex = testClassName.indexOf('(');
      if (openParenthesisIndex > 0) {
        testClassName = testClassName.substring(0, openParenthesisIndex);
      }
    }
    return StringUtils.defaultIfBlank(testClassName, defaultClassname);
  }

  private static void parseTestCase(SMInputCursor testCaseCursor, String testSuiteClassName, UnitTestClassReport report) throws XMLStreamException {
    report.add(parseTestResult(testCaseCursor, testSuiteClassName));
  }

  private static void setStackAndMessage(UnitTestResult result, SMInputCursor stackAndMessageCursor) throws XMLStreamException {
    result.setMessage(stackAndMessageCursor.getAttrValue("message"));
    String stack = stackAndMessageCursor.collectDescendantText();
    result.setStackTrace(stack);
  }

  private static UnitTestResult parseTestResult(SMInputCursor testCaseCursor, String testSuiteClassName) throws XMLStreamException {
    UnitTestResult detail = new UnitTestResult();
    String name = getTestCaseName(testCaseCursor);
    detail.setName(name);
    detail.setTestSuiteClassName(testSuiteClassName);

    String status = UnitTestResult.STATUS_OK;
    String time = testCaseCursor.getAttrValue("time");
    Long duration = null;

    SMInputCursor childNode = testCaseCursor.descendantElementCursor();
    if (childNode.getNext() != null) {
      String elementName = childNode.getLocalName();
      if ("skipped".equals(elementName)) {
        status = UnitTestResult.STATUS_SKIPPED;
        // bug with surefire reporting wrong time for skipped tests
        duration = 0L;

      } else if ("failure".equals(elementName)) {
        status = UnitTestResult.STATUS_FAILURE;
        setStackAndMessage(detail, childNode);

      } else if ("error".equals(elementName)) {
        status = UnitTestResult.STATUS_ERROR;
        setStackAndMessage(detail, childNode);
      }
    }
    while (childNode.getNext() != null) {
      // make sure we loop till the end of the elements cursor
    }
    if (duration == null) {
      duration = getTimeAttributeInMS(time);
    }
    detail.setDurationMilliseconds(duration);
    detail.setStatus(status);
    return detail;
  }

  private static long getTimeAttributeInMS(String value) throws XMLStreamException {
    // hardcoded to Locale.ENGLISH see http://jira.codehaus.org/browse/SONAR-602
    try {
      Double time = ParsingUtils.parseNumber(value, Locale.ENGLISH);
      return !Double.isNaN(time) ? (long) ParsingUtils.scaleValue(time * 1000, 3) : 0L;
    } catch (ParseException e) {
      throw new XMLStreamException(e);
    }
  }

  private static String getTestCaseName(SMInputCursor testCaseCursor) throws XMLStreamException {
    String classname = testCaseCursor.getAttrValue("classname");
    String name = testCaseCursor.getAttrValue("name");
    if (StringUtils.contains(classname, "$")) {
      return StringUtils.substringAfter(classname, "$") + "/" + name;
    }
    return name;
  }

}
