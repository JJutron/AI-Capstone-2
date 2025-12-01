# app.py

from fastapi import FastAPI, Form
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import json
import os
from typing import Dict

import httpx

from skin_fusion import run_fusion_from_request
from es_ltr_online import recommend_for_request


app = FastAPI(
    title="Vegan Cosmetics Recommendation API",
    description="피부 분석 + 제품 추천 서비스",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 분석 + LTR
@app.post("/analyze-and-recommend")
async def analyze_and_recommend(
    image_url: str = Form(...),
    survey: str = Form(...)
):

    # image_url 입력 검증
    if not image_url.startswith("http://") and not image_url.startswith("https://"):
        return {
            "error": "image_url은 반드시 http:// 또는 https:// 로 시작해야 합니다.",
            "detail": f"입력된 값: {image_url}"
        }

    os.makedirs("uploads", exist_ok=True)

    # URL에서 이미지 다운로드
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(image_url)
            resp.raise_for_status()

        base_name = os.path.basename(image_url.split("?")[0]) or "url_image.jpg"
        img_path = os.path.join("uploads", base_name)

        with open(img_path, "wb") as f:
            f.write(resp.content)

    except Exception as e:
        return {
            "error": "image_url에서 이미지를 가져오는 데 실패했습니다.",
            "detail": str(e),
        }

    # 설문 JSON 파싱
    try:
        survey_dict: Dict = json.loads(survey)
    except Exception:
        return {"error": "설문 JSON 형식 오류"}

    # 분석 파이프라인
    fusion = run_fusion_from_request(img_path, survey_dict)

    # LTR 추천
    recommendations = recommend_for_request(fusion, topk=3)

    return {
        "status": "success",
        "fusion": fusion,
        "recommendations": recommendations,
        "used_image": image_url
    }


# 헬스체크
@app.get("/health")
def health_check():
    return {"status": "ok"}


