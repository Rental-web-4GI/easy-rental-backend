package com.yowyob.easyrental.shared.events;

public record AuditEvent(String action, String module, String details) {}
