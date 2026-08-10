package br.fatec.esiii.socagent.gui.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.fatec.esiii.socagent.domain.Ioc;
import br.fatec.esiii.socagent.domain.Severity;

class EvidenceNodeTest {

    private EvidenceGroup arvore() {
        EvidenceGroup raiz = new EvidenceGroup("INC-0001", Severity.INFO);
        EvidenceGroup alerta = new EvidenceGroup("Exfiltracao detectada", Severity.LOW);
        alerta.add(EvidenceLeaf.ofIoc(Ioc.ip("185.220.101.7"), Severity.CRITICAL));
        alerta.add(EvidenceLeaf.ofIoc(Ioc.host("powershell.exe"), Severity.MEDIUM));
        raiz.add(alerta);
        raiz.add(EvidenceLeaf.ofNote("coleta forense concluida"));
        return raiz;
    }

    @Test
    @DisplayName("conta folhas recursivamente atraves de toda a arvore")
    void contaFolhas() {
        assertThat(arvore().leafCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("agrega a maior severidade dos descendentes")
    void agregaSeveridade() {
        EvidenceGroup raiz = arvore();

        assertThat(raiz.severity()).isEqualTo(Severity.INFO);
        assertThat(raiz.highestSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("folha e composto respondem a mesma interface")
    void tratamentoUniforme() {
        EvidenceNode folha = EvidenceLeaf.ofNote("observacao isolada");
        EvidenceNode composto = arvore();

        assertThat(folha.leafCount()).isEqualTo(1);
        assertThat(composto.leafCount()).isEqualTo(3);
        assertThat(folha.render(0)).contains("- observacao isolada");
        assertThat(composto.render(0)).contains("+ INC-0001");
    }

    @Test
    @DisplayName("folha recusa filhos")
    void folhaRecusaFilhos() {
        EvidenceNode folha = EvidenceLeaf.ofNote("sem filhos");

        assertThatThrownBy(() -> folha.add(EvidenceLeaf.ofNote("tentativa")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("folha");
    }

    @Test
    @DisplayName("calcula a profundidade da arvore")
    void calculaProfundidade() {
        assertThat(arvore().depth()).isEqualTo(3);
        assertThat(EvidenceLeaf.ofNote("isolada").depth()).isEqualTo(1);
    }
}
