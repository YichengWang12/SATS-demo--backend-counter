package com.ethan.counter.bean.res;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * trade info
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TradeInfo {
    private int id;
    private long uid;
    private String code;
    private String name;
    private int direction;
    private long price;
    private long tcount;
    private String date;
    private String time;
    private int oid;
}
