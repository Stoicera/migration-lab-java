package at.stoicera.migrationlab.aitestgen;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates the repository root, so every path in the harness can stay repo-relative. */
public final class Repo {

  private Repo() {}

  /** Walks up from the working directory to the directory holding {@code stages.md}. */
  public static Path root() {
    Path candidate = Path.of("").toAbsolutePath();
    while (candidate != null && !Files.isRegularFile(candidate.resolve("stages.md"))) {
      candidate = candidate.getParent();
    }
    if (candidate == null) {
      throw new IllegalStateException("run this from inside the migration-lab repository");
    }
    return candidate;
  }

  public static Path protocol() {
    return root().resolve("ai-testgen/PROTOCOL.md");
  }
}
