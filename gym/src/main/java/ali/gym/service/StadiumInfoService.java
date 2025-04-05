package ali.gym.service;

import ali.gym.entity.Stadium;
import ali.gym.entity.StatusInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StadiumInfoService {
    public List<StatusInfo> findAllStatus(Long gymId);

}
