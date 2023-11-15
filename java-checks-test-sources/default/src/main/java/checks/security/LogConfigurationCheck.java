package checks.security;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;

class Log4J2 {

  InputStream stream;
  File file;
  URL url;
  ClassLoader loader;
  Map<String, Level> levelMap;
  Configuration config;
  Appender appender;
  URI uri;
  LoggerContext context;
  Level level;
  Filter filter;

  // Questionable: creating a new custom configuration
  abstract class CustomConfigFactory extends ConfigurationFactory { } // Noncompliant [[sc=46;ec=66]] {{Make sure that this logger's configuration is safe.}}

  void fun() throws Exception {
    // Questionable: creating a new custom configuration
    ConfigurationBuilderFactory.newConfigurationBuilder(); // Noncompliant [[sc=5;ec=58]] {{Make sure that this logger's configuration is safe.}}

    // Questionable: setting loggers level can result in writing sensitive information in production
    Configurator.setAllLevels("com.example", Level.DEBUG); // Noncompliant
    Configurator.setLevel("com.example", Level.DEBUG); // Noncompliant
    Configurator.setLevel(levelMap); // Noncompliant
    Configurator.setRootLevel(Level.DEBUG); // Noncompliant

    // Questionable: this modifies the configuration
    config.addAppender(appender);  // Noncompliant

    context.setConfigLocation(uri);  // Noncompliant

    // Questionable: Load the configuration from a stream or file
    new ConfigurationSource(stream); // Noncompliant
    new ConfigurationSource(stream, file);// Noncompliant
    new ConfigurationSource(stream, url);// Noncompliant
    ConfigurationSource.fromResource("source", loader);// Noncompliant
    ConfigurationSource.fromUri(uri);// Noncompliant

    LoggerConfig loggerConfig = config.getRootLogger();
    loggerConfig.addAppender(appender, level, filter); // Noncompliant
    loggerConfig.setLevel(level); // Noncompliant

  }
}

class JavaLogging {
  java.util.logging.LogManager logManager;
  java.util.logging.Logger logger;
  java.util.logging.Handler handler;

  // === java.util.logging ===
  void fun(java.io.InputStream is) throws Exception {

    logManager.readConfiguration(is); // Noncompliant
    logger.setLevel(java.util.logging.Level.FINEST); // Noncompliant
    logger.addHandler(handler); // Noncompliant
  }
}

class LogBack {
  ch.qos.logback.classic.Logger logger;
  ch.qos.logback.core.FileAppender fileAppender;

  void fun() {
    // === Logback ===
    System.setProperty(ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE_PROPERTY, "config.xml"); // Noncompliant
    System.setProperty("someotherproperty", "config.xml");
    ch.qos.logback.classic.joran.JoranConfigurator configurator = new ch.qos.logback.classic.joran.JoranConfigurator(); // Noncompliant

    logger.addAppender(fileAppender); // Noncompliant
    logger.setLevel(ch.qos.logback.classic.Level.DEBUG); // Noncompliant
  }
}
