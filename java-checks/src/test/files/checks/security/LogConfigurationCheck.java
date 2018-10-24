import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.ConfigurationFactory;

class Log4J2 {

  // Questionable: creating a new custom configuration
  public class CustomConfigFactory extends ConfigurationFactory { // Noncompliant [[sc=44;ec=64]] {{Make sure that this logger's configuration is safe.}}
  }
  void fun() {
// Questionable: creating a new custom configuration
    ConfigurationBuilderFactory.newConfigurationBuilder(); // Noncompliant [[sc=5;ec=58]] {{Make sure that this logger's configuration is safe.}}

    // Questionable: setting loggers level can result in writing sensitive information in production
    Configurator.setAllLevels("com.example", org.apache.logging.log4j.Level.DEBUG); // Noncompliant
    Configurator.setLevel("com.example", org.apache.logging.log4j.Level.DEBUG); // Noncompliant
    java.util.Map<String,org.apache.logging.log4j.Level> levelMap;
    Configurator.setLevel(levelMap); // Noncompliant
    Configurator.setRootLevel(org.apache.logging.log4j.Level.DEBUG); // Noncompliant

    Configuration config;
    // Questionable: this modifies the configuration
    org.apache.logging.log4j.core.Appender appender;
    config.addAppender(appender);  // Noncompliant

    java.net.URI uri;
    LoggerContext context;
    context.setConfigLocation(uri);  // Noncompliant

    java.io.InputStream stream;
    java.io.File file;
    java.net.URL url;
    java.lang.ClassLoader loader;

    // Questionable: Load the configuration from a stream or file
    new ConfigurationSource(stream); // Noncompliant
    new ConfigurationSource(stream, file);// Noncompliant
    new ConfigurationSource(stream, url);// Noncompliant
    ConfigurationSource.fromResource("source", loader);// Noncompliant
    ConfigurationSource.fromUri(uri);// Noncompliant

    org.apache.logging.log4j.Level level;
    Filter filter;
    LoggerConfig loggerConfig = config.getRootLogger();
    loggerConfig.addAppender(appender, level, filter); // Noncompliant
    loggerConfig.setLevel(level); // Noncompliant

  }
}

class JavaLogging {
  // === java.util.logging ===
  void fun(java.io.InputStream is) {
    java.util.logging.LogManager logManager;
    logManager.readConfiguration(is); // Noncompliant

    java.util.logging.Logger logger;
    logger.setLevel(java.util.logging.Level.FINEST); // Noncompliant
    java.util.logging.Handler handler;
    logger.addHandler(handler); // Noncompliant
  }
}

class LogBack {
  void fun() {
    // === Logback ===
    System.setProperty(ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE_PROPERTY, "config.xml"); // Noncompliant
    System.setProperty("someotherproperty", "config.xml");
    ch.qos.logback.classic.joran.JoranConfigurator configurator = new ch.qos.logback.classic.joran.JoranConfigurator(); // Noncompliant

    ch.qos.logback.classic.Logger logger;
    ch.qos.logback.core.FileAppender fileAppender;
    logger.addAppender(fileAppender); // Noncompliant
    logger.setLevel(ch.qos.logback.classic.Level.DEBUG); // Noncompliant
  }
}
