# -*- coding: utf-8 -*-
"""Generate backend/mdFile/新卡牌专属效果.docx from the markdown tables."""
from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MD = ROOT / "backend" / "mdFile" / "新卡牌专属效果.md"
OUT = ROOT / "backend" / "mdFile" / "新卡牌专属效果.docx"
V2_MD = ROOT / "backend" / "mdFile" / "新卡牌专属效果（第二版）.md"
V2_OUT = ROOT / "backend" / "mdFile" / "新卡牌专属效果（第二版）.docx"
HEADERS = ["编号", "名称", "费用", "类型", "专属效果", "设计说明"]
CENTER_COLS = {0, 2, 3}


def set_run_font(run, size=10.5, bold=False, color=None, name="宋体"):
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = name
    if color:
        run.font.color.rgb = color
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.get_or_add_rFonts()
    rFonts.set(qn("w:ascii"), "Calibri")
    rFonts.set(qn("w:hAnsi"), "Calibri")
    rFonts.set(qn("w:eastAsia"), name)


def shade_cell(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tcPr.append(shd)


def set_cell_border(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = OxmlElement("w:tcBorders")
    for edge in ("top", "left", "bottom", "right"):
        element = OxmlElement(f"w:{edge}")
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), "4")
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), "8FAADC")
        tcBorders.append(element)
    tcPr.append(tcBorders)


