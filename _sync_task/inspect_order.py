# -*- coding: utf-8 -*-
import docx, sys
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

def iter_block_items(parent):
    for child in parent.element.body.iterchildren():
        if isinstance(child, CT_P):
            yield ('P', Paragraph(child, parent))
        elif isinstance(child, CT_Tbl):
            yield ('T', Table(child, parent))

for fname in sys.argv[1:]:
    print('#'*25, fname)
    d = docx.Document(fname)
    n = 0
    for kind, bi in iter_block_items(d):
        if kind == 'P':
            xml = bi._p.xml
            has_img = ('graphic' in xml) or ('blip' in xml) or ('w:drawing' in xml)
            if bi.text.strip() or has_img:
                tag = 'IMG ' if has_img else ''
                print('P[%d] %s%s: %s' % (n, tag, bi.style.name, bi.text.strip()[:60]))
        else:
            r0 = bi.rows[0].cells[0].text.strip()[:30] if bi.rows else ''
            print('T[%d] TABLE %dx%d: %s' % (n, len(bi.rows), len(bi.columns), r0))
        n += 1
    print()
