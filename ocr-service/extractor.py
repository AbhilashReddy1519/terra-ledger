import re
import io
import cv2
import numpy as np
from PIL import Image
import pytesseract
import pypdfium2 as pdfium


def preprocess_image(pil_img: Image.Image) -> np.ndarray:
    """Preprocess PIL image using OpenCV for optimal Tesseract OCR accuracy."""
    img_array = np.array(pil_img.convert("RGB"))
    gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
    
    # Scale up if image is low-res
    h, w = gray.shape
    if w < 1000:
        scale = 1000 / w
        gray = cv2.resize(gray, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)

    # Slight blur to remove scan noise, followed by Otsu's binarization
    blurred = cv2.GaussianBlur(gray, (3, 3), 0)
    thresh = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
    return thresh


def extract_images_from_file(content_bytes: bytes, filename: str) -> list[Image.Image]:
    """Convert input file (image or PDF) into a list of PIL Images."""
    lower_name = filename.lower()
    if lower_name.endswith(".pdf") or content_bytes.startswith(b"%PDF"):
        pdf = pdfium.PdfDocument(content_bytes)
        images = []
        for page_idx in range(len(pdf)):
            page = pdf[page_idx]
            pil_image = page.render(scale=2.0).to_pil()
            images.append(pil_image)
        return images
    else:
        # Standard image (PNG, JPG, TIFF, etc.)
        img = Image.open(io.BytesIO(content_bytes))
        return [img]


def parse_fields_from_text(raw_text: str) -> dict:
    """
    Extract structured land record fields from raw OCR text using robust patterns.
    """
    fields = {
        "deed_type": "",
        "owner_name": "",
        "survey_no": "",
        "area": "",
        "boundaries": "",
        "registration_date": "",
        "stamp_duty": ""
    }

    # 1. Deed Type
    m = re.search(r"DEED\s*TYPE\s*[:\-]\s*([^\r\n]+)", raw_text, re.IGNORECASE)
    if m:
        fields["deed_type"] = m.group(1).strip()
    else:
        for dtype in ["Sale Deed", "Gift Deed", "Partition Deed", "Conveyance Deed", "Title Deed", "Settlement Deed"]:
            if re.search(rf"\b{dtype}\b", raw_text, re.IGNORECASE):
                fields["deed_type"] = dtype
                break

    # 2. Owner Name
    m = re.search(r"OWNER\s*(?:NAME)?\s*[:\-]\s*([A-Za-z\s\.\,]+?)(?=(?:\r|\n|SURVEY|EXTENT|AREA|RECORD|$))", raw_text, re.IGNORECASE)
    if m:
        fields["owner_name"] = m.group(1).strip()

    # 3. Survey No / Plot No / Khasra No
    m = re.search(r"(?:SURVEY\s*(?:NO|NUMBER)?|PLOT\s*(?:NO|NUMBER)?|KHASRA\s*(?:NO|NUMBER)?)\s*[:\-]\s*([A-Za-z0-9/\-_]+)", raw_text, re.IGNORECASE)
    if m:
        fields["survey_no"] = m.group(1).strip()

    # 4. Extent / Area
    m = re.search(r"(?:EXTENT\s*/\s*AREA|EXTENT|AREA)\s*[:\-]\s*([0-9\.,]+\s*(?:Sq\.?\s*(?:Ft|Feet|Yards|Meters)?|Acres|Guntas|Hectares|Cents)?)", raw_text, re.IGNORECASE)
    if m:
        fields["area"] = m.group(1).strip()

    # 5. Boundaries
    m = re.search(r"BOUNDARIES\s*[:\-]\s*([^\r\n]+)", raw_text, re.IGNORECASE)
    if m:
        val = m.group(1).strip()
        val = re.sub(r"^BOUNDARIES\s*[:\-]\s*", "", val, flags=re.IGNORECASE)
        fields["boundaries"] = val
    else:
        m2 = re.search(r"(North:[\s\S]+?)(?=(?:THIS\s+DEED|IN\s+WITNESS|SUB-REGISTRAR|\r?\n\r?\n|$))", raw_text, re.IGNORECASE)
        if m2:
            val = re.sub(r"\s+", " ", m2.group(1)).strip()
            val = re.sub(r"^BOUNDARIES\s*[:\-]\s*", "", val, flags=re.IGNORECASE)
            fields["boundaries"] = val

    # 6. Registration Date
    m = re.search(r"(?:REGISTRATION\s+DATE|DATE)\s*[:\-]\s*(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4})", raw_text, re.IGNORECASE)
    if m:
        fields["registration_date"] = m.group(1).strip()

    # 7. Stamp Duty
    m = re.search(r"(?:STAMP\s+DUTY(?:\s*\(INR\))?|DUTY)\s*[:\-]\s*(?:INR|Rs\.?)?\s*([0-9,]+)", raw_text, re.IGNORECASE)
    if m:
        fields["stamp_duty"] = m.group(1).replace(",", "").strip()

    return fields


def extract_document(content_bytes: bytes, filename: str) -> dict:
    images = extract_images_from_file(content_bytes, filename)
    all_raw_text = []
    confidences = []

    for img in images:
        processed = preprocess_image(img)
        
        # Get OCR text
        text = pytesseract.image_to_string(processed)
        all_raw_text.append(text)

        # Get OCR confidence
        data = pytesseract.image_to_data(processed, output_type=pytesseract.Output.DICT)
        confs = [int(c) for c in data["conf"] if str(c).lstrip("-").isdigit() and int(c) >= 0]
        if confs:
            confidences.append(sum(confs) / len(confs))

    full_text = "\n".join(all_raw_text)
    avg_conf = (sum(confidences) / len(confidences)) / 100.0 if confidences else 0.85
    fields = parse_fields_from_text(full_text)

    return {
        "raw_text": full_text,
        "confidence": round(avg_conf, 3),
        "fields": fields
    }
