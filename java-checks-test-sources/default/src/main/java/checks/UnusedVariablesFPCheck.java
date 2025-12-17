package com;


public class UnusedVariablesFPCheck {
  public class DeobfuscatedUpdateManager {
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

    /**
     * Deobfuscated names with AI from :
     * ```java
     * package com;
     * <p>
     * public class BCid51 {
     * void HJid232(EJid229 YHid199, Yid24.RIid217<MBid37> QJid241) {
     * for (XFid148 RJid242 : YHid199.WIid222()) {
     * int DHid178;
     * if (Fid5.BGid151() == CGid152.DGid153) {
     * MBid37 IHid183 = RJid242.OFid139();
     * IHid183.AGid150();
     * }
     * }
     * }
     * }
     * ```
     */
    void processUpdates(
      DataContainer container
      // REMARK : the issue arises from the A.GenericCallback<ModelObject> callback that is not even used (indirect type resolution problem)
      , A.GenericCallback<X> callback
    ) {
      for (ItemElement element : container.getItems()) {
        if (SystemConfig.getMode() == ConfigMode.ENABLED) {
          ModelObject dataModel = element.getDataModel();
          dataModel.performAction();
        }
      }
    }

  }

  static class StringConcatenation {
//    private class AClass {
//      private class BClass<T> {
//        public T b;
//      }
//    }

    public String doSomething(AClass.BClass<String> instance) {
      String c = "Hi"; // Rule S1854
      return instance.b + c;
    }
  }

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

  }

  class Obvious {
//    void obvious() {
//      int i = 0; // doesn't raise issue
//      i = 1; // raises issue
//    }
  }
}
