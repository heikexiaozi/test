package ali.gym.api;
import ali.gym.entity.Gym;
import ali.gym.entity.GymDTO;
import ali.gym.entity.Result;
import ali.gym.entity.Stadium;
import ali.gym.service.GymServiceImpl;
import ali.gym.service.StadiumServiceImpl;
import ali.gym.service.StatusInfoServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
@RestController
public class api {
    @Autowired
    GymServiceImpl gymService;
    @Autowired
    StadiumServiceImpl stadiumService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    StatusInfoServiceImpl statusInfoService;
    @GetMapping("/gym/detail/{id}")
    public Result getGymById(@PathVariable Long id) {
        return gymService.getGymById(id);
    }
    /**
     2 体育馆
     2.1 查看体育馆信息
     2.1.1 接口说明
     url	http://xxxxx/gym/search
     协议	http
     请求方式	GET
     2.1.2 请求说明
     参数名称	是否必须	类型	描述
     name	是	String	体育馆名称
     2.1.3 url说明
     此接口用于搜索体育馆的信息
     2.1.4 返回说明
     参数名称	类型	描述
     success	Boolean	请求是否成功
     data	List<Gym>	存放返回的体育馆信息列表
     errorMsg	String	错误信息
     total	Long	表示返回的体育馆数量
     2.1.5 返回示例
     {
     "success": true,
     "errorMsg": null,
     "data": [
     {
     "id": 1,
     "name": "至尚乒乓球馆",
     "description": "暂无介绍",
     "typeId": 1,
     "images": null,
     "address": "成都市双流区怡心街道万顺路二段107号2号楼4层8号",
     "score": 5,
     "sold": 500,
     "stadiumNum": 10,
     "morningOpenHours": "8:00-12:00",
     "afternoonOpenHours": "15:00-17:00",
     "nightOpenHours": "20:00-22:00",
     "x": 104.018595,
     "y": 30.492588,
     "distance": null
     }
     ],
     "total": 1
     }
     */
    @GetMapping("/gym/search")
    public Result searchGym(@RequestParam(value = "searchKey", required = true) String searchKey) {
        return gymService.searchGym(searchKey);
    }
    @PostMapping("/gym/upload")
    public Result createGym(@RequestBody Gym gym) {
        return  gymService.createGym(gym);
    }
    //103.992768, 30.472375
    @GetMapping("/gym/sort")
    public Result sortGym(
            @RequestParam("city") String city,
            @RequestParam("typeId") Integer typeId,
            @RequestParam("sortId") Integer sortId,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y)
    throws Exception
    {
        List<GymDTO> gymList = gymService.getGymList(city, typeId, sortId,x,y );
        long size = (long)gymList.size();
        return Result.ok(gymList, size);
    }
    @PostMapping("/stadium/add")
    public Result addStadium(@RequestBody Stadium stadium) {
        boolean saved = stadiumService.save(stadium);
        if (saved) {
            return Result.ok();
        } else {
            return Result.fail("添加场地失败");
        }
    }
    @DeleteMapping("/stadium/delete/{id}")
    public Result deleteStadium(@PathVariable Long id) {
        boolean removed = stadiumService.removeById(id);
        if (removed) {
            return Result.ok();
        } else {
            return Result.fail("删除场地失败");
        }
    }
    @PutMapping("/stadium/update")
    public Result updateStadium(@RequestBody Stadium stadium) {
        boolean updated = stadiumService.updateById(stadium);
        if (updated) {
            return Result.ok();
        } else {
            return Result.fail("更新场地失败");
        }
    }
    @GetMapping("/stadium/get/{id}")
    public Result getStadiumById(@PathVariable Long id) {
        Stadium stadium = stadiumService.getById(id);
        if (stadium != null) {
            return Result.ok(stadium);
        } else {
            return Result.fail("未找到场地");
        }
    }
    @GetMapping("/stadium/list")
    public Result listStadiums(@RequestParam(required = false) Long gymId) {
        QueryWrapper<Stadium> queryWrapper = new QueryWrapper<>();
        if (gymId != null) {
            queryWrapper.eq("gymId", gymId);
        }
        List<Stadium> stadiums = stadiumService.list(queryWrapper);
        return Result.ok(stadiums, (long) stadiums.size());
    }
    @GetMapping("/a")
    public Result getA() {
        Integer i = statusInfoService.calculatePeriodNumPerDay(gymService.getById(1l));
        return Result.ok(i);
    }
}
