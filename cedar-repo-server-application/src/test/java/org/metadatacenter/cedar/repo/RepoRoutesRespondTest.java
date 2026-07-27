package org.metadatacenter.cedar.repo;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.repo.resources.TemplateElementsResource;
import org.metadatacenter.cedar.repo.resources.TemplateFieldsResource;
import org.metadatacenter.cedar.repo.resources.TemplateInstancesResource;
import org.metadatacenter.cedar.repo.resources.TemplatesResource;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.util.test.RouteSurface;

import java.util.HashMap;
import java.util.Map;

/**
 * Route safety net: probes every endpoint the four repo artifact resources declare,
 * unauthenticated, and requires each to answer 401. A 404/405 means the route vanished or changed
 * verb; any other status means an endpoint lost its authentication assertion. No fixtures and no
 * backend are involved.
 */
public class RepoRoutesRespondTest {

  static {
    // Must run before the test support boots the server, which reads the port env vars. Ports are
    // distinct from the dev server and from every other booting test class.
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_REPO_HTTP_PORT", "19023");
    environment.put("CEDAR_REPO_ADMIN_PORT", "19123");
    environment.put("CEDAR_REPO_STOP_PORT", "19223");
    CedarEnvironmentSource.setOverride(environment);
  }

  private static final DropwizardTestSupport<RepoServerConfiguration> SERVER =
      new DropwizardTestSupport<>(RepoServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  @Test
  public void everyRouteRejectsAnUnauthenticatedRequest() {
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(
            TemplatesResource.class,
            TemplateElementsResource.class,
            TemplateFieldsResource.class,
            TemplateInstancesResource.class),
        401);
  }

}
