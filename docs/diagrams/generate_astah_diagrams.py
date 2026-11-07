import os
import subprocess

DIAGRAMS_DIR = os.path.abspath('docs/diagrams')
os.makedirs(DIAGRAMS_DIR, exist_ok=True)
CHROME_PATH = r'C:\Program Files\Google\Chrome\Application\chrome.exe'

# Common Astah UML styles
ASTAH_DEF = '''
<defs>
    <!-- Arrowhead markers -->
    <marker id="astah-arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
        <path d="M0,0 L0,6 L9,3 z" fill="#000000" />
    </marker>
    <marker id="astah-open-arrow" markerWidth="12" markerHeight="12" refX="10" refY="4" orient="auto">
        <path d="M1,1 L10,4 L1,7" fill="none" stroke="#000000" stroke-width="1.5" />
    </marker>
    <marker id="astah-triangle" markerWidth="14" markerHeight="14" refX="12" refY="5" orient="auto">
        <path d="M1,1 L12,5 L1,9 z" fill="#FFFFFF" stroke="#000000" stroke-width="1.5" />
    </marker>
    <marker id="astah-diamond" markerWidth="16" markerHeight="16" refX="14" refY="5" orient="auto">
        <path d="M1,5 L8,1 L15,5 L8,9 z" fill="#000000" stroke="#000000" stroke-width="1" />
    </marker>
    <marker id="astah-crow" markerWidth="14" markerHeight="14" refX="12" refY="5" orient="auto">
        <path d="M1,1 L12,5 L1,9 M12,1 L12,9" fill="none" stroke="#000000" stroke-width="1.5" />
    </marker>
    
    <!-- Astah Yellow Gradients -->
    <linearGradient id="astah-yellow" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="#FFFFE6" />
        <stop offset="100%" stop-color="#FFFFCC" />
    </linearGradient>
    <linearGradient id="astah-yellow-dark" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="#FFFECB" />
        <stop offset="100%" stop-color="#FFF59D" />
    </linearGradient>
    <linearGradient id="astah-package-tab" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="#FFFFF0" />
        <stop offset="100%" stop-color="#FFFFCC" />
    </linearGradient>
    <linearGradient id="astah-node-side" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="#EDEDED" />
        <stop offset="100%" stop-color="#DCDCDC" />
    </linearGradient>
</defs>
'''

def render_actor(x, y, name):
    return f'''
    <g transform="translate({x},{y})">
        <!-- Astah Actor Stick Figure -->
        <circle cx="0" cy="-28" r="10" fill="#FFFFCC" stroke="#000000" stroke-width="1.5" />
        <line x1="0" y1="-18" x2="0" y2="8" stroke="#000000" stroke-width="1.5" />
        <line x1="-16" y1="-8" x2="16" y2="-8" stroke="#000000" stroke-width="1.5" />
        <line x1="0" y1="8" x2="-14" y2="28" stroke="#000000" stroke-width="1.5" />
        <line x1="0" y1="8" x2="14" y2="28" stroke="#000000" stroke-width="1.5" />
        <text x="0" y="44" font-family="Arial, Segoe UI, sans-serif" font-size="12" font-weight="bold" text-anchor="middle" fill="#000000">{name}</text>
    </g>
    '''

def render_use_case(cx, cy, rx, ry, lines):
    text_nodes = ""
    line_height = 14
    start_y = cy - (len(lines) - 1) * (line_height / 2) + 4
    for i, line in enumerate(lines):
        y_pos = start_y + i * line_height
        text_nodes += f'<text x="{cx}" y="{y_pos}" font-family="Arial, Segoe UI, sans-serif" font-size="11.5" text-anchor="middle" fill="#000000">{line}</text>'
    
    return f'''
    <ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="url(#astah-yellow)" stroke="#000000" stroke-width="1.5" />
    {text_nodes}
    '''

