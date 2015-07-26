import java.util.List;
import java.lang.*;

class Example<T> {
  List<String> list;
  Example(List<String> list) {
    this.list = list;
  }
  int method() {
    label:
    return 42;
  }
  enum MyEnumWithLongName {
    MY_ENUM;
    MyEnumWithLongName(){
    }
  }
}
