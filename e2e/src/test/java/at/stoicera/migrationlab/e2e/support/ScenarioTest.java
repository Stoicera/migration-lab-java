package at.stoicera.migrationlab.e2e.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import at.stoicera.migrationlab.e2e.config.TestConfig;

/**
 * Base for all scenario classes. One scenario = one ordered flow in one
 * browser session against a database freshly reset to the committed seed.
 * Scenario classes therefore cannot pollute each other, whatever their order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ScreenshotOnFailure.class)
public abstract class ScenarioTest implements ScreenshotOnFailure.E2eTestBase {

	protected WebDriver driver;
	protected Waits waits;

	@BeforeAll
	void startScenario() {
		DbReset.toSeedState();
		driver = DriverFactory.newDriver();
		waits = new Waits(driver);
		driver.get(TestConfig.baseUrl() + "/");
	}

	@AfterAll
	void endScenario() {
		if (driver != null) {
			driver.quit();
		}
	}

	@Override
	public WebDriver driver() {
		return driver;
	}
}
