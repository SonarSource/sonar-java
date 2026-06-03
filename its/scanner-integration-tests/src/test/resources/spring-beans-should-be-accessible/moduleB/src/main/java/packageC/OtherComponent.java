package packageC;

import org.springframework.stereotype.Component;
import packageA.SpringApp;

@Component
public class OtherComponent {
  public static final String NAME = SpringApp.APP_NAME;
}
