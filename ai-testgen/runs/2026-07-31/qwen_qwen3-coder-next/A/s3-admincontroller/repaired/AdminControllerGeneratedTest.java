package at.werkstatt.crm.gen;

import at.werkstatt.crm.controller.AdminController;
import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerGeneratedTest {

    @Mock
    private WerkstattService werkstattService;

    @Mock
    private Model model;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController();
        // Inject mock via reflection since no constructor injection is available
        Field field = ReflectionUtils.findField(AdminController.class, "werkstattService");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, adminController, werkstattService);
    }

    @Test
    void admin_getMapping_returnsAdminView_withStatistikAndFirmaName() {
        // Arrange
        Map<String, Object> statistik = new HashMap<>();
        statistik.put("totalAuftraege", 42L);
        statistik.put("offeneAuftraege", 10L);
        when(werkstattService.getAdminStatistik()).thenReturn(statistik);

        // Act
        String viewName = adminController.admin(model);

        // Assert
        assertThat(viewName).isEqualTo("admin");
        verify(model, times(1)).addAttribute("statistik", statistik);
        verify(model, times(1)).addAttribute(eq("statistikJson"), any(String.class));
        verify(model, times(1)).addAttribute(eq("firmaName"), isNull());
        verify(model, times(1)).addAttribute(eq("version"), isNull());
        verify(model, times(1)).addAttribute("meldung", null);

        // Verify JSON is valid (basic check)
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(model).addAttribute(eq("statistikJson"), jsonCaptor.capture());
        String json = jsonCaptor.getValue();
        assertThat(json).isNotBlank();
        assertThat(json).contains("\"totalAuftraege\":42");
    }

    @Test
    void bereinigen_postMapping_returnsAdminView_withMeldung() {
        // Arrange
        int anzahlGeloescht = 5;
        Map<String, Object> statistik = new HashMap<>();
        statistik.put("totalAuftraege", 37L);
        statistik.put("offeneAuftraege", 8L);

        when(werkstattService.bereinigeStornierte()).thenReturn(anzahlGeloescht);
        when(werkstattService.getAdminStatistik()).thenReturn(statistik);

        // Act
        String viewName = adminController.bereinigen(model);

        // Assert
        assertThat(viewName).isEqualTo("admin");
        verify(werkstattService, times(1)).bereinigeStornierte();
        verify(werkstattService, times(1)).getAdminStatistik();

        verify(model, times(1)).addAttribute("statistik", statistik);
        verify(model, times(1)).addAttribute(eq("statistikJson"), any(String.class));
        verify(model, times(1)).addAttribute(eq("firmaName"), isNull());
        verify(model, times(1)).addAttribute(eq("version"), isNull());

        ArgumentCaptor<String> meldungCaptor = ArgumentCaptor.forClass(String.class);
        verify(model).addAttribute(eq("meldung"), meldungCaptor.capture());
        assertThat(meldungCaptor.getValue())
            .isEqualTo(anzahlGeloescht + " stornierte Aufträge wurden endgültig gelöscht.");
    }

    @Test
    void bereinigen_withZeroDeleted_returnsCorrectMeldung() {
        // Arrange
        int anzahlGeloescht = 0;
        Map<String, Object> statistik = new HashMap<>();
        when(werkstattService.bereinigeStornierte()).thenReturn(anzahlGeloescht);
        when(werkstattService.getAdminStatistik()).thenReturn(statistik);

        // Act
        adminController.bereinigen(model);

        // Assert
        ArgumentCaptor<String> meldungCaptor = ArgumentCaptor.forClass(String.class);
        verify(model).addAttribute(eq("meldung"), meldungCaptor.capture());
        assertThat(meldungCaptor.getValue())
            .isEqualTo("0 stornierte Aufträge wurden endgültig gelöscht.");
    }

    @Test
    void bereinigen_withLargeNumber_returnsCorrectMeldung() {
        // Arrange
        int anzahlGeloescht = 12345;
        Map<String, Object> statistik = new HashMap<>();
        when(werkstattService.bereinigeStornierte()).thenReturn(anzahlGeloescht);
        when(werkstattService.getAdminStatistik()).thenReturn(statistik);

        // Act
        adminController.bereinigen(model);

        // Assert
        ArgumentCaptor<String> meldungCaptor = ArgumentCaptor.forClass(String.class);
        verify(model).addAttribute(eq("meldung"), meldungCaptor.capture());
        assertThat(meldungCaptor.getValue())
            .isEqualTo("12345 stornierte Aufträge wurden endgültig gelöscht.");
    }

    @Test
    void admin_callsGsonWithCorrectStatistik() {
        // Arrange
        Map<String, Object> statistik = new HashMap<>();
        statistik.put("key", "value");
        when(werkstattService.getAdminStatistik()).thenReturn(statistik);

        // Act
        adminController.admin(model);

        // Assert
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(model).addAttribute(eq("statistikJson"), jsonCaptor.capture());
        String json = jsonCaptor.getValue();
        assertThat(json).contains("\"key\":\"value\"");
    }

    @Test
    void admin_setsNullMeldation() {
        // Arrange
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

        // Act
        adminController.admin(model);

        // Assert
        verify(model).addAttribute("meldung", null);
    }

    @Test
    void bereinigen_setsMeldationWithCorrectFormat() {
        // Arrange
        int anzahl = 3;
        when(werkstattService.bereinigeStornierte()).thenReturn(anzahl);
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

        // Act
        adminController.bereinigen(model);

        // Assert
        ArgumentCaptor<String> meldungCaptor = ArgumentCaptor.forClass(String.class);
        verify(model).addAttribute(eq("meldung"), meldungCaptor.capture());
        String meldung = meldungCaptor.getValue();
        assertThat(meldung).matches("\\d+ stornierte Aufträge wurden endgültig gelöscht\\.");
    }

    @Test
    void admin_doesNotThrowException_whenServiceReturnsEmptyStatistik() {
        // Arrange
        Map<String, Object> emptyStatistik = new HashMap<>();
        when(werkstattService.getAdminStatistik()).thenReturn(emptyStatistik);

        // Act & Assert
        assertThatCode(() -> adminController.admin(model)).doesNotThrowAnyException();
    }

    @Test
    void bereinigen_doesNotThrowException_whenServiceReturnsEmptyStatistik() {
        // Arrange
        when(werkstattService.bereinigeStornierte()).thenReturn(0);
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

        // Act & Assert
        assertThatCode(() -> adminController.bereinigen(model)).doesNotThrowAnyException();
    }

    @Test
    void admin_callsGsonOnlyOnce() {
        // Arrange
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

        // Act
        adminController.admin(model);

        // Assert
        verify(model, times(1)).addAttribute(eq("statistikJson"), any(String.class));
    }

    @Test
    void bereinigen_callsGsonOnlyOnce() {
        // Arrange
        when(werkstattService.bereinigeStornierte()).thenReturn(0);
        when(werkstattService.getAdminStatistik()).thenReturn(new HashMap<>());

        // Act
        adminController.bereinigen(model);

        // Assert
        verify(model, times(1)).addAttribute(eq("statistikJson"), any(String.class));
    }
}
