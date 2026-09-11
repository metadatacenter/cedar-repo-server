package org.metadatacenter.cedar.repo;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.EmbeddedCedarNeo4j;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceIndexResource;
import org.metadatacenter.util.test.RouteSurface;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/** Exercises all dereference routes over HTTP with a controlled resource-server boundary. */
public class RepoRoutesRespondTest {

  private static final com.sun.net.httpserver.HttpServer RESOURCE;
  private static volatile int upstreamStatus = 200;
  private static volatile String upstreamBody = "{}";
  private static volatile String forwardedAuthorization;
  private static volatile String forwardedPath;
  private static volatile String forwardedAccept;
  private static final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();

  static {
    try {
      RESOURCE = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
      RESOURCE.createContext("/", exchange -> {
        calls.incrementAndGet();
        forwardedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        forwardedAccept = exchange.getRequestHeaders().getFirst("Accept");
        forwardedPath = exchange.getRequestURI().getPath();
        byte[] body = upstreamBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("ETag", "\"17\"");
        exchange.getResponseHeaders().set("Vary", "Accept");
        exchange.sendResponseHeaders(upstreamStatus, body.length);
        try (var output = exchange.getResponseBody()) { output.write(body); }
      });
      RESOURCE.start();
    } catch (java.io.IOException e) {
      throw new ExceptionInInitializerError(e);
    }
    EmbeddedCedarNeo4j.startAndRedirectEnvironment(Map.of(
        "CEDAR_REPO_HTTP_PORT", "0",
        "CEDAR_REPO_ADMIN_PORT", "0",
        "CEDAR_REPO_STOP_PORT", "0",
        "CEDAR_RESOURCE_SERVER_HOST", "127.0.0.1",
        "CEDAR_RESOURCE_HTTP_PORT", String.valueOf(RESOURCE.getAddress().getPort()),
        "CEDAR_MONGO_HOST", "127.0.0.1",
        "CEDAR_MONGO_PORT", "1"));
  }

  private static final DropwizardTestSupport<RepoServerConfiguration> SERVER =
      new DropwizardTestSupport<>(RepoServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static String authorization;

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
    CedarConfig cedarConfig = CedarConfig.getInstance(
        CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_REPO));
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authorization = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
    RESOURCE.stop(0);
  }

  /**
   * Every repo resource class that declares endpoints, read from what the booted application
   * actually registered rather than from a list kept by hand. A resource added to
   * {@code RepoServerApplication} and left out of such a list is a route this test silently stops
   * probing — which is the regression it exists to catch. {@code IndexResource} is excluded because
   * it is deliberately unauthenticated.
   */
  private static List<Class<?>> resourceClasses() {
    ResourceConfig resourceConfig = SERVER.getEnvironment().jersey().getResourceConfig();
    List<Object> registeredComponents = new ArrayList<>();
    registeredComponents.addAll(resourceConfig.getInstances());
    registeredComponents.addAll(resourceConfig.getSingletons());
    registeredComponents.addAll(resourceConfig.getClasses());
    registeredComponents.addAll(resourceConfig.getResources());
    return RouteSurface.registeredResourceClasses(registeredComponents, "org.metadatacenter").stream()
        .filter(resourceClass -> !CedarMicroserviceIndexResource.class.isAssignableFrom(resourceClass))
        .toList();
  }

  @Test
  public void everyRouteRejectsAnUnauthenticatedRequest() {
    List<Class<?>> resources = resourceClasses();
    Assertions.assertFalse(resources.isEmpty(), "No repo resource classes found by reflection");
    int before = calls.get();
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(resources),
        401);
    Assertions.assertEquals(before, calls.get(), "Missing credentials must not reach resource");
  }

  @Test
  public void everyArtifactKindDelegatesIdentityAndPreservesRepresentation() throws Exception {
    for (String route : List.of("templates", "template-elements", "template-fields", "template-instances")) {
      upstreamStatus = 200;
      upstreamBody = "{\"@id\":\"https://example.org/artifact\",\"schema:name\":\"Readable artifact\"}";
      HttpResponse<String> response = get(route);
      Assertions.assertEquals(200, response.statusCode(), response.body());
      Assertions.assertEquals(upstreamBody, response.body());
      Assertions.assertEquals(authorization, forwardedAuthorization);
      Assertions.assertEquals("application/json", forwardedAccept);
      CedarConfig config = CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_REPO));
      CedarResourceType type = switch (route) {
        case "templates" -> CedarResourceType.TEMPLATE;
        case "template-elements" -> CedarResourceType.ELEMENT;
        case "template-fields" -> CedarResourceType.FIELD;
        default -> CedarResourceType.INSTANCE;
      };
      Assertions.assertEquals("/" + route + "/" + config.getLinkedDataUtil().getLinkedDataId(type, "fixture-id"), forwardedPath);
      Assertions.assertEquals("\"17\"", response.headers().firstValue("ETag").orElseThrow());
      Assertions.assertTrue(response.headers().allValues("Vary").stream().anyMatch(v -> v.contains("Accept")));
      Assertions.assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
      Assertions.assertFalse(JsonMapper.STRICT_MAPPER.readTree(response.body()).has("_id"));
    }
  }

  @Test
  public void denialMissingArtifactAndOutageAreNeverReplacedByLocalReads() throws Exception {
    for (String route : List.of("templates", "template-elements", "template-fields", "template-instances")) {
      for (int status : List.of(401, 403, 404, 503)) {
        upstreamStatus = status;
        upstreamBody = "{\"status\":" + status + ",\"message\":\"resource decision\"}";
        int before = calls.get();
        HttpResponse<String> response = get(route);
        Assertions.assertEquals(status, response.statusCode(), response.body());
        Assertions.assertEquals(upstreamBody, response.body());
        Assertions.assertEquals(before + 1, calls.get(), "A read must not retry or fall back");
      }
    }
  }

  private static HttpResponse<String> get(String route) throws Exception {
    return CLIENT.send(HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + "/" + route + "/fixture-id"))
        .header("Authorization", authorization)
        .GET().build(), HttpResponse.BodyHandlers.ofString());
  }
}
