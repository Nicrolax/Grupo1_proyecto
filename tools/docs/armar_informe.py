"""
Arma docs/Informe_TrucoYa.pdf: la documentación del proyecto (docs/01 a 08) en un solo PDF, con portada,
índice y los diagramas. Necesita: pip install markdown playwright  (y un Chromium para Playwright).
Uso: python3 tools/docs/armar_informe.py
"""
import base64, datetime, os, re
import markdown

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DOCS = os.path.join(RAIZ, "docs")
PARTES = ["01-situacion-problema.md", "02-requisitos.md", "03-stack-tecnologico.md", "04-uml.md",
          "05-casos-de-uso.md", "06-infraestructura.md", "07-pruebas.md", "08-calidad.md"]

CSS = """
@page { size: A4; margin: 18mm 16mm 18mm 16mm; }
body { font-family: 'DejaVu Sans', Arial, sans-serif; font-size: 10pt; color: #222; line-height: 1.45; }
h1 { color: #5A2A1A; border-bottom: 2px solid #B8860B; padding-bottom: 4px; page-break-before: always; font-size: 20pt; }
h1.primero { page-break-before: avoid; }
h2 { color: #8A3A22; margin-top: 18px; font-size: 14pt; }
h3 { color: #5A2A1A; font-size: 11.5pt; margin-bottom: 4px; }
table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 8.6pt; page-break-inside: auto; }
tr { page-break-inside: avoid; }
th { background: #5A2A1A; color: #fff; text-align: left; padding: 4px 6px; }
td { border: 1px solid #d9cfb8; padding: 4px 6px; vertical-align: top; }
tr:nth-child(even) td { background: #faf6ec; }
code { background: #f3eee2; padding: 1px 3px; border-radius: 3px; font-size: 8.8pt; }
pre { background: #f3eee2; padding: 8px; border-radius: 4px; font-size: 8.4pt; white-space: pre-wrap; }
img { max-width: 100%; max-height: 235mm; display: block; margin: 8px auto; page-break-inside: avoid; }
blockquote { border-left: 4px solid #B8860B; margin: 8px 0; padding: 4px 12px; background: #fbf6ea; }
.portada { text-align: center; padding-top: 70mm; }
.portada .t { font-size: 34pt; color: #5A2A1A; font-weight: bold; letter-spacing: 3px; }
.portada .s { font-size: 14pt; color: #8A3A22; margin-top: 8px; }
.portada .g { margin-top: 40mm; font-size: 12pt; }
.indice { page-break-before: always; }
.indice li { margin: 4px 0; font-size: 11pt; }
"""


def imagen_embebida(m):
    alt, ruta = m.group(1), m.group(2)
    p = os.path.join(DOCS, ruta)
    if not os.path.exists(p):
        return m.group(0)
    with open(p, "rb") as f:
        b = base64.b64encode(f.read()).decode()
    return f"![{alt}](data:image/png;base64,{b})"


def main():
    cuerpo = []
    titulos = []
    for i, nombre in enumerate(PARTES):
        with open(os.path.join(DOCS, nombre), encoding="utf-8") as f:
            md = f.read()
        md = re.sub(r"!\[([^\]]*)\]\(([^)]+\.png)\)", imagen_embebida, md)
        md = re.sub(r"\]\((?!https?:|data:)([^)#]+)\)", lambda m: "](#)", md)   # enlaces internos: solo texto
        html = markdown.markdown(md, extensions=["tables", "fenced_code", "sane_lists"])
        t = re.search(r"<h1>(.*?)</h1>", html)
        titulos.append(re.sub("<[^>]+>", "", t.group(1)) if t else nombre)
        if i == 0:
            html = html.replace("<h1>", "<h1 class='primero'>", 1)
        cuerpo.append(html)
    fecha = datetime.date.today().strftime("%d/%m/%Y")
    portada = f"""<div class='portada'><div class='t'>TRUCO YA</div>
<div class='s'>El truco uruguayo dentro de Minecraft — Informe y documentación técnica</div>
<div class='g'>Programación Avanzada — UTU<br><b>Grupo 1</b><br>Nicolás Bentancour · Agustín Perdomo ·
Matías Piñeyro · Thiago Rodríguez<br><br>Versión 1.7.0 — {fecha}<br>
Repositorio: github.com/Nicrolax/Grupo1_proyecto</div></div>
<div class='indice'><h2>Índice</h2><ol>{''.join(f'<li>{t}</li>' for t in titulos)}</ol></div>
<div style='page-break-after: always'></div>"""
    html = f"<html><head><meta charset='utf-8'><style>{CSS}</style></head><body>{portada}{''.join(cuerpo)}</body></html>"
    tmp = os.path.join(DOCS, "_informe.html")
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(html)
    from playwright.sync_api import sync_playwright
    with sync_playwright() as pw:
        b = pw.chromium.launch()
        pg = b.new_page()
        pg.goto("file://" + tmp)
        pg.pdf(path=os.path.join(DOCS, "Informe_TrucoYa.pdf"), format="A4", print_background=True,
               display_header_footer=True, header_template="<span></span>",
               footer_template="<div style='font-size:8px;width:100%;text-align:center;color:#888'>"
                               "Truco Ya — Grupo 1 — página <span class='pageNumber'></span> de "
                               "<span class='totalPages'></span></div>",
               margin={"top": "16mm", "bottom": "16mm", "left": "14mm", "right": "14mm"})
        b.close()
    os.remove(tmp)
    print("docs/Informe_TrucoYa.pdf listo")


if __name__ == "__main__":
    main()
