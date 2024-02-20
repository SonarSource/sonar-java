package checks.sustainability;

import android.app.PendingIntent;
import android.content.Context;

public class AndroidFusedLocationProviderClientCheckSample {

  public void getLocationService(Context context, NoContext noContext, PendingIntent operation) {
    var service1 = context.getSystemService("location"); // Noncompliant [[sc=45;ec=55]] {{Use "FusedLocationProviderClient" instead of "LocationManager".}}
    context.getSystemService("location"); // Noncompliant

    var service2 = context.getSystemService("locale"); // Compliant
    var service3 = context.getSystemService(Context.LOCALE_SERVICE); // Compliant
    var service4 = context.getSystemService(Context.LOCATION_SERVICE); // Noncompliant

    var location = "location";
    var service5 = context.getSystemService(location); // Compliant due to current limitation in constant resolving
    var service6 = noContext.getSystemService("location"); // Compliant
  }

  private abstract static class NoContext {
    public abstract Object getSystemService(String name);
  }
}

