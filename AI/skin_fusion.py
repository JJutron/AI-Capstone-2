# skin_fusion

import os, json, mimetypes
from typing import Dict
import cv2, numpy as np
from PIL import Image, ImageOps
from google import genai
from google.genai import types

TARGET_SHORT = 768
MODEL = "gemini-2.0-flash"

PROMPT_TEXT = (
    "전처리된 얼굴 피부 이미지를 분석하여 JSON만 반환하라.\n"
    "평가 항목: acne(여드름), redness(홍조), melasma_darkspots(잡티), wrinkle(주름).\n"
    "각 항목은 다음 스키마로 제공하라:\n"
    "{"
    "acne:{score:number,reason:string}, "
    "redness:{score:number,reason:string}, "
    "melasma_darkspots:{score:number,reason:string}, "
    "wrinkle:{score:number,reason:string}"
    "}\n"
    "score는 0~100 범위의 실수이며, "
    "0은 없음·매우 양호, 100은 매우 심함을 의미한다. "
    "wrinkle.score는 잔주름과 깊은 주름의 분포와 강도를 종합적으로 반영하라."
)


def _load_exif_bgr(path: str):
    pil = Image.open(path)
    pil = ImageOps.exif_transpose(pil).convert("RGB")
    return cv2.cvtColor(np.array(pil), cv2.COLOR_RGB2BGR)

def _resize_short(bgr, short=TARGET_SHORT):
    h, w = bgr.shape[:2]
    s = min(h, w)
    if s == short:
        return bgr
    scale = short / float(s)
    new = (int(round(w * scale)), int(round(h * scale)))
    interp = cv2.INTER_AREA if scale < 1.0 else cv2.INTER_CUBIC
    return cv2.resize(bgr, new, interpolation=interp)

def _wb_grayworld(bgr, strength=0.5):
    x = bgr.astype(np.float32)
    means = x.reshape(-1, 3).mean(0) + 1e-6
    g = means.mean()
    gains = np.clip(g / means, 0.8, 1.2)
    gains = (1 - strength) * 1.0 + strength * gains
    x *= gains
    return np.clip(x, 0, 255).astype(np.uint8)

def _clahe_light(bgr, clip=1.8, tiles=8):
    lab = cv2.cvtColor(bgr, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    l = cv2.createCLAHE(clipLimit=clip, tileGridSize=(tiles, tiles)).apply(l)
    return cv2.cvtColor(cv2.merge((l, a, b)), cv2.COLOR_LAB2BGR)

def _morph_kernel(bgr, base: int = 768, ksize: int = 5):
    h, w = bgr.shape[:2]
    scale = min(h, w) / float(base)
    k = max(3, int(round(ksize * scale)))
    if k % 2 == 0:
        k += 1
    return cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (k, k))

def _skin_mask(bgr):
    ycrcb = cv2.cvtColor(bgr, cv2.COLOR_BGR2YCrCb)
    Y, Cr, Cb = cv2.split(ycrcb)
    m1 = (
        (Cr >= 135)
        & (Cr <= 180)
        & (Cb >= 85)
        & (Cb <= 135)
        & (Y >= 40)
        & (Y <= 240)
    )

    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    H, S, V = cv2.split(hsv)
    m2 = (H <= 25) & (S >= 30) & (S <= 180) & (V >= 60)

    m = (m1 & m2).astype(np.uint8) * 255

    k = _morph_kernel(bgr)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, k, iterations=1)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, k, iterations=2)

    num, labels, stats, _ = cv2.connectedComponentsWithStats(m, connectivity=8)
    if num > 1:
        largest = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
        m2 = np.zeros_like(m)
        m2[labels == largest] = 255
        m = m2
    return m

def preprocess_with_mask(bgr, bg_gray=220):
    bgr = _resize_short(bgr)
    bgr = _wb_grayworld(bgr, 0.5)
    bgr = _clahe_light(bgr, 1.8, 8)
    mask = _skin_mask(bgr)
    bg = np.full_like(bgr, (bg_gray, bg_gray, bg_gray), np.uint8)
    out = np.where(mask[..., None] == 255, bgr, bg)
    return out


def analyze_with_gemini(image_path: str, api_key: str) -> Dict:
    client = genai.Client(api_key=api_key)
    mime = mimetypes.guess_type(image_path)[0] or "image/jpeg"

    with open(image_path, "rb") as f:
        img_bytes = f.read()

    content = types.Content(
        role="user",
        parts=[
            types.Part(text=PROMPT_TEXT),
            types.Part(inline_data=types.Blob(mime_type=mime, data=img_bytes)),
        ],
    )

    resp = client.models.generate_content(
        model=MODEL,
        contents=[content],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            temperature=0.2,
            system_instruction="너는 피부 분석 전문가다. 반드시 JSON만 반환하라.",
        ),
    )

    txt = (resp.text or "").strip()
    start, end = txt.find("{"), txt.rfind("}")
    if start == -1 or end == -1:
        return {}
    try:
        return json.loads(txt[start : end + 1])
    except Exception:
        return {}


