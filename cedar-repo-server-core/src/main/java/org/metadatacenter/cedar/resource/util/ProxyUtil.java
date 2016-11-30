package org.metadatacenter.cedar.resource.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.metadatacenter.server.security.model.AuthRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.constant.HttpConnectionConstants.CONNECTION_TIMEOUT;
import static org.metadatacenter.constant.HttpConnectionConstants.SOCKET_TIMEOUT;

public class ProxyUtil {

  public static final String ZERO_LENGTH = "0";

  public static HttpResponse proxyGet(String url, HttpServletRequest request) throws IOException {
    Request proxyRequest = Request.Get(url)
        .connectTimeout(CONNECTION_TIMEOUT)
        .socketTimeout(SOCKET_TIMEOUT);
    proxyRequest.addHeader(AUTHORIZATION, request.getHeader(AUTHORIZATION));
    return proxyRequest.execute().returnResponse();
  }

  public static HttpResponse proxyGet(String url, AuthRequest authRequest) throws IOException {
    Request proxyRequest = Request.Get(url)
        .connectTimeout(CONNECTION_TIMEOUT)
        .socketTimeout(SOCKET_TIMEOUT);
    proxyRequest.addHeader(AUTHORIZATION, authRequest.getAuthHeader());
    return proxyRequest.execute().returnResponse();
  }

}
