package checks.tests;

import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TooManyAssertionsCheckCustom25 {

  private static final String GITLAB_AUTH_URL = "";
  private static final String GITLAB_AUTH_ENABLED = "";
  private static final String GITLAB_AUTH_APPLICATION_ID = "";
  private static final String GITLAB_AUTH_SECRET = "";
  private static final String GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP = "";
  private static final String GITLAB_AUTH_SYNC_USER_GROUPS = "";

  @Test
  void test1() { // Compliant
    assertEquals(1, g(1));
    assertEquals(2, g(2));
    assertEquals(3, g(2));
    assertEquals(4, g(2));
    assertEquals(5, g(2));
    assertEquals(6, g(2));
    assertEquals(7, g(1));
    assertEquals(8, g(1));
    assertEquals(9, g(1));
    assertEquals(10, g(1));
    assertEquals(11, g(1));
    assertEquals(12, g(1));
    assertEquals(13, g(1));
    assertEquals(14, g(1));
    assertEquals(15, g(1));
    assertEquals(16, g(1));
    assertEquals(17, g(1));
    assertEquals(18, g(1));
    assertEquals(19, g(1));
    assertEquals(20, g(1));
    assertEquals(21, g(1));
    assertEquals(22, g(1));
    assertEquals(23, g(1));
    assertEquals(24, g(1));
    assertEquals(25, g(1));
  }

  @Test
  void test2() { // Noncompliant {{Refactor this method to reduce the number of assertions from 26 to less than 25.}}
//     ^^^^^
    assertEquals(101, g(1));
    assertEquals(102, g(2));
    assertEquals(103, g(3));
    assertEquals(104, g(4));
    assertEquals(105, g(5));
    assertEquals(106, g(6));
    assertEquals(107, g(7));
    assertEquals(108, g(8));
    assertEquals(109, g(9));
    assertEquals(1010, g(10));
    assertEquals(1011, g(11));
    assertEquals(1012, g(12));
    assertEquals(1013, g(113));
    assertEquals(1014, g(14));
    assertEquals(1015, g(15));
    assertEquals(1016, g(16));
    assertEquals(1017, g(17));
    assertEquals(1018, g(18));
    assertEquals(1019, g(19));
    assertEquals(1020, g(20));
    assertEquals(1021, g(21));
    assertEquals(1022, g(22));
    assertEquals(1023, g(23));
    assertEquals(1024, g(24));
    assertEquals(1025, g(25));
    assertEquals(1026, g(26));
  }

  int g(int x) {
    return x + 100;
  }

  @Test
  void starngeTest() { //Compliant
    Config config = new Config();
    Settings settings = new Settings();

    assertThat(config.url()).isEqualTo("https://gitlab.com");
    settings.setProperty(GITLAB_AUTH_URL, "https://gitlab.com/api/");
    assertThat(config.url()).isEqualTo("https://gitlab.com/api");
    settings.setProperty(GITLAB_AUTH_URL, "https://gitlab.com/api");
    assertThat(config.url()).isEqualTo("https://gitlab.com/api");
    assertThat(config.isEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_ENABLED, "true");
    assertThat(config.isEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_APPLICATION_ID, "1234");
    assertThat(config.isEnabled()).isFalse().doesNotHave(null);
    settings.setProperty(GITLAB_AUTH_SECRET, "5678");
    assertThat(config.isEnabled()).isTrue();
    assertThat(config.applicationId()).isEqualTo("1234");
    assertThat(config.secret()).isEqualTo("5678");
    assertThat(config.allowUsersToSignUp()).isTrue();
    settings.setProperty(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "false");
    assertThat(config.allowUsersToSignUp()).isFalse();
    assertThat(config.syncUserGroups()).isFalse();
    settings.setProperty(GITLAB_AUTH_SYNC_USER_GROUPS, true);

    assertThat(URI.create(""))
      .hasFragment("1")
      .hasFragment("2")
      .hasFragment("3")
      .hasHost("");
  }

  private static class Config {
    public String url() {
      return "";
    }

    public boolean isEnabled() {
      return false;
    }

    public String applicationId() {
      return null;
    }

    public String secret() {
      return null;
    }

    public boolean allowUsersToSignUp() {
      return false;
    }

    public boolean syncUserGroups() {
      return false;
    }
  }

  private static class Settings {
    private void setProperty(String k, Object v) {
    }
  }

}
