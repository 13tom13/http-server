package ru.netology;

import java.nio.file.Files;
import java.nio.file.Path;


public class Main {


    public static void main(String[] args) {
        int port = 9999;
        Server server = new Server();
        server.addHandler("GET", "/messages/test.html", Request::getFile);
        server.addHandler("POST", "/messages/post.html", Request::getFile);
        server.addHandler("GET", "/get-test", (request, responseStream) -> {
            final var filePath = Path.of(".", "messages/get-test.html");
            final var mimeType = Files.probeContentType(filePath);
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{value}",
                    request.getQueryParam("value").get(0)
            ).getBytes();
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(content);
            responseStream.flush();
        });
        server.addHandler("POST", "/post-test", (request, responseStream) -> {
            final var filePath = Path.of(".", "messages/post-test.html");
            final var mimeType = Files.probeContentType(filePath);
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{value}",
                    request.getPostParam("value").get(0)
            ).getBytes();
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(content);
            responseStream.flush();
        });
        server.start(port);
    }
}


