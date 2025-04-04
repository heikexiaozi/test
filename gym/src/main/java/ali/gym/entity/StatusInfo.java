package ali.gym.entity;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public  class StatusInfo implements Serializable {
    //场地id
    private Long id;
    //场馆id
    private Long gymId;
    //场地号
    private Long stadiumId;
    //价格
    private Integer Price;
    //是否预定
    private boolean isReserved;
    //日期
    private Date ReservedDate;
    //开始时间
    private String startTime;
    //结束时间
    private String endTime;
}