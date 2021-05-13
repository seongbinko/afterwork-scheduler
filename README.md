# AFTERWORK Scheduler

## 개요

- 목적 : [애프터워크](https://afterwork.co.kr/) 서비스의 데이터를 정기적으로 최신화 하기 위함
- 개발 인원 : 3명 (고성빈, 김남석, 최재성)
- 담당 역할 : 
  - 고성빈: 프로젝트 인프라 구축, 테이블 설계, 테스트코드 작성
  - 김남석:
  - 최재성: 
- 개발 환경 : Springboot 2.4.5, JPA, Junit5, AWS EC2, AWS RDS
- 데이터베이스 : MariaDB 10.4
- 협업툴: Git, Notion, Slack
- 주요 기능 : 정기적으로 Selenium을 통한 취미 플랫폼 사이트들을 크롤링 후 데이터를 정제하여 db를 최신화
- 특징 : 목적이 다른 두 프로젝트, 비즈니스 로직이 담긴 [프로젝트](https://github.com/seongbinko/afterwork) 와 배치 프로젝트를 분리하여 운영  

## 타임라인