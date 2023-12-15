package com.ethan.counter.bean.res;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * position info
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PosiInfo {

    private int id;
    private long uid;
    private String code;
    private String name;
    private long cost;
    private long count;

}
