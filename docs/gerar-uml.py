"""Gera UML resumida dos cinco padroes com Pillow; nao depende de Ollama.

Os diagramas completos em Mermaid permanecem em arquitetura.md.
As imagens mostram os participantes centrais, nao todas as classes do projeto.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent / "uml"
OUT.mkdir(exist_ok=True)
FONT = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 23)
BOLD = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 26)
TITLE = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 36)
SMALL = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 21)


def diagram(name, title, left, interface, children, relation, note):
    image = Image.new("RGB", (1340, 950), "#f6f8fb")
    draw = ImageDraw.Draw(image)
    draw.text((45, 24), title, font=TITLE, fill="#10243a")
    boxes = {}

    def box(key, x, y, width, header, lines, stereotype=None):
        height = 112 + 34 * len(lines)
        boxes[key] = (x, y, width, height)
        draw.rectangle((x, y, x + width, y + height), fill="white", outline="#274d70", width=2)
        if stereotype:
            draw.text((x + 15, y + 12), f"<<{stereotype}>>", font=SMALL, fill="#476278")
        header_font = BOLD
        size = 26
        while draw.textlength(header, font=header_font) > width - 30 and size > 18:
            size -= 1
            header_font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", size)
        draw.text((x + 15, y + 42), header, font=header_font, fill="#10243a")
        draw.line((x, y + 89, x + width, y + 89), fill="#274d70", width=2)
        for index, line in enumerate(lines):
            draw.text((x + 15, y + 102 + 34 * index), line, font=FONT, fill="#22384c")

    box("left", 45, 110, 530, *left)
    box("interface", 755, 110, 540, *interface, stereotype="interface")
    for index, child in enumerate(children):
        box(str(index), 45 + 430 * index, 625, 390, *child)

    def line(points, dashed=False):
        for (x1, y1), (x2, y2) in zip(points, points[1:]):
            if not dashed:
                draw.line((x1, y1, x2, y2), fill="#274d70", width=2)
                continue
            length = int(abs(x2 - x1) + abs(y2 - y1))
            for offset in range(0, length, 17):
                a, b = offset / max(1, length), min(offset + 10, length) / max(1, length)
                draw.line((x1 + (x2 - x1) * a, y1 + (y2 - y1) * a,
                           x1 + (x2 - x1) * b, y1 + (y2 - y1) * b), fill="#274d70", width=2)

    # Associacao entre cliente/contexto e interface.
    y = 220
    line([(575, y), (755, y)])
    draw.line((742, y - 7, 755, y, 742, y + 7), fill="#274d70", width=2)
    draw.text((585, y - 34), relation, font=SMALL, fill="#274d70")

    # Realizacao UML: tracejado e triangulo vazio apontando para a interface.
    x, y, width, height = boxes["interface"]
    tip = (x + width / 2, y + height)
    centers = []
    for index in range(len(children)):
        cx, cy, cw, _ = boxes[str(index)]
        centers.append(cx + cw / 2)
        line([(cx + cw / 2, cy), (cx + cw / 2, 530)], dashed=True)
    line([(min(centers), 530), (max(max(centers), tip[0]), 530)], dashed=True)
    line([(tip[0], 530), (tip[0], tip[1] + 18)], dashed=True)
    draw.polygon([tip, (tip[0] - 10, tip[1] + 18), (tip[0] + 10, tip[1] + 18)],
                 fill="white", outline="#274d70", width=2)
    draw.text((45, 452), "Tracejado + triangulo vazio: implementa a interface.", font=SMALL, fill="#476278")
    draw.text((45, 890), note, font=SMALL, fill="#476278")
    image.save(OUT / f"{name}.png")


diagram("state", "State - ciclo de vida do incidente",
        ("Incident", ["- state: IncidentState", "+ approve(who)", "+ completeReview(who)"]),
        ("IncidentState", ["+ phase(): IncidentPhase", "+ approve(incident, who)", "+ completeReview(incident, who)"]),
        [("AwaitingApprovalState", ["+ approve(...)", "+ completeReview(...)", "+ deny(...)"]),
         ("ContainingState", ["+ allowsContainmentCommands()", "+ close(...)"]),
         ("ClosedState", ["+ phase()", "Estado terminal"])],
        "delega", "Estados concretos internos de IncidentStates; demais estados no Mermaid completo.")

diagram("command", "Command - execucao e reversao por incidente",
        ("CommandInvoker", ["- incidents: Map", "+ executeQueue(incident)", "+ undoLast(incident)"]),
        ("AgentCommand", ["+ execute(): CommandResult", "+ requiresApproval(): boolean", "+ undo(): CommandResult"]),
        [("IsolateHostCommand", ["+ execute()", "+ undo()"]),
         ("BlockIpCommand", ["+ execute()", "+ undo()"]),
         ("CollectForensicsCommand", ["+ execute()", "Acao sem reversao"])],
        "executa", "Tambem existe LookupMitreCommand. O gateway de contencao e simulado.")

diagram("strategy", "Strategy - politicas de planejamento",
        ("PlannerRegistry", ["- active: TriagePlanner", "+ activate(id)", "+ available()"]),
        ("TriagePlanner", ["+ id(): String", "+ displayName(): String", "+ plan(incident): Plan"]),
        [("PlanThenExecutePlanner", ["Plano completo", "usa ThreatAnalyst"]),
         ("ReActPlanner", ["Passos iterativos", "limite: 5 passos"]),
         ("HumanInTheLoopPlanner", ["Envolve outro planejador", "forca aprovacao"])],
        "seleciona", "ThreatAnalyst isola o modelo. HumanInTheLoopPlanner tambem usa Decorator.")

diagram("observer", "Observer - eventos para varios consumidores",
        ("AgentEventBus", ["- listeners: List", "+ subscribe(listener)", "+ publish(event)"]),
        ("AgentEventListener", ["+ onEvent(event)", "+ supports(event): boolean", "+ listenerName(): String"]),
        [("AuditTrailListener", ["Registra eventos", "em memoria"]),
         ("ConsoleListener", ["Exibe eventos", "no terminal"]),
         ("SocDashboardFrame", ["Atualiza a GUI", "na thread Swing"])],
        "notifica", "Falha em um observador e isolada pelo barramento. Eventos incluem incidentId.")

diagram("composite", "Composite - arvore de evidencias na GUI",
        ("SocDashboardFrame", ["- evidenceTree: JTree", "Converte EvidenceNode", "em nos visuais"]),
        ("EvidenceNode", ["+ children(): List", "+ leafCount(): int", "+ highestSeverity(): Severity"]),
        [("EvidenceGroup", ["- children: List", "+ add(EvidenceNode)", "Agrupa EvidenceNode"]),
         ("EvidenceLeaf", ["+ label(): String", "+ severity(): Severity", "Recusa filhos"])],
        "renderiza", "EvidenceTreeBuilder monta grupos e folhas; operacoes recursivas vivem em EvidenceNode.")

images = [Image.open(OUT / f"{name}.png") for name in ("state", "command", "strategy", "observer", "composite")]
sheet = Image.new("RGB", (1340, 950 * 3), "white")
for index, picture in enumerate(images):
    picture.thumbnail((670, 950))
    sheet.paste(picture, ((index % 2) * 670, (index // 2) * 950))
# Contato apenas para revisao local; nao entra na documentacao da entrega.
target = Path(__file__).resolve().parents[1] / "target" / "uml-review.png"
target.parent.mkdir(exist_ok=True)
sheet.save(target)
print("Diagramas UML gerados em", OUT)
