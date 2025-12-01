import os
import json
import re
import hashlib
from collections import defaultdict
from typing import Any, Dict, List, Optional

from elasticsearch import Elasticsearch, helpers
from sentence_transformers import SentenceTransformer

INDEX_NAME = "cosmetics_demo"

def connect_es() -> Elasticsearch:
    return Elasticsearch("http://3.37.61.177:9200", request_timeout=60)

EMB_MODEL_NAME = "jhgan/ko-sroberta-multitask"
emb_model = SentenceTransformer(EMB_MODEL_NAME)


def load_json(path: str) -> List[Dict[str, Any]]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def normalize_ingredients(raw: Optional[str]) -> List[str]:
    if not raw:
        return []
    parts = [p.strip() for p in raw.split(",")]
    return [p for p in parts if p]


def build_review_text(item: Dict[str, Any], max_reviews: int = 50) -> str:
    reviews = item.get("reviews") or []
    texts: List[str] = []
    for r in reviews[:max_reviews]:
        txt = r.get("reviewContent") or r.get("reviewTitle") or ""
        txt = str(txt).strip()
        if txt:
            texts.append(txt)
    if not texts:
        name = item.get("productName") or ""
        return str(name)
    return "\n".join(texts)


def extract_image_url(item: Dict[str, Any]) -> Optional[str]:
    images = item.get("images") or []
    if isinstance(images, list) and images:
        first = images[0]
        if isinstance(first, dict):
            url = first.get("imageUrl") or first.get("image_url")
            if url:
                return url
    return item.get("imageUrl") or item.get("image_url")


def make_product_id(item: Dict[str, Any], category: str) -> str:
    for key in ["nvMid", "productId", "id"]:
        if key in item:
            return f"{category}_{item[key]}"
    base = f"{category}|{item.get('productName','')}|{item.get('mallName','')}"
    return hashlib.md5(base.encode("utf-8")).hexdigest()


def recreate_index(es: Elasticsearch):
    if es.indices.exists(index=INDEX_NAME):
        print(f"[INFO] 기존 인덱스 {INDEX_NAME} 삭제 중...")
        es.indices.delete(index=INDEX_NAME)

    print(f"[INFO] 인덱스 {INDEX_NAME} 생성 중...")
    body = {
        "mappings": {
            "properties": {
                "product_id": {"type": "keyword"},
                "category": {"type": "keyword"},
                "productName": {
                    "type": "text",
                    "fields": {"keyword": {"type": "keyword"}}
                },
                "brand": {"type": "keyword"},
                "salePrice": {"type": "float"},
                "averageReviewScore": {"type": "float"},
                "totalReviewCount": {"type": "integer"},
                "ingredients": {"type": "keyword"},
                "review_text": {"type": "text"},
                "review_vector": {
                    "type": "dense_vector",
                    "dims": 768,
                    "index": True,
                    "similarity": "cosine",
                },
                "image_url": {"type": "keyword"},
                "xai_keywords": {"type": "keyword"},
            }
        }
    }
    es.indices.create(index=INDEX_NAME, body=body)
    print("[INFO] 인덱스 생성 완료.")


def build_doc(item: Dict[str, Any], category: str) -> Dict[str, Any]:
    ing_raw = (
        (item.get("ingredientsInfo") or {}).get("ingredients")
        or item.get("ingredients")
    )
    ingredients = normalize_ingredients(ing_raw)
    review_text = build_review_text(item)
    review_vec = emb_model.encode(review_text).tolist()
    image_url = extract_image_url(item)

    doc = {
        "product_id": make_product_id(item, category),
        "category": category,
        "productName": item.get("productName"),
        "brand": item.get("mallName"),
        "salePrice": item.get("salePrice"),
        "averageReviewScore": item.get("averageReviewScore"),
        "totalReviewCount": item.get("totalReviewCount"),
        "ingredients": ingredients,
        "review_text": review_text,
        "review_vector": review_vec,
        "image_url": image_url,
    }
    return doc


def bulk_index(es: Elasticsearch, json_path: str, category: str):
    data = load_json(json_path)
    print(f"[INFO] {json_path} ({category}) 문서 수: {len(data)}")

    actions = []
    for item in data:
        doc = build_doc(item, category)
        actions.append(
            {
                "_index": INDEX_NAME,
                "_id": doc["product_id"],
                "_source": doc,
            }
        )

    helpers.bulk(es, actions, request_timeout=600)
    print(f"[INFO] {category} 인덱싱 완료. 문서 수: {len(actions)}")


