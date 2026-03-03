package com.decisiontree.cijiu.entity.message;

import lombok.Data;

@Data
public class PushMessage {

    public String msgtype;
    public TextMessage text;
    public MarkdownMessage markdown;
    public ImageMessage image;

}
