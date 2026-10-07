package org.metadatacenter.cedar.repo.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarDependencyUnavailableException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.http.ProxyUtil;
import org.metadatacenter.util.http.ResponseRelay;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

/** Stable identifier adapter. Resource owns authorization; artifact owns the stored representation. */
public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  protected Response resolveArtifact(String id, CedarResourceType type) throws CedarException {
    CedarRequestContext context = buildRequestContext();
    context.must(context.user()).be(LoggedIn);
    String artifactId = linkedDataUtil.resolveResourceId(type, id.contains("/") ? id : linkedDataUtil.getLinkedDataId(type, id));
    String url = microserviceUrlUtil.getResource().getArtifactTypeWithId(type, artifactId, Optional.empty());
    // Keep dereferencing JSON-only. Do not forward arbitrary query parameters or caller-selected
    // downstream hosts, and never fall back to Mongo when resource denies or cannot answer a read.
    try (ClassicHttpResponse upstream = ProxyUtil.proxyGet(url, context,
        Map.of(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))) {
      return ResponseRelay.responseBuilder(upstream).build();
    } catch (IOException e) {
      throw new CedarDependencyUnavailableException("Downstream service is unavailable", e);
    }
  }
}
