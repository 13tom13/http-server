package ru.netology;

import java.nio.file.Files;


public class Main {


    public static void main(String[] args) {
        int port = 9999;
        Server server = new Server();
        server.addHandler("GET", "/messages", (request, responseStream) -> {
            request.setFilePath("messages/test.html");
            responseStream.write(request.getHeaders().getBytes());
            Files.copy(request.getFilePath(), responseStream);
            responseStream.flush();
        });
        server.addHandler("POST", "/messages", (request, responseStream) -> {
            System.out.println("POST request received");
            request.setFilePath("messages/post.html");
            responseStream.write(request.getHeaders().getBytes());
            Files.copy(request.getFilePath(), responseStream);
            responseStream.flush();
        });
        server.start(port);
    }
}


