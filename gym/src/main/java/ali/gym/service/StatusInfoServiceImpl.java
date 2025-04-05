package ali.gym.service;

import ali.gym.entity.Gym;
import ali.gym.entity.Stadium;
import ali.gym.entity.StatusInfo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StatusInfoServiceImpl implements StadiumInfoService {
    @Autowired
    @Qualifier("byteRedisTemplate") // 指定使用 byteRedisTemplate
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private StadiumService stadiumService;
    @Autowired
    private GymService gymService;
    private static final String STATUS_KEY_PREFIX = "status:";
    private static final String UPDATE_DAY_PREFIX = "updateday:";
    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final int CACHE_DAYS = 7;

    /**
     * 查找所有场地的状态信息
     *
     * @param gymId            场馆ID
     * @param periodNumPerDay  每天的时段数
     * @return 场地状态信息列表
     */
    public List<StatusInfo> findAllStatus(Long gymId) {
        List<StatusInfo> statusInfoList = new ArrayList<>();
        List<Stadium> stadiumList = stadiumService.lambdaQuery().eq(Stadium::getGymId, gymId).list();
        Gym gym = gymService.getById(gymId);
        Integer periodNumPerDay = calculatePeriodNumPerDay(gym);
        for (Stadium stadium : stadiumList) {
            statusInfoList.addAll(getStatusInfoForStadium(stadium, periodNumPerDay));
        }
        return statusInfoList;
    }
    /**
     * 计算每天的时段数
     *
     * @param gym 场馆对象
     * @return 每天的时段数
     */
    public Integer calculatePeriodNumPerDay(Gym gym) {
        int periodNumPerDay = 0;
        periodNumPerDay += calculateTimeSlots(gym.getMorningOpenHours());
        periodNumPerDay += calculateTimeSlots(gym.getAfternoonOpenHours());
        periodNumPerDay += calculateTimeSlots(gym.getNightOpenHours());
        return periodNumPerDay;
    }

    /**
     * 计算时间段的数量
     *
     * @param openHours 营业时间字符串，格式为 "HH:mm-HH:mm"
     * @return 时间段的数量
     */
    private int calculateTimeSlots(String openHours) {
        if (openHours == null || openHours.isEmpty()) {
            return 0;
        }

        String[] times = openHours.split("-");
        if (times.length != 2) {
            return 0;
        }

        LocalTime startTime = LocalTime.parse(times[0], DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse(times[1], DateTimeFormatter.ofPattern("HH:mm"));

        long duration = Duration.between(startTime, endTime).toHours();
        return (int) duration;
    }
    /**
     * 获取单个场地的状态信息
     *
     * @param stadium          场地对象
     * @param periodNumPerDay  每天的时段数
     * @return 场地状态信息列表
     */
    private List<StatusInfo> getStatusInfoForStadium(Stadium stadium, Integer periodNumPerDay) {
        List<StatusInfo> statusInfoList = new ArrayList<>();
        Long gymId = stadium.getGymId();
        Long stadiumId = stadium.getStadiumId();
        Long id = stadium.getId();

        String statusKey = STATUS_KEY_PREFIX + gymId + ":" + stadiumId + ":" + id;
        String updateDayKey = UPDATE_DAY_PREFIX + gymId + ":" + stadiumId + ":" + id;

        byte[] statusBytes = (byte[]) redisTemplate.opsForValue().get(statusKey);

        if (statusBytes == null) {
            // 如果redis中不存在，初始化状态
            statusBytes = new byte[CACHE_DAYS * periodNumPerDay / 8 + (CACHE_DAYS * periodNumPerDay % 8 == 0 ? 0 : 1)];
            redisTemplate.opsForValue().set(statusKey, statusBytes);
            redisTemplate.expire(statusKey, 7, TimeUnit.DAYS); // 设置过期时间
        }

        String lastUpdateDayStr = (String) redisTemplate.opsForValue().get(updateDayKey);
        LocalDate lastUpdateDay = null;
        if (lastUpdateDayStr != null) {
            lastUpdateDay = LocalDate.parse(lastUpdateDayStr, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        LocalDate today = LocalDate.now();

        if (lastUpdateDay == null || !lastUpdateDay.equals(today)) {
            // 如果不是今天，更新状态
            updateStatusBytes(statusBytes, lastUpdateDay, today, periodNumPerDay);
            redisTemplate.opsForValue().set(statusKey, statusBytes);
            redisTemplate.opsForValue().set(updateDayKey, today.format(DateTimeFormatter.ISO_LOCAL_DATE));
            redisTemplate.expire(updateDayKey, 7, TimeUnit.DAYS); // 设置过期时间
        }

        // 组装 StatusInfo
        for (int day = 0; day < CACHE_DAYS; day++) {
            for (int period = 0; period < periodNumPerDay; period++) {
                int bitIndex = day * periodNumPerDay + period;
                boolean isReserved = isBitSet(statusBytes, bitIndex);

                StatusInfo statusInfo = new StatusInfo();
                statusInfo.setId(id);
                statusInfo.setGymId(gymId);
                statusInfo.setStadiumId(stadiumId);
                statusInfo.setPrice(getPrice(stadium, period));
                statusInfo.setReserved(isReserved);
                statusInfo.setReservedDate(java.sql.Date.valueOf(today.plusDays(day)));
                statusInfo.setStartTime(String.format("%02d:00", period));
                statusInfo.setEndTime(String.format("%02d:00", period + 1));

                statusInfoList.add(statusInfo);
            }
        }

        return statusInfoList;
    }

    /**
     * 根据时间段获取价格
     *
     * @param stadium 场地对象
     * @param period  时间段
     * @return 价格
     */
    private Integer getPrice(Stadium stadium, int period) {
        if (period >= 8 && period <= 17) {
            return stadium.getAfternoonPrice();
        } else if (period >= 18 || period <= 7) {
            return stadium.getNightPrice();
        } else {
            return stadium.getMorningPrice();
        }
    }

    /**
     * 更新状态字节数组
     *
     * @param statusBytes     状态字节数组
     * @param lastUpdateDay   上次更新日期
     * @param today           今天日期
     * @param periodNumPerDay 每天的时段数
     */
    private void updateStatusBytes(byte[] statusBytes, LocalDate lastUpdateDay, LocalDate today, Integer periodNumPerDay) {
        if (lastUpdateDay == null) {
            return;
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastUpdateDay, today);
        if (daysBetween <= 0) {
            return;
        }

        int shiftBits = (int) daysBetween * periodNumPerDay;
        int totalBits = statusBytes.length * 8;

        // 将数据左移
        for (int i = 0; i < totalBits - shiftBits; i++) {
            boolean bit = isBitSet(statusBytes, i + shiftBits);
            setBit(statusBytes, i, bit);
        }

        // 将空出的位置设置为0
        for (int i = totalBits - shiftBits; i < totalBits; i++) {
            clearBit(statusBytes, i);
        }
    }

    /**
     * 检查指定位是否被设置
     *
     * @param bytes    字节数组
     * @param bitIndex 位索引
     * @return 是否被设置
     */
    private boolean isBitSet(byte[] bytes, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitOffset = bitIndex % 8;
        return (bytes[byteIndex] & (1 << bitOffset)) != 0;
    }

    /**
     * 设置指定位
     *
     * @param bytes    字节数组
     * @param bitIndex 位索引
     */
    private void setBit(byte[] bytes, int bitIndex, boolean value) {
        int byteIndex = bitIndex / 8;
        int bitOffset = bitIndex % 8;
        if (value) {
            bytes[byteIndex] |= (1 << bitOffset);
        } else {
            bytes[byteIndex] &= ~(1 << bitOffset);
        }
    }

    /**
     * 清除指定位
     *
     * @param bytes    字节数组
     * @param bitIndex 位索引
     */
    private void clearBit(byte[] bytes, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitOffset = bitIndex % 8;
        bytes[byteIndex] &= ~(1 << bitOffset);
    }



}