# ==============================================================================
# 1. DIAGRAMA DE CASOS DE USO (USE CASE)
# ==============================================================================
def create_use_case_svg():
    width = 1360
    height = 860
    
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
    <rect width="{width}" height="{height}" fill="#FFFFFF" />
    {ASTAH_DEF}
    
    <!-- Diagram Frame / Title (Astah Style) -->
    <rect x="20" y="15" width="{width-40}" height="42" fill="#FAFAFA" stroke="#B0B0B0" stroke-width="1" />
    <text x="35" y="41" font-family="Arial, Segoe UI, sans-serif" font-size="14" font-weight="bold" fill="#000000">uc [Diagrama] 4.1. Diagrama de Casos de Uso Geral - Âncora Gestão (Estilo Astah UML)</text>
    
    <!-- System Boundary -->
    <rect x="250" y="75" width="860" height="760" fill="#FCFCFC" stroke="#000000" stroke-width="1.5" />
    <rect x="250" y="75" width="280" height="26" fill="#F0F0F0" stroke="#000000" stroke-width="1.5" />
    <text x="260" y="93" font-family="Arial, Segoe UI, sans-serif" font-size="12" font-weight="bold" fill="#000000">«system» Âncora Gestão Platform</text>
    
    <!-- Left Actors -->
    {render_actor(115, 230, "Administrador Global")}
    {render_actor(115, 600, "Síndico / Gestor")}
    
    <!-- Right Actors -->
    {render_actor(1245, 250, "Porteiro")}
    {render_actor(1245, 600, "Morador")}
    
    <!-- Use Cases (Column 1 - Left inside boundary) -->
    <!-- Admin / Manager use cases -->
    {render_use_case(430, 145, 135, 25, ["Cadastrar e Inativar", "Condomínios"])}
    {render_use_case(430, 225, 135, 25, ["Parametrizar Setores e", "Importar CSV de Unidades"])}
    {render_use_case(430, 305, 135, 25, ["Edição Direta de Tabelas", "(12 Entidades do BD)"])}
    {render_use_case(430, 385, 135, 25, ["Gerenciar Planos Comerciais", "e Módulos da Plataforma"])}
    {render_use_case(430, 465, 135, 25, ["Gerenciar Vínculos e", "Aprovações de Moradores"])}
    {render_use_case(430, 545, 135, 25, ["Publicar Comunicados", "e Avisos Gerais"])}
    {render_use_case(430, 625, 135, 25, ["Gerenciar Contas, Cotas", "e Boletos Bancários"])}
    {render_use_case(430, 715, 135, 25, ["Emitir Relatórios", "Gerenciais Consolidados"])}
    
    <!-- Use Cases (Column 2 - Right inside boundary) -->
    <!-- Doorman / Resident use cases -->
    {render_use_case(890, 145, 140, 25, ["Registrar Entrada de", "Encomendas na Portaria"])}
    {render_use_case(890, 225, 140, 25, ["Registrar Retirada /", "Baixa de Encomendas"])}
    {render_use_case(890, 305, 140, 25, ["Chat Privado Individual", "Portaria ↔ Morador"])}
    {render_use_case(890, 385, 140, 25, ["Consultar Histórico de", "Encomendas da Unidade"])}
    {render_use_case(890, 465, 140, 25, ["Solicitar, Aprovar e", "Contrapropor Reservas"])}
    {render_use_case(890, 545, 140, 25, ["Controle de Portaria", "(Visitantes / Veículos)"])}
    {render_use_case(890, 625, 140, 25, ["Participar de Assembleias", "e Votar em Pautas"])}
    {render_use_case(890, 715, 140, 25, ["Consultar Assistente IA", "de Regimento Interno"])}
    
    <!-- Association Lines (Astah Solid thin black line 1.2px) -->
    <!-- Admin associations -->
    <line x1="150" y1="210" x2="295" y2="145" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="220" x2="295" y2="225" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="230" x2="295" y2="305" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="240" x2="295" y2="385" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="250" x2="305" y2="465" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="260" x2="310" y2="545" stroke="#000000" stroke-width="1.2" />
    
    <!-- Manager associations -->
    <line x1="150" y1="580" x2="310" y2="465" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="590" x2="295" y2="545" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="600" x2="295" y2="625" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="610" x2="295" y2="715" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="570" x2="750" y2="465" stroke="#000000" stroke-width="1.2" />
    <line x1="150" y1="565" x2="750" y2="385" stroke="#000000" stroke-width="1.2" />
    
    <!-- Doorman associations -->
    <line x1="1200" y1="230" x2="1030" y2="145" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="240" x2="1030" y2="225" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="250" x2="1030" y2="305" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="260" x2="1030" y2="385" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="270" x2="1030" y2="545" stroke="#000000" stroke-width="1.2" />
    
    <!-- Resident associations -->
    <line x1="1200" y1="570" x2="1030" y2="305" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="580" x2="1030" y2="385" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="590" x2="1030" y2="465" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="600" x2="1030" y2="545" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="610" x2="1030" y2="625" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="620" x2="1030" y2="715" stroke="#000000" stroke-width="1.2" />
    <line x1="1200" y1="630" x2="565" y2="625" stroke="#000000" stroke-width="1.2" stroke-dasharray="3,3" />
    <text x="680" y="650" font-family="Arial, Segoe UI, sans-serif" font-size="10" fill="#666666">«consulta própria»</text>
    
    </svg>'''
    return svg

# ==============================================================================
# 2. DIAGRAMA ENTIDADE-RELACIONAMENTO / MODELO DE DADOS (ERD / MER ASTAH)
# ==============================================================================
def render_entity(x, y, w, title, pk_list, attr_list):
    header_h = 26
    pk_h = len(pk_list) * 16 + 6
    attr_h = len(attr_list) * 16 + 8
    total_h = header_h + pk_h + attr_h
    
    pk_texts = ""
    for i, pk in enumerate(pk_list):
        pk_texts += f'<text x="{x+10}" y="{y+header_h+16+i*16}" font-family="Consolas, monospace" font-size="11" font-weight="bold" fill="#000000">PK  {pk}</text>'
        
    attr_texts = ""
    for i, attr in enumerate(attr_list):
        is_fk = "FK" in attr
        weight = "bold" if is_fk else "normal"
        color = "#990000" if is_fk else "#000000"
        attr_texts += f'<text x="{x+10}" y="{y+header_h+pk_h+16+i*16}" font-family="Consolas, monospace" font-size="11" font-weight="{weight}" fill="{color}">{attr}</text>'
        
    return f'''
    <g>
        <!-- Entity Container -->
        <rect x="{x}" y="{y}" width="{w}" height="{total_h}" fill="url(#astah-yellow)" stroke="#000000" stroke-width="1.5" />
        <!-- Header -->
        <rect x="{x}" y="{y}" width="{w}" height="{header_h}" fill="url(#astah-yellow-dark)" stroke="#000000" stroke-width="1" />
        <text x="{x+w/2}" y="{y+18}" font-family="Arial, Segoe UI, sans-serif" font-size="12" font-weight="bold" text-anchor="middle" fill="#000000">{title}</text>
        <!-- PK Divider -->
        <line x1="{x}" y1="{y+header_h+pk_h}" x2="{x+w}" y2="{y+header_h+pk_h}" stroke="#000000" stroke-width="1" />
        {pk_texts}
        {attr_texts}
    </g>
    '''

def create_erd_svg():
    width = 1440
    height = 980
    
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
    <rect width="{width}" height="{height}" fill="#FFFFFF" />
    {ASTAH_DEF}
    
    <!-- Title Frame -->
    <rect x="20" y="15" width="{width-40}" height="42" fill="#FAFAFA" stroke="#B0B0B0" stroke-width="1" />
    <text x="35" y="41" font-family="Arial, Segoe UI, sans-serif" font-size="14" font-weight="bold" fill="#000000">erd [Diagrama] 4.2. Modelo Entidade-Relacionamento / Relacional (Estilo Astah ERD)</text>
    
    <!-- Row 1: Condominium Core & Billing -->
    {render_entity(40, 80, 240, "tb_billing_plan", ["id : bigint"], ["name : varchar(100)", "price : numeric(10,2)", "max_units : integer", "is_active : boolean"])}
    {render_entity(340, 80, 270, "tb_condominium", ["id : bigint"], ["name : varchar(150)", "address : varchar(255)", "code : varchar(50)", "tax_id : varchar(20)", "image_url : text", "is_active : boolean", "FK billing_plan_id : bigint"])}
    {render_entity(670, 80, 240, "tb_sector", ["id : bigint"], ["name : varchar(100)", "structural_type : varchar", "FK condominium_id : bigint"])}
    {render_entity(970, 80, 220, "tb_unit", ["id : bigint"], ["identifier : varchar(50)", "FK sector_id : bigint"])}
    {render_entity(1230, 80, 180, "tb_resident", ["id : bigint"], ["role_type : varchar", "FK user_id : bigint", "FK unit_id : bigint"])}
    
    <!-- Row 2: Packages, Chat, Notifications -->
    {render_entity(970, 260, 230, "tb_package", ["id : bigint"], ["description : varchar(255)", "status : varchar(30)", "received_at : timestamp", "picked_up_at : timestamp", "FK unit_id : bigint", "FK recipient_id : bigint", "FK received_by_id : bigint", "FK picked_up_by_id : bigint"])}
    {render_entity(650, 310, 260, "tb_doorman_chat_message", ["id : bigint"], ["content : text", "sent_at : timestamp", "is_read : boolean", "FK condominium_id : bigint", "FK sender_id : bigint", "FK recipient_id : bigint", "FK unit_id : bigint"])}
    {render_entity(340, 310, 250, "tb_notification", ["id : bigint"], ["title : varchar(150)", "message : text", "type : varchar(50)", "created_at : timestamp", "is_read : boolean", "FK user_id : bigint"])}
    
    <!-- Row 3: Users & Security -->
    {render_entity(40, 360, 250, "tb_user", ["id : bigint"], ["name : varchar(150)", "email : varchar(150)", "password : varchar(255)", "phone : varchar(30)", "profile_image_url : text", "provider : varchar(30)", "created_at : timestamp"])}
    {render_entity(40, 600, 250, "tb_membership", ["id : bigint"], ["status : varchar(30)", "joined_at : timestamp", "FK user_id : bigint", "FK condominium_id : bigint"])}
    {render_entity(340, 600, 250, "tb_unit_payment", ["id : bigint"], ["title : varchar(150)", "amount : numeric(10,2)", "due_date : date", "status : varchar(30)", "digitable_line : text", "FK unit_id : bigint"])}
    
    <!-- Row 4: Areas, Reservations, Assemblies -->
    {render_entity(650, 600, 250, "tb_common_area", ["id : bigint"], ["name : varchar(100)", "capacity : integer", "rules : text", "is_active : boolean", "FK condominium_id : bigint"])}
    {render_entity(950, 600, 260, "tb_reservation", ["id : bigint"], ["start_at : timestamp", "end_at : timestamp", "status : varchar(30)", "notes : text", "proposed_start_at : timestamp", "proposed_end_at : timestamp", "FK common_area_id : bigint", "FK user_id : bigint"])}
    {render_entity(1240, 600, 180, "tb_assembly", ["id : bigint"], ["title : varchar", "scheduled_at : time", "status : varchar", "FK condo_id : bigint"])}
    
    <!-- ER Connectors (Orthogonal lines with Astah 1..* crow's foot) -->
    <!-- BillingPlan -> Condominium -->
    <line x1="280" y1="140" x2="340" y2="140" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- Condominium -> Sector -->
    <line x1="610" y1="140" x2="670" y2="140" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- Sector -> Unit -->
    <line x1="910" y1="140" x2="970" y2="140" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- Unit -> Resident -->
    <line x1="1190" y1="140" x2="1230" y2="140" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    
    <!-- Unit -> Package -->
    <line x1="1080" y1="180" x2="1080" y2="260" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- Condominium -> Chat -->
    <path d="M 470,240 L 470,280 L 780,280 L 780,310" fill="none" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- User -> Resident -->
    <path d="M 290,400 L 1320,400 L 1320,180" fill="none" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- User -> Notification -->
    <line x1="290" y1="380" x2="340" y2="380" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- User -> Membership -->
    <line x1="165" y1="530" x2="165" y2="600" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- Unit -> UnitPayment -->
    <path d="M 1030,180 L 1030,230 L 465,230 L 465,600" fill="none" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- Condominium -> CommonArea -->
    <path d="M 400,240 L 400,560 L 775,560 L 775,600" fill="none" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    <!-- CommonArea -> Reservation -->
    <line x1="900" y1="670" x2="950" y2="670" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-crow)" />
    
    </svg>'''
    return svg

