package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.google.gson.Gson;

import at.werkstatt.crm.controller.AdminController;
import at.werkstatt.crm.service.WerkstattService;

@ExtendWith(MockitoExtension.class)
class AdminControllerGeneratedTest {

	@Mock
	private WerkstattService werkstattService;

	private AdminController adminController;

	private final Gson gson = new Gson();

	private static final String FIRMA_NAME = "Test Werkstatt GmbH";
	private static final String VERSION = "1.2.3";

	@BeforeEach
	void setUp() {
		adminController = new AdminController();
		ReflectionTestUtils.setField(adminController, "werkstattService", werkstattService);
		ReflectionTestUtils.setField(adminController, "firmaName", FIRMA_NAME);
		ReflectionTestUtils.setField(adminController, "version", VERSION);
	}

	// ---------------------------------------------------------------
	// admin()
	// ---------------------------------------------------------------

	@Test
	void admin_happyPath_populatesModelAndReturnsAdminView() {
		Map<String, Object> statistik = new LinkedHashMap<>();
		statistik.put("anzahlKunden", 42);
		statistik.put("anzahlAuftraege", 7);
		when(werkstattService.getAdminStatistik()).thenReturn(statistik);

		Model model = new ExtendedModelMap();

		String view = adminController.admin(model);

		assertThat(view).isEqualTo("admin");
		assertThat(model.asMap()).containsEntry("statistik", statistik);
		assertThat(model.asMap()).containsEntry("statistikJson", gson.toJson(statistik));
		assertThat(model.asMap()).containsEntry("firmaName", FIRMA_NAME);
		assertThat(model.asMap()).containsEntry("version", VERSION);
		assertThat(model.asMap()).containsKey("meldung");
		assertThat(model.asMap().get("meldung")).isNull();

		verify(werkstattService).getAdminStatistik();
	}

	@Test
	void admin_withEmptyStatistik_stillPopulatesModelCorrectly() {
		Map<String, Object> statistik = new HashMap<>();
		when(werkstattService.getAdminStatistik()).thenReturn(statistik);

		Model model = new ExtendedModelMap();

		String view = adminController.admin(model);

		assertThat(view).isEqualTo("admin");
		assertThat(model.asMap()).containsEntry("statistik", statistik);
		assertThat(model.asMap()).containsEntry("statistikJson", "{}");
		assertThat(model.asMap()).containsEntry("firmaName", FIRMA_NAME);
		assertThat(model.asMap()).containsEntry("version", VERSION);
	}

	@Test
	void admin_withNullStatistik_producesNullJson() {
		when(werkstattService.getAdminStatistik()).thenReturn(null);

		Model model = new ExtendedModelMap();

		String view = adminController.admin(model);

		assertThat(view).isEqualTo("admin");
		assertThat(model.asMap()).containsEntry("statistik", null);
		assertThat(model.asMap()).containsEntry("statistikJson", "null");
	}

	// ---------------------------------------------------------------
	// bereinigen()
	// ---------------------------------------------------------------

	@Test
	void bereinigen_happyPath_withDeletedEntries_setsMeldungAndReturnsAdminView() {
		Map<String, Object> statistik = new LinkedHashMap<>();
		statistik.put("anzahlKunden", 5);
		when(werkstattService.bereinigeStornierte()).thenReturn(3);
		when(werkstattService.getAdminStatistik()).thenReturn(statistik);

		Model model = new ExtendedModelMap();

		String view = adminController.bereinigen(model);

		assertThat(view).isEqualTo("admin");
		assertThat(model.asMap()).containsEntry("statistik", statistik);
		assertThat(model.asMap()).containsEntry("statistikJson", gson.toJson(statistik));
		assertThat(model.asMap()).containsEntry("firmaName", FIRMA_NAME);
		assertThat(model.asMap()).containsEntry("version", VERSION);
		assertThat(model.asMap()).containsEntry("meldung", "3 stornierte Aufträge wurden endgültig gelöscht.");

		verify(werkstattService).bereinigeStornierte();
		verify(werkstattService).getAdminStatistik();
	}

	@Test
	void bereinigen_withZeroDeletedEntries_setsMeldungWithZero() {
		Map<String, Object> statistik = new HashMap<>();
		when(werkstattService.bereinigeStornierte()).thenReturn(0);
		when(werkstattService.getAdminStatistik()).thenReturn(statistik);

		Model model = new ExtendedModelMap();

		String view = adminController.bereinigen(model);

		assertThat(view).isEqualTo("admin");
		assertThat(model.asMap()).containsEntry("meldung", "0 stornierte Aufträge wurden endgültig gelöscht.");
	}

	@Test
	void bereinigen_withNegativeAnzahl_stillFormatsMeldung() {
		// Defensive edge case: even an unexpected negative value must be reflected verbatim.
		Map<String, Object> statistik = new HashMap<>();
		when(werkstattService.bereinigeStornierte()).thenReturn(-1);
		when(werkstattService.getAdminStatistik()).thenReturn(statistik);

		Model model = new ExtendedModelMap();

		String view = adminController.bereinigen(model);

		assertThat(view).isEqualTo("admin");
		assertThat(model.asMap()).containsEntry("meldung", "-1 stornierte Aufträge wurden endgültig gelöscht.");
	}
}
