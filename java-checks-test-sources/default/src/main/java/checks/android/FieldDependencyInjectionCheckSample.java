package checks.android;

import android.app.Service;
import android.view.View;
import javax.inject.Inject;
import android.app.Activity;
import android.app.Fragment;

public class FieldDependencyInjectionCheckSample {

  class NormalClass {
    @Inject // Noncompliant {{Remove this field injection and use constructor injection instead.}}
    private String injected;
  }

  class SomeActivity extends Activity {
    @Inject // Compliant : Activities are managed by the framework, one cannot use constructor injection
    private String injected;
  }

  class SomeFragment extends Fragment {
    @Inject // Compliant : Fragments are managed by the framework, one cannot use constructor injection
    private String injected;
  }

  class SomeView extends View {
    @Inject // Compliant : Views are managed by the framework, one cannot use constructor injection
    private String injected;
  }

  class SomeService extends Service {
    @Inject // Compliant : Services are managed by the framework, one cannot use constructor injection
    private String injected;
  }
}
