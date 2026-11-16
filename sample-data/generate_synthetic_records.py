import os
import json
from PIL import Image, ImageDraw, ImageFont

DATASET = [
    {
        "id": "REC-001",
        "filename": "record_01_sale_deed.png",
        "deed_type": "Sale Deed",
        "owner_name": "Ramesh Kumar Sharma",
        "survey_no": "SY-142/3A",
        "area": "2400 Sq. Ft.",
        "boundaries": "North: 40ft Main Road, South: Plot 15, East: Sy 142/3B, West: Private Property",
        "registration_date": "2024-03-15",
        "stamp_duty": "125000"
    },
    {
        "id": "REC-002",
        "filename": "record_02_gift_deed.png",
        "deed_type": "Gift Deed",
        "owner_name": "Sunita Devi",
        "survey_no": "SY-88/1B",
        "area": "1800 Sq. Ft.",
        "boundaries": "North: Municipal Park, South: Sy 88/1C, East: 30ft Road, West: Open Plot 12",
        "registration_date": "2023-11-20",
        "stamp_duty": "45000"
    },
    {
        "id": "REC-003",
        "filename": "record_03_partition_deed.png",
        "deed_type": "Partition Deed",
        "owner_name": "Vikramaditya Rao",
        "survey_no": "PLOT-42A",
        "area": "3200 Sq. Ft.",
        "boundaries": "North: Sector 4 Road, South: Plot 42-B, East: Plot 41, West: Drainage Canal",
        "registration_date": "2024-01-10",
        "stamp_duty": "80000"
    },
    {
        "id": "REC-004",
        "filename": "record_04_conveyance_deed.png",
        "deed_type": "Conveyance Deed",
        "owner_name": "Ananya Deshmukh",
        "survey_no": "KHASRA-512/7",
        "area": "1.25 Acres",
        "boundaries": "North: River Canal, South: Agricultural Land, East: Village Road, West: Forest Border",
        "registration_date": "2023-08-05",
        "stamp_duty": "210000"
    },
    {
        "id": "REC-005",
        "filename": "record_05_title_deed.png",
        "deed_type": "Title Deed",
        "owner_name": "Mohammed Farooq Khan",
        "survey_no": "SY-209/4",
        "area": "4500 Sq. Ft.",
        "boundaries": "North: National Highway 44, South: Sy 209/5, East: Service Lane, West: Sy 208",
        "registration_date": "2024-05-22",
        "stamp_duty": "315000"
    },
    {
        "id": "REC-006",
        "filename": "record_06_sale_deed.png",
        "deed_type": "Sale Deed",
        "owner_name": "Priya Sunderraj",
        "survey_no": "PLOT-108",
        "area": "1500 Sq. Ft.",
        "boundaries": "North: 20ft Cross Road, South: Plot 109, East: Plot 105, West: Avenue Road",
        "registration_date": "2024-02-18",
        "stamp_duty": "95000"
    },
    {
        "id": "REC-007",
        "filename": "record_07_settlement_deed.png",
        "deed_type": "Settlement Deed",
        "owner_name": "Harpreet Singh Gill",
        "survey_no": "SY-73/2",
        "area": "2.50 Acres",
        "boundaries": "North: Farm Road, South: Canal Waterway, East: Sy 73/3, West: Sy 72/1",
        "registration_date": "2023-09-30",
        "stamp_duty": "150000"
    },
    {
        "id": "REC-008",
        "filename": "record_08_sale_deed.png",
        "deed_type": "Sale Deed",
        "owner_name": "Rajeshwari Venkatesh",
        "survey_no": "SY-315/1A",
        "area": "2800 Sq. Ft.",
        "boundaries": "North: Layout Boundary, South: Internal Road, East: Plot 22, West: Sy 315/1B",
        "registration_date": "2024-06-12",
        "stamp_duty": "160000"
    },
    {
        "id": "REC-009",
        "filename": "record_09_conveyance_deed.pdf",
        "deed_type": "Conveyance Deed",
        "owner_name": "Amit Arvind Joshi",
        "survey_no": "PLOT-87C",
        "area": "2000 Sq. Ft.",
        "boundaries": "North: Sector 9 Park, South: Sector Road, East: Plot 87-B, West: Plot 88",
        "registration_date": "2024-04-01",
        "stamp_duty": "110000"
    },
    {
        "id": "REC-010",
        "filename": "record_10_gift_deed.pdf",
        "deed_type": "Gift Deed",
        "owner_name": "Lakshmi Narayanan",
        "survey_no": "KHASRA-94/6",
        "area": "3600 Sq. Ft.",
        "boundaries": "North: Panchayat Road, South: Khasra 94/7, East: Khasra 95, West: Water Tank",
        "registration_date": "2023-12-14",
        "stamp_duty": "60000"
    }
]

