package at.werkstatt.crm.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.controller.AdminController;
import at.werkstatt.crm.service.WerkstattService;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

/**
 * Harness validation, stratum shape S3 (mixed tech: JSP view name + gson serialization inside the
 * controller). See WerkstattServiceSmokeTest.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerSmokeTest {

  @Mock private WerkstattService werkstattService;

  @InjectMocks private AdminController controller;

  @Test
  void admin_fuellt_das_model_und_zeigt_auf_die_jsp() {
    ReflectionTestUtils.setField(controller, "firmaName", "KFZ Moser GmbH");
    Map<String, Object> statistik = Collections.<String, Object>singletonMap("kunden", 10);
    when(werkstattService.getAdminStatistik()).thenReturn(statistik);
    Model model = new ExtendedModelMap();

    String viewName = controller.admin(model);

    assertThat(viewName).isEqualTo("admin");
    assertThat(model.asMap()).containsEntry("firmaName", "KFZ Moser GmbH");
    assertThat(model.asMap().get("statistikJson")).isEqualTo("{\"kunden\":10}");
  }
}
