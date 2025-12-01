# API Documentation

## Authentication

All protected endpoints require JWT Bearer token in the Authorization header:
```
Authorization: Bearer <token>
```

## 1. Analysis Result Retrieval API

### GET /api/analysis/{analysisId}

Retrieves detailed analysis result by analysis ID.

**Headers:**
```
Authorization: Bearer <token>
```

**Response Schema:**
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userName": "홍길동",
    "skinMbtiType": "DSPW",
    "skinDisplayName": "중성",
    "headline": "당신이 느끼는 건조하고 민감한 피부",
    "skinDescription": "수분 부족과 장벽 약화로 인해 주름과 색소침착이 동시에 나타날 수 있어요.",
    "whiteListIngredients": [
      "세라마이드",
      "히알루론산",
      "레티놀(저농도)",
      "비타민C",
      "나이아신아마이드(저농도)"
    ],
    "whiteListRecommendation": "장벽 강화와 함께 안티에이징 케어를 병행하는 저자극 루틴을 추천드립니다.",
    "blackListIngredients": [
      "알코올",
      "고농도 AHA",
      "고농도 레티놀"
    ],
    "axis": {
      "oil": 1.5,
      "dry": 1.0,
      "sensitivity": 1.04,
      "wrinkle": 0.8,
      "pigment": 0.9
    },
    "concerns": {
      "acne": {
        "score": 15,
        "reason": "..."
      },
      "redness": {
        "score": 20,
        "reason": "..."
      },
      "melasma_darkspots": {
        "score": 10,
        "reason": "..."
      }
    },
    "actions": {
      "canRetake": true,
      "canShare": true,
      "canSave": true
    }
  }
}
```

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/analysis/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 2. Profile Retrieval API

### GET /api/profile

Retrieves user profile with image, skin type, concerns, last analysis, and history.

**Headers:**
```
Authorization: Bearer <token>
```

**Response Schema:**
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "profileImageUrl": "https://vegin-media-submit.s3.ap-northeast-2.amazonaws.com/profile/user123.jpg",
    "skinType": "건성",
    "concerns": ["여드름", "홍조", "색소침착"],
    "lastAnalysis": {
      "mbti": "DSPW",
      "skinType": "중성",
      "date": "2024-01-15T10:30:00+09:00"
    },
    "history": [
      {
        "analysisId": 1,
        "imageUrl": "https://vegin-media-submit.s3.ap-northeast-2.amazonaws.com/analysis/1/image.jpg",
        "createdAt": "2024-01-15T10:30:00+09:00"
      },
      {
        "analysisId": 2,
        "imageUrl": "https://vegin-media-submit.s3.ap-northeast-2.amazonaws.com/analysis/2/image.jpg",
        "createdAt": "2024-01-10T14:20:00+09:00"
      }
    ]
  }
}
```

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/api/profile" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 3. Profile Update API

### PUT /api/profile

Updates or creates user profile. If profile exists, it updates; otherwise, it creates a new one.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body Schema:**
```json
{
  "skinType": "건성",
  "concerns": ["여드름", "홍조"],
  "mbti": "DSPW",
  "tone": "쿨톤"
}
```

**Response Schema:**
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

**Example cURL:**
```bash
curl -X PUT "http://localhost:8080/api/profile" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "skinType": "건성",
    "concerns": ["여드름", "홍조"],
    "mbti": "DSPW",
    "tone": "쿨톤"
  }'
```

---

## 4. Authentication Endpoints

### POST /api/auth/signup

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userId": 1
  }
}
```

**Example cURL:**
```bash
curl -X POST "http://localhost:8080/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "nickname": "홍길동"
  }'
```

### POST /api/auth/login

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": 1,
    "email": "user@example.com"
  }
}
```

**Example cURL:**
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

---

## 5. Analysis Upload API

### POST /api/analysis/image

Uploads skin image and triggers analysis.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: Image file (multipart file)
- `survey`: Survey JSON string

**Response:**
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "analysisId": 1,
    "imageUrl": "https://vegin-media-submit.s3.ap-northeast-2.amazonaws.com/analysis/1/image.jpg"
  }
}
```

**Example cURL:**
```bash
curl -X POST "http://localhost:8080/api/analysis/image" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "file=@/path/to/image.jpg" \
  -F 'survey={"survey": {"q1": "answer1"}}'
```

---

## Error Responses

All errors follow this schema:

```json
{
  "code": 400,
  "message": "Error message",
  "data": null
}
```

**Common Status Codes:**
- `200`: Success
- `400`: Bad Request (validation errors, business logic errors)
- `401`: Unauthorized (missing or invalid JWT token)
- `404`: Not Found (resource not found)
- `500`: Internal Server Error

---

## MBTI Skin Types

The system supports the following MBTI skin types:

- **DSPT**: 건조하고 민감한 피부 (Dry, Sensitive, Pigment, Tight)
- **DSPW**: 건조하고 민감한 피부 (Dry, Sensitive, Pigment, Wrinkle)
- **DSNT**: 건조하고 민감하지 않은 피부 (Dry, Sensitive, Non-pigment, Tight)
- **DSNW**: 건조하고 민감하지 않은 피부 (Dry, Sensitive, Non-pigment, Wrinkle)
- **DOPT**: 건조하고 유분이 많은 피부 (Dry, Oily, Pigment, Tight)
- **DOPW**: 건조하고 유분이 많은 피부 (Dry, Oily, Pigment, Wrinkle)
- **DONT**: 건조하고 유분이 많지 않은 피부 (Dry, Oily, Non-pigment, Tight)
- **DONW**: 건조하고 유분이 많지 않은 피부 (Dry, Oily, Non-pigment, Wrinkle)
- **OSPT**: 유분이 많고 민감한 피부 (Oily, Sensitive, Pigment, Tight)
- **OSPW**: 유분이 많고 민감한 피부 (Oily, Sensitive, Pigment, Wrinkle)
- **OSNT**: 유분이 많고 민감하지 않은 피부 (Oily, Sensitive, Non-pigment, Tight)
- **OSNW**: 유분이 많고 민감하지 않은 피부 (Oily, Sensitive, Non-pigment, Wrinkle)
- **OOPT**: 유분이 많고 유분이 많은 피부 (Oily, Oily, Pigment, Tight)
- **OOPW**: 유분이 많고 유분이 많은 피부 (Oily, Oily, Pigment, Wrinkle)
- **OONT**: 유분이 많고 유분이 많지 않은 피부 (Oily, Oily, Non-pigment, Tight)
- **OONW**: 유분이 많고 유분이 많지 않은 피부 (Oily, Oily, Non-pigment, Wrinkle)

Each MBTI type includes:
- Display name
- Headline (marketing text)
- Description
- White list ingredients (recommended)
- White list recommendation (text)
- Black list ingredients (to avoid)

