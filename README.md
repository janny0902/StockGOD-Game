# StockGOD-Game

주식의신 스톡갓 서비스 저장소입니다.

한국 주식 차트 기반의 문제 풀이형 게임을 통해 사용자 의사결정 데이터를 수집하고, 학습용 데이터셋으로 축적하는 것을 목표로 합니다.

## 구성
- frontend: Vue 3 + Vite
- backend: Spring Boot
- infra/postgres/init: 게임 DB 초기 스키마
- infra/kong: 게이트웨이 설정(플랫폼 연동 시 사용)

## 빠른 실행
1. 환경변수 파일 준비
   - .env.example을 복사해 .env로 생성
2. 컨테이너 실행
   - docker compose up -d --build
3. 접속
   - 프론트엔드: http://localhost:25173
   - 백엔드 헬스: http://localhost:28080/health

## 주요 기능
- 라운드 기반 차트 문제 풀이
- LONG/SHORT/HOLD 포지션 선택
- 이유카드 및 지표 선택 데이터 저장
- 결과 공개 및 세션 성과 집계

## 폴더 구조
- services/frontend
- services/backend
- infra/postgres/init
- infra/kong

## 비고
- 플랫폼 저장소(gemma4-fork)에서 서브모듈로 연결해 운영할 수 있습니다.
- 공개 저장소에서는 실 비밀번호/토큰을 커밋하지 마세요.
