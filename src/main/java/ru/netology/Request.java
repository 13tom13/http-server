package ru.netology;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Request {

   private final String method;

    private Path filePath;

    private final BufferedReader body;

    public void setFilePath(String path) {
        this.filePath = Path.of(".", path);
    }

    public String getMethod() {
        return method;
    }

    public Path getFilePath() {
        return filePath;
    }

    public BufferedReader getBody() {
        return body;
    }

    public String getHeaders() throws IOException {
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    public Request(String method, String path, BufferedReader in){
        this.body = in;
        this.method = method;
        this.filePath = Path.of(".", path);
    }




}
