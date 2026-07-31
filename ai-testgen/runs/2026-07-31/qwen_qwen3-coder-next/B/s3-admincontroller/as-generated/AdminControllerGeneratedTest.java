package at.werkstatt.crm.gen;

import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerGeneratedTest {

    @Mock
    private WerkstattService werkstattService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(werkstattService, "TestFirma", "1.2.3");
    }

    @Test
    void statistik_returnsExpectedStructure_whenCalled() {
        // Arrange
        Map<String, Object> expectedStatistik = new HashMap<>();
        expectedStatistik.put("kunden", 10);
        expectedStatistik.put("fahrzeuge", 15);
        expectedStatistik.put("auftraege", 20);
        when(werkstattService.getAdminStatistik()).thenReturn(expectedStatistik);

        // Act
        Map<String, Object> result = adminController.statistik();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).containsEntry("firmaName", "TestFirma");
        assertThat(result).containsEntry("version", "1.2.3");
        assertThat(result).containsEntry("statistik", expectedStatistik);
    }

    @Test
    void statistik_returnsEmptyStatistik_whenServiceReturnsEmptyMap() {
        // Arrange
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

        // Act
        Map<String, Object> result = adminController.statistik();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).containsEntry("firmaName", "TestFirma");
        assertThat(result).containsEntry("version", "1.2.3");
        assertThat(result).containsEntry("statistik", new HashMap<>());
    }

    @Test
    void bereinigen_returnsCorrectCountAndMessage_whenDeletionsSuccess() {
        // Arrange
        int deletedCount = 5;
        when(werkstattService.bereinigeStornierte()).thenReturn(deletedCount);

        // Act
        Map<String, Object> result = adminController.bereinigen();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("geloescht", deletedCount);
        assertThat(result).containsEntry("meldung", deletedCount + " stornierte Aufträge wurden endgültig gelöscht.");
    }

    @Test
    void bereinigen_handlesZeroDeletionsCorrectly() {
        // Arrange
        int deletedCount = 0;
        when(werkstattService.bereinigeStornierte()).thenReturn(deletedCount);

        // Act
        Map<String, Object> result = adminController.bereinigen();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("geloescht", deletedCount);
        assertThat(result).containsEntry("meldung", deletedCount + " stornierte Aufträge wurden endgültig gelöscht.");
    }

    @Test
    void bereinigen_handlesLargeDeletionCount() {
        // Arrange
        int deletedCount = 1000;
        when(werkstattService.bereinigeStornierte()).thenReturn(deletedCount);

        // Act
        Map<String, Object> result = adminController.bereinigen();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("geloescht", deletedCount);
        assertThat(result).containsEntry("meldung", deletedCount + " stornierte Aufträge wurden endgültig gelöscht.");
    }

    @Test
    void constructor_injectsAllDependenciesCorrectly() {
        // Arrange & Act
        AdminController controller = new AdminController(werkstattService, "FirmaX", "2.0");

        // Assert
        assertThat(controller).isNotNull();
        // Note: We cannot directly access private fields, but we can verify behavior via public methods
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());
        Map<String, Object> stat = controller.statistik();
        assertThat(stat).containsEntry("firmaName", "FirmaX");
        assertThat(stat).containsEntry("version", "2.0");
    }
}
