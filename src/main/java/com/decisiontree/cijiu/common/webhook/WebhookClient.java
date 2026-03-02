package com.decisiontree.cijiu.common.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.pattern.PathPattern;

import java.util.List;

@Component
public class WebhookClient {

    private static final Logger logger = LoggerFactory.getLogger(WebhookClient.class);
    private final RestTemplate restTemplate;

    @Value("${webhook.urls}")
    private List<String> urls;

    @Value("${webhook.connectTimeout}")
    private int connectTimeout;

    @Value("${webhook.readTimeout}")
    private int readTimeout;

    @Value("${webhook.maxRetryTime}")
    private int maxRetryTime;

    @Value("${webhook.retryInterval}")
    private int retryInterval;

    public WebhookClient() {
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {{
            setConnectTimeout(connectTimeout);
            setReadTimeout(readTimeout);
        }});
    }

    public void sendWebhookMessage(Object message) {
        if (urls == null || urls.isEmpty()) {
            logger.warn("未配置Webhook, 跳过消息推送");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(message, headers);

        for (String url : urls) {
            sendWithRetry(url, entity);
        }
    }

    private void sendWithRetry(String url, HttpEntity<Object> entity) {
        int retryCount = 0;
        while (retryCount <= maxRetryTime) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Webhook推送成功: 地址:{}", url);
                    return;
                } else {
                    logger.warn("Webhook推送失败: 地址:{},状态码:{}", url, response.getStatusCode());
                }
            } catch (Exception e) {
                logger.error("Webhook推送失败: 地址:{},异常:{}", url, e.getMessage(), e);
            }

            retryCount++;
            if (retryCount <= maxRetryTime) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                logger.info("Webhook消息推送重试: 地址:{},重试次数:{}", url, retryCount);
            }
        }

        logger.error("Webhook消息推送最终失败: 地址:{},已重试{}次", url, maxRetryTime);
    }

}