TOKEN_RE = re.compile(r"[가-힣A-Za-z0-9]{2,}")

STOPWORDS = {
    # 일반적인 말투 / 조사 / 강조
    "정말", "너무", "진짜", "그냥", "좀", "조금", "약간",
    "그리고", "그래서", "하지만", "근데", "이런", "저런",
    "이제", "다시", "오늘", "내일", "어제",
    "아주", "계속", "많이", "거의", "살짝",
    "아직",

    # 구매/배송/가격 등 설명에 큰 도움 안 되는 것들
    "사용", "제품", "구매", "배송", "포장", "가격",
    "판매", "주문", "재구매", "서비스",
    "가성비", "세일", "할인",

    # 시간/횟수
    "처음", "이번", "예전", "요즘", "항상", "매번", "며칠", "하루",
    "한번", "전에", "요즘에",

    # 문장 끝/동사 활용
    "좋아요", "좋았어요", "좋네요", "좋습니다", "좋았습니",
    "좋고", "좋아서", "좋은", "좋다고",
    "느낌이", "느낌",
    "있어요", "있습니다", "있어", "있는",
    "됩니다", "됐어요", "됐습니다",
    "했어요", "하네요", "합니다", "해요",
    "사용중", "사용해요", "사용하고", "사용중입니다",
    "쓰고있어요", "쓰고있습니다", "쓰고",
    "쓰면", "써보고", "쓰기",

    # 바르다/사용하다 동사류
    "발라요", "발랐어요", "바르면", "바르고", "바를때",
    "발라줬어요", "발라줍니다", "발라주고",

    # 너무 일반적인 화장품/피부 단어
    "피부", "피부가", "피부에", "피부는", "피부를",
    "얼굴", "얼굴이",
    "크림", "에센스", "토너", "로션", "스킨",
    "세럼", "앰플", "마스크", "팩", "기초", "화장품",
    "수분크림",

    # 상품 메타 관련
    "제품이", "제품은", "제품이라", "제품이에요", "제품입니다",
    "상품", "상품이", "비건",

    # 리뷰 관용구 / 형식적인 말
    "꾸준히", "만족합니다", "만족해요", "만족스럽고", "만족스러워요",
    "효과가", "효과는", "효과도", "효과를",
    "도움이", "도움", "감사합니다",

    # 기타
    "빠른", "빠르게", "빨리",
}


STEM_PATTERNS = [
    (re.compile(r"^(촉촉)[가-힣]*$"), "촉촉"),
    (re.compile(r"^(산뜻)[가-힣]*$"), "산뜻"),
    (re.compile(r"^(가볍)[가-힣]*$"), "가볍"),
    (re.compile(r"^(무겁)[가-힣]*$"), "무겁"),
    (re.compile(r"^(끈적)[가-힣]*$"), "끈적"),
    (re.compile(r"^(보송)[가-힣]*$"), "보송"),
    (re.compile(r"^(쫀쫀)[가-힣]*$"), "쫀쫀"),
]


def _normalize_token(tok: str) -> str:
    for pat, base in STEM_PATTERNS:
        if pat.match(tok):
            return base
    return tok


def tokenize(text: str):
    raw = TOKEN_RE.findall(text.lower())
    tokens = []
    for t in raw:
        if re.search(r"\d", t):
            continue
        if t in STOPWORDS:
            continue
        norm = _normalize_token(t)
        tokens.append(norm)
    return tokens


# 효과/고민 관련 키워드 (제형 제외)
EFFECT_KEYWORDS = {
    # 보습/수분
    "보습", "보습력", "보습감",
    "수분", "수분감",

    # 진정/쿨링
    "진정", "진정이", "진정효과", "진정되는", "진정되고",
    "쿨링", "쿨링감", "시원",

    # 고민 계열
    "트러블", "여드름",
    "민감", "민감성", "예민", "예민한",
    "자극", "저자극",

    "건조", "속건조", "각질",
    "유분", "유분감", "번들거림",
    "모공", "피지",

    "미백", "톤업", "기미", "잡티",
    "탄력", "주름",

    # 피부 타입
    "지성", "건성", "복합성", "수부지", "중성",
}


