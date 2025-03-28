# DBMS기반 SQL분석 및 단계별 시각화 시스템, YESQL
MySQL ANTLR API Repository
----

## 🔍 Overview
- 프로젝트 주제 : SQL문을 입력하면 데이터 처리 과정을 단계별로 시각화해주는 플랫폼
- 프로젝트 배경
  - 복잡한 중첩 서브 질의문의 경우 결과값 도출 과정이 한 번에 이해하기 어려움
  - 이를 이해하기 위해 서브 질의문 단위로 실행을 단계별 복기하는 과정을 거침
- 프로젝트 목적
  - SQL문 분석 및 단계별 시각화
  - 중첩 질의문 학습 과정에 대한 시각적 이해 증진
- 주요 기능
  - 사용자별 데이터베이스 생성
  - SQL 컴파일을 통한 결과 도출 및 에러 메세지 출력
  - 기존 DB 파일 업로드 및 다운로드
  - SQL 실행 과정 단계별 시각화

## 🎯 What I did
- SQL 컴파일을 통한 결과 도출 및 에러 메세지 출력
  - SQL문을 입력하면 SQL컴파일러를 통해 도출된 결과를 반환
- MySQL ANTLR 구문분석기 활용, SQL Parse tree 객체 반환 로직 구현
  - ANTLR SQL Parse Tree를 순회하는 Visitor와 Listener 인터페이스 구현체
  - 서브 질의문을 분기점으로 각 단계별 실행 순서대로 객체화하여 반환
- AWS, Nginx 기반 서버 배포 및 관리
- SQL 문법 커버리지, 시각화 규칙 설정

## 🌱 Team
- FE : 조민경
- BE : 김조현
- BE : 방지원
- BE : 최성연

## Demo
👉 ### [YESQL Demo Video](https://www.youtube.com/watch?v=DmY69Nw5GiU)

### MySQL Parse Tree 생성 예시
![image](https://github.com/user-attachments/assets/c860ff1a-64b7-4047-9cb9-9f90e7639157)

### 단계별 시각화 화면
![image](https://github.com/user-attachments/assets/fe2148a7-a3d2-44f9-bf1a-4bf1ccd15378)


## 📌 Architecture
### 1️⃣ System Architecture
![image](https://github.com/user-attachments/assets/1ec238da-2869-445b-91af-f06bb6894751)

### 2️⃣ SQL Parse Tree traversal logic
![image](https://github.com/user-attachments/assets/05f1e684-4664-470d-9343-8a36cc5c3b77)



## 📕 Tech Stack
### 1️⃣ Franework & Library
- JDK 17
- SpringBoot 3.1.5
- antlr 4.13.1

### 2️⃣ Build Tools
- Gradle
  
### 3️⃣ Database
- MySQL 8.1.0

### 4️⃣ Infra
- AWS EC2
- AWS RDS
- AWS S3
- Nginx


