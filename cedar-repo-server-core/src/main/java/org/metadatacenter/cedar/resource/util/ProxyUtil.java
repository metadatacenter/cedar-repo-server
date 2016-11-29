package org.metadatacenter.cedar.resource.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.metadatacenter.server.security.model.AuthRequest;
import play.mvc.Http;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.apache.http.HttpHeaders.*;
import static org.metadatacenter.constant.HttpConnectionConstants.CONNECTION_TIMEOUT;
import static org.metadatacenter.constant.HttpConnectionConstants.SOCKET_TIMEOUT;

public class ProxyUtil {

  public static final String ZERO_LENGTH = "0";

  public static HttpResponse proxyGet(String url, Http.Request request) throws IOException {
    Request proxyRequest = Request.Get(url)
        .connectTimeout(CONNECTION_TIMEOUT)
        .socketTimeout(SOCKET_TIMEOUT);
    proxyRequest.addHeader(AUTHORIZATION, request.getHeader(AUTHORIZATION));
    return proxyRequest.execute().returnResponse();
  }

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

  public static HttpResponse proxyDelete(String url, Http.Request request) throws IOException {
    Request proxyRequest = Request.Delete(url)
        .connectTimeout(CONNECTION_TIMEOUT)
        .socketTimeout(SOCKET_TIMEOUT);
    proxyRequest.addHeader(AUTHORIZATION, request.getHeader(AUTHORIZATION));
    proxyRequest.addHeader(CONTENT_LENGTH, ZERO_LENGTH);
    proxyRequest.addHeader(CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
    return proxyRequest.execute().returnResponse();
  }

  public static HttpResponse proxyPost(String url, Http.Request request) throws IOException {
    JsonNode jsonBody = request.body().asJson();
    return proxyPost(url, request, jsonBody.toString());
  }

  public static HttpResponse post(String url, Http.Request request, String content) throws IOException {
    return proxyPost(url, request, content);
  }

  public static HttpResponse proxyPost(String url, Http.Request request, String content) throws IOException {
    Request proxyRequest = Request.Post(url)
        .bodyString(content, ContentType.APPLICATION_JSON)
        .connectTimeout(CONNECTION_TIMEOUT)
        .socketTimeout(SOCKET_TIMEOUT);
    proxyRequest.addHeader(AUTHORIZATION, request.getHeader(AUTHORIZATION));
    return proxyRequest.execute().returnResponse();
  }

  public static HttpResponse proxyPut(String url, Http.Request request) throws IOException {
    JsonNode jsonBody = request.body().asJson();
    return proxyPut(url, request, jsonBody.toString());
  }

  public static HttpResponse proxyPut(String url, Http.Request request, String content) throws IOException {
    Request proxyRequest = Request.Put(url)
        .bodyString(content, ContentType.APPLICATION_JSON)
        .connectTimeout(CONNECTION_TIMEOUT)
        .socketTimeout(SOCKET_TIMEOUT);
    proxyRequest.addHeader(AUTHORIZATION, request.getHeader(AUTHORIZATION));
    return proxyRequest.execute().returnResponse();
  }

  public static void proxyResponseHeaders(HttpResponse proxyResponse, Http.Response response) {
    HeaderIterator headerIterator = proxyResponse.headerIterator();
    while (headerIterator.hasNext()) {
      Header header = headerIterator.nextHeader();
      if (CONTENT_TYPE.equals(header.getName())) {
        response.setHeader(header.getName(), header.getValue());
      }
    }
  }

}
