package ru.netology;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Request implements RequestContext {

    private String method;

    private String path;

    private String characterEncoding = String.valueOf(StandardCharsets.UTF_8);

    private List<String> headers;

    private String contentType;

    private int contentLength = 0;

    private String body;

    private String query;

    private final InputStream in;

    public Request(InputStream in) {
        this.in = in;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

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

    public List<NameValuePair> getQueryParams() {
        return URLEncodedUtils.parse(this.getQuery(), StandardCharsets.UTF_8);
    }

    public List<String> getQueryParam(String name) {
        final var params = getQueryParams();
        List<String> paramValue = new ArrayList<>();
        for (final var param : params) {
            if (param.getName().equals(name)) paramValue.add(param.getValue());
        }
        return paramValue;
    }

    public List<NameValuePair> getPostParams() {
        return URLEncodedUtils.parse(this.getBody(), StandardCharsets.UTF_8);
    }

    public List<String> getPostParam(String name) {
        final var params = getPostParams();
        List<String> paramValue = new ArrayList<>();
        for (final var param : params) {
            if (param.getName().equals(name)) paramValue.add(param.getValue());
        }
        return paramValue;
    }

    public void getFile(BufferedOutputStream out) throws IOException {
        final var filePath = this.getFilePath();
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }


    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    public List<FileItem> getParts() throws FileUploadException {
        // Создание фактории для элементов файла, сохраняемых на диск:
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(100);
        factory.setRepository(new File("temp"));
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Парсинг запроса:
        return upload.parseRequest(this);
    }

    public FileItem getPart(String name) throws FileUploadException {
        for (FileItem item : this.getParts()) {
            if (item.getFieldName().equals(name)) return item;
        }
        return null;
    }
}
