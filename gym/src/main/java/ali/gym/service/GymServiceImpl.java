package ali.gym.service;
import ali.gym.entity.GymDTO;
import ali.gym.entity.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ali.gym.entity.Gym;
import ali.gym.mapper.GymMapper;
import ali.gym.service.GymService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class GymServiceImpl extends ServiceImpl<GymMapper, Gym> implements GymService {
    @Autowired
    private GymMapper gymMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    // Redis key模板 - 使用城市和类型组合
    private static final String GYM_CITY_TYPE_LIST_KEY = "gym:address:%s:type:%d";
    /**
     * 根据地址中的城市、类型获取体育馆列表并排序
     * @param city 城市名
     * @param type 体育馆类型id
     * @param sort 排序类型（1-评分排序，2-销量排序，3-距离排序）
     * @param x 经度（可选）
     * @param y 维度（可选）
     */
    public List<GymDTO> getGymList(String city, Integer type, Integer sort, Double x, Double y) throws Exception {
        if (StringUtils.isEmpty(city)) {
            throw new Exception("城市为空");
        }
        // 1. 构建缓存key
        String cacheKey = String.format(GYM_CITY_TYPE_LIST_KEY, city, type);
        // 2. 尝试从Redis获取体育馆列表
        List<GymDTO> gymList = getGymListFromCache(cacheKey);
        // 3. 缓存未命中，从数据库查询
        if (CollectionUtils.isEmpty(gymList)) {
            // 构建查询条件 - 使用地址模糊查询
            QueryWrapper<Gym> queryWrapper = new QueryWrapper<Gym>()
                    .like("address", city)  // 地址中包含城市名
                    .eq("typeId", type);
            // 从数据库查询
            List<Gym> gyms = gymMapper.selectList(queryWrapper);
            // 转换为DTO
            gymList = gyms.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            // 存入Redis缓存
            if (!CollectionUtils.isEmpty(gymList)) {
                cacheGymList(cacheKey, gymList);
            }
        }
        // 4. 根据排序类型进行排序
        if (sort != null && !CollectionUtils.isEmpty(gymList)) {
            switch (sort) {
                case 1: // 评分排序
                    gymList.sort((a, b) -> b.getScore().compareTo(a.getScore()));
                    break;
                case 2: // 销量排序
                    gymList.sort((a, b) -> b.getSold().compareTo(a.getSold()));
                    break;
                case 3: // 距离排序
                    if (x == null || y == null) {
                        throw new Exception("距离排序需要提供经纬度参数");
                    }
                    calculateDistanceAndSort(gymList, x, y);
                    break;
                default:
                    throw new Exception("无效的排序类型");
            }
        }
        return gymList;
    }
    /**
     * 从缓存获取体育馆列表
     */
    private List<GymDTO> getGymListFromCache(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(value)) {
                return JSON.parseArray(value, GymDTO.class);
            }
        } catch (Exception e) {
            log.error("Redis get error, key: {}", key, e);
        }
        return null;
    }
    /**
     * 缓存体育馆列表
     */
    private void cacheGymList(String key, List<GymDTO> gymList) {
        try {
            String value = JSON.toJSONString(gymList);
            // 设置缓存时间为30分钟，因为是模糊查询的结果
            redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis set error, key: {}", key, e);
        }
    }
    private GymDTO convertToDTO(Gym gym) {
        return GymDTO.builder()
                .id(gym.getId())
                .name(gym.getName())
                .typeId(gym.getTypeId())
                .images(gym.getImages())
                .address(gym.getAddress())
                .sold(gym.getSold())
                .score(gym.getScore())
                .stadiumNum(gym.getStadiumNum())
                .x(gym.getX())
                .y(gym.getY())
                .build();
    }
    /**
     * 计算距离并排序
     */
    private void calculateDistanceAndSort(List<GymDTO> gymList, Double x, Double y) {
        for (GymDTO gym : gymList) {
            if (gym.getX() != null && gym.getY() != null) {
                double distance = calculateDistance(x, y, gym.getX(), gym.getY());
                gym.setDistance(distance);
            }
        }
        gymList.sort(Comparator.comparing(
                gym -> gym.getDistance() != null ? gym.getDistance() : Double.MAX_VALUE
        ));
    }
    /**
     * 计算两点之间的距离（米）
     */
    private double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        double R = 6371; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c * 1000; // 转换为米
    }
    @Override
    public Result getGymById(Long id) {
        try {
            Gym gym = getById(id);
            if (gym == null) {
                return Result.fail("体育馆不存在");
            }
            return Result.ok(gym);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
    @Override
    public Result searchGym(String searchKey) {
        try {
            LambdaQueryWrapper<Gym> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(Gym::getName, searchKey)
                    .or()
                    .like(Gym::getDescription, searchKey)
                    .or()
                    .like(Gym::getAddress, searchKey);
            List<Gym> gymList = list(queryWrapper);
            long total = gymList.size();
            return Result.ok(gymList, total);
        } catch (Exception e) {
            return Result.fail("搜索体育馆失败：" + e.getMessage());
        }
        }
    @Override
    public Result createGym(Gym gym) {
        try {
            boolean success = save(gym);
            if (success) {
                return Result.ok(gym);
            } else {
                return Result.fail("体育馆创建失败");
            }
        } catch (Exception e) {
            return Result.fail("创建体育馆失败：" + e.getMessage());
        }
    }
}
