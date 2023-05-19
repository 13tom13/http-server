package ru.netology;

import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class Main {


    public static void main(String[] args) {
        int port = 9999;
        Server server = new Server();
        server.addHandler("GET", "/messages", (request, responseStream) -> {

        });
        server.addHandler("POST", "/messages", (request, responseStream) -> {

        });
        server.start(port);
    }
}


