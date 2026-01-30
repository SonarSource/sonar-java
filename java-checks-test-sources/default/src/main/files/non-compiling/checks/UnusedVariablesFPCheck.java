package checks;


public class UnusedVariablesFPCheck {
  public class DeobfuscatedUpdateManager {
    // @formatter:off
// uncommenting the following code makes the issue disappear, as the semantic is fully resolved
//    private interface DataContainer {
//      Iterable<ItemElement> getItems();
//    }
//
//    private interface ModelObject {
//      void performAction();
//    }
//
//    private interface ItemElement {
//      ModelObject getDataModel();
//    }
//
//    private static class SystemConfig {
//      static ConfigMode getMode() {
//        return ConfigMode.ENABLED;
//      }
//    }
//
//    private enum ConfigMode {
//      ENABLED,
//      DISABLED
//    }
//
//    static class A {
//      interface GenericCallback<T> { }
//    }
    // @formatter:on

    void processUpdates(
      DataContainer container
      // REMARK : the issue arises from the A.GenericCallback<ModelObject> callback that is not even used (indirect type resolution problem)
      , A.GenericCallback<X> callback
    ) {
      for (ItemElement element : container.getItems()) {
        if (SystemConfig.getMode() == ConfigMode.ENABLED) {
          ModelObject dataModel = element.getDataModel(); // Compliant - false positive was raised here, dataModel is used in the next line
          dataModel.performAction();
        }
      }
    }

  }

  static class StringConcatenation {
    // @formatter:off
// uncommenting the following code makes the issue disappear, as the semantic is fully resolved
//    private class AClass {
//      private class BClass<T> {
//        public T b;
//      }
//    }
    // @formatter:on

    public String doSomething(AClass.BClass<String> instance) {
      String c = "Hi"; // Compliant - false positive was raised here, c is used in the next line
      return instance.b + c;
    }
  }

/*
  A user reported a FP on enhanced switch statements like the one below.
  However I was not able to reproduce it in a minimal example.
  https://community.sonarsource.com/t/false-positive-for-s1854-unused-assignments-should-be-removed/114110/12

  static class EnhancedSwitch {
//    private enum DocumentStatus {
//      DOC01,
//      DOC02
//    }
//
//    private interface Document {
//      void setStatus(DocumentStatus status);
//    }
//
//    private interface Event {
//    }
//
//    private class SimpleStatusChangedEvent implements Event {
//    }
//
//    private class NeedClientRecheckEvent implements Event {
//    }
//    private interface DocumentRepository {
//      void save(Document document);
//    }

    void ko(Event event, Document document, DocumentRepository documentRepository) {
      final DocumentStatus status = switch (event) {
        case SimpleStatusChangedEvent ignored -> DocumentStatus.DOC01;
        case NeedClientRecheckEvent ignored -> DocumentStatus.DOC02;
      };
      document.setStatus(status);
      // ...
      documentRepository.save(document);
    }

  }*/
}
