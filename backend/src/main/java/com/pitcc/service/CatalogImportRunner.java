package com.pitcc.service;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "catalog.import.enabled",
        havingValue = "true"
)
public class CatalogImportRunner implements ApplicationRunner {

  private static final Logger log =
          LoggerFactory.getLogger(CatalogImportRunner.class);

  private final CatalogImportService service;
  private final String game;
  private final String query;
  private final String mode;

  public CatalogImportRunner(
          CatalogImportService service,
          @Value("${catalog.import.game:}") String game,
          @Value("${catalog.import.query:}") String query,
          @Value("${catalog.import.mode:query}") String mode) {

    this.service = service;
    this.game = game;
    this.query = query;
    this.mode = mode;
  }

  @Override
  public void run(ApplicationArguments args) {
    CatalogImportReport report =
            switch (mode.trim().toLowerCase(Locale.ROOT)) {
              case "query" -> service.importByQuery(game, query);
              case "all" -> service.importAll(game);
              default -> throw new IllegalArgumentException(
                      "Modo de importação inválido. Use query ou all."
              );
            };

    log.info(
            "IMPORT_REPORT source={} mapped={} persisted={} "
                    + "notAttempted={} aborted={} issues={}",
            report.source(),
            report.mapped(),
            report.persisted(),
            report.notAttempted(),
            report.aborted(),
            report.issues().size()
    );

    report.issues().forEach(issue -> log.warn(
            "IMPORT_ISSUE source={} externalId={} stage={} reason={}",
            issue.source(),
            issue.externalId(),
            issue.stage(),
            issue.message()
    ));

    if (!report.successful()) {
      throw new IllegalStateException(
              "Importação terminou com falhas. "
                      + "Consulte IMPORT_REPORT e IMPORT_ISSUE."
      );
    }
  }
}