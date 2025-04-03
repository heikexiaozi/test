package ali.gym.api;
import ali.gym.entity.Gym;
import ali.gym.entity.GymDTO;
import ali.gym.entity.Result;
import ali.gym.entity.Stadium;
import ali.gym.service.GymServiceImpl;
import ali.gym.service.StadiumServiceImpl;
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
    @GetMapping("/gym/detail/{id}")
    public Result getGymById(@PathVariable Long id) {
        return gymService.getGymById(id);
    }
    @GetMapping("/gym/search")
    public Result searchGym(@RequestParam(value = "searchKey", required = true) String searchKey) {
        return gymService.searchGym(searchKey);
    }
    @PostMapping("/gym/upload")
    public Result createGym(@RequestBody Gym gym) {
        return  gymService.createGym(gym);
    }
    //103.992768, 30.472375
    @GetMapping("/gym/sort/{city}/{typeId}/{sortId}/{x}/{y}")
    public Result sortGym(@PathVariable (value = "sortId") Integer sortId,@PathVariable (value = "typeId") Integer typeId,@PathVariable (value = "city") String city,@PathVariable(value = "x",required = false) double x,@PathVariable(value = "y",required = false) double y) throws Exception {
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
}
