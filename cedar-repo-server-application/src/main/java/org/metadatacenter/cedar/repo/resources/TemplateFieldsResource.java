package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.artifact.SchemaArtifactDocument;
import org.metadatacenter.util.http.CedarError;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;

@Path("/template-fields")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Template fields")
@SecurityRequirement(name = "api_key")
public class TemplateFieldsResource extends AbstractRepoResource {

  public TemplateFieldsResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  @GET
  @Timed
  @Path("/{id}")
  @Operation(summary = "Resolve a template field identifier",
      description = "Dereference the bare identifier ending an artifact's @id. The resource server "
          + "enforces the caller's read permission and workspace access and returns the stored JSON. "
          + "Repo preserves its response body, status and representation headers.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The stored template field",
          content = @Content(schema = @Schema(implementation = SchemaArtifactDocument.class))),
      @ApiResponse(responseCode = "401", description = "No valid credentials",
          content = @Content(schema = @Schema(implementation = CedarError.class))),
      @ApiResponse(responseCode = "403", description = "The caller lacks read permission or access to this artifact",
          content = @Content(schema = @Schema(implementation = CedarError.class))),
      @ApiResponse(responseCode = "404", description = "No such artifact",
          content = @Content(schema = @Schema(implementation = CedarError.class))),
      @ApiResponse(responseCode = "503", description = "A required downstream service is unavailable",
          content = @Content(schema = @Schema(implementation = CedarError.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error",
          content = @Content(schema = @Schema(implementation = CedarError.class)))
  })
  public Response findTemplateField(
      @Parameter(description = "The bare identifier ending the artifact's IRI.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    return resolveArtifact(id, CedarResourceType.FIELD);
  }
}
