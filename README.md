# AI-Capstone-2
AI 융합 캡스톤 디자인 프로젝트
# 🌿 Vegin - AI 기반 맞춤형 피부 분석 및 화장품 추천 서비스

<div align="center">

![Vue](https://img.shields.io/badge/Vue-3.5.24-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9.3-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=FastAPI&logoColor=white)

**AI와 머신러닝을 활용한 개인 맞춤형 피부 분석 및 화장품 추천 플랫폼**

[🚀 배포 사이트](https://vegin.academy) | [📖 API 문서](./backend-fresh-clean/API_DOCUMENTATION.md) | [🐛 이슈 리포트](https://github.com/JJutron/AI-Capstone-2/issues)

</div>

---

## 📋 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [시작하기](#-시작하기)
- [배포](#-배포)
- [API 문서](#-api-문서)
- [기여하기](#-기여하기)

---

## 🎯 프로젝트 소개

**Vegin**은 AI 기술을 활용하여 사용자의 피부 상태를 분석하고, 개인 맞춤형 화장품을 추천하는 서비스입니다.

- 📸 **실시간 얼굴 인식**: MediaPipe를 활용한 정확한 얼굴 감지 및 가이드
- 🤖 **AI 피부 분석**: FastAPI 기반 딥러닝 모델로 피부 타입, 고민 요소 분석
- 🎨 **MBTI 기반 분류**: 16가지 피부 MBTI 타입으로 개인 맞춤 분석
- 🛍️ **맞춤형 추천**: 피부 타입에 최적화된 화장품 추천 시스템
- 🔐 **안전한 인증**: JWT + OAuth2 (Google) 기반 보안 인증

---

## ✨ 주요 기능

### 1. 피부 분석
- 실시간 카메라를 통한 얼굴 촬영
- MediaPipe Face Mesh를 활용한 정확한 얼굴 감지
- AI 기반 피부 타입 분석 (건성/지성, 민감/둔감, 색소/비색소, 탄력/주름)
- 피부 고민 요소 분석 (여드름, 홍조, 잡티, 주름)

### 2. MBTI 기반 분류
- 16가지 피부 MBTI 타입 (예: DSPW, OSPT 등)
- 각 타입별 맞춤 설명 및 케어 가이드
- 추천 성분 및 주의 성분 제공

### 3. 화장품 추천
- 카테고리별 추천 (스킨/토너, 앰플/세럼, 로션/크림)
- 네이버 쇼핑 연동으로 바로 구매 가능
- 상품 상세 정보 및 리뷰 제공

### 4. 사용자 관리
- 이메일/비밀번호 회원가입 및 로그인
- Google OAuth2 소셜 로그인
- 프로필 관리 및 분석 히스토리 조회

---

## 🛠 기술 스택

### Frontend
- **Framework**: Vue 3.5.24 (Composition API)
- **Language**: TypeScript 5.9.3
- **Build Tool**: Vite 7.2.2
- **State Management**: Pinia 3.0.4
- **Routing**: Vue Router 4.0.0
- **HTTP Client**: Axios 1.13.2
- **Face Detection**: MediaPipe Face Mesh

### Backend
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 17
- **Security**: Spring Security + JWT + OAuth2
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Migration**: Flyway 9.22.3
- **API Documentation**: SpringDoc OpenAPI 3

### AI/ML
- **Framework**: FastAPI
- **Face Detection**: MediaPipe Face Mesh
- **Vision Model**: Custom Deep Learning Model

### Infrastructure
- **Cloud**: AWS (EC2, S3, CloudFront, Route53)
- **Web Server**: Nginx
- **Container**: Docker

---

## 📁 프로젝트 구조
