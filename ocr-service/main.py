"""
ocr-service  –  M0 walking skeleton
Provides a /health endpoint and a stub /ocr/extract endpoint.
Real Tesseract + OpenCV processing is wired in M2.
"""
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import pytesseract
from PIL import Image
import io
import cv2
import numpy as np

app = FastAPI(title="terra-ledger OCR service", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok", "tesseract_version": str(pytesseract.get_tesseract_version())}


@app.post("/ocr/extract")
async def extract(file: UploadFile = File(...)):
    """
    M0 stub: accepts any image, runs a quick Tesseract pass,
    returns raw text.  Field extraction is added in M2.
    """
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Only image files are accepted")

    contents = await file.read()
    image = Image.open(io.BytesIO(contents))
    raw_text = pytesseract.image_to_string(image)

    return JSONResponse({
        "raw_text": raw_text,
        "fields":   {},          # populated in M2
        "note":     "M0 stub – field extraction added in M2"
    })
