package ru.netology;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 9999;

    private static final int THREAD_COUNT = 64;

    private static final ExecutorService THREADPOOL = Executors.newFixedThreadPool(THREAD_COUNT);

    public static void main(String[] args) {
        Server server = new Server(PORT, THREADPOOL);
        server.start();
    }
}


