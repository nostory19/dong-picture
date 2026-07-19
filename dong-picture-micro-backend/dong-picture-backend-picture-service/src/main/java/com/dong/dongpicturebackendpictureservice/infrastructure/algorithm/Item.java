package com.dong.dongpicturebackendpictureservice.infrastructure.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Item {
    private String key;
    private int count;

    public String key() {
        return key;
    }

    public int count() {
        return count;
    }
}
