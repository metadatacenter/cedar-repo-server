package org.metadatacenter.cedar.repo.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarDependencyUnavailableException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.http.ProxyUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

/** Stable identifier adapter. Resource owns authorization; artifact owns the stored representation. */
public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  private static final List<String> REPRESENTATION_HEADERS = List.of(
      HttpHeaders.CONTENT_TYPE, HttpHeaders.ETAG, HttpHeaders.VARY);

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  protected Response resolveArtifact(String id, CedarResourceType type) throws CedarException {
    CedarRequestContext context = buildRequestContext();
    context.must(context.user()).be(LoggedIn);
    String artifactId = linkedDataUtil.getLinkedDataId(type, id);
    String url = microserviceUrlUtil.getResource().getArtifactTypeWithId(type, artifactId, Optional.empty());
    // Keep dereferencing JSON-only. Do not forward arbitrary query parameters or caller-selected
    // downstream hosts, and never fall back to Mongo when resource denies or cannot answer a read.
    try (ClassicHttpResponse upstream = ProxyUtil.proxyGet(url, context,
        Map.of(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))) {
      Response.ResponseBuilder result = Response.status(upstream.getCode());
      for (Header header : upstream.getHeaders()) {
        if (REPRESENTATION_HEADERS.stream().anyMatch(name -> name.equalsIgnoreCase(header.getName()))) {
          result.header(header.getName(), header.getValue());
        }
      }
      if (upstream.getEntity() != null) {
        result.entity(EntityUtils.toByteArray(upstream.getEntity()));
      }
      return result.build();
    } catch (IOException e) {
      throw new CedarDependencyUnavailableException("Downstream service is unavailable", e);
    }
  }
}
