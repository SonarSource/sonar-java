package checks.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.SneakyThrows;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.machinelearning.MachineLearningClient;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsReusableResourcesInitializedOnceCheckSample {

  class RequestHandlerImpl implements RequestHandler<Object, Void> {

    public RequestHandlerImpl() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
      S3Client.builder().build();
      MachineLearningClient.builder().build();
      DriverManager.getConnection("foo");
      var customClient = new FooClient();
      called();
      notCalled();
    }

    @SneakyThrows
    @Override
    public Void handleRequest(Object o, Context context) {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build(); // Noncompliant
//                                                                       ^^^^^
      S3Client.builder().build(); // Noncompliant
//                       ^^^^^
      MachineLearningClient.builder().build(); // Noncompliant
//                                    ^^^^^
      DriverManager.getConnection("foo"); // Noncompliant
//                  ^^^^^^^^^^^^^
      var customClient = new FooClient(); // Noncompliant
//                           ^^^^^^^^^
      var compliant = new Object();
      build();

      called();
      return null;
    }

    // Similar signature but not same:
    public Void handleRequest(Object o, Context context, Object foo) throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
      S3Client.builder().build();
      MachineLearningClient.builder().build();
      DriverManager.getConnection("foo");
      var customClient = new FooClient();
      var compliant = new Object();

      called();
      return null;
    }

    void called() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build(); // Noncompliant
      S3Client.builder().build(); // Noncompliant
      MachineLearningClient.builder().build(); // Noncompliant
      var customClient = new FooClient(); // Noncompliant
      DriverManager.getConnection("foo"); // Noncompliant
      transitiveCallee1();
    }

    void notCalled() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
      S3Client.builder().build();
      MachineLearningClient.builder().build();
      var customClient = new FooClient();
      DriverManager.getConnection("foo");
    }

    void transitiveCallee1() throws SQLException {
      transitiveCallee2();
    }

    void transitiveCallee2() throws SQLException {
      transitiveCallee3();
    }

    void transitiveCallee3() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build(); // Noncompliant
      S3Client.builder().build(); // Noncompliant
      MachineLearningClient.builder().build(); // Noncompliant
      var customClient = new FooClient(); // Noncompliant
      DriverManager.getConnection("foo"); // Noncompliant
    }
  }

  class RequestStreamHandlerImpl implements RequestStreamHandler {

    public RequestStreamHandlerImpl() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
      S3Client.builder().build();
      MachineLearningClient.builder().build();
      var customClient = new FooClient();
      DriverManager.getConnection("foo");
      called();
      notCalled();
    }

    @SneakyThrows
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build(); // Noncompliant
      S3Client.builder().build(); // Noncompliant
      MachineLearningClient.builder().build(); // Noncompliant
      var customClient = new FooClient(); // Noncompliant
      DriverManager.getConnection("foo"); // Noncompliant

      called();
    }

    // Similar signature but not same:
    public void handleRequest(Object o, Context context) throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
      S3Client.builder().build();
      MachineLearningClient.builder().build();
      DriverManager.getConnection("foo");
      var customClient = new FooClient();
      var compliant = new Object();

      called();
    }

    void called() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build(); // Noncompliant
      S3Client.builder().build(); // Noncompliant
      MachineLearningClient.builder().build(); // Noncompliant
      var customClient = new FooClient(); // Noncompliant
      DriverManager.getConnection("foo"); // Noncompliant

      transitiveCallee1();
    }

    void notCalled() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
      S3Client.builder().build();
      MachineLearningClient.builder().build();
      var customClient = new FooClient();
      DriverManager.getConnection("foo");
    }

    void transitiveCallee1() throws SQLException {
      transitiveCallee2();
    }

    void transitiveCallee2() throws SQLException {
      transitiveCallee3();

      // Also ensure we don't loop:
      transitiveCallee1();
    }

    void transitiveCallee3() throws SQLException {
      S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build(); // Noncompliant
      S3Client.builder().build(); // Noncompliant
      MachineLearningClient.builder().build(); // Noncompliant
      var customClient = new FooClient(); // Noncompliant
      DriverManager.getConnection("foo"); // Noncompliant
    }
  }

  private class FooClient implements SdkClient {
    @Override
    public String serviceName() {
      return null;
    }

    @Override
    public void close() {

    }
  }

  static Object build() {
    return null;
  }
}
