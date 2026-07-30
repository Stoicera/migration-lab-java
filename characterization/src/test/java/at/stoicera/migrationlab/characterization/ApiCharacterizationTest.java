package at.stoicera.migrationlab.characterization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Golden-master captures of every read endpoint against the seeded stand.
 * Comparison is on parsed JSON trees (key order and whitespace irrelevant,
 * values exact). A mismatch means: the migration changed observable
 * behaviour — which is exactly what these tests exist to catch.
 *
 * On failure the received document is written to
 * target/characterization-received/ for diffing against src/test/resources/golden/.
 */
@DisplayName("API-Charakterisierung: Lese-Endpunkte gegen Golden Master")
class ApiCharacterizationTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@BeforeAll
	static void resetDatabase() {
		Stand.resetToSeed();
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({
			"kunden.json,               /api/kunden",
			"kunde-1.json,              /api/kunden/1",
			"kunde-1-fahrzeuge.json,    /api/kunden/1/fahrzeuge",
			"fahrzeuge.json,            /api/fahrzeuge",
			"auftraege.json,            /api/auftraege",
			"auftraege-fertig.json,     /api/auftraege?status=FERTIG",
			"auftrag-5.json,            /api/auftraege/5",
			"rechnungen.json,           /api/rechnungen",
			"rechnung-4.json,           /api/rechnungen/4",
			"bericht-monat-2026.json,   /api/bericht/monat?jahr=2026",
			"bericht-topkunden-2026.json, /api/bericht/topkunden?jahr=2026"
	})
	void endpointMatchesGolden(String goldenFile, String endpoint) throws Exception {
		var response = Stand.get(endpoint);
		assertThat(response.statusCode()).as("HTTP status of " + endpoint).isEqualTo(200);

		JsonNode received = MAPPER.readTree(response.body());
		JsonNode golden = readGolden(goldenFile);

		if (!received.equals(golden)) {
			Path out = Path.of("target", "characterization-received", goldenFile);
			Files.createDirectories(out.getParent());
			Files.writeString(out, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(received));
			assertThat(received)
					.as(endpoint + " differs from golden/" + goldenFile
							+ " — received copy written to " + out)
					.isEqualTo(golden);
		}
	}

	@Test
	@DisplayName("JSP-Adminseite (HTML, Datum maskiert)")
	void adminPageMatchesGolden() throws Exception {
		var response = Stand.get("/admin");
		assertThat(response.statusCode()).isEqualTo(200);

		String normalized = normalizeAdminHtml(response.body());
		String golden;
		try (InputStream in = getClass().getResourceAsStream("/golden/admin.html")) {
			assertThat(in).as("golden/admin.html missing").isNotNull();
			golden = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		if (!normalized.equals(golden)) {
			Path out = Path.of("target", "characterization-received", "admin.html");
			Files.createDirectories(out.getParent());
			Files.writeString(out, normalized);
		}
		assertThat(normalized).isEqualTo(golden);
	}

	static String normalizeAdminHtml(String html) {
		// the JSP renders the current date — the only volatile value on the page
		return html.replaceAll("\\d{2}\\.\\d{2}\\.\\d{4}", "XX.XX.XXXX");
	}

	private JsonNode readGolden(String name) throws Exception {
		try (InputStream in = getClass().getResourceAsStream("/golden/" + name)) {
			assertThat(in).as("golden file missing: " + name
					+ " — capture it per characterization/README.md").isNotNull();
			return MAPPER.readTree(in);
		}
	}
}
