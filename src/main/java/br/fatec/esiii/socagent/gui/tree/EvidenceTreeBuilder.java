package br.fatec.esiii.socagent.gui.tree;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.domain.Severity;
import br.fatec.esiii.socagent.mitre.MitreRepository;
import br.fatec.esiii.socagent.state.Incident;

/**
 * Monta a arvore de evidencias a partir de um incidente.
 *
 * <p>Estrutura produzida:
 * <pre>
 * Incidente (composto)
 *   +-- Alerta (composto)
 *   |     +-- IOC (folha)
 *   +-- Tecnicas ATT&CK (composto)
 *   |     +-- Tecnica (folha)
 *   +-- Veredito (composto)
 *         +-- Justificativa (folha)
 * </pre>
 */
@Component
public class EvidenceTreeBuilder {

    private final MitreRepository mitreRepository;

    public EvidenceTreeBuilder(MitreRepository mitreRepository) {
        this.mitreRepository = mitreRepository;
    }

    public EvidenceNode build(Incident incident) {
        EvidenceGroup root = new EvidenceGroup(
                "%s | %s | host %s".formatted(incident.id(), incident.phase().label(), incident.affectedHost()),
                incident.highestSeverity());

        incident.alerts().forEach(alert -> {
            EvidenceGroup alertNode = new EvidenceGroup(
                    "[%s] %s (%s)".formatted(alert.id(), alert.message(), alert.source()),
                    alert.severity());
            alert.indicators().forEach(ioc -> alertNode.add(EvidenceLeaf.ofIoc(ioc, alert.severity())));
            root.add(alertNode);
        });

        incident.verdict().ifPresent(verdict -> {
            if (!verdict.techniqueIds().isEmpty()) {
                EvidenceGroup techniques = new EvidenceGroup("Tecnicas MITRE ATT&CK", Severity.MEDIUM);
                mitreRepository.findAll(verdict.techniqueIds())
                        .forEach(technique -> techniques.add(EvidenceLeaf.ofTechnique(technique)));
                root.add(techniques);
            }

            EvidenceGroup verdictNode = new EvidenceGroup(
                    "Veredito: %s (confianca %.2f)".formatted(verdict.classification(), verdict.confidence()),
                    Severity.INFO);
            verdictNode.add(EvidenceLeaf.ofNote(verdict.rationale()));
            root.add(verdictNode);
        });

        incident.approver().ifPresent(approver -> {
            EvidenceGroup approval = new EvidenceGroup("Decisao humana", Severity.INFO);
            approval.add(EvidenceLeaf.ofNote("%s por %s"
                    .formatted(incident.isApproved() ? "Aprovado" : "Negado", approver)));
            incident.approvalReason().ifPresent(reason -> approval.add(EvidenceLeaf.ofNote(reason)));
            root.add(approval);
        });

        return root;
    }
}
