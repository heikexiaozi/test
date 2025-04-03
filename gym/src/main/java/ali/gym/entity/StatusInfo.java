package ali.gym.entity;


import lombok.Data;

import java.io.Serializable;

@Data
public class StatusInfo implements Serializable {
    //场地id
    private Long id;
    //场馆id
    private Long gymId;
    //
    private Long stadiumId;
    //价格id
    private Integer Price;
    //是否预定
    private boolean isReserved;
    //开始时间
    private String startTime;
    //结束时间
    private String endTime;
}
