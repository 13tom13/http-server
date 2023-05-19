package ru.netology;

import java.nio.file.Path;
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

    public Path getFilePath() {
        return Path.of(".", path);
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






}
