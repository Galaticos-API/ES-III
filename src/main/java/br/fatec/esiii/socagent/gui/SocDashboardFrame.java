package br.fatec.esiii.socagent.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import br.fatec.esiii.socagent.gui.component.Chip;
import br.fatec.esiii.socagent.gui.component.EventLogPanel;
import br.fatec.esiii.socagent.gui.component.EvidenceTreeCellRenderer;
import br.fatec.esiii.socagent.gui.component.PhaseStepper;
import br.fatec.esiii.socagent.gui.theme.UiTheme;
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
 * <p>Cada regiao corresponde a um padrao, e cada uma traz dica de contexto
 * dizendo qual: a trilha de fases e o State, a arvore e o Composite, o registro
 * de eventos e o Observer, o seletor de planejamento e o Strategy e os botoes
 * de acao sao Commands.
 *
 * <p>A janela e ela propria um {@link AgentEventListener}. Como o agente publica
 * de uma thread de trabalho, toda atualizacao visual volta para a EDT.
 */
public class SocDashboardFrame extends JFrame implements AgentEventListener {

    private final transient IncidentTriageService triageService;
    private final transient EvidenceTreeBuilder treeBuilder;
    private final transient SampleAlertCatalog catalog;
    private final transient AgentEventBus eventBus;
    private final transient Runnable onToggleTheme;

    private final JComboBox<String> scenarioSelector = new JComboBox<>();
    private final JComboBox<String> plannerSelector = new JComboBox<>();
    private final JLabel scenarioDescription = new JLabel();
    private final PhaseStepper stepper = new PhaseStepper();
    private final EventLogPanel eventLog = new EventLogPanel();
    private final JTree evidenceTree = new JTree(new DefaultMutableTreeNode());
    private final JProgressBar progress = new JProgressBar();

    private final JButton triageButton = new JButton("Triar incidente");
    private final JButton approveButton = new JButton("Aprovar contencao");
    private final JButton denyButton = new JButton("Negar");
    private final JButton undoButton = new JButton("Desfazer ultima acao");

    private final JPanel approvalBanner = new JPanel(new BorderLayout(UiTheme.GAP_MD, 0));
    private final JLabel approvalText = new JLabel();
    private final Chip severityChip = new Chip("", UiTheme.muted());
    private final JLabel statusLabel = new JLabel("Pronto");
    private final JLabel treeEmptyState = new JLabel();

    private static final String CARD_EMPTY = "vazio";
    private static final String CARD_TREE = "arvore";
    private final JPanel treeArea = new JPanel(new CardLayout());

    private transient Incident current;

