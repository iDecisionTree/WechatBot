package com.decisiontree.cijiu.controller;

import com.decisiontree.cijiu.entity.message.PushMessage;
import com.decisiontree.cijiu.entity.message.ResponseRequest;
import com.decisiontree.cijiu.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping("/receive")
    public String verifyUrl(@RequestParam(name = "msg_signature") String msgSignature, @RequestParam(name = "timestamp") String timestamp, @RequestParam(name = "nonce") String nonce, @RequestParam(name = "echostr") String echostr) {
        return messageService.verifyUrl(msgSignature, timestamp, nonce, echostr);
    }

    @PostMapping("/receive")
    public String receive(@RequestParam(name = "msg_signature") String msgSignature, @RequestParam(name = "timestamp") String timestamp, @RequestParam(name = "nonce") String nonce, @RequestBody String request) {
        return messageService.receive(msgSignature, timestamp, nonce, request);
    }

    @PostMapping("/response")
    public void response(@RequestBody ResponseRequest request) {
        messageService.response(request);
    }

    @PostMapping("/push")
    public void push(@RequestBody PushMessage request) {
        messageService.push(request);
    }

}
