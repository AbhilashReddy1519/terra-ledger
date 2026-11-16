"""
ocr-service – M2 Implementation
Provides /health and /ocr/extract endpoint using OpenCV preprocessing,
Tesseract OCR, and regex field extraction for land record deeds.
"""
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import pytesseract
from extractor import extract_document

app = FastAPI(title="terra-ledger OCR service", version="0.2.0")


@app.get("/health")
def health():
    return {
        "status": "ok",
        "tesseract_version": str(pytesseract.get_tesseract_version())
    }


@app.post("/ocr/extract")
async def extract(file: UploadFile = File(...)):
    """
    Accepts scanned land records (PDF, PNG, JPG/JPEG).
    Preprocesses with OpenCV, performs OCR via Tesseract, and extracts
    structured land record fields.
    """
    filename = file.filename or "upload.png"
    lower_name = filename.lower()
    allowed_extensions = (".pdf", ".png", ".jpg", ".jpeg", ".tif", ".tiff")
    
    if not any(lower_name.endswith(ext) for ext in allowed_extensions):
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type. Supported types: {', '.join(allowed_extensions)}"
        )

    try:
        contents = await file.read()
        result = extract_document(contents, filename)
        return JSONResponse(result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR extraction failed: {str(e)}")
