package br.fatec.esiii.socagent.domain;

import java.util.Objects;

/**
 * Indicador de comprometimento (Indicator of Compromise).
 * E a folha da arvore de evidencias exibida na GUI.
 */
public record Ioc(IocType type, String value) {

    public Ioc {
        Objects.requireNonNull(type, "type nao pode ser nulo");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value do IOC nao pode ser vazio");
        }
        value = value.trim();
    }

    public enum IocType {
        IP,
        DOMAIN,
        FILE_HASH,
        USER_ACCOUNT,
        PROCESS
    }

    public static Ioc ip(String value) {
        return new Ioc(IocType.IP, value);
    }

    public static Ioc host(String value) {
        return new Ioc(IocType.PROCESS, value);
    }

    @Override
    public String toString() {
        return "%s:%s".formatted(type, value);
    }
}
