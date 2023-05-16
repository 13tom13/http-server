package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {
    private final List<String> defaultFilenames;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();

    private final ExecutorService threadPool;

    private static final int DEFAULT_PORT = 9999;

    private static final int DEFAULT_THREADPOOL_SIZE = 64;

    private static final String DEFAULT_PATH = "public";

    public Server(int threadPoolSize, String defaultPath) {
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.defaultFilenames = getFilenames(defaultPath);
    }

    public Server(int threadPoolSize) {
        this (threadPoolSize, DEFAULT_PATH);
    }

    public Server(String defaultPath) {
        this (DEFAULT_THREADPOOL_SIZE, defaultPath);
    }

    public Server (){
        this (DEFAULT_THREADPOOL_SIZE, DEFAULT_PATH);
    }



    protected static List<String> getFilenames(String path){
        return Stream.of(Objects.requireNonNull(new File(path).listFiles()))
                .map(File :: toString)
                .map(value -> value.substring(value.lastIndexOf("\\")))
                .map(s -> s.replace("\\","/"))
                .collect(Collectors.toList());
    }

    public void addHandler(String method, String path, Handler handler) {

        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);
        } else {
            handlers.put(method, new ConcurrentHashMap<>());
            handlers.get(method).put(path, handler);
        }
    }

    private boolean searchHandler(String method, String path) {
        return handlers.containsKey(method) && handlers.get(method).containsKey(path);
    }

    private Handler getHandler (String method, String path){
        return handlers.get(method).get(path);
    }

    private void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void requestOk(BufferedOutputStream out, String mimeType, long length) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
    }

    private void defaultRequest(String path, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", DEFAULT_PATH, path);
        System.out.println(filePath);
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            requestOk(out, mimeType, content.length);
            out.write(content);
            out.flush();
        }
        requestOk(out, mimeType, length);
        Files.copy(filePath, out);
        out.flush();
    }

    private void getFavicon (BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "src/main/resources/favicon.ico");
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        requestOk(out, mimeType, length);
        Files.copy(filePath, out);
        out.flush();
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("start server...");
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> connection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        this.start(DEFAULT_PORT);
    }

    private void connection(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1

            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            String method = parts[0];
            String path = parts[1];

            if (path.equals("/favicon.ico")){
                getFavicon(out);
                return;
            }

            if (defaultFilenames.contains(path)) {
                defaultRequest(path, out);
            } else if (searchHandler(method,path)){
               getHandler(method,path).handle(new Request(method,path, in),out);
            } else {
                badRequest(out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
