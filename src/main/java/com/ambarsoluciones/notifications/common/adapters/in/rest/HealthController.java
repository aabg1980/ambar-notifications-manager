package com.ambarsoluciones.notifications.common.adapters.in.rest;

import java.time.Instant;
import com.ambarsoluciones.notifications.common.adapters.in.rest.dto.HeartbeatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	private final String serviceName;

	public HealthController(@Value("${spring.application.name}") String serviceName) {
		this.serviceName = serviceName;
	}

	@GetMapping("/ping")
	public HeartbeatResponse ping() {
		return new HeartbeatResponse("pong", serviceName, Instant.now());
	}
}
