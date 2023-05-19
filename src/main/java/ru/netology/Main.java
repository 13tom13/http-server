package ru.netology;

import java.nio.file.Files;
import java.nio.file.Path;


public class Main {


    public static void main(String[] args) {
        int port = 9999;
        Server server = new Server();
        server.addHandler("GET", "/messages/test.html", (request, responseStream) -> {
            server.getFile(responseStream, request);
        });
        server.addHandler("POST", "/messages/post.html", (request, responseStream) -> {
            server.getFile(responseStream, request);
        });
//        server.addHandler("POST", "/testForMulti", (request, responseStream) -> {
//            try {
//                server.testForMulti(request);
//            } catch (FileUploadException e) {
//                throw new RuntimeException(e);
//            }
//
//        });
        server.addHandler("GET", "/get-test", (request, responseStream) -> {
            final var filePath = Path.of(".", "messages/get-test.html");
            final var mimeType = Files.probeContentType(filePath);
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{value}",
                    server.getQueryParam("value", request).get(0)
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
                    server.getPostParam("value", request).get(0)
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


