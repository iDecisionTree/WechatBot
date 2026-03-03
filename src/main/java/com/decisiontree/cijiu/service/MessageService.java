package com.decisiontree.cijiu.service;

import com.decisiontree.cijiu.common.aes.AesException;
import com.decisiontree.cijiu.common.aes.WXBizJsonMsgCrypt;
import com.decisiontree.cijiu.common.http.HttpClient;
import com.decisiontree.cijiu.common.result.Result;
import com.decisiontree.cijiu.common.webhook.WebhookClient;
import com.decisiontree.cijiu.entity.message.PushMessage;
import com.decisiontree.cijiu.entity.message.ResponseRequest;
import com.decisiontree.cijiu.entity.message.Response;
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

    @Value("${wechat.push.webhook.url}")
    private String pushUrl;

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
        logger.info("企业微信接收消息: msg_signature:{},timestamp:{},nonce:{},request:{}", msgSignature, timestamp, nonce, request);
        try {
            WXBizJsonMsgCrypt wxBizJsonMsgCrypt = new WXBizJsonMsgCrypt(receiveToken, receiveEncodingAESKey, "");
            String msg = wxBizJsonMsgCrypt.DecryptMsg(msgSignature, timestamp, nonce, request);
            logger.info("企业微信接收消息解密: {}", msg);

            new Thread(() -> {
                try {
                    webhookClient.sendWebhookMessage(msg);
                } catch (Exception e) {
                    logger.error("Webhook推送失败: 异常:{}", e.getMessage(), e);
                }
            }).start();

            return "success";
        } catch (AesException e) {
            logger.error("企业微信接收消息失败: 错误:{}", e.getMessage(), e);
            return "error";
        }
    }

    public void response(ResponseRequest request) {
        Response response = new Response();
        response.msgtype = request.getMsgtype();
        response.markdown = request.getMarkdown();

        Result result = httpClient.doPost(request.response_url, response);
        if (result.errcode != 0) {
            logger.error("企业微信回复消息失败: 错误代码:{},错误信息:{}", result.errcode, result.errmsg);
        }
    }

    public void push(PushMessage request) {
        Result result = httpClient.doPost(pushUrl, request);
        if (result.errcode != 0) {
            logger.error("企业微信推送消息失败: 错误代码:{},错误信息:{}", result.errcode, result.errmsg);
        }
    }

}
