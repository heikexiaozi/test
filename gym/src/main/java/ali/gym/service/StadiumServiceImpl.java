package ali.gym.service;

import ali.gym.entity.Stadium;
import ali.gym.entity.StatusInfo;
import ali.gym.mapper.StadiumMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StadiumServiceImpl extends ServiceImpl<StadiumMapper, Stadium> implements StadiumService {

}