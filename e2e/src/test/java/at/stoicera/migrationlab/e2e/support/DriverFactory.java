package at.stoicera.migrationlab.e2e.support;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * One headless Chrome per scenario class. No implicit waits — ever. All synchronisation is explicit
 * (see Waits); mixing implicit and explicit waits is the classic flakiness source.
 */
public final class DriverFactory {

  private DriverFactory() {}

  public static WebDriver newDriver() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    options.addArguments("--window-size=1600,1000");
    options.addArguments("--disable-gpu");
    if (System.getenv("CI") != null) {
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
    }
    WebDriver driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    return driver;
  }
}
