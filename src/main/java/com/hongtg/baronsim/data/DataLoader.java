package com.hongtg.baronsim.data;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * src/main/resources/data 아래의 JSON 파일을 읽어 record로 바꿔 준다.
 */
public final class DataLoader {

    private static final Gson GSON = new Gson();

    private DataLoader() {
        // 정적 메서드만 있는 클래스라 객체를 만들지 않는다
    }

    /** data/baron.json을 읽는다. */
    public static BaronData loadBaron() {
        return load("/data/baron.json", BaronData.class);
    }

    /** data/heimerdinger.json을 읽는다. */
    public static HeimerData loadHeimer() {
        return load("/data/heimerdinger.json", HeimerData.class);
    }

    /**
     * 클래스패스의 JSON 리소스를 읽어 type 객체로 만든다.
     * 디스크 경로가 아니라 리소스 경로라서 드라이브 글자와 상관없이 동작한다.
     */
    static <T> T load(String resourcePath, Class<T> type) {
        try (InputStream in = DataLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("리소스를 찾을 수 없습니다: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                T result = GSON.fromJson(reader, type);
                if (result == null) {
                    throw new IllegalStateException("비어 있는 JSON입니다: " + resourcePath);
                }
                return result;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("JSON을 읽지 못했습니다: " + resourcePath, e);
        }
    }
}