def draw_synthetic_deed(item, out_path):
    width, height = 1400, 1900
    img = Image.new("RGB", (width, height), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)

    # Outer and inner border
    draw.rectangle([(30, 30), (width - 30, height - 30)], outline=(40, 40, 40), width=4)
    draw.rectangle([(45, 45), (width - 45, height - 45)], outline=(100, 100, 100), width=2)

    # Load high-res TrueType fonts
    font_path = "C:/Windows/Fonts/arial.ttf"
    if not os.path.exists(font_path):
        font_path = "arial.ttf"
    
    try:
        font_title = ImageFont.truetype(font_path, 32)
        font_header = ImageFont.truetype(font_path, 26)
        font_bold = ImageFont.truetype(font_path, 24)
        font_normal = ImageFont.truetype(font_path, 22)
    except Exception:
        font_title = ImageFont.load_default()
        font_header = ImageFont.load_default()
        font_bold = ImageFont.load_default()
        font_normal = ImageFont.load_default()

    # Header / Government banner
    draw.rectangle([(50, 50), (width - 50, 180)], fill=(245, 247, 250))
    draw.text((220, 65), "GOVERNMENT OF LAND ADMINISTRATION & REGISTRATION", fill=(10, 10, 10), font=font_title)
    draw.text((400, 110), "DEPARTMENT OF REVENUE & LAND TITLES", fill=(50, 50, 50), font=font_header)
    draw.text((430, 145), "CERTIFICATE OF REGISTERED TITLE DEED", fill=(20, 20, 120), font=font_header)

    draw.line([(50, 190), (width - 50, 190)], fill=(0, 0, 0), width=3)

    # Document details block
    y = 230
    draw.text((100, y), f"RECORD IDENTIFIER : {item['id']}", fill=(0, 0, 0), font=font_bold)
    y += 50
    draw.text((100, y), f"DEED TYPE : {item['deed_type'].upper()}", fill=(0, 0, 0), font=font_bold)
    y += 50
    draw.text((100, y), f"OWNER NAME : {item['owner_name']}", fill=(0, 0, 0), font=font_bold)
    y += 50
    draw.text((100, y), f"SURVEY NO : {item['survey_no']}", fill=(0, 0, 0), font=font_bold)
    y += 50
    draw.text((100, y), f"EXTENT / AREA : {item['area']}", fill=(0, 0, 0), font=font_bold)
    y += 50
    draw.text((100, y), f"REGISTRATION DATE : {item['registration_date']}", fill=(0, 0, 0), font=font_bold)
    y += 50
    draw.text((100, y), f"STAMP DUTY (INR) : {item['stamp_duty']}", fill=(0, 0, 0), font=font_bold)
    y += 60

    draw.line([(100, y), (width - 100, y)], fill=(160, 160, 160), width=2)
    y += 40

    # Boundaries section
    draw.text((100, y), "SCHEDULE OF PROPERTY & BOUNDARIES:", fill=(0, 0, 0), font=font_bold)
    y += 45
    draw.text((120, y), f"BOUNDARIES : {item['boundaries']}", fill=(20, 20, 20), font=font_normal)
    y += 80

    # Legal preamble & clause
    legal_text = [
        "THIS DEED WITNESSETH that in consideration of the stamp duty duly discharged and verified,",
        "the absolute title, peaceful ownership, and possession of the aforesaid schedule property are hereby",
        f"vested in the name of the registered owner {item['owner_name']}, free from all encumbrances, charges,",
        "mortgages, and claims whatsoever, subject to statutory land revenue laws and municipal regulations.",
        "",
        "IN WITNESS WHEREOF the executing parties have affixed their signatures on this registered deed",
        f"on this date {item['registration_date']} in the presence of the Sub-Registrar and official witnesses."
    ]

    for line in legal_text:
        draw.text((100, y), line, fill=(40, 40, 40), font=font_normal)
        y += 40

    # Stamps and Seal
    y = height - 320
    draw.rectangle([(120, y), (420, y + 180)], outline=(0, 100, 0), width=3)
    draw.text((140, y + 30), "SUB-REGISTRAR OFFICE", fill=(0, 100, 0), font=font_bold)
    draw.text((140, y + 70), "OFFICIALLY RECORDED", fill=(0, 100, 0), font=font_normal)
    draw.text((140, y + 110), f"DATE: {item['registration_date']}", fill=(0, 100, 0), font=font_normal)

    draw.rectangle([(width - 450, y), (width - 120, y + 180)], outline=(120, 0, 0), width=3)
    draw.text((width - 420, y + 30), "STAMP DUTY PAID", fill=(120, 0, 0), font=font_bold)
    draw.text((width - 420, y + 70), f"DUTY: INR {item['stamp_duty']}", fill=(120, 0, 0), font=font_normal)
    draw.text((width - 420, y + 110), "VERIFIED OFFICIAL SEAL", fill=(120, 0, 0), font=font_normal)

    # Save
    if out_path.endswith(".pdf"):
        img.save(out_path, "PDF", resolution=150.0)
    else:
        img.save(out_path, "PNG")

def main():
    os.makedirs("sample-data", exist_ok=True)
    expected_records = {}

    for item in DATASET:
        out_path = os.path.join("sample-data", item["filename"])
        draw_synthetic_deed(item, out_path)
        expected_records[item["id"]] = item
        print(f"Generated: {out_path}")

    with open("sample-data/expected_extractions.json", "w") as f:
        json.dump(expected_records, f, indent=2)
    print("Saved sample-data/expected_extractions.json")

if __name__ == "__main__":
    main()
