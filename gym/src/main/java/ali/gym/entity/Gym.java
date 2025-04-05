package ali.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@TableName("ip_gym")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gym implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     *  体育馆id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /*
     * 体育馆名称
     */
    private String name;

    /*
     * 体育馆简介
     */
    private String description;

    /*
     * 体育馆类型id
     */
    private Integer typeId;

    /*
     * 体育馆展示图片，不同图片之间使用,连接
     */
    private String images;

    /*
     * 体育馆地址
     */
    private String address;

    /*
     * 体育馆每月预定购买场馆销量
     */
//    private Integer sold;

    /*
     * 评分
     */
    private Integer score;
    private Integer sold;
    /*
     * 拥有的场馆数量
     */
    private Integer stadiumNum;

    /*
     * 早上营业时间
     */
    private String morningOpenHours;

    /*
     * 下午营业时间
     */
    private String afternoonOpenHours;

    /*
     * 晚上营业时间
     */
    private String nightOpenHours;

    /*
     * 经度
     */
    private Double x;

    /*
     * 纬度
     */
    private Double y;

    /*
     * 创建时间
     */

    @TableField(exist = false)
    private Integer distance;
    /*
     * 更新时间
     */



}