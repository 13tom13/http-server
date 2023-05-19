package ru.netology;

import org.apache.hc.client5.http.classic.HttpClient;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Array;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();

    private final ExecutorService threadPool;

    private static final int DEFAULT_PORT = 9999;

    private static final int DEFAULT_THREADPOOL_SIZE = 64;

    private static final String DEFAULT_PATH = "public";

    public static final String GET = "GET";
    public static final String POST = "POST";

    public Server(int threadPoolSize, String defaultPath) {
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    public Server(int threadPoolSize) {
        this(threadPoolSize, DEFAULT_PATH);
    }

    public Server(String defaultPath) {
        this(DEFAULT_THREADPOOL_SIZE, defaultPath);
    }

    public Server() {
        this(DEFAULT_THREADPOOL_SIZE, DEFAULT_PATH);
    }

    private void connection(Socket socket) {
        final var allowedMethods = List.of(GET, POST);
        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            Request request = new Request();
            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }

            request.setMethod(requestLine[0]);
            if (!allowedMethods.contains(request.getMethod())) {
                badRequest(out);
                return;
            }
            System.out.println("method: " + request.getMethod());


            if (!requestLine[1].startsWith("/")) {
                badRequest(out);
                return;
            }

            //возвращаем иконку
            if (requestLine[1].equals("/favicon.ico")) {
                getFavicon(out);
                return;
            }

            if (request.getMethod().equals(GET)) {
                if (requestLine[1].contains("?")) {
                    request.setQuery(requestLine[1].substring(requestLine[1].indexOf('?') + 1));
                    request.setPath(requestLine[1].substring(0, requestLine[1].indexOf('?')));
                    System.out.println("path: " + request.getPath());
                    System.out.println("query: " + request.getQuery());
                } else {
                    request.setPath(requestLine[1]);
                    System.out.println("path: " + request.getPath());
                }
            } else {
                request.setPath(requestLine[1]);
                System.out.println("path: " + request.getPath());
            }

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            request.setHeaders(Arrays.asList(new String(headersBytes).split("\r\n")));
            System.out.println("headers: " + request.getHeaders());

            // для GET тела нет
            if (!request.getMethod().equals(GET)) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = extractHeader(request.getHeaders(), "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);
                    request.setBody(new String(bodyBytes));
                    System.out.println("body: " + request.getBody());
                }
            }

            if (searchHandler(request.getMethod(), request.getPath())){
                System.out.println("handler found");
                getHandler(request.getMethod(), request.getPath()).handle(request,out);
            }else {
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected static List<String> getFilenames(String path) {
        return Stream.of(Objects.requireNonNull(new File(path).listFiles()))
                .map(File::toString)
                .map(value -> value.substring(value.lastIndexOf("\\")))
                .map(s -> s.replace("\\", "/"))
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

    private Handler getHandler(String method, String path) {
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

    private void getFavicon(BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "src/main/resources/favicon.ico");
        System.out.println(filePath);
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);
        requestOk(out, mimeType, length);
        Files.copy(filePath, out);
        out.flush();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
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

}
