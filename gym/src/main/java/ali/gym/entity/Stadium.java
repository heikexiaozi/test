package ali.gym.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("ip_stadium")
public class Stadium implements Serializable {
    private static final long serialVersionUID = 1L;
    /*
     *  场地id
     */
    @TableId(value = "id")
    private Long id;
    /*t
     * 体育馆id
     */
//    @TableId(value = "gymId")

    @TableField(value = "gymId")
    private Long gymId;
    /*场地号*/
    private Long stadiumId;

    /*
     * 场地名
     */
    private String name;
    /*
     * 早上价格
     */
    private Integer morningPrice;
    /*
     * 下午价格
     */
    private Integer afternoonPrice;
    /*
     * 晚上价格
     */
    private Integer nightPrice;
    public void generateId() {
        if (this.gymId != null && this.stadiumId != null) {
            this.id = this.gymId * 1000 + this.stadiumId; // 假设 gymId 是 5 位数，stadiumId 是 3 位数
        }
    }
}
