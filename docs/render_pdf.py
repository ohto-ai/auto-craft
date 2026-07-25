#!/usr/bin/env python3
"""Render AutoCraft-用户指南.md to a styled HTML, then convert to PDF via weasyprint."""

import markdown
import sys, os

MD_FILE = os.path.join(os.path.dirname(__file__), "AutoCraft-用户指南.md")
OUT_HTML = os.path.join(os.path.dirname(__file__), "AutoCraft-用户指南.html")
OUT_PDF  = os.path.join(os.path.dirname(__file__), "AutoCraft-用户指南.pdf")

with open(MD_FILE, "r", encoding="utf-8") as f:
    md_text = f.read()

# Convert markdown to HTML body
body = markdown.markdown(md_text, extensions=["fenced_code", "tables", "nl2br"])

CSS = """
body {
    font-family: "Microsoft YaHei", "Noto Sans SC", "Segoe UI", sans-serif;
    max-width: 800px; margin: 40px auto; padding: 0 20px;
    color: #222; line-height: 1.8; font-size: 14px;
    background: #fff;
}
h1 { font-size: 28px; border-bottom: 3px solid #4CAF50; padding-bottom: 8px; color: #2E7D32; }
h2 { font-size: 22px; border-bottom: 2px solid #81C784; padding-bottom: 6px; margin-top: 32px; color: #388E3C; }
h3 { font-size: 18px; margin-top: 24px; color: #43A047; }
h4 { font-size: 15px; margin-top: 16px; color: #555; }
code {
    background: #f4f4f4; padding: 2px 6px; border-radius: 3px;
    font-family: "Cascadia Code", "Fira Code", Consolas, monospace; font-size: 13px;
}
pre {
    background: #263238; color: #ECEFF1; padding: 14px 18px;
    border-radius: 6px; overflow-x: auto; font-size: 13px;
    line-height: 1.5;
}
pre code { background: none; padding: 0; color: inherit; }
table { border-collapse: collapse; width: 100%; margin: 16px 0; }
th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
th { background: #4CAF50; color: white; }
tr:nth-child(even) { background: #f9f9f9; }
blockquote {
    border-left: 4px solid #4CAF50; margin: 16px 0;
    padding: 8px 16px; background: #E8F5E9; color: #2E7D32;
}
hr { border: none; border-top: 1px solid #ddd; margin: 24px 0; }
strong { color: #333; }
a { color: #1976D2; }
ol li, ul li { margin: 4px 0; }

@media print {
    body { margin: 0; padding: 0; }
    @page { margin: 25mm 20mm; size: A4; }
}
"""

html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>AutoCraft 用户指南</title>
<style>{CSS}</style>
</head>
<body>
{body}
</body>
</html>
"""

with open(OUT_HTML, "w", encoding="utf-8") as f:
    f.write(html)
print(f"HTML → {OUT_HTML}")

# Try weasyprint for PDF
try:
    from weasyprint import HTML
    HTML(filename=OUT_HTML).write_pdf(OUT_PDF)
    print(f"PDF  → {OUT_PDF}")
except ImportError:
    print("weasyprint not installed, skipping PDF generation.")
    print("Install: pip install weasyprint")
    print("Or open the HTML in a browser and Print → Save as PDF.")
