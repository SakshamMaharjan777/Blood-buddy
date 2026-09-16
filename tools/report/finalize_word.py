# -*- coding: utf-8 -*-
"""
Post-processes the built report with Microsoft Word (COM):
  1. opens the DOCX
  2. updates all fields (fills the TOC with real headings + page numbers)
  3. saves the DOCX and exports a PDF next to it

Run:  python tools/report/finalize_word.py
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DOCX = os.path.normpath(os.path.join(HERE, "..", "..", "docs",
                                     "BloodBuddy_Final_Report.docx"))
if len(sys.argv) > 1:          # optional override: python finalize_word.py <input.docx>
    DOCX = os.path.normpath(sys.argv[1])
PDF = DOCX[:-5] + ".pdf"

import win32com.client as wc  # noqa: E402  (installed with pywin32)

word = wc.Dispatch("Word.Application")
word.Visible = False
word.DisplayAlerts = 0
try:
    doc = word.Documents.Open(DOCX)
    # Word's built-in TOC entry styles ("toc 1".."toc 9", "TOC Heading") default to
    # the theme heading font (Cambria). Set them via COM so Word itself persists
    # Arial into styles.xml and honours it when the TOC is regenerated.
    for i in range(1, 10):
        try:
            doc.Styles("toc %d" % i).Font.Name = "Arial"
        except Exception:
            pass
    try:
        doc.Styles("TOC Heading").Font.Name = "Arial"
    except Exception:
        pass
    for toc in doc.TablesOfContents:
        toc.Update()
    doc.Fields.Update()
    for toc in doc.TablesOfContents:
        toc.Update()
    doc.Save()
    doc.SaveAs2(PDF, FileFormat=17)  # 17 = wdExportFormatPDF
    doc.Close(False)
    print("Saved:", DOCX)
    print("Saved:", PDF)
finally:
    word.Quit()
