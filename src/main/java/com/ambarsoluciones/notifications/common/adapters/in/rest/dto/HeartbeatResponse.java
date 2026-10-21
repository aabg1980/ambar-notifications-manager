package com.ambarsoluciones.notifications.common.adapters.in.rest.dto;

import java.time.Instant;

public record HeartbeatResponse(String status, String serviceName, Instant timestamp) {
}
