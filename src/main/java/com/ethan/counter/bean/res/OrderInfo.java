package com.ethan.counter.bean.res;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * order info
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class OrderInfo {
    private int id;
    private long uid;
    private String code;
    private String name;
    private int direction;
    private int type;
    private long price;
    private long count;
    private int status;
    private String date;
    private String time;
}
