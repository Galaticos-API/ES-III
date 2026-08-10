package br.fatec.esiii.socagent.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import br.fatec.esiii.socagent.gui.tree.EvidenceNode;
import br.fatec.esiii.socagent.gui.tree.EvidenceTreeBuilder;
import br.fatec.esiii.socagent.observer.AgentEvent;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.observer.AgentEventListener;
import br.fatec.esiii.socagent.service.IncidentTriageService;
import br.fatec.esiii.socagent.service.SampleAlertCatalog;
import br.fatec.esiii.socagent.state.Incident;
import br.fatec.esiii.socagent.state.IncidentPhase;
import br.fatec.esiii.socagent.strategy.TriagePlanner;

/**
 * Painel de operacao do agente.
 *
 * <p>Cada regiao da tela corresponde a um padrao: a arvore a esquerda e o
 * Composite, o log a direita e o Observer, o seletor no topo e o Strategy e os
 * botoes inferiores sao Commands habilitados conforme o State.
 *
 * <p>A janela e ela propria um {@link AgentEventListener}: registra-se no
 * barramento e recebe os eventos do agente. Como o agente publica de uma
 * thread de trabalho, toda atualizacao visual e reenviada para a EDT.
 */
public class SocDashboardFrame extends JFrame implements AgentEventListener {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.systemDefault());

    private final transient IncidentTriageService triageService;
    private final transient EvidenceTreeBuilder treeBuilder;
    private final transient SampleAlertCatalog catalog;

    private final JComboBox<String> scenarioSelector = new JComboBox<>();
    private final JComboBox<String> plannerSelector = new JComboBox<>();
    private final JTextArea eventLog = new JTextArea();
    private final JTree evidenceTree = new JTree(new DefaultMutableTreeNode("Nenhum incidente"));
    private final JLabel statusLabel = new JLabel("Pronto");

    private final JButton triageButton = new JButton("Triar incidente");
    private final JButton approveButton = new JButton("Aprovar contencao");
    private final JButton denyButton = new JButton("Negar");
    private final JButton undoButton = new JButton("Desfazer ultima acao");

    private transient Incident current;

    public SocDashboardFrame(IncidentTriageService triageService, EvidenceTreeBuilder treeBuilder,
            SampleAlertCatalog catalog, AgentEventBus eventBus) {
        super("Agente de Triagem de Incidentes de Seguranca");
        this.triageService = triageService;
        this.treeBuilder = treeBuilder;
        this.catalog = catalog;

        buildLayout();
        wireActions();
        eventBus.subscribe(this);
        refreshControls();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        catalog.scenarios().forEach(scenario -> scenarioSelector.addItem(scenario.name()));
        triageService.planners().available().stream()
                .map(TriagePlanner::displayName)
                .forEach(plannerSelector::addItem);
        plannerSelector.setSelectedItem(triageService.planners().active().displayName());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Cenario:"));
        top.add(scenarioSelector);
        top.add(Box.createHorizontalStrut(24));
        top.add(new JLabel("Estrategia (Strategy):"));
        top.add(plannerSelector);

        eventLog.setEditable(false);
        eventLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane treeScroll = new JScrollPane(evidenceTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Arvore de evidencias (Composite)"));
        treeScroll.setPreferredSize(new Dimension(520, 0));

        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Eventos do agente (Observer)"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, logScroll);
        split.setResizeWeight(0.5);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bottom.add(triageButton);
        bottom.add(approveButton);
        bottom.add(denyButton);
        bottom.add(undoButton);
        bottom.add(Box.createHorizontalStrut(24));
        bottom.add(statusLabel);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void wireActions() {
        plannerSelector.addActionListener(event -> {
            int index = plannerSelector.getSelectedIndex();
            if (index >= 0) {
                TriagePlanner selected = triageService.planners().available().get(index);
                triageService.planners().activate(selected.id());
                appendLog("Estrategia ativa: " + selected.displayName());
            }
        });

        triageButton.addActionListener(event -> runTriage());
        approveButton.addActionListener(event -> {
            triageService.approve(current, null);
            refreshAll();
        });
        denyButton.addActionListener(event -> {
            triageService.deny(current, null, "Negado pelo operador no painel");
            refreshAll();
        });
        undoButton.addActionListener(event -> {
            triageService.undoLastContainment(current);
            refreshAll();
        });
    }

    /**
     * A triagem chama o modelo de linguagem e pode levar segundos. Rodar isso na
     * EDT congelaria a interface, entao vai para um {@link SwingWorker}.
     */
    private void runTriage() {
        String scenarioName = (String) scenarioSelector.getSelectedItem();
        var scenario = catalog.byName(scenarioName);

        setBusy(true);
        appendLog("Iniciando triagem do cenario: " + scenarioName);

        new SwingWorker<Incident, Void>() {
            @Override
            protected Incident doInBackground() {
                Incident incident = triageService.open(scenario.alerts());
                triageService.triage(incident);
                return incident;
            }

            @Override
            protected void done() {
                try {
                    current = get();
                } catch (Exception ex) {
                    appendLog("Falha na triagem: " + ex.getMessage());
                } finally {
                    setBusy(false);
                    refreshAll();
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // Observer
    // ---------------------------------------------------------------

    @Override
    public void onEvent(AgentEvent event) {
        String line = "%s | %-20s | %s%s".formatted(
                TIME.format(event.occurredAt()),
                event.type(),
                event.title(),
                event.detail() == null || event.detail().isBlank() ? "" : " -- " + event.detail());
        SwingUtilities.invokeLater(() -> {
            appendLog(line);
            refreshControls();
        });
    }

    // ---------------------------------------------------------------
    // Atualizacao da interface
    // ---------------------------------------------------------------

    private void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            refreshTree();
            refreshControls();
        });
    }

    private void refreshTree() {
        if (current == null) {
            return;
        }
        EvidenceNode root = treeBuilder.build(current);
        evidenceTree.setModel(new DefaultTreeModel(toSwingNode(root)));
        for (int row = 0; row < evidenceTree.getRowCount(); row++) {
            evidenceTree.expandRow(row);
        }
    }

    /** Converte o Composite de dominio na arvore que o Swing sabe renderizar. */
    private TreeNode toSwingNode(EvidenceNode node) {
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(
                node.isLeaf()
                        ? node.label()
                        : "%s  (%d evidencia(s), max %s)"
                                .formatted(node.label(), node.leafCount(), node.highestSeverity()));
        node.children().forEach(child -> swingNode.add((DefaultMutableTreeNode) toSwingNode(child)));
        return swingNode;
    }

    private void refreshControls() {
        boolean hasIncident = current != null;
        IncidentPhase phase = hasIncident ? current.phase() : null;

        approveButton.setEnabled(hasIncident && phase == IncidentPhase.AWAITING_APPROVAL);
        denyButton.setEnabled(hasIncident && phase == IncidentPhase.AWAITING_APPROVAL);
        undoButton.setEnabled(hasIncident && triageService.invoker().undoableCount() > 0);

        statusLabel.setText(hasIncident
                ? "Incidente %s | Fase: %s | Comandos reversiveis: %d"
                        .formatted(current.id(), phase.label(), triageService.invoker().undoableCount())
                : "Pronto");
    }

    private void setBusy(boolean busy) {
        triageButton.setEnabled(!busy);
        scenarioSelector.setEnabled(!busy);
        plannerSelector.setEnabled(!busy);
        if (busy) {
            statusLabel.setText("Consultando o modelo...");
        }
    }

    private void appendLog(String line) {
        eventLog.append(line + System.lineSeparator());
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    /** Exposto para diagnostico: o texto do Composite sem depender do Swing. */
    public String renderEvidenceAsText() {
        return current == null ? "" : treeBuilder.build(current).render(0);
    }
}
