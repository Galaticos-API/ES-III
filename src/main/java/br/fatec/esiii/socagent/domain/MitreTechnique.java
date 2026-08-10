package br.fatec.esiii.socagent.domain;

/**
 * Tecnica do framework MITRE ATT&CK usada para classificar o comportamento observado.
 */
public record MitreTechnique(String id, String name, String tactic, String description) {

    public static MitreTechnique unknown(String id) {
        return new MitreTechnique(id, "Desconhecida", "N/A",
                "Tecnica nao encontrada na base local do ATT&CK.");
    }

    @Override
    public String toString() {
        return "%s - %s (%s)".formatted(id, name, tactic);
    }
}
