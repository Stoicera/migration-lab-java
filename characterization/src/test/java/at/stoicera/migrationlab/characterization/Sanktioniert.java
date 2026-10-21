package at.stoicera.migrationlab.characterization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test whose expectation forks on {@link Stand#STAND} because the modern stand deliberately
 * behaves differently (ADR-0004 rule 2). The marker carries no behaviour; it tells {@link
 * NachweisBericht} which register entry sanctions the fork, so the equivalence report can say
 * "sanktionierte Abweichung" with a source instead of "identisch". A fork without this marker is a
 * review finding.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Sanktioniert {

  /** Register id, e.g. {@code SD-1}, or the ADR that sanctions the change. */
  String value();

  /** Repository path of the register entry. */
  String quelle();
}
