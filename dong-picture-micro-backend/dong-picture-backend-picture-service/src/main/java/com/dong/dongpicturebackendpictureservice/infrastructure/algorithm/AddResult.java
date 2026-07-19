package com.dong.dongpicturebackendpictureservice.infrastructure.algorithm;

import lombok.Data;

@Data
public class AddResult {
    private final String expelledKey;
    private final boolean isHotKey;
    private final String currentKey;

    public AddResult(String expelledKey, boolean isHotKey, String currentKey) {
        this.expelledKey = expelledKey;
        this.isHotKey = isHotKey;
        this.currentKey = currentKey;
    }
}
