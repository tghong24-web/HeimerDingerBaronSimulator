package com.hongtg.baronsim.ui;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.scene.image.Image;

/**
 * 아이콘 불러오기 (9-5단계).
 *
 * 스킬·아이템·하이머 아이콘은 라이엇 Data Dragon(ddragon.leagueoflegends.com)에서 받아
 * 사용자 폴더(~/.heimer-baron-sim/icons/<패치 버전>/)에 저장해 두고, 다음부터는 저장본을 쓴다.
 * 아이템은 items.json의 한글 이름으로 Data Dragon의 ko_KR 아이템 목록에서 찾는다 (id를 따로 적지 않아도 된다).
 * 인터넷이 안 되거나 이름을 못 찾으면 콜백을 부르지 않는다 → 화면에는 글자 자리표시가 남는다.
 *
 * 둥지 배경·바론 토큰은 프로젝트 리소스(resources/images/)에서 읽는다. 없으면 null → 도형으로 그린다.
 *
 * 네트워크는 백그라운드 스레드에서 하고, 그림은 Platform.runLater로 JavaFX 스레드에 넘긴다
 * (C++ Qt의 QMetaObject::invokeMethod(…, Qt::QueuedConnection)과 같은 역할).
 */
final class Icons {

    private static final String DDRAGON = "https://ddragon.leagueoflegends.com";
    private static final Path CACHE_ROOT = Path.of(System.getProperty("user.home"), ".heimer-baron-sim", "icons");

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "icon-loader");
        t.setDaemon(true);   // 창을 닫으면 같이 끝나게
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** 버전과 이름표(스킬·아이템 파일 이름)는 한 번만 받는다 */
    private static CompletableFuture<Catalog> catalog;

    private record Catalog(String version, String[] spellFiles, Map<String, String> itemFileByName) {
    }

    private Icons() {
    }

    // ── 공개 메서드 ──

    /** 하이머 스킬 아이콘. key는 'Q', 'W', 'E', 'R' */
    static void spell(char key, Consumer<Image> onLoad) {
        int index = "QWER".indexOf(key);
        catalog().thenAccept(c -> {
            if (c != null && index >= 0 && c.spellFiles()[index] != null) {
                fetchImage(c.version(), "spell", c.spellFiles()[index], onLoad);
            }
        });
    }

    /** 하이머딩거 챔피언 아이콘 */
    static void champion(Consumer<Image> onLoad) {
        catalog().thenAccept(c -> {
            if (c != null) {
                fetchImage(c.version(), "champion", "Heimerdinger.png", onLoad);
            }
        });
    }

    /** 아이템 아이콘. items.json의 한글 이름으로 찾는다 */
    static void item(String koreanName, Consumer<Image> onLoad) {
        catalog().thenAccept(c -> {
            if (c == null) {
                return;
            }
            String file = c.itemFileByName().get(normalize(koreanName));
            if (file != null) {
                fetchImage(c.version(), "item", file, onLoad);
            }
        });
    }

    /**
     * resources/images/ 아래 그림을 가로 width 픽셀로 줄여서 읽는다 (원본이 커도 메모리를 덜 쓰게). 없으면 null
     */
    static Image resource(String fileName, double width) {
        URL url = Icons.class.getResource("/images/" + fileName);
        return url == null ? null : new Image(url.toExternalForm(), width, 0, true, true);
    }

    // ── 내부 ──

    private static synchronized CompletableFuture<Catalog> catalog() {
        if (catalog == null) {
            catalog = CompletableFuture.supplyAsync(Icons::loadCatalog, POOL);
        }
        return catalog;
    }

    /** 최신 버전 → 하이머 챔피언 정보(스킬 파일 이름) → 아이템 목록. 실패하면 null */
    private static Catalog loadCatalog() {
        try {
            String version = latestVersion();
            JsonObject champ = JsonParser.parseString(text(version, "champion.json",
                    DDRAGON + "/cdn/" + version + "/data/ko_KR/champion/Heimerdinger.json"))
                    .getAsJsonObject().getAsJsonObject("data").getAsJsonObject("Heimerdinger");
            JsonArray spells = champ.getAsJsonArray("spells");
            String[] spellFiles = new String[4];
            for (int i = 0; i < 4 && i < spells.size(); i++) {
                spellFiles[i] = spells.get(i).getAsJsonObject().getAsJsonObject("image").get("full").getAsString();
            }

            JsonObject items = JsonParser.parseString(text(version, "item.json",
                    DDRAGON + "/cdn/" + version + "/data/ko_KR/item.json"))
                    .getAsJsonObject().getAsJsonObject("data");
            Map<String, String> byName = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : items.entrySet()) {
                JsonObject it = e.getValue().getAsJsonObject();
                String name = normalize(it.get("name").getAsString());
                boolean onRift = it.has("maps") && it.getAsJsonObject("maps").has("11")
                        && it.getAsJsonObject("maps").get("11").getAsBoolean();
                // 같은 이름이 여러 개(아레나용 등)면 소환사의 협곡(맵 11) 것을 우선
                if (onRift || !byName.containsKey(name)) {
                    byName.put(name, it.getAsJsonObject("image").get("full").getAsString());
                }
            }
            return new Catalog(version, spellFiles, byName);
        } catch (Exception e) {
            System.err.println("[아이콘] Data Dragon을 읽지 못해 자리표시를 씁니다: " + e.getMessage());
            return null;
        }
    }

    /** 인터넷에서 최신 버전을 받고, 안 되면 저장해 둔 버전 중 가장 최근 것 */
    private static String latestVersion() throws IOException, InterruptedException {
        try {
            String json = get(DDRAGON + "/api/versions.json");
            return JsonParser.parseString(json).getAsJsonArray().get(0).getAsString();
        } catch (IOException e) {
            if (Files.isDirectory(CACHE_ROOT)) {
                try (Stream<Path> dirs = Files.list(CACHE_ROOT)) {
                    return dirs.filter(Files::isDirectory)
                            .map(p -> p.getFileName().toString())
                            .max(Icons::compareVersion)
                            .orElseThrow(() -> e);
                }
            }
            throw e;
        }
    }

    /** "14.10.1"과 "14.9.1"을 숫자로 비교 */
    private static int compareVersion(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? parseOrZero(x[i]) : 0;
            int q = i < y.length ? parseOrZero(y[i]) : 0;
            if (p != q) {
                return Integer.compare(p, q);
            }
        }
        return 0;
    }

    private static int parseOrZero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 저장본이 있으면 그것을, 없으면 받아서 저장 */
    private static String text(String version, String cacheName, String url) throws IOException, InterruptedException {
        Path file = CACHE_ROOT.resolve(version).resolve(cacheName);
        if (Files.exists(file)) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        String body = get(url);
        Files.createDirectories(file.getParent());
        Files.writeString(file, body, StandardCharsets.UTF_8);
        return body;
    }

    private static void fetchImage(String version, String kind, String fileName, Consumer<Image> onLoad) {
        POOL.submit(() -> {
            try {
                Path file = CACHE_ROOT.resolve(version).resolve(kind).resolve(fileName);
                if (!Files.exists(file)) {
                    byte[] bytes = getBytes(DDRAGON + "/cdn/" + version + "/img/" + kind + "/" + fileName);
                    Files.createDirectories(file.getParent());
                    Files.write(file, bytes);
                }
                Image image = new Image(file.toUri().toString());
                if (!image.isError()) {
                    Platform.runLater(() -> onLoad.accept(image));
                }
            } catch (Exception e) {
                System.err.println("[아이콘] " + fileName + " 실패: " + e.getMessage());
            }
        });
    }

    private static String get(String url) throws IOException, InterruptedException {
        return new String(getBytes(url), StandardCharsets.UTF_8);
    }

    private static byte[] getBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("HTTP " + res.statusCode() + " " + url);
        }
        return res.body();
    }

    /** 띄어쓰기와 '·' 차이로 못 찾는 일을 줄인다 */
    private static String normalize(String name) {
        return name.replace(" ", "").replace("·", "").replace("'", "").trim();
    }
}