# ==============================================================================
# 3. DIAGRAMA DE CLASSES E CAMADAS (CLASS DIAGRAM ASTAH)
# ==============================================================================
def render_class_box(x, y, w, stereotype, name, attrs, methods):
    header_h = 36 if stereotype else 26
    attr_h = (len(attrs) * 15 + 6) if attrs else 12
    method_h = (len(methods) * 15 + 6) if methods else 12
    total_h = header_h + attr_h + method_h
    
    stereo_text = f'<text x="{x+w/2}" y="{y+14}" font-family="Arial, Segoe UI, sans-serif" font-size="10" font-style="italic" text-anchor="middle" fill="#333333">«{stereotype}»</text>' if stereotype else ""
    title_y = y + 28 if stereotype else y + 18
    title_text = f'<text x="{x+w/2}" y="{title_y}" font-family="Arial, Segoe UI, sans-serif" font-size="12" font-weight="bold" text-anchor="middle" fill="#000000">{name}</text>'
    
    attr_texts = ""
    for i, a in enumerate(attrs):
        attr_texts += f'<text x="{x+8}" y="{y+header_h+13+i*15}" font-family="Arial, Segoe UI, sans-serif" font-size="11" fill="#000000">{a}</text>'
        
    method_texts = ""
    for i, m in enumerate(methods):
        method_texts += f'<text x="{x+8}" y="{y+header_h+attr_h+13+i*15}" font-family="Arial, Segoe UI, sans-serif" font-size="11" fill="#000000">{m}</text>'
        
    return f'''
    <g>
        <!-- Class Box -->
        <rect x="{x}" y="{y}" width="{w}" height="{total_h}" fill="url(#astah-yellow)" stroke="#000000" stroke-width="1.5" />
        {stereo_text}
        {title_text}
        <line x1="{x}" y1="{y+header_h}" x2="{x+w}" y2="{y+header_h}" stroke="#000000" stroke-width="1" />
        {attr_texts}
        <line x1="{x}" y1="{y+header_h+attr_h}" x2="{x+w}" y2="{y+header_h+attr_h}" stroke="#000000" stroke-width="1" />
        {method_texts}
    </g>
    '''

