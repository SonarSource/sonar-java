package checks.spring;

import javax.persistence.Entity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

class PersistentEntityUsedAsRequestParameterCheckExcludedSample {
  @Entity
  class Campaign {
  }

  @Entity
  class UserDetail {
  }

  @Entity
  class Other {
  }

  @interface PartnerCampaign {
  }

  @Controller
  class SomeController {
    @GetMapping("/a")
    public void annotated(@PartnerCampaign Campaign campaign) { // Compliant, annotation declared
    }

    @GetMapping("/b")
    public void typed(Other other, UserDetail userDetail) { // Noncompliant
    }

    @GetMapping("/c")
    public void notDeclared(Campaign campaign) { // Noncompliant
    }
  }
}
