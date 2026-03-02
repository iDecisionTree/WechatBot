package com.decisiontree.cijiu.entity.message;

import lombok.Data;

@Data
public class ResponseContent {

    public String msgtype;
    public MarkdownMessage markdown;

}
