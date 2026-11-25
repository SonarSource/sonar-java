import com.example.missing.Service;
import com.example.missing.*;
import static com.example.missing.Helpers.help;
import java.util.List;

class Unresolved extends MissingBase implements MissingInterface {

  Service service;
  List<Missing> items;
  Missing.Nested nested;

  @MissingAnnotation(value = "x", count = 2)
  void run(Missing argument) {
    Missing local = new Missing(argument);
    local.doSomething(1, "two");
    service.call().chain().end();
    help(local);
    unknownMethod();
    int length = local.size() + items.size();
    String text = argument.name;
    Object created = new Unknown<String>() {
      void inner() {
      }
    };
    List<String> names = items.stream().map(Missing::name).toList();
    for (Missing item : items) {
      item.process();
    }
    try {
      local.risky();
    } catch (MissingException e) {
      e.handle();
    }
    Missing[] array = new Missing[length];
    Object cast = (Missing) created;
    boolean test = created instanceof Missing missing && missing.valid();
  }
}