def set_cell_margins(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement("w:tcMar")
    for m, v in (("top", 40), ("left", 60), ("bottom", 40), ("right", 60)):
        node = OxmlElement(f"w:{m}")
        node.set(qn("w:w"), str(v))
        node.set(qn("w:type"), "dxa")
        tcMar.append(node)
    tcPr.append(tcMar)


def set_cell_valign(cell):
    vAlign = OxmlElement("w:vAlign")
    vAlign.set(qn("w:val"), "center")
    cell._tc.get_or_add_tcPr().append(vAlign)


def write_cell(cell, text, *, header=False, center=False):
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER if (header or center) else WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.08
    run = p.add_run(text)
    if header:
        set_run_font(run, 10.5, True, RGBColor(0xC0, 0x00, 0x00), "微软雅黑")
        run.italic = True
        shade_cell(cell, "E2EFDA")
    else:
        set_run_font(run, 10.5, False, RGBColor(0x2D, 0x2D, 0x2D), "宋体")
    set_cell_border(cell)
    set_cell_margins(cell)
    set_cell_valign(cell)


def add_table(doc, rows):
    table = doc.add_table(rows=1 + len(rows), cols=6)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    tblPr = table._tbl.tblPr
    tblW = OxmlElement("w:tblW")
    tblW.set(qn("w:w"), "5000")
    tblW.set(qn("w:type"), "pct")
    tblPr.append(tblW)
    widths = [Cm(2.2), Cm(2.6), Cm(1.5), Cm(1.8), Cm(8.6), Cm(7.3)]
    for i, h in enumerate(HEADERS):
        write_cell(table.rows[0].cells[i], h, header=True, center=True)
        table.rows[0].cells[i].width = widths[i]
    for r, row in enumerate(rows, start=1):
        for c, val in enumerate(row):
            cell = table.rows[r].cells[c]
            write_cell(cell, val, center=(c in CENTER_COLS))
            cell.width = widths[c]
            if r % 2 == 0:
                shade_cell(cell, "F7FAFC")


def add_heading(doc, text, level=1):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(14 if level == 1 else 10)
    p.paragraph_format.space_after = Pt(8)
    run = p.add_run(text)
    if level == 1:
        set_run_font(run, 16, True, RGBColor(0x1F, 0x4E, 0x79), "微软雅黑")
    else:
        set_run_font(run, 13, True, RGBColor(0x2E, 0x75, 0xB6), "微软雅黑")


def add_body(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.25
    run = p.add_run(text)
    set_run_font(run, 11, False, RGBColor(0x33, 0x33, 0x33), "宋体")


def parse_md_tables(text):
    sections = []
    heading = None
    note = None
    rows = []
    in_table = False
    for line in text.splitlines():
        if line.startswith("## "):
            if heading and rows:
                sections.append((heading, note, rows))
            heading = line[3:].replace("`", "").strip()
            note = None
            rows = []
            in_table = False
            continue
        if line.startswith("|") and "编号" in line and "名称" in line:
            in_table = True
            continue
        if in_table and line.startswith("|") and set(line.replace("|", "").strip()) <= set("-: "):
            continue
        if in_table and line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if len(cells) >= 6:
                rows.append(cells[:6])
            continue
        if in_table and not line.startswith("|"):
            in_table = False
        if heading and not rows and line.strip() and not line.startswith("|") and not line.startswith("#") and not line.startswith("---"):
            note = line.strip()
    if heading and rows:
        sections.append((heading, note, rows))
    return sections


def main():
    sections = parse_md_tables(MD.read_text(encoding="utf-8"))
    doc = Document()
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width, section.page_height = section.page_height, section.page_width
    section.left_margin = Cm(1.4)
    section.right_margin = Cm(1.4)
    section.top_margin = Cm(1.4)
    section.bottom_margin = Cm(1.4)

    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = title.add_run("新卡牌专属效果")
    set_run_font(run, 22, True, RGBColor(0x1F, 0x4E, 0x79), "微软雅黑")

    add_body(doc, "记录需胜利解锁的收藏卡（及图鉴样例卡 T-01）当前专属效果。费用、部门、卡牌类型不变。效果以结构化配置为准，与库内 cards.description 一致。")
    add_body(doc, "范围：O-16～O-19、S-07～S-24、P-05～P-20、O-20～O-44、T-01、H-01～H-03、B-01～B-07")
    add_body(doc, "不含：初始 27 张旧卡（S-01～S-06、P-01～P-04、O-01～O-15、L-01 / L-02）")

    add_heading(doc, "设计原则", 2)
    for line in [
        "整套机制（类型 + 时机 + 延迟 + 数值 + 目标）互不重复，也不复刻旧卡整套。",
        "公共卡强度按费用：0 费≈2、1 费≈3、2 费≈4、3 费≈6。",
        "老板卡同费明显高于公共卡（约高一档费用）。",
        "销售偏输出，采购偏防御，公共可混搭；目标「自己 / 一名玩家 / 双方」视为不同效果。",
    ]:
        add_body(doc, "· " + line)

    for heading, note, rows in sections:
        if heading == "修订记录":
            continue
        add_heading(doc, heading, 2)
        if note:
            add_body(doc, note)
        add_table(doc, rows)

    rec_heading = None
    rec_rows = []
    in_rec = False
    for line in MD.read_text(encoding="utf-8").splitlines():
        if line.startswith("## ") and "修订记录" in line:
            rec_heading = "修订记录"
            continue
        if rec_heading and line.startswith("|") and "日期" in line:
            in_rec = True
            continue
        if in_rec and line.startswith("|") and set(line.replace("|", "").strip()) <= set("-: "):
            continue
        if in_rec and line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            rec_rows.append(cells)
        elif in_rec:
            break
    if rec_heading:
        add_heading(doc, rec_heading, 2)
        rec = doc.add_table(rows=1 + len(rec_rows), cols=2)
        rec.alignment = WD_TABLE_ALIGNMENT.CENTER
        write_cell(rec.rows[0].cells[0], "日期", header=True, center=True)
        write_cell(rec.rows[0].cells[1], "说明", header=True, center=True)
        for i, row in enumerate(rec_rows, start=1):
            write_cell(rec.rows[i].cells[0], row[0], center=True)
            write_cell(rec.rows[i].cells[1], row[1] if len(row) > 1 else "")

    tmp = OUT.with_suffix(".tmp.docx")
    doc.save(tmp)
    try:
        tmp.replace(OUT)
        print("wrote", OUT, "sections", len(sections))
    except PermissionError:
        alt = OUT.with_name("新卡牌专属效果-新表.docx")
        tmp.replace(alt)
        print("locked, wrote", alt, "sections", len(sections))


def save_doc(doc, out: Path):
    tmp = out.with_suffix(".tmp.docx")
    doc.save(tmp)
    try:
        tmp.replace(out)
        print("wrote", out)
    except PermissionError:
        alt = out.with_name(out.stem + "-新表.docx")
        tmp.replace(alt)
        print("locked, wrote", alt)


def main_v2():
    sections = parse_md_tables(V2_MD.read_text(encoding="utf-8"))
    doc = Document()
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width, section.page_height = section.page_height, section.page_width
    section.left_margin = Cm(1.4)
    section.right_margin = Cm(1.4)
    section.top_margin = Cm(1.4)
    section.bottom_margin = Cm(1.4)

    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = title.add_run("新卡牌专属效果（第二版）")
    set_run_font(run, 22, True, RGBColor(0x1F, 0x4E, 0x79), "微软雅黑")

    add_body(doc, "记录自 2026-08-24 起新增的收藏卡专属效果。第一版（此前全部收藏卡）见同目录《新卡牌专属效果.docx》。")
    add_body(doc, "费用、部门、卡牌类型以入库配置为准；效果与库内 cards.description 一致。表格列：编号、名称、费用、类型、专属效果、设计说明。")

    add_heading(doc, "设计原则", 2)
    for line in [
        "只收录今日起新加的卡，不回写第一版已有卡面。",
        "整套机制（类型 + 时机 + 延迟 + 数值 + 目标）不与旧卡、第一版新卡重复。",
        "老板卡同费明显高于公共卡（约高一档费用）。",
    ]:
        add_body(doc, "· " + line)

    for heading, note, rows in sections:
        if heading == "修订记录":
            continue
        add_heading(doc, heading, 2)
        if note:
            add_body(doc, note)
        add_table(doc, rows)

    text = V2_MD.read_text(encoding="utf-8")
    rec_heading = None
    rec_rows = []
    in_rec = False
    for line in text.splitlines():
        if line.startswith("## ") and "修订记录" in line:
            rec_heading = "修订记录"
            continue
        if rec_heading and line.startswith("|") and "日期" in line:
            in_rec = True
            continue
        if in_rec and line.startswith("|") and set(line.replace("|", "").strip()) <= set("-: "):
            continue
        if in_rec and line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            rec_rows.append(cells)
        elif in_rec:
            break
    if rec_heading:
        add_heading(doc, rec_heading, 2)
        rec = doc.add_table(rows=1 + len(rec_rows), cols=2)
        rec.alignment = WD_TABLE_ALIGNMENT.CENTER
        write_cell(rec.rows[0].cells[0], "日期", header=True, center=True)
        write_cell(rec.rows[0].cells[1], "说明", header=True, center=True)
        for i, row in enumerate(rec_rows, start=1):
            write_cell(rec.rows[i].cells[0], row[0], center=True)
            write_cell(rec.rows[i].cells[1], row[1] if len(row) > 1 else "")

    save_doc(doc, V2_OUT)


if __name__ == "__main__":
    import sys
    if "v2" in sys.argv:
        main_v2()
    else:
        main()
