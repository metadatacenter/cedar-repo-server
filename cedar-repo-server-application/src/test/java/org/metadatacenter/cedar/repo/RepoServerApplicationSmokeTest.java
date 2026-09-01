package org.metadatacenter.cedar.repo;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.environment.CedarEnvironmentSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Boots the real application through the Dropwizard test support harness and exercises the wiring no
 * backend is needed for. This catches configuration and startup rot that a config-only test
 * cannot see.
 */
public class RepoServerApplicationSmokeTest {

  static {
    // Must run before the test support boots the server, which reads the port env vars.
    // OS-assigned server ports, so the test instance never collides with a running dev server.
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_REPO_HTTP_PORT", "0");
    environment.put("CEDAR_REPO_ADMIN_PORT", "0");
    environment.put("CEDAR_REPO_STOP_PORT", "0");
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

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  private HttpResponse<String> get(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .GET()
        .build();
    return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  public void indexIsServed() throws Exception {
    HttpResponse<String> response = get("/");
    Assertions.assertEquals(200, response.statusCode());
    Assertions.assertTrue(response.body().contains("name"));
  }

  /**
   * The repo server ships an API spec, so it advertises the documentation links and serves the
   * document.
   *
   * <p>It shipped none until its resource classes were annotated, and it was this test that held the
   * quiet side of the documentation gate. That side is now held by
   * {@code GroupServerApplicationSmokeTest}, whose server still has no spec.
   */
  @Test
  public void apiDocumentationIsAdvertisedAndServed() throws Exception {
    Assertions.assertTrue(get("/").body().contains("apiDocs"),
        "A service with a spec should advertise its documentation");

    HttpResponse<String> spec = get("/swagger-api/swagger.json");
    Assertions.assertEquals(200, spec.statusCode(), "The advertised spec path should serve the document");
    Assertions.assertTrue(spec.body().contains("openapi"), "The document served should be an OpenAPI spec");
  }

  @Test
  public void protectedEndpointRejectsMissingCredentials() throws Exception {
    HttpResponse<String> response = get("/templates/x");
    Assertions.assertEquals(401, response.statusCode());
  }

}
