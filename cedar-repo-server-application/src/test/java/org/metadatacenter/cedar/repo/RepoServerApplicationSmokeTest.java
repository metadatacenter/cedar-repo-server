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
   * The repo server ships no API spec, so it neither advertises documentation nor serves any.
   *
   * <p>Both used to happen regardless: the asset bundle was registered from shared library code
   * whether or not the service had a document, and the index resource advertised swagger.json and
   * the Swagger UI from the root of every service. On the ten that ship no spec, a caller followed
   * either link to a 404.
   */
  @Test
  public void noApiDocumentationIsAdvertisedOrServed() throws Exception {
    Assertions.assertFalse(get("/").body().contains("apiDocs"),
        "A service with no spec should advertise no documentation links");
    Assertions.assertEquals(404, get("/swagger-api/swagger.json").statusCode(),
        "A service with no spec should serve nothing at the spec path");
  }

  @Test
  public void protectedEndpointRejectsMissingCredentials() throws Exception {
    HttpResponse<String> response = get("/templates/x");
    Assertions.assertEquals(401, response.statusCode());
  }

}