def render_package(x, y, w, h, name):
    return f'''
    <g>
        <path d="M {x},{y+20} L {x},{y+h} L {x+w},{y+h} L {x+w},{y+20} L {x+160},{y+20} L {x+150},{y} L {x},{y} Z" fill="#FCFCFC" stroke="#000000" stroke-width="1.5" />
        <path d="M {x},{y} L {x+150},{y} L {x+160},{y+20} L {x},{y+20} Z" fill="url(#astah-package-tab)" stroke="#000000" stroke-width="1.5" />
        <text x="{x+10}" y="{y+14}" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" fill="#000000">{name}</text>
    </g>
    '''

def create_class_diagram_svg():
    width = 1380
    height = 920
    
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
    <rect width="{width}" height="{height}" fill="#FFFFFF" />
    {ASTAH_DEF}
    
    <!-- Title Frame -->
    <rect x="20" y="15" width="{width-40}" height="42" fill="#FAFAFA" stroke="#B0B0B0" stroke-width="1" />
    <text x="35" y="41" font-family="Arial, Segoe UI, sans-serif" font-size="14" font-weight="bold" fill="#000000">class [Diagrama] 4.3. Diagrama de Classes e Arquitetura em Camadas (Estilo Astah UML)</text>
    
    <!-- Package 1: Client Layer (Flutter) -->
    {render_package(30, 75, 1320, 160, "Client Layer (Flutter Front-End)")}
    {render_class_box(50, 110, 180, "Widget", "HomePage", ["- _tab: int", "- _packages: List"], ["+ build(): Widget", "+ _load(): Future"])}
    {render_class_box(250, 110, 180, "Widget", "DoormanPage", ["- _selectedUnit: Unit", "- _activeTab: int"], ["+ build(): Widget", "+ _receivePkg(): void"])}
    {render_class_box(450, 110, 180, "Widget", "ManagerPage", ["- _condoId: int", "- _members: List"], ["+ build(): Widget", "+ _exportReport(): void"])}
    {render_class_box(650, 110, 180, "Widget", "AdminPage", ["- _selectedTable: int", "- _page: int"], ["+ build(): Widget", "+ _saveRecord(): void"])}
    {render_class_box(870, 110, 220, "Service", "CommunityApi", ["+ baseUrl: String", "- dio: Dio"], ["+ getPackages(): Future", "+ sendChatMessage(): Future"])}
    {render_class_box(1110, 110, 220, "Service", "RealtimeService", ["- stream: EventSource", "+ isConnected: bool"], ["+ connect(token): void", "+ onEvent(): Stream"])}
    
    <!-- Package 2: Controller Layer (Spring Boot Web) -->
    {render_package(30, 260, 1320, 165, "Controller Layer (Spring Boot REST API)")}
    {render_class_box(50, 295, 240, "RestController", "PackageController", ["- service: PackageService"], ["+ receive(dto): ResponseEntity", "+ pickup(id, dto): ResponseEntity"])}
    {render_class_box(310, 295, 250, "RestController", "DoormanChatController", ["- service: DoormanChatService"], ["+ sendMessage(dto): ResponseEntity", "+ getMessages(uId): ResponseEntity"])}
    {render_class_box(580, 295, 240, "RestController", "ReservationController", ["- service: ReservationService"], ["+ request(dto): ResponseEntity", "+ counterPropose(): ResponseEntity"])}
    {render_class_box(840, 295, 240, "RestController", "UnitController", ["- service: UnitService"], ["+ importCsv(file): ResponseEntity", "+ findByCondo(): ResponseEntity"])}
    {render_class_box(1100, 295, 230, "RestController", "CondominiumController", ["- service: CondominiumService"], ["+ create(dto): ResponseEntity", "+ updateImage(): ResponseEntity"])}
    
    <!-- Package 3: Service Layer (Business) -->
    {render_package(30, 455, 1320, 190, "Service Layer (Spring Boot Business Logic)")}
    {render_class_box(50, 490, 240, "Service", "PackageService", ["- repo: PackageRepository", "- realtime: RealtimeService"], ["+ receivePackage(dto): PackageDTO", "+ markAsPickedUp(id): void"])}
    {render_class_box(310, 490, 250, "Service", "DoormanChatService", ["- repo: ChatRepository", "- realtime: RealtimeService"], ["+ sendMessage(dto): MessageDTO", "+ getConversation(): List"])}
    {render_class_box(580, 490, 240, "Service", "ReservationService", ["- repo: ReservationRepository", "- realtime: RealtimeService"], ["+ checkAvailability(): boolean", "+ approve(id): ReservationDTO"])}
    {render_class_box(840, 490, 240, "Service", "UnitService", ["- repo: UnitRepository", "- sectorRepo: SectorRepo"], ["+ importCsv(condoId, file): Result", "+ validateIdentifier(): boolean"])}
    {render_class_box(1100, 490, 230, "Service", "RealtimeService (Backend)", ["- emitters: Map<Long, SseEmitter>"], ["+ emitToUser(uId, event): void", "+ emitToCondo(cId, ev): void"])}
    
    <!-- Package 4: Repository Layer (Spring Data JPA) -->
    {render_package(30, 675, 1320, 195, "Repository Layer (Spring Data JPA / PostgreSQL)")}
    {render_class_box(50, 715, 240, "Repository", "PackageRepository", ["«JpaRepository<Package, Long>»"], ["+ findByUnitId(uId): List", "+ findPendingByCondo(): List"])}
    {render_class_box(310, 715, 250, "Repository", "DoormanChatMessageRepository", ["«JpaRepository<Message, Long>»"], ["+ findByConversation(): List", "+ markAllAsRead(uId): void"])}
    {render_class_box(580, 715, 240, "Repository", "ReservationRepository", ["«JpaRepository<Reservation, Long>»"], ["+ findOverlapping(): List", "+ findByUserId(uId): List"])}
    {render_class_box(840, 715, 240, "Repository", "UnitRepository", ["«JpaRepository<Unit, Long>»"], ["+ existsBySectorAndId(): boolean", "+ findBySectorCondoId(): List"])}
    {render_class_box(1100, 715, 230, "Repository", "CondominiumRepository", ["«JpaRepository<Condo, Long>»"], ["+ findByCode(code): Optional", "+ findAllActive(): List"])}
    
    <!-- Astah Dependency / Association Arrows -->
    <!-- Client to Controller: HTTP REST -->
    <line x1="980" y1="210" x2="980" y2="260" stroke="#000000" stroke-width="1.5" stroke-dasharray="5,4" marker-end="url(#astah-open-arrow)" />
    <text x="1000" y="248" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" fill="#000000">HTTP REST (JSON)</text>
    
    <!-- Client Realtime to Backend Realtime: SSE -->
    <path d="M 1220,210 L 1220,490" fill="none" stroke="#000000" stroke-width="1.5" stroke-dasharray="5,4" marker-end="url(#astah-open-arrow)" />
    <text x="1230" y="248" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" fill="#000000">Server-Sent Events (SSE)</text>
    
    <!-- Controllers to Services -->
    <line x1="170" y1="390" x2="170" y2="490" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="435" y1="390" x2="435" y2="490" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="700" y1="390" x2="700" y2="490" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="960" y1="390" x2="960" y2="490" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="1215" y1="390" x2="1215" y2="490" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    
    <!-- Services to Repositories -->
    <line x1="170" y1="585" x2="170" y2="715" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="435" y1="585" x2="435" y2="715" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="700" y1="585" x2="700" y2="715" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    <line x1="960" y1="585" x2="960" y2="715" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-open-arrow)" />
    
    <!-- Services to Backend RealtimeService -->
    <path d="M 290,520 L 1100,520" fill="none" stroke="#000000" stroke-width="1.2" stroke-dasharray="4,4" marker-end="url(#astah-open-arrow)" />
    
    </svg>'''
    return svg

# ==============================================================================
# 4. DIAGRAMA DE SEQUÊNCIA (SEQUENCE DIAGRAM ASTAH)
# ==============================================================================
def create_sequence_diagram_svg():
    width = 1260
    height = 800
    
    lifelines = [
        ("porteiro", "porteiro: Porteiro", 110, True),
        ("ctrl", "chatCtrl: DoormanChatController", 330, False),
        ("service", "chatService: DoormanChatService", 570, False),
        ("db", "db: PostgreSQL", 800, False),
        ("realtime", "realtime: RealtimeService", 1010, False),
        ("morador", "morador: Morador", 1180, True)
    ]
    
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
    <rect width="{width}" height="{height}" fill="#FFFFFF" />
    {ASTAH_DEF}
    
    <!-- Title Frame -->
    <rect x="20" y="15" width="{width-40}" height="42" fill="#FAFAFA" stroke="#B0B0B0" stroke-width="1" />
    <text x="35" y="41" font-family="Arial, Segoe UI, sans-serif" font-size="14" font-weight="bold" fill="#000000">sd [Diagrama] 4.4. Diagrama de Sequência: Comunicação Privada e Tempo Real SSE (Estilo Astah UML)</text>
    '''
    
    # Render Lifeline Headers & Dashed Lines
    top_y = 80
    bottom_y = 740
    
    for id_name, label, x, is_actor in lifelines:
        if is_actor:
            svg += f'''
            <g transform="translate({x}, {top_y+15})">
                <circle cx="0" cy="-14" r="8" fill="#FFFFCC" stroke="#000000" stroke-width="1.5" />
                <line x1="0" y1="-6" x2="0" y2="10" stroke="#000000" stroke-width="1.5" />
                <line x1="-12" y1="-1" x2="12" y2="-1" stroke="#000000" stroke-width="1.5" />
                <line x1="0" y1="10" x2="-10" y2="24" stroke="#000000" stroke-width="1.5" />
                <line x1="0" y1="10" x2="10" y2="24" stroke="#000000" stroke-width="1.5" />
                <text x="0" y="38" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">{label}</text>
            </g>
            '''
        else:
            w_box = 190
            svg += f'''
            <rect x="{x-w_box/2}" y="{top_y}" width="{w_box}" height="40" fill="url(#astah-yellow)" stroke="#000000" stroke-width="1.5" />
            <text x="{x}" y="{top_y+24}" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle" fill="#000000">{label}</text>
            '''
        # Dashed Lifeline
        svg += f'<line x1="{x}" y1="{top_y+55}" x2="{x}" y2="{bottom_y}" stroke="#666666" stroke-width="1.2" stroke-dasharray="6,4" />\n'
        
    # Activation Bars (Astah white/pale yellow boxes)
    def activation(x, y, h):
        return f'<rect x="{x-7}" y="{y}" width="14" height="{h}" fill="#FFFFFF" stroke="#000000" stroke-width="1.2" />\n'
        
    # Messages
    # 0. Connect SSE
    svg += activation(1180, 160, 40)
    svg += activation(1010, 160, 40)
    svg += '<line x1="1180" y1="170" x2="1017" y2="170" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-arrow)" />\n'
    svg += '<text x="1100" y="164" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle" fill="#000000">1: GET /realtime/stream (Conexão persistente SSE)</text>\n'
    svg += '<line x1="1017" y1="195" x2="1173" y2="195" stroke="#000000" stroke-width="1.5" stroke-dasharray="4,4" marker-end="url(#astah-open-arrow)" />\n'
    svg += '<text x="1100" y="190" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-style="italic" text-anchor="middle" fill="#333333">1.1: SseEmitter registrado (ID_Morador)</text>\n'
    
    # 2. Porteiro envia mensagem
    svg += activation(110, 240, 420)
    svg += activation(330, 250, 400)
    svg += '<line x1="117" y1="260" x2="323" y2="260" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-arrow)" />\n'
    svg += '<text x="220" y="253" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle" fill="#000000">2: POST /doorman-chat/messages {payload}</text>\n'
    
    # 3. Ctrl -> Service
    svg += activation(570, 280, 340)
    svg += '<line x1="337" y1="290" x2="563" y2="290" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-arrow)" />\n'
    svg += '<text x="450" y="283" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle" fill="#000000">2.1: sendMessage(unitId, content)</text>\n'
    
    # 4. Service -> DB (save message)
    svg += activation(800, 320, 60)
    svg += '<line x1="577" y1="330" x2="793" y2="330" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-arrow)" />\n'
    svg += '<text x="685" y="323" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle" fill="#000000">2.2: save(tb_doorman_chat_message)</text>\n'
    svg += '<line x1="793" y1="365" x2="577" y2="365" stroke="#000000" stroke-width="1.5" stroke-dasharray="4,4" marker-end="url(#astah-open-arrow)" />\n'
    svg += '<text x="685" y="360" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-style="italic" text-anchor="middle" fill="#333333">2.3: Entidade persistida (is_read=false)</text>\n'
    
    # 5. Service -> DB (save notification)
    svg += activation(800, 400, 50)
    svg += '<line x1="577" y1="410" x2="793" y2="410" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-arrow)" />\n'
    svg += '<text x="685" y="403" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle" fill="#000000">2.4: save(tb_notification)</text>\n'
    svg += '<line x1="793" y1="440" x2="577" y2="440" stroke="#000000" stroke-width="1.5" stroke-dasharray="4,4" marker-end="url(#astah-open-arrow)" />\n'
    svg += '<text x="685" y="435" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-style="italic" text-anchor="middle" fill="#333333">2.5: Notificação gerada</text>\n'
    
    # 6. Service -> Realtime (emit event)
    svg += activation(1010, 470, 70)
    svg += '<line x1="577" y1="480" x2="1003" y2="480" stroke="#000000" stroke-width="1.5" marker-end="url(#astah-arrow)" />\n'
    svg += '<text x="790" y="473" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle" fill="#000000">2.6: emitToUser(moradorId, "CHAT_MESSAGE_RECEIVED")</text>\n'
    
    # 7. Realtime -> Morador SSE
    svg += activation(1180, 500, 60)
    svg += '<line x1="1017" y1="510" x2="1173" y2="510" stroke="#000000" stroke-width="1.5" stroke-dasharray="5,3" marker-end="url(#astah-open-arrow)" />\n'
    svg += '<text x="1100" y="503" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle" fill="#990000">2.7: Evento SSE (Nova mensagem privada)</text>\n'
    
    # Note on Morador
    svg += f'''
    <g transform="translate(1080, 565)">
        <path d="M 0,0 L 140,0 L 155,15 L 155,45 L 0,45 Z" fill="#FFFFE0" stroke="#000000" stroke-width="1" />
        <path d="M 140,0 L 140,15 L 155,15 Z" fill="#DDDDBB" stroke="#000000" stroke-width="1" />
        <text x="8" y="18" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-weight="bold" fill="#000000">Badge vermelho no chat</text>
        <text x="8" y="34" font-family="Arial, Segoe UI, sans-serif" font-size="10" fill="#333333">e alerta sonoro de aviso</text>
    </g>
    '''
    
    # 8. Service returns to Ctrl
    svg += '<line x1="563" y1="590" x2="337" y2="590" stroke="#000000" stroke-width="1.5" stroke-dasharray="4,4" marker-end="url(#astah-open-arrow)" />\n'
    svg += '<text x="450" y="583" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-style="italic" text-anchor="middle" fill="#333333">2.8: ChatMessageDTO</text>\n'
    
    # 9. Ctrl returns to Porteiro
    svg += '<line x1="323" y1="640" x2="117" y2="640" stroke="#000000" stroke-width="1.5" stroke-dasharray="4,4" marker-end="url(#astah-open-arrow)" />\n'
    svg += '<text x="220" y="633" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle" fill="#000000">2.9: HTTP 201 Created (Mensagem entregue)</text>\n'
    
    svg += '</svg>'
    return svg

