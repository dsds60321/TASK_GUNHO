package dev.gunho.api.global.service;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebClientService {

    private final WebClient.Builder webClientBuilder;

    /**
     * 특정 Base URL에 대한 WebClient 생성
     */
    public WebClient getWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10))  // 응답 타임아웃
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10))  // 읽기 시간 초과
                                .addHandlerLast(new WriteTimeoutHandler(10)));

        return webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                })
                .build();

    }

    /**
     * GET 요청 처리
     * @param baseUrl Base URL
     * @param uri 요청 URI
     * @param params QueryString 파라미터
     * @param headers 요청 헤더
     * @param returnClass 반환 타입
     * @return 응답 객체
     */
    public <T> T get(String baseUrl, String uri, MultiValueMap<String, String> params, Map<String, String> headers, Class<T> returnClass) {
        try {
            return getWebClient(baseUrl)
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(uri).queryParams(params).build())
                    .headers(httpHeaders -> {
                        if (headers != null) {
                            httpHeaders.setAll(headers);
                        }
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(UTF_8)
                    .retrieve()
                    .onStatus(status ->
                            status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        log.error("[WebClientService] HTTP 요청 실패: statusCode={}, uri={}", clientResponse.statusCode(), uri);
                        return clientResponse.createException();
                    })
                    .bodyToMono(returnClass)
                    .blockOptional()
                    .orElseThrow(() -> new RuntimeException(
                            String.format("Empty response from API: %s%s", baseUrl, uri)));

        } catch (Exception e) {
            log.error("[WebClientService] GET 요청 중 오류 발생: baseUrl={}, uri={}, error={}", baseUrl, uri, e.getMessage());
            throw new RuntimeException("WebClient GET 요청 실패", e);
        }
    }


    /**
     * POST 요청 처리
     * @param baseUrl Base URL
     * @param uri 요청 URI
     * @param body 요청 Body
     * @param headers 요청 헤더
     * @param returnClass 반환 타입
     * @return 응답 객체
     */
    public <T> T post(String baseUrl, String uri, Object body, Map<String, String> headers, Class<T> returnClass) {
        try {
            return getWebClient(baseUrl)
                    .post()
                    .uri(uri)
                    .headers(httpHeaders -> {
                        if (headers != null) {
                            httpHeaders.setAll(headers);
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(UTF_8)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status ->
                            status.is4xxClientError()
                                    || status.is5xxServerError(), clientResponse -> {
                        log.error("[WebClientService] HTTP 요청 실패: statusCode={}, uri={}", clientResponse.statusCode(), uri);
                        return clientResponse.createException();
                    })
                    .bodyToMono(returnClass)
                    .blockOptional()
                    .orElse(null);
        } catch (Exception e) {
            log.error("[WebClientService] POST 요청 중 오류 발생: ", e);
            return null;
        }
    }


}
