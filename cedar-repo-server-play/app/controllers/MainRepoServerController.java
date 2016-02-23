package controllers;

import org.metadatacenter.server.play.AbstractCedarController;
import play.mvc.Result;

public class MainRepoServerController extends AbstractRepoServerController {

  public static Result index() {
    return ok("CEDAR Repo Server.");
  }

  /* For CORS */
  public static Result preflight(String all) {
    return AbstractCedarController.preflight(all);
  }

}
