package com.decisiontree.cijiu.service;

import com.decisiontree.cijiu.common.aes.AesException;
import com.decisiontree.cijiu.common.aes.WXBizJsonMsgCrypt;
import com.decisiontree.cijiu.common.http.HttpClient;
import com.decisiontree.cijiu.common.webhook.WebhookClient;
import com.decisiontree.cijiu.entity.message.Response;
import com.decisiontree.cijiu.entity.message.ResponseContent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    public static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Resource
    private WebhookClient webhookClient;

    @Resource
    private HttpClient httpClient;

    @Value("${wechat.corpId}")
    private String corpId;

    @Value("${wechat.robot.message.receive.token}")
    private String receiveToken;

    @Value("${wechat.robot.message.receive.encodingAESKey}")
    private String receiveEncodingAESKey;

    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echostr) {
        logger.info("企业微信校验URL: msg_signature:{},timestamp:{},nonce:{},echostr:{}", msgSignature, timestamp, nonce, echostr);

        String result = null;
        try {
            WXBizJsonMsgCrypt wxBizJsonMsgCrypt = new WXBizJsonMsgCrypt(receiveToken, receiveEncodingAESKey, "");
            result = wxBizJsonMsgCrypt.VerifyURL(msgSignature, timestamp, nonce, echostr);
        } catch (AesException e) {
            logger.error("企业微信校验URL失败: 异常原因:{},异常代码:{}", e.getMessage(), e.getCode());
        }

        return result;
    }

    public String receive(String msgSignature, String timestamp, String nonce, String request) {
        logger.info("企业微信推送消息: msg_signature:{},timestamp:{},nonce:{},request:{}", msgSignature, timestamp, nonce, request);
        try {
            WXBizJsonMsgCrypt wxBizJsonMsgCrypt = new WXBizJsonMsgCrypt(receiveToken, receiveEncodingAESKey, "");
            String msg = wxBizJsonMsgCrypt.DecryptMsg(msgSignature, timestamp, nonce, request);
            logger.info("企业微信推送消息解密: {}", msg);

            new Thread(() -> {
                try {
                    webhookClient.sendWebhookMessage(msg);
                } catch (Exception e) {
                    logger.error("企业微信消息推送Webhook推送失败: 异常:{}", e.getMessage(), e);
                }
            }).start();

            return "success";
        } catch (AesException e) {
            logger.error("企业微信推送消息失败: 异常原因:{},异常代码:{}", e.getMessage(), e.getCode());
            return "error";
        }
    }

    public void response(Response request) {
        ResponseContent responseContent = new ResponseContent();
        responseContent.msgtype = request.getMsgtype();
        responseContent.markdown = request.getMarkdown();

        String msg = httpClient.doPost(request.response_url, responseContent);
    }

}
