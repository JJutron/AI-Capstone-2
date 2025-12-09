#es_ltr_online

import os, json, math
from typing import Dict, List, Optional
from collections import defaultdict

from elasticsearch import Elasticsearch
from sentence_transformers import SentenceTransformer
import xgboost as xgb


_ES = None
_MODEL = None
_BOOSTER = None


def get_es():
    global _ES
    if _ES is None:
        es_url = os.getenv("ES_URL", "http://52.78.47.96:9200")
        _ES = Elasticsearch(es_url, request_timeout=30)
    return _ES


def get_model():
    global _MODEL
    if _MODEL is None:
        _MODEL = SentenceTransformer("jhgan/ko-sroberta-multitask")
    return _MODEL


def get_booster():
    global _BOOSTER
    if _BOOSTER is None:
        booster = xgb.Booster()
        booster.load_model("ltr_booster.json")
        _BOOSTER = booster
    return _BOOSTER


CATEGORIES = ["cream", "essence", "skintoner"]

POS_ING = {
    "pigment": {"niacinamide", "비타민c", "arbutin", "트라넥삼산", "감초", "코직"},
    "sensitivity": {"panthenol", "판테놀", "cica", "병풀", "알란토인", "베타글루칸", "알로에", "세라마이드"},
    "dry": {"히알루론산", "글리세린", "스쿠알란", "세라마이드", "콜레스테롤", "요소"},
    "acne": {"살리실산", "바하", "아젤라익", "아연"},
}
NEG_ING = {
    "sensitivity": {"향", "향료", "퍼퓸", "알코올", "에탄올", "에센셜 오일", "티트리 오일"},
    "acne": {"코코넛 오일", "아이소프로필 미리스테이트", "라놀린"},
}

def level_oil(x: float) -> str:
    if x > 1.60:
        return "high"
    elif x > 0.85:
        return "mid"
    else:
        return "low"


def level_sen(x: float) -> str:
    if x > 1.30:
        return "high"
    elif x > 0.90:
        return "mid"
    else:
        return "low"


def level_dry(x: float) -> str:
    if x >= 3:
        return "high"
    elif x >= 2:
        return "mid"
    else:
        return "low"


def level_pig(x: float) -> str:
    if x > 1.65:
        return "high"
    elif x > 1.25:
        return "mid"
    else:
        return "low"


def _make_pos_neg_vocab(fusion):
    idx = fusion["indices"]

    oil = float(idx.get("oil", 0.0))
    dry = float(idx.get("dry", 0.0))
    sens = float(idx.get("sensitivity", 0.0))
    pigment = float(idx.get("pigment", 0.0))

    oil_level = level_oil(oil)
    dry_level = level_dry(dry)
    sen_level = level_sen(sens)
    pig_level = level_pig(pigment)

    pos, neg = set(), set()

    if pig_level == "high":
        pos |= POS_ING["pigment"]

    if sen_level == "high":
        pos |= POS_ING["sensitivity"]
        neg |= NEG_ING["sensitivity"]

    if dry_level == "high":
        pos |= POS_ING["dry"]

    if oil_level == "high":
        pos |= POS_ING["acne"]
        neg |= NEG_ING["acne"]

    return pos, neg


def _mbti_to_text(mbti: str) -> str:
    if not mbti:
        return ""

    mbti = mbti.upper()
    parts = []

    first = {
        "O": "유분이 많은",          
        "D": "건조한",              
    }

    second = {
        "S": "민감한",              
        "R": "저항성이 좋은",       
    }

    third = {
        "P": "색소 침착이 고민인",   
        "N": "색소 침착이 적은",     
    }

    fourth = {
    "W": "주름이 고민인",       
    "T": "탱탱한",              
}

    tables = [first, second, third, fourth]

    for i, ch in enumerate(mbti):
        if i < len(tables) and ch in tables[i]:
            parts.append(tables[i][ch])

    return " ".join(parts)


def search_candidates(fusion, per_cat: int = 15):
    es = get_es()
    model = get_model()

    skin_type = fusion.get("skin_type", "")
    mbti_text = _mbti_to_text(fusion.get("skin_mbti", ""))

    qtext = (skin_type + " " + mbti_text).strip()
    if not qtext:
        qtext = skin_type or "피부"  
    qvec = model.encode(qtext).tolist()

    out: Dict[str, List[dict]] = {}

    for cat in CATEGORIES:
        body = {
            "size": per_cat,
            "query": {
                "bool": {
                    "filter": [{"term": {"category": cat}}],
                    "must": [
                        {
                            "script_score": {
                                "query": {"match_all": {}},
                                "script": {
                                    "source": "cosineSimilarity(params.qvec, 'review_vector') + 1.0",
                                    "params": {"qvec": qvec},
                                },
                            }
                        }
                    ],
                }
            },
            "_source": [
                "product_id",
                "productName",
                "brand",
                "salePrice",
                "ingredients",
                "averageReviewScore",
                "totalReviewCount",
                "image_url",
                "xai_keywords",
            ],
        }

        hits = es.search(index="cosmetics_demo", body=body)["hits"]["hits"]

        cat_rows: List[dict] = []
        for h in hits:
            src = dict(h["_source"])
            src["category"] = cat
            src["score_es"] = h["_score"]
            src["image_url"] = src.get("image_url")
            src["xai_keywords"] = src.get("xai_keywords") or []

            cat_rows.append(src)

        out[cat] = cat_rows

    return out


def featurize(results, fusion):
    pos, neg = _make_pos_neg_vocab(fusion)
    X, group, info = [], [], []

    scale = 0.5 

    for cat in CATEGORIES:
        rows = results.get(cat, [])
        group.append(len(rows))
        for r in rows:
            ings = r.get("ingredients", [])
            pos_hits = sum(1 for x in ings if x in pos)
            neg_hits = sum(1 for x in ings if x in neg)
            avg = r.get("averageReviewScore") or 0.0
            cnt = math.log1p(r.get("totalReviewCount") or 0.0)
            price = math.log1p(r.get("salePrice") or 0.0)
            X.append(
                [
                    pos_hits * scale,
                    neg_hits * scale,
                    (pos_hits - neg_hits) * scale,
                    avg,
                    cnt,
                    price,
                    r["score_es"],
                ]
            )
            info.append(r)
    return X, group, info


def _topk_per_category(ranked: List[dict], k: int = 3) -> List[dict]:
    out: List[dict] = []

    for cat in CATEGORIES:
        cat_items = [item for item in ranked if item.get("category") == cat]
        cat_items.sort(key=lambda x: x.get("score_ltr", 0.0), reverse=True)
        out.extend(cat_items[:k])

    return out


def recommend_for_request(fusion_json: dict, topk: int = 3) -> List[dict]:

    raw = search_candidates(fusion_json, per_cat=15)
    X, groups, info = featurize(raw, fusion_json)

    dtest = xgb.DMatrix(X)
    booster = get_booster()
    preds = booster.predict(dtest)

    ranked: List[dict] = []
    for r, p in zip(info, preds):
        r = dict(r)
        r["score_ltr"] = float(p)
        ranked.append(r)

    ranked.sort(key=lambda x: x["score_ltr"], reverse=True)

    topk_ranked = _topk_per_category(ranked, k=topk)
    
    for item in topk_ranked:
        item["xai_keywords"] = item.get("xai_keywords") or []
        item.pop("review_text", None)
        item.pop("review_vector", None)

    return topk_ranked
