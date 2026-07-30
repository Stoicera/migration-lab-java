package at.stoicera.migrationlab.e2e.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Every failure leaves a screenshot in target/screenshots — flaky analysis
 * starts with evidence, not with re-running and hoping.
 */
public class ScreenshotOnFailure implements TestWatcher {

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		context.getTestInstance().ifPresent(instance -> {
			if (instance instanceof E2eTestBase base && base.driver() instanceof TakesScreenshot shooter) {
				try {
					Path dir = Path.of("target", "screenshots");
					Files.createDirectories(dir);
					String name = context.getRequiredTestClass().getSimpleName() + "_"
							+ context.getRequiredTestMethod().getName() + "_"
							+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".png";
					Files.write(dir.resolve(name), shooter.getScreenshotAs(OutputType.BYTES));
					System.err.println("[e2e] screenshot saved: target/screenshots/" + name);
				} catch (Exception e) {
					System.err.println("[e2e] screenshot failed: " + e);
				}
			}
		});
	}

	public interface E2eTestBase {
		WebDriver driver();
	}
}
