package ali.gym.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymDTO {
    private Long id;
    private String name;
    private Integer typeId;
    private String images;
    private String address;
    private Integer sold;
    private Integer score;
    private Integer stadiumNum;
    private Double x;
    private Double y;
    private Double distance;  // 用于距离排序，不是数据库字段
}