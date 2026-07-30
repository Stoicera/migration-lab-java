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
 * The B4 search endpoint additionally carries the suite's only SANCTIONED
 * divergence (ADR-0004): hostile input behaves differently per stand, so
 * those pins fork on the {@code stand} system property.
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
			"kunden-suche-hofer.json,   /api/kunden?suche=Hofer",
			"kunden-suche-meier-leer.json, /api/kunden?suche=Meier",
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

	// -----------------------------------------------------------------
	// B4 search endpoint under HOSTILE input. The one place legacy and
	// modern legally differ: stage 4 replaced the string-concatenated
	// search SQL (LEGACY_NOTES B4) with bind parameters. ADR-0004
	// (sanctioned divergence): BOTH sides' exact observed behaviour is
	// pinned; -Dstand=legacy|modern selects which expectation applies.
	// -----------------------------------------------------------------

	@Test
	@DisplayName("B4-Suche, Injektionseingabe %' OR '1'='1 — sanktionierte Divergenz (ADR-0004)")
	void sucheMitInjektionsEingabe() throws Exception {
		var response = Stand.get("/api/kunden?suche=" + urlEncode("%' OR '1'='1"));
		assertThat(response.statusCode()).isEqualTo(200);

		JsonNode body = MAPPER.readTree(response.body());
		assertThat(body.isArray()).isTrue();
		if (Stand.isModern()) {
			// bind parameter: the whole input is one literal — matches nothing
			assertThat(body.size()).as("modern: Injektionseingabe trifft nichts").isZero();
		} else {
			// concatenated SQL: OR '1'='1 disables the filter — the whole
			// customer table (10 seed rows) leaks
			assertThat(body.size()).as("legacy: Injektionseingabe leakt alle Kunden").isEqualTo(10);
		}
	}

	@Test
	@DisplayName("B4-Suche, einzelnes Hochkomma — sanktionierte Divergenz (ADR-0004)")
	void sucheMitEinzelnemHochkomma() throws Exception {
		var response = Stand.get("/api/kunden?suche=" + urlEncode("'"));
		if (Stand.isModern()) {
			// bind parameter: a lone quote is just a character nobody is named after
			assertThat(response.statusCode()).isEqualTo(200);
			JsonNode body = MAPPER.readTree(response.body());
			assertThat(body.isArray()).isTrue();
			assertThat(body.size()).isZero();
		} else {
			// broken SQL — Boot 1.5 answers with its default error JSON
			// (observed: the driver dies with ArrayIndexOutOfBoundsException)
			assertThat(response.statusCode()).isEqualTo(500);
			JsonNode body = MAPPER.readTree(response.body());
			assertThat(body.path("status").asInt()).isEqualTo(500);
			assertThat(body.path("error").asText()).isEqualTo("Internal Server Error");
			assertThat(body.path("exception").asText()).isEqualTo("java.lang.ArrayIndexOutOfBoundsException");
			assertThat(body.path("path").asText()).isEqualTo("/api/kunden");
		}
	}

	private static String urlEncode(String value) {
		return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
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
