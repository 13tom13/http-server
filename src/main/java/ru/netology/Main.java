package ru.netology;

import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class Main {


    public static void main(String[] args) {
        int port = 9999;
        Server server = new Server();
        server.addHandler("GET", "/messages/test.html", (request, responseStream) -> {
            System.out.println();
            System.out.println("from handler");
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            System.out.println("path from request: " + request.getPath());
            Path filePath = Path.of(".", request.getPath().replace("\\","/"));
            System.out.println("path from handler: " + filePath);
            System.out.println();
            Files.copy(filePath, responseStream);
            responseStream.flush();
        });
        server.addHandler("POST", "/messages", (request, responseStream) -> {

        });
        server.start(port);
    }
}