# 사용감(텍스처/센서리) 키워드
FEELING_KEYWORDS = {
    "촉촉", "쫀쫀", "산뜻",
    "끈적", "끈적임",
    "보송",
    "가볍", "가볍게", "가벼운",
    "무겁", "무거운",
    "리치",
    "시원", "쿨링",
}


# 대표적인 성분 키워드
INGREDIENT_KEYWORDS = {
    "나이아신아마이드", "비타민c", "트라넥삼산",
    "레티놀", "아데노신",
    "히알루론산", "판테놀", "세라마이드", "콜레스테롤", "스쿠알란",
    "병풀", "시카", "센텔라", "마데카소사이드", "알란토인", "알로에",
    "녹차", "프로폴리스",
    "살리실산", "bha", "aha",
}


NEGATIVE_PROBLEM_TOKENS = {
    "여드름", "트러블",
    "지성", "건성", "복합성", "수부지",
    "민감", "민감성", "예민", "예민한",
    "유분", "유분감",
    "번들거림",
    "건조", "속건조",
    "각질",
    "당김", "속당김",
    "악건성",
    "자극",
    "가려움", "가렵다", "가렵고",
    "따가움", "따갑다", "따가워요",
    "홍조", "붉은기",
    "모공", "피지",
    "기미", "잡티", "색소침착",
    "주름", "팔자주름",
}


TAIL_CANON = {
    "완화": "완화",
    "완화되는": "완화",
    "완화된": "완화",
    "개선": "개선",
    "개선되는": "개선",
    "개선되고": "개선",
    "케어": "케어",
    "진정": "진정",
    "진정되는": "진정",
    "진정되고": "진정",
    "줄어들고": "개선",
    "줄어든": "개선",
    "줄어들었어요": "개선",
    "줄었어요": "개선",
    "없어졌어요": "완화",
    "사라졌어요": "완화",
    "옅어지는": "완화",
    "옅어졌어요": "완화",
    "좋아졌어요": "개선",
    "좋아졌다": "개선",
    "나아졌어요": "개선",
    "없어요": "완화",
    "없고": "완화",
    "없어서": "완화",
}
POSITIVE_TAIL_TOKENS = set(TAIL_CANON.keys())


def canonicalize_tail(tok: str) -> str:
    return TAIL_CANON.get(tok, tok)


TAIL_ALLOWED_NEG = {
    "완화": NEGATIVE_PROBLEM_TOKENS,
    "개선": NEGATIVE_PROBLEM_TOKENS,
    "케어": {
        "여드름", "트러블", "모공", "피지", "기미", "잡티",
    },
    "진정": {
        "트러블", "여드름",
        "민감", "민감성", "예민", "예민한",
        "자극",
        "홍조", "붉은기",
    },
}


def _is_verb_like(tok: str) -> bool:
    if tok in NEGATIVE_PROBLEM_TOKENS:
        return False
    if tok in EFFECT_KEYWORDS:
        return False
    if tok in INGREDIENT_KEYWORDS:
        return False
    if tok in FEELING_KEYWORDS:
        return False
    if len(tok) >= 3 and (tok.endswith("요") or tok.endswith("다")):
        return True
    return False


def _build_improvement_keyword(freq: dict) -> str:
    neg_in_review = [n for n in NEGATIVE_PROBLEM_TOKENS if freq.get(n, 0) > 0]
    raw_tail_in_review = [t for t in POSITIVE_TAIL_TOKENS if freq.get(t, 0) > 0]

    if neg_in_review and raw_tail_in_review:
        tail_freq_canon = defaultdict(int)
        for t in raw_tail_in_review:
            c = canonicalize_tail(t)
            tail_freq_canon[c] += freq[t]

        best_pair = None
        best_score = -1

        for canon_tail, t_freq in tail_freq_canon.items():
            allowed_negs = TAIL_ALLOWED_NEG.get(canon_tail, NEGATIVE_PROBLEM_TOKENS)
            for neg in neg_in_review:
                if neg not in allowed_negs:
                    continue
                score = freq[neg] * t_freq
                if score > best_score:
                    best_score = score
                    best_pair = (neg, canon_tail)

        if best_pair:
            neg, canon_tail = best_pair
            return f"{neg} {canon_tail}"

    if neg_in_review:
        neg_in_review.sort(key=lambda n: freq[n], reverse=True)
        return f"{neg_in_review[0]} 개선"

    if raw_tail_in_review:
        tail = canonicalize_tail(max(raw_tail_in_review, key=lambda t: freq[t]))
        return f"피부 {tail}"

    return "보습 개선"


