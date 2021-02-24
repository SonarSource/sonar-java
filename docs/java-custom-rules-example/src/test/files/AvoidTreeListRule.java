import org.apache.commons.collections4.list.TreeList;
import java.util.ArrayList;

class A {
  void foo() {
    TreeList myList = new TreeList(new ArrayList<>()); // Noncompliant {{Avoid using TreeList}}
    // Noncompliant@+1
    MyList myOtherList = new MyList(); // as MyList extends the TreeList, we expect an issue here
  }
}

class MyList extends TreeList {

}
