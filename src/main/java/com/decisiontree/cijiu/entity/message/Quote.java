package com.decisiontree.cijiu.entity.message;

import lombok.Data;

@Data
public class Quote {

    public String msgtype;
    public TextMessage text;
    public ImageMessage image;

}