# ==============================================================================
# 5. DIAGRAMA DE IMPLANTAÇÃO (DEPLOYMENT DIAGRAM ASTAH)
# ==============================================================================
def render_3d_node(x, y, w, h, depth, stereotype, name):
    return f'''
    <g>
        <!-- Isometric 3D Node (Astah Style) -->
        <!-- Front Face -->
        <rect x="{x}" y="{y+depth}" width="{w}" height="{h}" fill="url(#astah-yellow)" stroke="#000000" stroke-width="1.5" />
        <!-- Top Face -->
        <polygon points="{x},{y+depth} {x+depth},{y} {x+w+depth},{y} {x+w},{y+depth}" fill="url(#astah-yellow-dark)" stroke="#000000" stroke-width="1.5" />
        <!-- Right Face -->
        <polygon points="{x+w},{y+depth} {x+w+depth},{y} {x+w+depth},{y+h} {x+w},{y+depth+h}" fill="url(#astah-node-side)" stroke="#000000" stroke-width="1.5" />
        
        <!-- Header Text -->
        <text x="{x+w/2}" y="{y+depth+20}" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-style="italic" text-anchor="middle" fill="#333333">«{stereotype}»</text>
        <text x="{x+w/2}" y="{y+depth+38}" font-family="Arial, Segoe UI, sans-serif" font-size="13" font-weight="bold" text-anchor="middle" fill="#000000">{name}</text>
    </g>
    '''

