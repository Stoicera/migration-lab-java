package at.werkstatt.crm.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The testbed compiles the code under test from another module's source tree, so it has to
 * duplicate that module's dependency block. Duplication without a guard is drift waiting to happen:
 * modern/pom.xml could gain a dependency and the testbed would silently measure a different
 * classpath than the app runs on.
 *
 * <p>This test fails the harness-validation run the moment the two blocks diverge. It compares
 * groupId:artifactId:version:scope; exclusions are not compared (they only ever remove things from
 * the classpath, and the testbed copies them verbatim).
 */
class PomDriftGuardTest {

  private static final Path CODE_UNDER_TEST_POM = Paths.get("..", "..", "..", "modern", "pom.xml");
  private static final Path TESTBED_POM = Paths.get("pom.xml");

  @Test
  void der_testbed_deklariert_exakt_die_dependencies_des_moduls_unter_test()
      throws IOException, ParserConfigurationException, SAXException {
    assertThat(CODE_UNDER_TEST_POM)
        .as("run from the testbed module directory (mvn -f ai-testgen/testbed/modern/pom.xml)")
        .exists();

    List<String> unterTest = directDependencies(CODE_UNDER_TEST_POM);
    List<String> imTestbed = directDependencies(TESTBED_POM);

    assertThat(unterTest).isNotEmpty();
    assertThat(imTestbed)
        .as(
            "testbed classpath drifted from the code under test — copy the dependency block of %s"
                + " verbatim into %s",
            CODE_UNDER_TEST_POM, TESTBED_POM)
        .containsAll(unterTest);
  }

  /** groupId:artifactId:version:scope of every {@code <project><dependencies><dependency>}. */
  private static List<String> directDependencies(Path pom)
      throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    Document document;
    try (var in = Files.newInputStream(pom)) {
      document = factory.newDocumentBuilder().parse(in);
    }
    List<String> coordinates = new ArrayList<>();
    NodeList blocks = document.getDocumentElement().getElementsByTagName("dependencies");
    for (int i = 0; i < blocks.getLength(); i++) {
      Node block = blocks.item(i);
      // only <project><dependencies>, never <dependencyManagement><dependencies>
      if (!"project".equals(block.getParentNode().getNodeName())) {
        continue;
      }
      NodeList dependencies = ((Element) block).getElementsByTagName("dependency");
      for (int j = 0; j < dependencies.getLength(); j++) {
        Element dependency = (Element) dependencies.item(j);
        if (!"dependencies".equals(dependency.getParentNode().getNodeName())) {
          continue; // <exclusions> children are not dependencies
        }
        if ("test".equals(text(dependency, "scope"))) {
          continue; // the test stack is the testbed's own, not the app's
        }
        coordinates.add(
            text(dependency, "groupId")
                + ":"
                + text(dependency, "artifactId")
                + ":"
                + text(dependency, "version")
                + ":"
                + text(dependency, "scope"));
      }
    }
    return coordinates;
  }

  private static String text(Element parent, String tag) {
    NodeList found = parent.getElementsByTagName(tag);
    for (int i = 0; i < found.getLength(); i++) {
      if (found.item(i).getParentNode() == parent) {
        return found.item(i).getTextContent().trim();
      }
    }
    return "";
  }
}
