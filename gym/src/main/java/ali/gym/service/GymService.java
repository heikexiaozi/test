package ali.gym.service;

import ali.gym.entity.Gym;
import ali.gym.entity.Result;
import com.baomidou.mybatisplus.extension.service.IService;

public interface GymService extends IService<Gym> {
    Result getGymById(Long id);
    Result searchGym(String searchKey);
    Result createGym(Gym gym);
}