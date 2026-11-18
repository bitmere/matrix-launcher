import xml.etree.ElementTree as ET

tree = ET.parse('artifacts/tv_ui_inspected.xml')
for e in tree.iter():
    t = e.attrib.get('text', '').strip()
    if t:
        print(f"TEXT: {t}")