def create_deployment_diagram_svg():
    width = 1260
    height = 800
    
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
    <rect width="{width}" height="{height}" fill="#FFFFFF" />
    {ASTAH_DEF}
    
    <!-- Title Frame -->
    <rect x="20" y="15" width="{width-40}" height="42" fill="#FAFAFA" stroke="#B0B0B0" stroke-width="1" />
    <text x="35" y="41" font-family="Arial, Segoe UI, sans-serif" font-size="14" font-weight="bold" fill="#000000">dep [Diagrama] 4.5. Diagrama de Implantação e Infraestrutura Docker (Estilo Astah UML)</text>
    
    <!-- Client Nodes -->
    {render_3d_node(40, 110, 260, 140, 16, "device", "Cliente Navegador Web")}
    <rect x="55" y="175" width="230" height="60" fill="#FFFFFF" stroke="#000000" stroke-width="1" />
    <text x="170" y="200" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">«artifact»</text>
    <text x="170" y="218" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle">Flutter Web Application (SPA)</text>
    
    {render_3d_node(40, 360, 260, 140, 16, "device", "Cliente Dispositivo Mobile")}
    <rect x="55" y="425" width="230" height="60" fill="#FFFFFF" stroke="#000000" stroke-width="1" />
    <text x="170" y="450" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">«artifact»</text>
    <text x="170" y="468" font-family="Arial, Segoe UI, sans-serif" font-size="11" text-anchor="middle">App Android / iOS (Flutter Engine)</text>
    
    <!-- Large Host Server 3D Node -->
    {render_3d_node(400, 80, 820, 680, 24, "device", "Servidor Hospedeiro Docker (Docker Compose Engine)")}
    
    <!-- Nested Containers inside Docker Host -->
    <!-- Container 1: Frontend -->
    {render_3d_node(440, 170, 350, 180, 14, "execution environment", "Contêiner: frontend (Porta 5500)")}
    <rect x="460" y="240" width="310" height="95" fill="#FFFFFF" stroke="#000000" stroke-width="1" />
    <text x="615" y="265" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">«software» Nginx 1.25 Alpine</text>
    <text x="615" y="285" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Ativos compilados /app/build/web</text>
    <text x="615" y="305" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Roteamento SPA (fallback index.html)</text>
    
    <!-- Container 2: Backend API -->
    {render_3d_node(830, 170, 360, 240, 14, "execution environment", "Contêiner: api (Porta 8080)")}
    <rect x="850" y="240" width="320" height="155" fill="#FFFFFF" stroke="#000000" stroke-width="1" />
    <text x="1010" y="265" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">«software» Spring Boot 3.3.6 (Java 21)</text>
    <text x="1010" y="285" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">• Spring Security (JWT RSA-2048)</text>
    <text x="1010" y="305" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">• Realtime SSE (/realtime/stream)</text>
    <text x="1010" y="325" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">• Flyway Database Migrations (V1..V14)</text>
    <text x="1010" y="345" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">• Spring Data JPA & Hibernate</text>
    <text x="1010" y="365" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">• Apache Commons CSV Import Engine</text>
    
    <!-- Container 3: PostgreSQL Database -->
    {render_3d_node(830, 470, 360, 190, 14, "database system", "Contêiner: postgres (Porta 5433:5432)")}
    <rect x="850" y="540" width="320" height="105" fill="#FFFFFF" stroke="#000000" stroke-width="1" />
    <text x="1010" y="565" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">«database» PostgreSQL 16 Alpine</text>
    <text x="1010" y="585" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Volume montado: postgres_data</text>
    <text x="1010" y="605" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Esquema relacional: 14 migrações ativas</text>
    <text x="1010" y="625" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Índices em chat, reservas e notificações</text>
    
    <!-- Container 4: PgAdmin -->
    {render_3d_node(440, 470, 350, 190, 14, "execution environment", "Contêiner: pgadmin (Porta 5050)")}
    <rect x="460" y="540" width="310" height="105" fill="#FFFFFF" stroke="#000000" stroke-width="1" />
    <text x="615" y="565" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" text-anchor="middle">«software» pgAdmin 4 Console Web</text>
    <text x="615" y="585" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Gerenciamento visual do banco de dados</text>
    <text x="615" y="605" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" text-anchor="middle">Inspeção de tabelas e consultas SQL</text>
    
    <!-- Network Communication Lines (Astah Connectors) -->
    <!-- Client Web -> Frontend Nginx -->
    <line x1="316" y1="190" x2="440" y2="230" stroke="#000000" stroke-width="1.5" />
    <text x="370" y="200" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-weight="bold" fill="#000000">«HTTP / 5500»</text>
    
    <!-- Client Web / Mobile -> Backend API -->
    <path d="M 316,210 L 360,210 L 360,330 L 830,330" fill="none" stroke="#000000" stroke-width="1.5" />
    <text x="375" y="320" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-weight="bold" fill="#000000">«REST / SSE 8080»</text>
    
    <line x1="316" y1="440" x2="830" y2="350" stroke="#000000" stroke-width="1.5" />
    <text x="560" y="415" font-family="Arial, Segoe UI, sans-serif" font-size="10.5" font-weight="bold" fill="#000000">«REST / SSE 8080»</text>
    
    <!-- Backend API -> PostgreSQL -->
    <line x1="1010" y1="424" x2="1010" y2="470" stroke="#000000" stroke-width="2" />
    <text x="1025" y="450" font-family="Arial, Segoe UI, sans-serif" font-size="11" font-weight="bold" fill="#000000">«JDBC / 5432»</text>
    
    <!-- PgAdmin -> PostgreSQL -->
    <line x1="804" y1="580" x2="830" y2="580" stroke="#000000" stroke-width="1.5" stroke-dasharray="4,4" />
    <text x="815" y="570" font-family="Arial, Segoe UI, sans-serif" font-size="10" fill="#333333">«SQL»</text>
    
    </svg>'''
    return svg

# ==============================================================================
# MAIN GENERATOR & RENDERER
# ==============================================================================
def main():
    diagrams = [
        ("4.1_diagrama_casos_de_uso_astah", create_use_case_svg(), 1360, 860),
        ("4.2_diagrama_mer_erd_astah", create_erd_svg(), 1440, 980),
        ("4.3_diagrama_classes_camadas_astah", create_class_diagram_svg(), 1380, 920),
        ("4.4_diagrama_sequencia_astah", create_sequence_diagram_svg(), 1260, 800),
        ("4.5_diagrama_implantacao_astah", create_deployment_diagram_svg(), 1260, 800)
    ]
    
    for name, svg_content, w, h in diagrams:
        svg_file = os.path.join(DIAGRAMS_DIR, f"{name}.svg")
        html_file = os.path.join(DIAGRAMS_DIR, f"{name}.html")
        png_file = os.path.join(DIAGRAMS_DIR, f"{name}.png")
        
        # Save SVG
        with open(svg_file, "w", encoding="utf-8") as f:
            f.write(svg_content)
        print(f"Saved SVG: {svg_file}")
        
        # Save HTML wrapper for Chrome rendering
        with open(html_file, "w", encoding="utf-8") as f:
            f.write(f'''<!DOCTYPE html><html><head><meta charset="utf-8"><style>body{{margin:0;padding:0;background:#FFFFFF;overflow:hidden;}}</style></head><body>{svg_content}</body></html>''')
            
        # Render high-res PNG via Chrome Headless
        cmd = [
            CHROME_PATH,
            '--headless=new',
            '--disable-gpu',
            '--force-device-scale-factor=2',
            f'--screenshot={png_file}',
            f'--window-size={w},{h}',
            'file:///' + html_file.replace('\\', '/')
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if os.path.exists(png_file):
            size_kb = os.path.getsize(png_file) / 1024
            print(f"Generated PNG (300 DPI): {png_file} ({size_kb:.1f} KB)")
        else:
            print(f"Error generating PNG: {res.stderr}")

if __name__ == "__main__":
    main()
