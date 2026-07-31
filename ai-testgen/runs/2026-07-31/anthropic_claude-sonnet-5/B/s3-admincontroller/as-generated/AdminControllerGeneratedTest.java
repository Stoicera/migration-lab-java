package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.controller.AdminController;
import at.werkstatt.crm.service.WerkstattService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminControllerGeneratedTest {

  @Mock private WerkstattService werkstattService;

  private AdminController adminController;

  private static final String FIRMA_NAME = "Musterwerkstatt GmbH";
  private static final String VERSION = "1.2.3";

  @BeforeEach
  void setUp() {
    adminController = new AdminController(werkstattService, FIRMA_NAME, VERSION);
  }

  @Test
  void statistik_returnsFirmaNameVersionAndStatistikFromService() {
    Map<String, Object> serviceStatistik = new HashMap<>();
    serviceStatistik.put("anzahlKunden", 42);
    serviceStatistik.put("anzahlAuftraege", 100);
    when(werkstattService.getAdminStatistik()).thenReturn(serviceStatistik);

    Map<String, Object> result = adminController.statistik();

    assertThat(result).hasSize(3);
    assertThat(result.get("firmaName")).isEqualTo(FIRMA_NAME);
    assertThat(result.get("version")).isEqualTo(VERSION);
    assertThat(result.get("statistik")).isEqualTo(serviceStatistik);

    verify(werkstattService).getAdminStatistik();
    verifyNoMoreInteractions(werkstattService);
  }

  @Test
  void statistik_withNullStatistikFromService_returnsNullEntry() {
    when(werkstattService.getAdminStatistik()).thenReturn(null);

    Map<String, Object> result = adminController.statistik();

    assertThat(result).containsKey("statistik");
    assertThat(result.get("statistik")).isNull();
    assertThat(result.get("firmaName")).isEqualTo(FIRMA_NAME);
    assertThat(result.get("version")).isEqualTo(VERSION);

    verify(werkstattService).getAdminStatistik();
  }

  @Test
  void statistik_withEmptyStatistikMap_returnsEmptyMap() {
    Map<String, Object> emptyStatistik = new HashMap<>();
    when(werkstattService.getAdminStatistik()).thenReturn(emptyStatistik);

    Map<String, Object> result = adminController.statistik();

    assertThat(result.get("statistik")).isEqualTo(emptyStatistik);
    assertThat(((Map<?, ?>) result.get("statistik"))).isEmpty();
  }

  @Test
  void statistik_usesVersionDefaultWhenConfiguredWithDefaultPlaceholder() {
    AdminController controllerWithDefault =
        new AdminController(werkstattService, FIRMA_NAME, "?");
    when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

    Map<String, Object> result = controllerWithDefault.statistik();

    assertThat(result.get("version")).isEqualTo("?");
    assertThat(result.get("firmaName")).isEqualTo(FIRMA_NAME);
  }

  @Test
  void bereinigen_withPositiveCount_returnsGermanMeldungAndCount() {
    when(werkstattService.bereinigeStornierte()).thenReturn(5);

    Map<String, Object> result = adminController.bereinigen();

    assertThat(result).hasSize(2);
    assertThat(result.get("geloescht")).isEqualTo(5);
    assertThat(result.get("meldung")).isEqualTo("5 stornierte Aufträge wurden endgültig gelöscht.");

    verify(werkstattService).bereinigeStornierte();
    verifyNoMoreInteractions(werkstattService);
  }

  @Test
  void bereinigen_withZeroCount_returnsGermanMeldungWithZero() {
    when(werkstattService.bereinigeStornierte()).thenReturn(0);

    Map<String, Object> result = adminController.bereinigen();

    assertThat(result.get("geloescht")).isEqualTo(0);
    assertThat(result.get("meldung")).isEqualTo("0 stornierte Aufträge wurden endgültig gelöscht.");
  }

  @Test
  void bereinigen_withLargeCount_returnsCorrectMeldung() {
    when(werkstattService.bereinigeStornierte()).thenReturn(12345);

    Map<String, Object> result = adminController.bereinigen();

    assertThat(result.get("geloescht")).isEqualTo(12345);
    assertThat(result.get("meldung"))
        .isEqualTo("12345 stornierte Aufträge wurden endgültig gelöscht.");
  }

  @Test
  void bereinigen_callsServiceExactlyOnce() {
    when(werkstattService.bereinigeStornierte()).thenReturn(1);

    adminController.bereinigen();

    verify(werkstattService).bereinigeStornierte();
    verifyNoMoreInteractions(werkstattService);
  }
}
