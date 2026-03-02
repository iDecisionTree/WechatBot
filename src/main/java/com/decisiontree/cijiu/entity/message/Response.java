package com.decisiontree.cijiu.entity.message;

import lombok.Data;

@Data
public class Response {

    public String response_url;
    public String msgtype;
    public MarkdownMessage markdown;

}
