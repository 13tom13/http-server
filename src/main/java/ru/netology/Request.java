package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Request {

    private String method;

    private String path;

    private List<String> headers;

    private String body;

    private String query;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<NameValuePair> getQueryParams() {
        return  URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
    }

    public List<String> getQueryParam(String name){
        final var params =  getQueryParams();
        List<String> paramValue = new ArrayList<>();
        for (final var param : params){
            if (param.getName().equals(name)) paramValue.add(param.getValue());
        }
        return paramValue;
    }

}
