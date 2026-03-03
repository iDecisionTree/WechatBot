package com.decisiontree.cijiu.entity.message;

import lombok.Data;

@Data
public class ReceiveMessage {

    public String msgid;
    public String aibotid;
    public String chatid;
    public String chattype;
    public User from;
    public String msgType;
    public String response_url;
    public TextMessage text;
    public Quote quote;

}
