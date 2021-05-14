# AFTER WORK - BACKEND SCHEDULER REPOSITORY

## [AFTER WORK](https://afterwork.co.kr/) 서비스 소개

- 넘쳐나는 취미 플랫폼 클래스들을 한 번에 모아볼 수 있는 사이트

- 유명 플랫폼 7개사 클래스 인기, 가격 한 번에 비교
- 구글/네이버/카톡 아이디로 간편하게 소셜 로그인
- 사용자별 관심 지역, 분야 설정을 통한 맞춤 추천

## 🎯 Target

- 퇴근 후 뭐하지? 고민하는 직장인
- 직장인 58% 재택근무 경험…90% “증가된 여가시간에 새 취미생활 하고파” <br/> ('20.03 직장인 소셜미디어 블라인드 설문조사 결과)
- 넘쳐나는 취미 플랫폼과 클래스 정보들로 혼란스러운 이들

### AFTER WORK - FRONTEND REPOSITORY

- https://github.com/miniPinetree/_AfterWork

### AFTER WORK - BACKEND SERVER REPOSITORY

- https://github.com/seongbinko/afterwork

## 개요

- 목적 : [애프터워크](https://afterwork.co.kr/) 서비스의 데이터를 정기적으로 최신화 하기 위함
- 개발 인원 : 3명 (고성빈, 김남석, 최재성)
- 개발 기간 : 2021.04.27 ~ 2021.05.12
- 운영 기간 : 2021.05.13 ~ 진행중  
- 담당 역할 : 
  - 고성빈: 프로젝트 인프라 구축, 테이블 설계, 테스트코드 작성
  - 김남석: 클래스톡, 하비인더박스, 마이비스킷, 클래스101 웹사이트 자동 크롤링 구현
  - 최재성: 하비풀, 모카클래스, 탈잉 웹사이트 자동 크롤링 구현
- 개발 환경 : Springboot 2.4.5, JPA, Junit5, AWS EC2, AWS RDS
- 데이터베이스 : MariaDB 10.4
- 협업툴: Git, Notion, Slack
- 주요 기능 : 정기적으로 Selenium을 통한 취미 플랫폼 사이트들을 크롤링 후 데이터를 정제하여 db를 최신화
- 특징 : 목적이 다른 두 프로젝트, 비즈니스 로직이 담긴 [프로젝트](https://github.com/seongbinko/afterwork) 와 배치 프로젝트를 분리하여 운영  

## 성능 개선

- 코드 개선을 통해 데이터 최신화 소요시간이 2시간 -> 1시간(최종)으로 코드 개선이 이루어짐
  - MariaDB rewriteBatchedStatements=true 옵션 사용
  - product status Y -> N으로 변경하는 부분 JPA 벌크연산으로 처리
  - save 메소드 대신 saveAll 메소드 사용

## 타임라인

| 일자       | 진행 목록                                                    |
| ---------- | ------------------------------------------------------------ |
| 2021.04.27 | Mybiskit 웹 사이트의 크롤링 클래스 구현, 상세 가격 및 사이트 이름 정보 추가 - [김남석] <br/> Hobbyful, Mochaclass, Taling 웹 사이트의 크롤링 클래스 구현 - [최재성] |
| 2021.04.28 | 인기도/사이트 이름/상품 상태 정보 추가 - [최재성] <br/> 카테고리 테이블에 이미지 주소 칼럼 추가 - [고성빈] <br/> HobbyintheBox 웹 사이트의 크롤링 클래스 구현 - [김남석] |
| 2021.04.29 | Classtok 웹 사이트의 클롤링 클래스 구현, Headless 옵션 을 웹 드라이버에 추가 - [김남석] <br/> 모은 데이터 관리가 쉽게 각 테이블의 변수명 'id' 를 알맞는 '테이블명 id' 로 바꿈 - [고성빈] <br/> Headless 옵션 을 웹 드라이버에 추가, 통일 하기로한 가격 형식으로 fix, Hobbyful 웹  사이트의 크롤링을 위한 카테고리 추가 - [최재성]|
| 2021.04.30 | Mochaclass 상품 URL 을 크롤링 할 시 한글 깨짐 현상 fix - [최재성] <br/> Classtok의 상품 사이트 URL 데이터 fix - [김남석]|
| 2021.05.01 | Taling 웹 사이트 크롤링을 위한 TalingMacro 클래스 구현 - [최재성] |
| 2021.05.04 | Mybiskit 상품에 대한 상태 관리 메소드 구현 - [김남석] |
| 2021.05.05 | Class101 웹 사이트의 크롤링 클래스 구현 - [김남석] <br/> 크롤링 한 Mochaclass 상품 이미지 URL fix - [최재성] |
| 2021.05.06 | Database 에 이미지 URL 컬럼 저장 용량 늘림 - [고성빈] <br/> Mochaclass 상품 이미지 URL decode 삭제 - [최재성] <br/> 크롤링 한 사이트 Update 기능 구현, Y->N 처리 - [김남석] <br/> Afterwork-Scheduler 실행  시간 2시간 - [김남석] |
| 2021.05.07 | SeleniumTest 에서 SourceUpdate 클래스로 refactor, update 기능 추가 - [최재성] <br/> 사이트 이름을 영어에서 한글로 변경 - [김남석] [최재성]|
| 2021.05.08 | Afterwork의 비즈니스로직과 통합된 코드를 Afterwork-scheduler로 프로젝트 분리 및 초기 설정 및 인프라 구축 - [고성빈]<br> isOffline 유뮤 확인을 위한 컬럼 추가 - [고성빈] <br/> Afterwork-Scheduler 실행  시간 1시간 25분 - [김남석] |
| 2021.05.09 | 상품의 isOffline 데이터 추가 - [김남석] |
| 2021.05.10 | 상품의 isOffline 데이터 추가, 인기도 데이터가 삽입이 안되는 현상 fix - [최재성] <br/> Afterwork-Scheduler 실행  시간 1시간 10분 - [최재성] |
| 2021.05.11 | 상품에 isOffline isOnline 이 둘다 false 현상 fix - [최재성] <br/> 성능 개선을 위한 전반적인 코드 수정, Y->N으로 변경 하는 기능을 일괄처리로 전환, 풀 받았을때 크롬 경로를 쉽게 지정 할 수 있는 메소드 추가, 남석/재성 크롤링 코드를 한개로 통일 및 수정, update 및 new 상품에 대한 처리를 개선 (List에 저장뒤 한번에 saveAll()), Hobbyinthebox 기능에 대한 테스트 코드 작성 - [고성빈] |
| 2021.05.12 | saveAll()을 하게 되면 중복 현상이 발생 하여 new 상품에 대한 처리는 즉각 save()로 변경 - [김남석] [최재성] <br/> 작성한 테스트 코드를 JPA 상태감지 기능으로 수정 , License 추가 - [고성빈] <br/> Afterwork-Scheduler 실행  시간 1시간 - [김남석] [최재성] |
| 2021.05.13 | class101크롤링 중 ClassName이 바뀌는 현상 으로 By.className -> By.xpath로 변경 - [김남석] <br/> 모카클래스 및 탈잉 온라인 오프라인 유무 수정, 하비풀 상품 JPA 검색 조건 변경- [최재성] |
| 2021.05.14 | 탈잉 상품 중에서 이미지 URL 이 없는 상품은 아예 save 하지 않게 수정 - [최재성] |
