# 하이머딩거 바론 처치 시뮬레이터

Java 연습용 개인 프로젝트. 설계는 DESIGN.md를 따른다. 설계와 다르게 구현해야 하면 먼저 DESIGN.md 수정을 제안한다.

## 작업 규칙
- 작성자는 C++ 경험자이고 Java를 배우는 중이다. 설명은 C++ 개념과 대응시켜서 한다.
- `calc`, `sim` 패키지의 로직은 작성자가 직접 짠다. 요청받지 않으면 대신 구현하지 말고 리뷰와 힌트를 준다.
- 반복 작업(JSON 로더, JavaFX 레이아웃 뼈대, 빌드 설정)은 직접 작성해도 된다.
- `calc`와 `sim`은 `ui`를 참조하지 않는다.
- 패치마다 바뀌는 게임 수치는 코드에 넣지 않고 `src/main/resources/data/`의 JSON에 둔다.
- 게임 수치는 추측하지 않는다. 확실하지 않으면 연습 모드 실측이 필요하다고 말한다.

## 명령어
- 빌드와 테스트: `./gradlew build` (Windows: `gradlew.bat build`)
- 테스트만: `./gradlew test`
- 실행: `./gradlew run`

## 구조
- 루트 패키지: `com.hongtg.baronsim`
- `model/` `data/` `calc/` `sim/` `ui/` (역할은 DESIGN.md 5장)
- Java 21, Gradle(Kotlin DSL), JavaFX 21, JUnit 5, Gson

## 진행 상황
DESIGN.md 7장의 체크박스를 기준으로 한다. 단계가 끝나면 체크하고 커밋한다.
