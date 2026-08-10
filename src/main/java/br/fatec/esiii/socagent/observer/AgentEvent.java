package br.fatec.esiii.socagent.observer;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Evento imutavel publicado pelo agente. E a mensagem trocada no padrao Observer.
 */
public record AgentEvent(
        EventType type,
        String incidentId,
        String title,
        String detail,
        Instant occurredAt,
        Map<String, Object> attributes) {

    public AgentEvent {
        Objects.requireNonNull(type, "type nao pode ser nulo");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static AgentEvent of(EventType type, String incidentId, String title, String detail) {
        return new AgentEvent(type, incidentId, title, detail, Instant.now(), Map.of());
    }

    public AgentEvent with(String key, Object value) {
        var merged = new java.util.HashMap<>(attributes);
        merged.put(key, value);
        return new AgentEvent(type, incidentId, title, detail, occurredAt, merged);
    }

    public enum EventType {
        INCIDENT_CREATED,
        STATE_CHANGED,
        TRIAGE_COMPLETED,
        PLAN_CREATED,
        COMMAND_QUEUED,
        COMMAND_EXECUTED,
        COMMAND_FAILED,
        COMMAND_UNDONE,
        APPROVAL_REQUESTED,
        APPROVAL_GRANTED,
        APPROVAL_DENIED,
        INCIDENT_CLOSED
    }
}
