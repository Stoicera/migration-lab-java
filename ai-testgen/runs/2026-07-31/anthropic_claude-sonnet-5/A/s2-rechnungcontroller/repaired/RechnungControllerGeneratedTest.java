package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import at.werkstatt.crm.controller.RechnungController;
import at.werkstatt.crm.model.Rechnung;
import at.werkstatt.crm.service.WerkstattService;

@ExtendWith(MockitoExtension.class)
class RechnungControllerGeneratedTest {

	@Mock
	private WerkstattService werkstattService;

	private RechnungController controller;

	@BeforeEach
	void setUp() {
		controller = new RechnungController();
		ReflectionTestUtils.setField(controller, "werkstattService", werkstattService);
	}

	@Test
	void liste_returnsAllRechnungen() {
		Rechnung r1 = new Rechnung();
		r1.setId(1L);
		Rechnung r2 = new Rechnung();
		r2.setId(2L);
		List<Rechnung> rechnungen = Arrays.asList(r1, r2);
		when(werkstattService.getAlleRechnungen()).thenReturn(rechnungen);

		List<Rechnung> result = controller.liste();

		assertThat(result).hasSize(2).containsExactly(r1, r2);
		verify(werkstattService, times(1)).getAlleRechnungen();
	}

	@Test
	void liste_returnsEmptyList() {
		when(werkstattService.getAlleRechnungen()).thenReturn(Collections.emptyList());

		List<Rechnung> result = controller.liste();

		assertThat(result).isEmpty();
	}

	@Test
	void einzeln_found_returnsOk() {
		Rechnung r = new Rechnung();
		r.setId(5L);
		when(werkstattService.getRechnung(5L)).thenReturn(r);

		ResponseEntity<Rechnung> response = controller.einzeln(5L);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(r);
	}

	@Test
	void einzeln_notFound_returnsNotFound() {
		when(werkstattService.getRechnung(99L)).thenReturn(null);

		ResponseEntity<Rechnung> response = controller.einzeln(99L);

		assertThat(response.getStatusCodeValue()).isEqualTo(404);
		assertThat(response.getBody()).isNull();
	}

	@Test
	void erstellen_success_returnsOk() {
		Rechnung r = new Rechnung();
		r.setId(10L);
		r.setAuftragId(20L);
		when(werkstattService.erstelleRechnung(20L)).thenReturn(r);

		ResponseEntity<?> response = controller.erstellen(20L);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(r);
	}

	@Test
	void erstellen_exception_returns500WithMessage() {
		when(werkstattService.erstelleRechnung(anyLong())).thenThrow(new RuntimeException("Fehler beim Erstellen"));

		ResponseEntity<?> response = controller.erstellen(30L);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Fehler beim Erstellen");
	}

	@Test
	void bezahlt_success_returnsOk() {
		Rechnung r = new Rechnung();
		r.setId(40L);
		r.setBezahlt(true);
		when(werkstattService.setzeBezahlt(40L)).thenReturn(r);

		ResponseEntity<?> response = controller.bezahlt(40L);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(r);
	}

	@Test
	void bezahlt_exception_returns500WithMessage() {
		when(werkstattService.setzeBezahlt(anyLong())).thenThrow(new RuntimeException("Rechnung nicht gefunden"));

		ResponseEntity<?> response = controller.bezahlt(50L);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Rechnung nicht gefunden");
	}

	@Test
	void bezahlt_exceptionWithNullMessage_returns500WithNullBody() {
		when(werkstattService.setzeBezahlt(anyLong())).thenThrow(new RuntimeException());

		ResponseEntity<?> response = controller.bezahlt(60L);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isNull();
	}
}
