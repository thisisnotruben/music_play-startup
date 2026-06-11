package rarlog.me.Startup.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rarlog.me.Startup.service.HealthService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @Operation(summary = "Takes time for the service to init, ping this to find out if ready")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content()),
            @ApiResponse(responseCode = "503", content = @Content()),
    })
    @GetMapping("/isReady")
    public ResponseEntity<String> isReady() {
        log.info("healthcheck called");
        if (healthService.isReady()) {
            return ResponseEntity.ok("ready");
        }
        return ResponseEntity.status(HttpStatusCode.valueOf(503)).body("Not ready");
    }

}
