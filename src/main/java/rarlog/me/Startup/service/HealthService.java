package rarlog.me.Startup.service;

import org.springframework.stereotype.Service;

import lombok.Data;

@Data
@Service
public class HealthService {

    private boolean isReady = false;
    
}
