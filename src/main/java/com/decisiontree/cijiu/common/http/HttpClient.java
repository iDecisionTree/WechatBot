package com.decisiontree.cijiu.common.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Component
public class HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    private final RestTemplate restTemplate;

    @Value("${http.connectTimeout}")
    private int connectTimeout;

    @Value("${http.readTimeout}")
    private int readTimeout;

    @Value("${http.maxRetryTime}")
    private int maxRetryTime;

    @Value("${http.retryInterval}")
    private int retryInterval;

    public HttpClient() {
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {{
            setConnectTimeout(connectTimeout);
            setReadTimeout(readTimeout);
        }});
    }

    public String doPost(String url, Object requestBody, HttpHeaders headers) {
        HttpHeaders finalHeaders = headers == null ? new HttpHeaders() : headers;
        if (!finalHeaders.containsHeader(HttpHeaders.CONTENT_TYPE)) {
            finalHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        }

        HttpEntity<Object> entity = new HttpEntity<>(requestBody, finalHeaders);
        return executeWithRetry(url, HttpMethod.POST, entity, String.class);
    }

    public String doGet(String url, HttpHeaders headers) {
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return executeWithRetry(url, HttpMethod.GET, entity, String.class);
    }

    private <T> T executeWithRetry(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
        if (url == null || url.isEmpty()) {
            logger.error("HTTP请求地址为空");
            return null;
        }

        int retryCount = 0;
        while (retryCount <= maxRetryTime) {
            try {
                ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);
                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("HTTP请求成功: 地址:{}, 状态码:{}", url, response.getStatusCode());
                    return response.getBody();
                } else {
                    logger.warn("HTTP请求失败: 地址:{}, 状态码:{}", url, response.getStatusCode());
                }
            } catch (Exception e) {
                logger.error("HTTP请求异常: 地址:{}, 异常:{}", url, e.getMessage(), e);
            }

            retryCount++;
            if (retryCount <= maxRetryTime) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("HTTP请求重试被中断: 地址:{}", url);
                    break;
                }
                logger.info("HTTP请求重试: 地址:{}, 重试次数:{}", url, retryCount);
            }
        }

        logger.error("HTTP请求最终失败: 地址:{}, 已重试{}次", url, maxRetryTime);
        return null;
    }

    public String doPost(String url, Object requestBody) {
        return doPost(url, requestBody, null);
    }

    public String doGet(String url) {
        return doGet(url, null);
    }

    public void setTimeout(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {{
            setConnectTimeout(connectTimeout);
            setReadTimeout(readTimeout);
        }});
        logger.info("HTTP客户端超时已更新: 连接{}ms, 读取{}ms", connectTimeout, readTimeout);
    }
}