    public SocDashboardFrame(IncidentTriageService triageService, EvidenceTreeBuilder treeBuilder,
            SampleAlertCatalog catalog, AgentEventBus eventBus, Runnable onToggleTheme) {
        super("Agente de Triagem de Incidentes de Seguranca");
        this.triageService = triageService;
        this.treeBuilder = treeBuilder;
        this.catalog = catalog;
        this.eventBus = eventBus;
        this.onToggleTheme = onToggleTheme;

        buildLayout();
        wireActions();
        eventBus.subscribe(this);
        refreshControls();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 640));
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    /** Remove a inscricao ao fechar, para que a janela antiga nao continue recebendo eventos. */
    @Override
    public void dispose() {
        eventBus.unsubscribe(this);
        super.dispose();
    }

    // ---------------------------------------------------------------
    // Montagem
    // ---------------------------------------------------------------

    private void buildLayout() {
        setLayout(new BorderLayout());
        JPanel root = new JPanel(new BorderLayout(0, UiTheme.GAP_MD));
        root.setBorder(BorderFactory.createEmptyBorder(UiTheme.GAP_MD, UiTheme.GAP_LG,
                UiTheme.GAP_MD, UiTheme.GAP_LG));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Triagem de incidentes");
        title.setFont(UiTheme.fontTitle());

        JToggleButton themeToggle = new JToggleButton(UiTheme.isDark() ? "Tema claro" : "Tema escuro");
        themeToggle.setToolTipText("Alterna entre tema claro e escuro");
        themeToggle.addActionListener(event -> onToggleTheme.run());

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(themeToggle, BorderLayout.EAST);

        catalog.scenarios().forEach(scenario -> scenarioSelector.addItem(scenario.name()));
        triageService.planners().available().stream()
                .map(TriagePlanner::displayName)
                .forEach(plannerSelector::addItem);
        plannerSelector.setSelectedItem(triageService.planners().active().displayName());
        plannerSelector.setToolTipText("Padrao Strategy: troca a politica de planejamento em execucao");
        scenarioSelector.setToolTipText("Conjunto de alertas que sera submetido ao agente");

        triageButton.putClientProperty("JButton.buttonType", "roundRect");
        triageButton.setToolTipText("Executa a triagem. O agente para antes de qualquer acao destrutiva");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.GAP_SM, 0));
        controls.setOpaque(false);
        controls.add(labelFor("CENARIO"));
        controls.add(scenarioSelector);
        controls.add(Box.createHorizontalStrut(UiTheme.GAP_MD));
        controls.add(labelFor("ESTRATEGIA"));
        controls.add(plannerSelector);
        controls.add(Box.createHorizontalStrut(UiTheme.GAP_MD));
        controls.add(triageButton);

        scenarioDescription.setFont(UiTheme.fontSubtitle());
        scenarioDescription.setForeground(UiTheme.muted());
        JPanel descriptionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        descriptionRow.setOpaque(false);
        descriptionRow.add(scenarioDescription);

        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setPreferredSize(new Dimension(0, 4));

        header.add(titleRow);
        header.add(Box.createVerticalStrut(UiTheme.GAP_MD));
        header.add(alignLeft(controls));
        header.add(Box.createVerticalStrut(UiTheme.GAP_XS));
        header.add(alignLeft(descriptionRow));
        header.add(Box.createVerticalStrut(UiTheme.GAP_SM));
        header.add(progress);
        header.add(alignLeft(stepper));
        return header;
    }

    private JComponent buildBody() {
        evidenceTree.setCellRenderer(new EvidenceTreeCellRenderer());
        evidenceTree.setRootVisible(true);
        evidenceTree.setShowsRootHandles(true);
        evidenceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        evidenceTree.setBorder(BorderFactory.createEmptyBorder(UiTheme.GAP_SM, UiTheme.GAP_SM, 0, 0));

        treeEmptyState.setText("<html><div style='text-align:center'>"
                + "Nenhum incidente aberto.<br>Escolha um cenario e clique em <b>Triar incidente</b>."
                + "</div></html>");
        treeEmptyState.setHorizontalAlignment(JLabel.CENTER);
        treeEmptyState.setForeground(UiTheme.muted());
        treeEmptyState.setFont(UiTheme.fontBody());

        JScrollPane treeScroll = new JScrollPane(evidenceTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());

        // CardLayout alterna entre o estado vazio e a arvore sem remontar o painel
        treeArea.setOpaque(false);
        treeArea.add(treeEmptyState, CARD_EMPTY);
        treeArea.add(treeScroll, CARD_TREE);

        JPanel left = card("Arvore de evidencias",
                "Padrao Composite: grupos e folhas respondem a mesma interface",
                treeArea);

        JPanel right = card("Registro de eventos",
                "Padrao Observer: o painel e um observador inscrito no barramento",
                eventLog);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.52);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(UiTheme.GAP_SM);
        return split;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);

        approvalText.setFont(UiTheme.fontBody());
        approveButton.putClientProperty("JButton.buttonType", "roundRect");
        approveButton.setToolTipText("Libera a fila de comandos. Padrao Command + State");
        denyButton.setToolTipText("Encerra o incidente sem executar nenhuma acao");

        JPanel approvalActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.GAP_SM, 0));
        approvalActions.setOpaque(false);
        approvalActions.add(denyButton);
        approvalActions.add(approveButton);

        approvalBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.warning(), 1, true),
                BorderFactory.createEmptyBorder(UiTheme.GAP_SM, UiTheme.GAP_MD,
                        UiTheme.GAP_SM, UiTheme.GAP_MD)));
        approvalBanner.setBackground(UiTheme.tint(UiTheme.warning()));
        approvalBanner.add(approvalText, BorderLayout.CENTER);
        approvalBanner.add(approvalActions, BorderLayout.EAST);
        approvalBanner.setVisible(false);

        statusLabel.setFont(UiTheme.fontSubtitle());
        statusLabel.setForeground(UiTheme.muted());
        undoButton.setToolTipText("Reverte a ultima contencao aplicada. Acao compensatoria do Command");

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        JPanel statusLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.GAP_SM, 0));
        statusLeft.setOpaque(false);
        statusLeft.add(severityChip);
        statusLeft.add(statusLabel);
        statusRow.add(statusLeft, BorderLayout.WEST);
        statusRow.add(undoButton, BorderLayout.EAST);

        footer.add(approvalBanner);
        footer.add(Box.createVerticalStrut(UiTheme.GAP_SM));
        footer.add(statusRow);
        return footer;
    }

    private JPanel card(String title, String hint, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.GAP_SM));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, UiTheme.GAP_SM, 0, UiTheme.GAP_SM));

        JLabel label = new JLabel(title);
        label.setFont(UiTheme.fontLabel());
        label.setForeground(UiTheme.muted());
        label.setToolTipText(hint);

        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createLineBorder(UiTheme.border(), 1, true));
        body.setBackground(UiTheme.surface());
        body.add(content, BorderLayout.CENTER);

        panel.add(label, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JLabel labelFor(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.fontLabel());
        label.setForeground(UiTheme.muted());
        return label;
    }

    private JComponent alignLeft(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                component.getPreferredSize().height));
        return component;
    }

    // ---------------------------------------------------------------
    // Acoes
    // ---------------------------------------------------------------

    private void wireActions() {
        updateScenarioDescription();
        scenarioSelector.addActionListener(event -> updateScenarioDescription());

        plannerSelector.addActionListener(event -> {
            int index = plannerSelector.getSelectedIndex();
            if (index >= 0) {
                TriagePlanner selected = triageService.planners().available().get(index);
                triageService.planners().activate(selected.id());
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

    private void updateScenarioDescription() {
        String name = (String) scenarioSelector.getSelectedItem();
        if (name != null) {
            scenarioDescription.setText(catalog.byName(name).description());
        }
    }

    /**
     * A triagem consulta o modelo e pode levar segundos. Na EDT isso congelaria
     * a interface, entao vai para um {@link SwingWorker}.
     */
    private void runTriage() {
        var scenario = catalog.byName((String) scenarioSelector.getSelectedItem());
        setBusy(true);

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
                    statusLabel.setText("Falha na triagem: " + ex.getMessage());
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
        SwingUtilities.invokeLater(() -> {
            eventLog.append(event);
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
        showTree();
    }

    private void showTree() {
        ((CardLayout) treeArea.getLayout()).show(treeArea, CARD_TREE);
    }

    /** Converte o Composite de dominio na arvore que o Swing renderiza. */
    private DefaultMutableTreeNode toSwingNode(EvidenceNode node) {
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(node);
        node.children().forEach(child -> swingNode.add(toSwingNode(child)));
        return swingNode;
    }

    private void refreshControls() {
        boolean hasIncident = current != null;
        IncidentPhase phase = hasIncident ? current.phase() : null;
        boolean awaiting = phase == IncidentPhase.AWAITING_APPROVAL;

        stepper.setPhase(phase);
        approveButton.setEnabled(awaiting);
        denyButton.setEnabled(awaiting);
        undoButton.setEnabled(hasIncident && triageService.invoker().undoableCount() > 0);

        approvalBanner.setVisible(awaiting);
        if (awaiting) {
            approvalText.setText("<html><b>Aprovacao humana requerida.</b> "
                    + "%d comando(s) aguardam decisao. Nada foi executado.</html>"
                    .formatted(triageService.invoker().pendingCount()));
        }

        if (hasIncident) {
            severityChip.setVisible(true);
            severityChip.update(current.highestSeverity().name(),
                    UiTheme.severityColor(current.highestSeverity()));
            statusLabel.setText("%s  ·  host %s  ·  %d comando(s) reversivel(is)".formatted(
                    current.id(), current.affectedHost(), triageService.invoker().undoableCount()));
        } else {
            severityChip.setVisible(false);
            statusLabel.setText("Pronto");
        }
    }

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        triageButton.setEnabled(!busy);
        scenarioSelector.setEnabled(!busy);
        plannerSelector.setEnabled(!busy);
        if (busy) {
            statusLabel.setText("Consultando o modelo de linguagem...");
        }
    }
}