def _pick_best_from_group(group: set, freq: dict, banned: set) -> Optional[str]:
    cand = [
        (t, c)
        for t, c in freq.items()
        if t in group and t not in banned and not _is_verb_like(t)
    ]
    if not cand:
        return None
    cand.sort(key=lambda x: x[1], reverse=True)
    return cand[0][0]


def _pick_fallback_any(freq: dict, banned: set) -> Optional[str]:
    cand = [
        (t, c)
        for t, c in freq.items()
        if t not in banned and not _is_verb_like(t)
    ]
    if not cand:
        return None
    cand.sort(key=lambda x: x[1], reverse=True)
    return cand[0][0]


def extract_keywords_from_review_text(review_text, ingredients=None, topk=3):
    if not review_text:
        return ["보습 개선", "촉촉", "진정"]

    freq = defaultdict(int)

    if isinstance(review_text, list):
        texts = [str(x) for x in review_text]
    else:
        texts = [str(review_text)]

    for txt in texts:
        for tok in tokenize(txt):
            freq[tok] += 1

    if not freq:
        return ["보습 개선", "촉촉", "진정"]

    slot1 = _build_improvement_keyword(freq)
    banned = set(slot1.split())

    slot2 = _pick_best_from_group(FEELING_KEYWORDS, freq, banned)
    if slot2 is None:
        slot2 = _pick_fallback_any(freq, banned)
    if slot2:
        banned.add(slot2)

    formulation_group = (
        (EFFECT_KEYWORDS | INGREDIENT_KEYWORDS)
        - FEELING_KEYWORDS
        - NEGATIVE_PROBLEM_TOKENS
    )

    slot3 = _pick_best_from_group(formulation_group, freq, banned)
    if slot3 is None:
        slot3 = _pick_fallback_any(freq, banned)

    slots = [slot1, slot2 or "촉촉", slot3 or "진정"]
    return slots[:topk]


def run_xai_indexing(es: Elasticsearch):
    query = {
        "query": {"match_all": {}},
        "_source": ["review_text", "xai_keywords", "ingredients"],
    }

    scan_iter = helpers.scan(
        es,
        index=INDEX_NAME,
        query=query,
        preserve_order=False,
    )

    actions = []
    updated_cnt = 0

    for doc in scan_iter:
        _id = doc["_id"]
        src = doc.get("_source", {})

        review_text = src.get("review_text")
        ingredients = src.get("ingredients")

        keywords = extract_keywords_from_review_text(
            review_text,
            ingredients=ingredients,
            topk=3,
        )

        action = {
            "_op_type": "update",
            "_index": INDEX_NAME,
            "_id": _id,
            "doc": {"xai_keywords": keywords},
        }
        actions.append(action)
        updated_cnt += 1

        if len(actions) >= 1000:
            helpers.bulk(es, actions)
            actions.clear()
            print(f"[XAI] 중간 커밋: 현재까지 {updated_cnt}건 업데이트")

    if actions:
        helpers.bulk(es, actions)

    print(f"[XAI] 완료: xai_keywords 업데이트 {updated_cnt}건")


def main():
    es = connect_es()

    recreate_index(es)

    base = os.getcwd()
    files = [
        (os.path.join(base, "cream.json"), "cream"),
        (os.path.join(base, "essense.json"), "essence"),
        (os.path.join(base, "skintoner.json"), "skintoner"),
    ]

    for path, cat in files:
        if not os.path.exists(path):
            print(f"[WARN] {path} 파일 없음, 건너뜀.")
            continue
        bulk_index(es, path, cat)

    print("[DONE] ES 기본 인덱싱 완료.")

    run_xai_indexing(es)
    print("[DONE] XAI 키워드 인덱싱까지 전체 완료.")


if __name__ == "__main__":
    main()