MAP: Dict[str, Dict[str, int]] = {
    "q1": {"없어요": 0, "T존 일부(이마 혹은 코)": 1, "T존 전체(이마와 코)": 2, "얼굴 전체": 3},
    "q2": {
        "전혀 안 보여요": 0,
        "지금은 없지만 가끔 보여요": 1,
        "부분적으로 붉게 보여요": 2,
        "전체적으로 붉게 보여요": 3,
    },
    "q3": {"없어요": 0, "U존 일부(볼 혹은 턱)": 1, "U존 전체(볼과 턱)": 2, "얼굴 전체": 3},
    "q4": {
        "전혀 생기지 않아요": 0,
        "표정을 지을 때만 생겨요": 1,
        "표정 짓지 않아도 약간 있어요": 2,
        "표정 짓지 않아도 많이 있어요": 3,
    },
    "q5": {
        "주름이 없어요": 0,
        "잔주름이에요": 1,
        "깊은 주름이에요": 2,
        "잔주름과 깊은 주름 다 있어요": 3,
    },
    "q6": {
        "전혀 생기지 않아요": 0,
        "미소 지을 때만 약간 생겨요": 1,
        "미소 지을 때 진하게 생겨요": 2,
        "미소 짓지 않아도 생겨요": 3,
    },
    "q7": {
        "전혀 안 보여요": 0,
        "거의 안 보여요": 1,
        "약간 눈에 띄어요": 2,
        "곳곳에 많이 보여요": 3,
    },
    "q8": {
        "주름이 없어요": 0,
        "잔주름이에요": 1,
        "깊은 주름이에요": 2,
        "잔주름과 깊은 주름 다 있어요": 3,
    },
    "q9": {
        "외출 전보다 윤기가 없어요": 0,
        "외출 전과 변함이 없어요": 1,
        "약간 번들거리고 윤기가 있어요": 2,
        "많이 번들거리고 기름져요": 3,
    },
    "q10": {
        "전혀 안 보여요": 0,
        "가끔 붉어지면 보여요": 1,
        "특정부위에 눈에 띄어요": 2,
        "곳곳에 많이 보여요": 3,
    },
}

def _to_0_3(x):
    try:
        v = float(x)
    except Exception:
        v = 0.0
    return round(max(0, min(100, v)) / 100 * 3, 2)

def _decide_skin_type(oil: float, dry: float) -> str:
    if oil >= 2 and dry <= 1:
        return "지성"
    if dry >= 2 and oil <= 1:
        return "건성"
    if oil >= 2 and dry >= 2:
        return "복합성"
    return "중성"
    

def compute_skin_mbti(indices: Dict[str, float]) -> str:
    oil = float(indices.get("oil", 0.0))
    sens = float(indices.get("sensitivity", 0.0))
    pigment = float(indices.get("pigment", 0.0))
    wrinkle = float(indices.get("wrinkle", 0.0))

    od = "O" if oil >= 1.5 else "D"
    sr = "S" if sens >= 1.5 else "R"
    pn = "P" if pigment >= 1.5 else "N"
    aw = "A" if wrinkle >= 1.5 else "W"

    return od + sr + pn + aw

def skin_mbti_from_fusion(fusion: Dict) -> str:
    return compute_skin_mbti(fusion.get("indices", {}))


def assess_skin_type_from_survey(answers: Dict[str, str]) -> Dict:
    s = {k: MAP[k].get(v, 0) for k, v in answers.items()}

    oil = round(0.6 * s["q1"] + 0.4 * s["q9"], 2)
    dry = float(s["q3"])
    sens = round(0.7 * s["q2"] + 0.3 * s["q10"], 2)
    wrinkle = round(0.4 * s["q4"] + 0.6 * ((s["q5"] + s["q8"]) / 2), 2)
    pigment = float(s["q7"])

    skin = _decide_skin_type(oil, dry)

    return {
        "skin_type": skin,
        "indices": {
            "oil": oil,
            "dry": dry,
            "sensitivity": sens,
            "wrinkle": wrinkle,
            "pigment": pigment,
        },
    }

def _safe_score(gemini: dict, key: str) -> float:
    try:
        return _to_0_3(gemini.get(key, {}).get("score", 0))
    except Exception:
        return 0.0

def assess_with_gemini(survey_dict: Dict, gemini: Dict) -> Dict:
    answers = survey_dict["survey"] if "survey" in survey_dict else survey_dict

    base = assess_skin_type_from_survey(answers)
    idx = base["indices"]
    fused = dict(idx)

    acne = _safe_score(gemini, "acne")
    red = _safe_score(gemini, "redness")
    mel = _safe_score(gemini, "melasma_darkspots")
    wrinkle_v = _safe_score(gemini, "wrinkle")

    fused["sensitivity"] = round(0.4 * idx["sensitivity"] + 0.6 * red, 2)
    fused["pigment"] = round(0.3 * idx["pigment"] + 0.7 * mel, 2)
    fused["oil"] = round(0.7 * idx["oil"] + 0.3 * acne, 2)
    fused["dry"] = idx["dry"]
    fused["wrinkle"] = round(0.6 * idx["wrinkle"] + 0.4 * wrinkle_v, 2)

    skin = _decide_skin_type(fused["oil"], fused["dry"])
    mbti = compute_skin_mbti(fused)

    return {
        "skin_type": skin,
        "skin_mbti": mbti,
        "indices": fused,
        "vision_raw": gemini,
    }

def run_fusion_from_request(image_path: str, survey_dict: Dict) -> Dict:
    bgr = _load_exif_bgr(image_path)
    processed = preprocess_with_mask(bgr)

    root, ext = os.path.splitext(image_path)
    if not ext:
        ext = ".jpg"
    pre_path = f"{root}_Pre{ext}"
    cv2.imwrite(pre_path, processed)

    api_key = os.getenv("GEMINI_API_KEY")
    gemini = analyze_with_gemini(pre_path, api_key)

    fusion = assess_with_gemini(survey_dict, gemini)
    return fusion