## [1.4.3](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.4.2...v1.4.3) (2025-07-15)


### Bug Fixes

* broadcast schedule created by chatbot ([2c35a77](https://github.com/100-hours-a-week/11-ellu-be/commit/2c35a7729fd9f0561f09ff7a8b16afaa0941da9f))
* broadcast schedule created by chatbot ([#185](https://github.com/100-hours-a-week/11-ellu-be/issues/185)) ([fa9d9f5](https://github.com/100-hours-a-week/11-ellu-be/commit/fa9d9f5856692db8297577b56e155ca9f1b072b3))
* caching 수정 ([03eee41](https://github.com/100-hours-a-week/11-ellu-be/commit/03eee4103ed1366be31b6725c54be5be1758ab20))
* changed query for fetching chat history ([fd9b5c8](https://github.com/100-hours-a-week/11-ellu-be/commit/fd9b5c8387a15aef46a13c15d2bc87e3bedb387d))
* ChatConversationRepository 쿼리 메소드명 수정 ([14ff36e](https://github.com/100-hours-a-week/11-ellu-be/commit/14ff36e39f2afa12d7287d8ed010485ee865d8b0))
* ChatService에서 수정된 repository 메소드명 사용 ([033556c](https://github.com/100-hours-a-week/11-ellu-be/commit/033556c8fef6807f3b980a19d1b6aa5fcc38f8cf))
* deleted unused files ([903fb41](https://github.com/100-hours-a-week/11-ellu-be/commit/903fb41d95e47b4b5dda11de484092e6ece8a8ec))
* deleted unused files ([#183](https://github.com/100-hours-a-week/11-ellu-be/issues/183)) ([082130d](https://github.com/100-hours-a-week/11-ellu-be/commit/082130d3ace4ea3a42f15b6c62a8ab65e8292bfc))
* implement user-to-pod routing via Redis for SSE message delivery ([95b92c0](https://github.com/100-hours-a-week/11-ellu-be/commit/95b92c0877065c0fad330c9b07604455be82e34f))
* implement user-to-pod routing via Redis for STOMP message delivery ([61991b8](https://github.com/100-hours-a-week/11-ellu-be/commit/61991b8fa4d76d350d11c72f25c4131fa0dba6b3))
* redis에 session 정보 저장 ([#184](https://github.com/100-hours-a-week/11-ellu-be/issues/184)) ([868118b](https://github.com/100-hours-a-week/11-ellu-be/commit/868118bab21f8bd628a1bb210af503d05e9cfc1f))
* revised forwarding logic ([286228d](https://github.com/100-hours-a-week/11-ellu-be/commit/286228d4e4a46d74c182363fcbabc6c44905c476))
* revised forwarding logic(notification domain) ([4055dc9](https://github.com/100-hours-a-week/11-ellu-be/commit/4055dc9e2af36e1426ed33e8ea0fa30f6e9a4bcc))
* 채팅 기록 조회를 위한 쿼리 수정 ([#178](https://github.com/100-hours-a-week/11-ellu-be/issues/178)) ([7e7e106](https://github.com/100-hours-a-week/11-ellu-be/commit/7e7e10606ae27cd74c025a638e374bd67de04456))

## [1.4.2](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.4.1...v1.4.2) (2025-07-10)


### Bug Fixes

* changed query for fetching chat history ([7e28870](https://github.com/100-hours-a-week/11-ellu-be/commit/7e2887008a219362772340e21708da86ddaa9e64))

## [1.4.1](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.4.0...v1.4.1) (2025-07-10)


### Bug Fixes

* ChatConversationRepository 쿼리 메소드명 수정 ([6df19c1](https://github.com/100-hours-a-week/11-ellu-be/commit/6df19c11321c61e8f37d8a42d72bce875d5cf5cb))
* ChatService에서 수정된 repository 메소드명 사용 ([2cdb06d](https://github.com/100-hours-a-week/11-ellu-be/commit/2cdb06d564adaa87d4bbc7bf3d43728af7ca95d8))

# [1.4.0](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.3.0...v1.4.0) (2025-07-08)


### Bug Fixes

* add support for LocalDateTime serialization in Redis configuration ([0aed0eb](https://github.com/100-hours-a-week/11-ellu-be/commit/0aed0ebdd7bbda8f4d853fc5afdf1ae7733449fc))
* adjust locking mechanism to mitigate cache stampede risk(revise lock acquisition logic) ([a57cc6b](https://github.com/100-hours-a-week/11-ellu-be/commit/a57cc6b2730400ff509b9cf02f6f11207fabf894))
* change payload for /achievements/daily ([e491312](https://github.com/100-hours-a-week/11-ellu-be/commit/e49131268904109b9182e0a659005b72926c69bf))
* circular dependency 해결(책임 분리) ([8f88ea4](https://github.com/100-hours-a-week/11-ellu-be/commit/8f88ea4eaad4b06c21d4f2e21d981b3e914b7a37))
* fastapi api 수정에 따른 수정 및 코드 리팩토링 ([81018da](https://github.com/100-hours-a-week/11-ellu-be/commit/81018da1a0804595d894e39e188e775c94601f46))
* kafka consumer에서 db에 저장하도록 수정 ([c59938b](https://github.com/100-hours-a-week/11-ellu-be/commit/c59938b022f8e133e25ea89702bb8f7f5f880b84))
* notification ttl 변경(3시간->5분) ([ed57d71](https://github.com/100-hours-a-week/11-ellu-be/commit/ed57d711321d73858dcc4dca1012beb36461cf3b))
* plan 저장 시 category도 저장하도록 수정 ([fdf5522](https://github.com/100-hours-a-week/11-ellu-be/commit/fdf5522d30057527dc5a94503bdda02ce5c1c855))
* plan 저장 시 category도 저장하도록 수정 ([#172](https://github.com/100-hours-a-week/11-ellu-be/issues/172)) ([79a593e](https://github.com/100-hours-a-week/11-ellu-be/commit/79a593e01d0ffdebb3b95860b3f72284f1d10a23))
* query 수정 ([a4b3833](https://github.com/100-hours-a-week/11-ellu-be/commit/a4b3833fc0fce2a3e41218b8fb70f466144868e7))
* redis lock 직접 구현한 파일 삭제 ([cf1cae1](https://github.com/100-hours-a-week/11-ellu-be/commit/cf1cae1c996ded64ad9355228ebdd86c81e3737b))
* redisson 적용 ([15e418e](https://github.com/100-hours-a-week/11-ellu-be/commit/15e418ea2107e399c3e58863d6e7bf4824b99eb1))
* transaction이 끝난 후 kafka에 produce하도록 수정(proxy 문제 해결) ([8f6a262](https://github.com/100-hours-a-week/11-ellu-be/commit/8f6a262dd5ee125ba44fe3f64fef79197a14169c))
* unify notification DTO to resolve inconsistency in Redis caching ([95e853e](https://github.com/100-hours-a-week/11-ellu-be/commit/95e853ee855ed1b986af3497e055d41e4ebd4b33))
* 사용자 스케줄 및 plan 생성 시 save를 saveAll로 수정함 ([041d4bc](https://github.com/100-hours-a-week/11-ellu-be/commit/041d4bc3966b6573c1ffc5b0287c5a0483f4e6cf))
* 새로운 일정 생성 시 알림 수신 대상 변경 ([ed7c6eb](https://github.com/100-hours-a-week/11-ellu-be/commit/ed7c6eb1e5959cbb9691c5238736c130b4b878c1))
* 초대 알림 수락 시 초대 처리 알림 수신자와 발신자의 프로젝트 목록 캐시 업데이트 ([e8c7223](https://github.com/100-hours-a-week/11-ellu-be/commit/e8c7223a2c36f19aae0fdf2d08a03a2bcf8c89c0))
* 프로젝트 수정 시 프로젝트에서 방출된 사람, 새로 초대된 사람의 캐시 업데이트 ([e41b5ec](https://github.com/100-hours-a-week/11-ellu-be/commit/e41b5ec2c2a1b51723f1be27ca23056d2d77cc0b))
* 프로젝트 캐싱 버그 수정 ([a2b8bcd](https://github.com/100-hours-a-week/11-ellu-be/commit/a2b8bcd261811b3eec183336040d01a3b4878562))


### Features

* ai server 관련 스케줄 조회 기능 api 구현 ([f4cf9bc](https://github.com/100-hours-a-week/11-ellu-be/commit/f4cf9bca7fd8a555022143101b70423c06d51890))
* ai 서버에 사용자가 선택한 스케줄에 대한 정보 제공하는 api 구현 ([9bfc574](https://github.com/100-hours-a-week/11-ellu-be/commit/9bfc57498e50607a85a18bb55816c13361244cc7))
* applied redis lock to prevent cache stampede ([36c326a](https://github.com/100-hours-a-week/11-ellu-be/commit/36c326a37a1d5afa5986a88e354ede3ad549637c))
* implemented apis for schedule achievement report ([acc7ff5](https://github.com/100-hours-a-week/11-ellu-be/commit/acc7ff54177f5c163687de8862f6c0522cafa2e0))
* redis caching for notification ([442445e](https://github.com/100-hours-a-week/11-ellu-be/commit/442445e422691610bdec9be509260e0d039e4241))
* redis for caching project information ([2c6baba](https://github.com/100-hours-a-week/11-ellu-be/commit/2c6baba4dd6f09575d91acff77ce68540b3e2d2a))
* Rewrote commented-out API call for AI schedule recommendation ([db59cac](https://github.com/100-hours-a-week/11-ellu-be/commit/db59cacf36c7c6a1b95da3064ecd329735d9f681))
* ttl에 jitter 적용 ([30fdddf](https://github.com/100-hours-a-week/11-ellu-be/commit/30fdddfab94bd77c87c76946909f4728ec83b775))
* 스케줄 완료율 대시보드 ([#170](https://github.com/100-hours-a-week/11-ellu-be/issues/170)) ([d45a27b](https://github.com/100-hours-a-week/11-ellu-be/commit/d45a27bfcf797a7b74f8768d0bd6d55f5e728830))
* 유저의 회의록 선택 결과 전송  ([#168](https://github.com/100-hours-a-week/11-ellu-be/issues/168)) ([06f5dde](https://github.com/100-hours-a-week/11-ellu-be/commit/06f5dde8dcc223baf01d4cc631d8ba562e780ce8))

# [1.3.0](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.2.0...v1.3.0) (2025-07-03)


### Bug Fixes

* otel 콜렉터 의존성 버전 수정 ([7f25c52](https://github.com/100-hours-a-week/11-ellu-be/commit/7f25c5206bd22b07703a169a1dfb066b68aeba98))
* otel 콜렉터 의존성 버전 수정 ([c5eeb8a](https://github.com/100-hours-a-week/11-ellu-be/commit/c5eeb8a4f14ef23dfa9e2a809f0400e8719a5972))
* 누락된 파일 추가 ([87d6813](https://github.com/100-hours-a-week/11-ellu-be/commit/87d6813a52fa0eca04544738dc551004cb704581))
* 대화 기록 조회 쿼리 수정 ([b72a72c](https://github.com/100-hours-a-week/11-ellu-be/commit/b72a72ce8795dd3841c88a8e7db6b6d766126164))
* 로그 수정 및 예외 처리 추가 ([e297ce7](https://github.com/100-hours-a-week/11-ellu-be/commit/e297ce72c2efd16287b55c61b5e5353b557d3f8c))
* 로그 수정 및 예외 처리 추가 ([fc3837a](https://github.com/100-hours-a-week/11-ellu-be/commit/fc3837a24e8a430eef1f61973193b0a7c525efd5))


### Features

* Otel jdbc 콜렌터 추가 ([c8bdc88](https://github.com/100-hours-a-week/11-ellu-be/commit/c8bdc8877c18da1e46410d4ddff1bb1c91d57553))
* Otel jdbc 콜렌터 추가 ([a238545](https://github.com/100-hours-a-week/11-ellu-be/commit/a2385457d8a11b5d389dc97193ff36c2c57892b6))

# [1.2.0](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.1.0...v1.2.0) (2025-06-20)


### Bug Fixes

* @MessageMapping 컨트롤러에서 트랜잭션없이 Lazy 필드에 접근-> fetch join 으로 연관 데이터 즉시 로딩 ([ab5edc4](https://github.com/100-hours-a-week/11-ellu-be/commit/ab5edc4c431d476b6066a83230d7cfb4e4d42886))
* @MessageMapping 컨트롤러에서 트랜잭션없이 Lazy 필드에 접근-> fetch join 으로 연관 데이터 즉시 로딩 ([6c95c1c](https://github.com/100-hours-a-week/11-ellu-be/commit/6c95c1c2543a8511dea467dc65b4171377d10b32))
* application.yml에 kafka 환경변수 반영되게 다시 수정 ([8f8f64f](https://github.com/100-hours-a-week/11-ellu-be/commit/8f8f64f9971d975bc93c547bb7b96b36ad7f978d))
* batch size 수정 ([f270cc0](https://github.com/100-hours-a-week/11-ellu-be/commit/f270cc0ff219ed70047607de29770ccbb9648cd5))
* batch size 수정 ([b88a02c](https://github.com/100-hours-a-week/11-ellu-be/commit/b88a02ce53d8d9921e26f194c8d7e42fb29fc0ce))
* chatbot endpoint 수정 ([bd0b34b](https://github.com/100-hours-a-week/11-ellu-be/commit/bd0b34b7eaabc283192bed84403062c580aa66ec))
* chatbot endpoint 수정 ([ddd37d1](https://github.com/100-hours-a-week/11-ellu-be/commit/ddd37d19b478eb3d992b6a049d09a707037a9667))
* dto 필드 수정(keyword -> task) ([c1df971](https://github.com/100-hours-a-week/11-ellu-be/commit/c1df971ebbc46fb3cf6f0eacacbd23c40c8f8c0b))
* dto 필드 수정(keyword -> task) ([c1a7143](https://github.com/100-hours-a-week/11-ellu-be/commit/c1a71432443bd7e1a9c8df424a8293c81ae351c3))
* endpoint 수정 ([12adbbc](https://github.com/100-hours-a-week/11-ellu-be/commit/12adbbcd2cfa1aade5e7bfb3c918fed0bf55f3fa))
* endpoint 수정 ([af5c3c8](https://github.com/100-hours-a-week/11-ellu-be/commit/af5c3c827b386b4f6893825395738b1401f528ef))
* kafka streams를 위해 yml파일에 application-id 추가 ([73c2f62](https://github.com/100-hours-a-week/11-ellu-be/commit/73c2f62b1d6c28d3f492f8bff18f13c775666466))
* kafka streams를 위해 yml파일에 application-id 추가 ([9173188](https://github.com/100-hours-a-week/11-ellu-be/commit/9173188105a1bf1515127c5b566003e41be9e1cd))
* kafka topic 명시적 생성 ([2251687](https://github.com/100-hours-a-week/11-ellu-be/commit/22516876a3246c0c2358b24908541c3065c41226))
* kafka 환경변수 수정 ([de67f9a](https://github.com/100-hours-a-week/11-ellu-be/commit/de67f9a3f3ab039dc0fbf0d221bf347b7201669c))
* meeting note dto id field 타입 수정 ([#56](https://github.com/100-hours-a-week/11-ellu-be/issues/56)) ([14abcc3](https://github.com/100-hours-a-week/11-ellu-be/commit/14abcc34c3e5049500f7c600b6063e32b8a56d61))
* plan 저장 api response 수정 ([fc2a8c7](https://github.com/100-hours-a-week/11-ellu-be/commit/fc2a8c7c1be1067c0c8abf5a78c402b62b36e288))
* plan 저장 api response 수정 ([d67f5cd](https://github.com/100-hours-a-week/11-ellu-be/commit/d67f5cdf221bb42ac9196132ac9bd72a3a53fc94))
* plan 저장 기능 수정 ([9333b32](https://github.com/100-hours-a-week/11-ellu-be/commit/9333b328f909401923ef736cd02f906ac51d6b84))
* plan 저장 기능 수정 ([8f63f94](https://github.com/100-hours-a-week/11-ellu-be/commit/8f63f9412cd6bac5de8cd200890e600527486c44))
* pricipal에서 사용자 id 추출하도록 수정, lombok으로 명시적 생성자 대체 ([b24e62f](https://github.com/100-hours-a-week/11-ellu-be/commit/b24e62fb72681723c5cb741f374e2343d845db2d))
* pricipal에서 사용자 id 추출하도록 수정, lombok으로 명시적 생성자 대체 ([79bb917](https://github.com/100-hours-a-week/11-ellu-be/commit/79bb917575165714f50647ee6b28ab7e9ad56e33))
* StompController의 비즈니스 로직 분리 ([19613b9](https://github.com/100-hours-a-week/11-ellu-be/commit/19613b9c61efcaa41bb7535a37095475f25500a0))
* StompController의 비즈니스 로직 분리 ([c25c49e](https://github.com/100-hours-a-week/11-ellu-be/commit/c25c49ed8f1ce218923648f36ccc6d3e5300d241))
* timezone 설정 ([#113](https://github.com/100-hours-a-week/11-ellu-be/issues/113)) ([f4c3e2f](https://github.com/100-hours-a-week/11-ellu-be/commit/f4c3e2fc49016938affe14166d8d8c91e6ba1aec))
* topic 이름 수정 ([2ba53f9](https://github.com/100-hours-a-week/11-ellu-be/commit/2ba53f905241fa9c0f0afd6b175cd850043658f0))
* topic 이름 수정 ([61a0c11](https://github.com/100-hours-a-week/11-ellu-be/commit/61a0c11c98d2293683d173671c24556375dc3fc9))
* update stage.yml with consistent deployment structure ([91bc5dd](https://github.com/100-hours-a-week/11-ellu-be/commit/91bc5dd345d7c942bea181feb5fe0b97d96fcbdf))
* update stage.yml with consistent deployment structure ([c13abd4](https://github.com/100-hours-a-week/11-ellu-be/commit/c13abd488bda8877c532df669ff26aba081b5f72))
* webclient complete처리 수정 ([1ed2af7](https://github.com/100-hours-a-week/11-ellu-be/commit/1ed2af7f72b77e1837df64f382b6e0eb46ea29cf))
* webclient complete처리 수정 ([e60c74a](https://github.com/100-hours-a-week/11-ellu-be/commit/e60c74ae8295af17f7fbd6f2991fce20e18c2970))
* websocket send 요청 시 header로 userId전달 ([4c0fc53](https://github.com/100-hours-a-week/11-ellu-be/commit/4c0fc5350847aef5e15d7f44caaee1fbf9a34366))
* websocket send 요청 시 header로 userId전달 ([63abbfd](https://github.com/100-hours-a-week/11-ellu-be/commit/63abbfd9c676eba45bed24b0cb3c868bd4b459db))
* wiki 업로드 기능 수정 ([15b4342](https://github.com/100-hours-a-week/11-ellu-be/commit/15b434263fa23484d0d9a7d7bf98417068e19cbf))
* wiki 업로드 기능 수정 ([41dc575](https://github.com/100-hours-a-week/11-ellu-be/commit/41dc575740a0f6101c7b699aaa926dcdff967f5a))
* wiki 임베딩 알림 안 보내지는 현상 해결 ([ea981f6](https://github.com/100-hours-a-week/11-ellu-be/commit/ea981f669fc9657c14d2f1afbf9c599765afc87e))
* wiki 임베딩 알림 안 보내지는 현상 해결 ([512cd6a](https://github.com/100-hours-a-week/11-ellu-be/commit/512cd6ad5028b683cbd5908906fc7d4c8bc021e2))
* ws cors ([9956419](https://github.com/100-hours-a-week/11-ellu-be/commit/9956419cd595d8931714b1d0c23f4f7a053b4a74))
* ws cors ([#147](https://github.com/100-hours-a-week/11-ellu-be/issues/147)) ([148a2c6](https://github.com/100-hours-a-week/11-ellu-be/commit/148a2c6b36c8a9b8797c270bbfb56fa1ab8f6a32))
* 보안 필터에서 관리자 엔드포인트 화이트리스트 처리 ([#104](https://github.com/100-hours-a-week/11-ellu-be/issues/104)) ([eb85e22](https://github.com/100-hours-a-week/11-ellu-be/commit/eb85e2203882303f1557f8288c0292eec146a774))
* 불필요한 flush() 제거 및 linger.ms 설정으로 Kafka 비동기 처리 개선 ([1d8fd7b](https://github.com/100-hours-a-week/11-ellu-be/commit/1d8fd7b0c13481ab8dd66002c9337a568f1734d0))
* 불필요한 flush() 제거 및 linger.ms 설정으로 Kafka 비동기 처리 개선 ([e223245](https://github.com/100-hours-a-week/11-ellu-be/commit/e22324520cfc8f72b48c37c88c9d447de03b3a9f))
* 비동기 처리 추가 및 개발 환경 쿠키 설정 추가 ([#63](https://github.com/100-hours-a-week/11-ellu-be/issues/63)) ([b725151](https://github.com/100-hours-a-week/11-ellu-be/commit/b7251516834937b120b7c09054f06f89d8f54c15))
* 스케줄 id로 처리하도록 버그 수정 ([b449823](https://github.com/100-hours-a-week/11-ellu-be/commit/b44982362f83aa51cc601443f7305485172560a1))
* 스케줄 id로 처리하도록 버그 수정 ([e14c34a](https://github.com/100-hours-a-week/11-ellu-be/commit/e14c34a055d5eaf07a221153f7492f9c31babfd7))
* 에러 로그 수정 ([2eccdd9](https://github.com/100-hours-a-week/11-ellu-be/commit/2eccdd9f9ea00fd3b5502056b4a8723e23f7b164))
* 에러 로그 수정 ([d2d7da7](https://github.com/100-hours-a-week/11-ellu-be/commit/d2d7da7b9f66902c1e787c161bcf94222672d8b7))
* 위키 url 전송 방식으로 변경, 회의록 업로드하여 일정 추출하는 기능 연동을 위한 dto 수정 ([9c5bb08](https://github.com/100-hours-a-week/11-ellu-be/commit/9c5bb08e2253319caff222968313a2987c875136))
* 위키 url 전송 방식으로 변경, 회의록 업로드하여 일정 추출하는 기능 연동을 위한 dto 수정 ([a83511a](https://github.com/100-hours-a-week/11-ellu-be/commit/a83511a4bbbec01d0273751335b94305efe74d00))
* 응답 형식 수정 ([862dc81](https://github.com/100-hours-a-week/11-ellu-be/commit/862dc810357065f8d2ed434cbf43b29bf5bf9e20))
* 응답 형식 수정 ([51a34ec](https://github.com/100-hours-a-week/11-ellu-be/commit/51a34ec4a8edcc49a5d071b70242ba21522482b8))
* 챗봇 api 누락된 dto 필드 추가 ([77668d9](https://github.com/100-hours-a-week/11-ellu-be/commit/77668d985ea3b33a6b649ac4d71deabe3ab4f181))
* 챗봇 api 누락된 dto 필드 추가 ([7673fa1](https://github.com/100-hours-a-week/11-ellu-be/commit/7673fa18b65467f6df1fdd33e4b9db7f2e6d3321))
* 챗봇 스트리밍 기능 수정 ([8016f50](https://github.com/100-hours-a-week/11-ellu-be/commit/8016f5073b0eedbe4ce9ff62cce9d9f6a51e9d2b))
* 챗봇 스트리밍 기능 수정 ([c443d51](https://github.com/100-hours-a-week/11-ellu-be/commit/c443d511ab0bb08a114153ebcde6321f06306153))
* 토픽 이름 application.yml에 정의 ([6f04fbc](https://github.com/100-hours-a-week/11-ellu-be/commit/6f04fbcb6f9bd7dd0f0add811d8385da9baa5b94))
* 토픽 이름 application.yml에 정의 ([33f77c4](https://github.com/100-hours-a-week/11-ellu-be/commit/33f77c489fd2fec081ed0496fb4c7b6d0597a876))
* 프로젝트 id로 구독하고, 스케줄 id로 처리하도록 버그 수정 ([25bd7e5](https://github.com/100-hours-a-week/11-ellu-be/commit/25bd7e5eea3d85f7fb8f7be46f3e70e7ba9b5d12))
* 프로젝트 id로 구독하고, 스케줄 id로 처리하도록 버그 수정 ([50a56f2](https://github.com/100-hours-a-week/11-ellu-be/commit/50a56f2bc0dfa4f05c4b3e811d79ddd223fe92b5))
* 프로젝트 스케줄 삭제 권한 수정 ([760ae9a](https://github.com/100-hours-a-week/11-ellu-be/commit/760ae9a45fe53246edb37250321e41ab93a747d4))
* 프로젝트 스케줄 삭제 권한 수정 ([9dd63d9](https://github.com/100-hours-a-week/11-ellu-be/commit/9dd63d9250ab14956cfd26e48b089ed283bde7d8))
* 프로젝트 스케줄 생성 시 assignee저장 로직 추가 ([00b4fd2](https://github.com/100-hours-a-week/11-ellu-be/commit/00b4fd2f4f895518075ccc35d762a83cd86adba6))
* 프로젝트 스케줄 생성 시 assignee저장 로직 추가 ([d4e49fd](https://github.com/100-hours-a-week/11-ellu-be/commit/d4e49fd6c1f62f0eeb0dbacfbf51b7a69d71c7eb))
* 프로젝트 스케줄 수정 권한 수정 ([68196e9](https://github.com/100-hours-a-week/11-ellu-be/commit/68196e96a5452c214bd21ffa41e50820d4722e0d))
* 프로젝트 스케줄 수정 권한 수정 ([b2b8e1e](https://github.com/100-hours-a-week/11-ellu-be/commit/b2b8e1e10c9df28a29bb3334daf578054864e2a8))
* 프로젝트 알림 보낼 때 멤버소속여부 확인하는 로직 버그 수정 ([cd35f0f](https://github.com/100-hours-a-week/11-ellu-be/commit/cd35f0f14543a5b1a80d2337ce6878143f0f22e4))
* 프로젝트 알림 보낼 때 멤버소속여부 확인하는 로직 버그 수정 ([b721831](https://github.com/100-hours-a-week/11-ellu-be/commit/b721831d80f76f7320c21d4508fa081458819536))
* 프로젝트 알림 수신자, 송신자가 잘못 지정된 버그 수정 ([c158f94](https://github.com/100-hours-a-week/11-ellu-be/commit/c158f94e2adc8cc22627725aedc4157906d33ede))
* 프로젝트 알림 수신자, 송신자가 잘못 지정된 버그 수정 ([b71e510](https://github.com/100-hours-a-week/11-ellu-be/commit/b71e510b71dc5b9e047f8e59c470bb022546ca76))
* 프로젝트 초대 응답 수정 ([c4e0351](https://github.com/100-hours-a-week/11-ellu-be/commit/c4e0351a95079e779ea23d5c95f3fad7e710a0e2))
* 프로젝트 초대 응답 수정 ([ffe1ee4](https://github.com/100-hours-a-week/11-ellu-be/commit/ffe1ee45ca85f1b6a747026dff81c5eb0ef81c48))
* 회의록 기반 일정 추출 consumer의 group id 변경 ([64504b9](https://github.com/100-hours-a-week/11-ellu-be/commit/64504b98a48d7b64ba44c330e6cba3990a01a211))
* 회의록 기반 일정 추출 consumer의 group id 변경 ([b774815](https://github.com/100-hours-a-week/11-ellu-be/commit/b774815a6b3a32dd49310e32fd622c3160ccd936))
* 회의록 업로드 기능 버그 수정 ([b37732a](https://github.com/100-hours-a-week/11-ellu-be/commit/b37732af24ac57ac5ae006acd4916d047584ac1d))
* 회의록 업로드 기능 버그 수정 ([ade98fa](https://github.com/100-hours-a-week/11-ellu-be/commit/ade98fa5ebcd01144a352582d723f6743b5d34ed))
* 회의록 업로드 기능 연동 및 버그 수정 ([55f449c](https://github.com/100-hours-a-week/11-ellu-be/commit/55f449c08266fbb0faa8cefa95deb627dae124fb))
* 회의록 업로드 기능 연동 및 버그 수정 ([732c871](https://github.com/100-hours-a-week/11-ellu-be/commit/732c8715b0a6714f5bcf554463f6c72cabc7b020))


### Features

* added project invitation exception & slight change in nickname live suggest logic ([#83](https://github.com/100-hours-a-week/11-ellu-be/issues/83)) ([47f6883](https://github.com/100-hours-a-week/11-ellu-be/commit/47f68838844d5b91fa06ebae4622e2e82188dd9c))
* preserve project calendar color when copying events to personal calendar ([e68d698](https://github.com/100-hours-a-week/11-ellu-be/commit/e68d6980c262e16ee3c2a84dc2cf36da3d368603))
* project detail view dto & project update logic revised ([#85](https://github.com/100-hours-a-week/11-ellu-be/issues/85)) ([6877361](https://github.com/100-hours-a-week/11-ellu-be/commit/68773619c9b00d0a5ae8cf195dd2fcfefc6441c6))
* sse 로깅 추가 ([#98](https://github.com/100-hours-a-week/11-ellu-be/issues/98)) ([1bcc56e](https://github.com/100-hours-a-week/11-ellu-be/commit/1bcc56e263a104e92c610c68d023b28c221f88d5))
* sync event color when taking event to personal calendar ([8ccd892](https://github.com/100-hours-a-week/11-ellu-be/commit/8ccd892b5d94aee3860b423091ba0483bf9cecd2))
* websocket으로 일정 수정 이벤트 전파하는 기능 구현 ([2b2a618](https://github.com/100-hours-a-week/11-ellu-be/commit/2b2a618d30331c39c4c4030ae3099c355e2d64ec))
* websocket으로 일정 수정 이벤트 전파하는 기능 구현 ([219c09c](https://github.com/100-hours-a-week/11-ellu-be/commit/219c09c2eeea5588dab4cfb07ca36f2623c3a92e))
* 메트릭 정보 endpoint 추가 ([eaef3f0](https://github.com/100-hours-a-week/11-ellu-be/commit/eaef3f07b0ec20d0622069fc8045efc935ca45cb))
* 위키 임베딩 완료 웹훅 구현 ([4a83551](https://github.com/100-hours-a-week/11-ellu-be/commit/4a83551ec57cb73c9152c0e37e681ca2544c3ad2))
* 위키 임베딩 완료 웹훅 구현 ([6c586d9](https://github.com/100-hours-a-week/11-ellu-be/commit/6c586d9bc9c32b2374d7f70f9a025aa201d4d356))
* 챗봇 기능 구현 ([fd5c541](https://github.com/100-hours-a-week/11-ellu-be/commit/fd5c5411c77402860db2b876ddc5ceadaff6923d))
* 챗봇 기능 구현 ([79bf604](https://github.com/100-hours-a-week/11-ellu-be/commit/79bf604eec9c52543c137a4acef19dbd403f6234))
* 챗봇과의 대화를 통해 개인 플랜 생성하는 api 구현 ([f2ba4d9](https://github.com/100-hours-a-week/11-ellu-be/commit/f2ba4d92afd0b9bf2b9b479f22d40922c3977182))
* 챗봇과의 대화를 통해 개인 플랜 생성하는 api 구현 ([8db8fd6](https://github.com/100-hours-a-week/11-ellu-be/commit/8db8fd64950669fc45a129480e111c0ebbbacc11))
* 최근 24시간동안의 대화 목록 조회 api ([a941515](https://github.com/100-hours-a-week/11-ellu-be/commit/a94151547ce7b3a76b087bef6437ff1f62730e59))
* 프로젝트 멤버들도 프로젝트 색상 조회 가능하게 수정 ([7bb186c](https://github.com/100-hours-a-week/11-ellu-be/commit/7bb186c80c1e5e0aef9183db5539d3ba268fc209))
* 프로젝트 멤버들도 프로젝트 색상 조회 가능하게 수정 ([c4ea85d](https://github.com/100-hours-a-week/11-ellu-be/commit/c4ea85d1922f8ca3bf00db5324ffa2c9cc9d03c6))
* 프로젝트 색상 필드 처리([#71](https://github.com/100-hours-a-week/11-ellu-be/issues/71)) ([#72](https://github.com/100-hours-a-week/11-ellu-be/issues/72)) ([aab6a75](https://github.com/100-hours-a-week/11-ellu-be/commit/aab6a754485bcc7fc7abb6568b0a9e38dc87e456))
* 프로젝트 초대 수락/거절 알림 ([#110](https://github.com/100-hours-a-week/11-ellu-be/issues/110)) ([953e304](https://github.com/100-hours-a-week/11-ellu-be/commit/953e3040e005b9818f44e52aec21e9c81eafba61))


### Reverts

* Revert "fix: meeting note dto id field 타입 수정" ([#58](https://github.com/100-hours-a-week/11-ellu-be/issues/58)) ([fef09df](https://github.com/100-hours-a-week/11-ellu-be/commit/fef09df075a4152a078362a833e58119449f45b3)), closes [#56](https://github.com/100-hours-a-week/11-ellu-be/issues/56)

# [1.1.0](https://github.com/100-hours-a-week/11-ellu-be/compare/v1.0.0...v1.1.0) (2025-06-05)


### Bug Fixes

* meeting note dto id field 타입 수정 ([#56](https://github.com/100-hours-a-week/11-ellu-be/issues/56)) ([206ab89](https://github.com/100-hours-a-week/11-ellu-be/commit/206ab8936261424ed5aaf360be84b8cb6d4300a2))
* timezone 설정 ([#113](https://github.com/100-hours-a-week/11-ellu-be/issues/113)) ([953442c](https://github.com/100-hours-a-week/11-ellu-be/commit/953442cadc8158ed73c6d4a2151bbe580e75b947))
* 보안 필터에서 관리자 엔드포인트 화이트리스트 처리 ([#104](https://github.com/100-hours-a-week/11-ellu-be/issues/104)) ([2c7d641](https://github.com/100-hours-a-week/11-ellu-be/commit/2c7d641e40145f80d2613346c186bc43348d1125))
* 비동기 처리 추가 및 개발 환경 쿠키 설정 추가 ([#63](https://github.com/100-hours-a-week/11-ellu-be/issues/63)) ([f4116a1](https://github.com/100-hours-a-week/11-ellu-be/commit/f4116a1f0f58c9603e83cc2e1dfc02975998eea6))


### Features

* added project invitation exception & slight change in nickname live suggest logic ([#83](https://github.com/100-hours-a-week/11-ellu-be/issues/83)) ([a9ecb35](https://github.com/100-hours-a-week/11-ellu-be/commit/a9ecb3573818b73b575eb62dee30ecca5ea55bd1))
* project detail view dto & project update logic revised ([#85](https://github.com/100-hours-a-week/11-ellu-be/issues/85)) ([7305e0c](https://github.com/100-hours-a-week/11-ellu-be/commit/7305e0c7646587b5599483fe8020d28b18ef98e5))
* sse 로깅 추가 ([#98](https://github.com/100-hours-a-week/11-ellu-be/issues/98)) ([119c896](https://github.com/100-hours-a-week/11-ellu-be/commit/119c896a5903913031757b51dae7ecd8a11e4031))
* 메트릭 정보 endpoint 추가 ([86acd28](https://github.com/100-hours-a-week/11-ellu-be/commit/86acd28a6c5fd61c820e94701994ec0df6d509b6))
* 프로젝트 색상 필드 처리([#71](https://github.com/100-hours-a-week/11-ellu-be/issues/71)) ([#72](https://github.com/100-hours-a-week/11-ellu-be/issues/72)) ([53b2e94](https://github.com/100-hours-a-week/11-ellu-be/commit/53b2e94976fdc66d19afafeaec9e9c1dd41bcd0c))
* 프로젝트 초대 수락/거절 알림 ([#110](https://github.com/100-hours-a-week/11-ellu-be/issues/110)) ([07b0523](https://github.com/100-hours-a-week/11-ellu-be/commit/07b0523c868bae4d255c5ad43e68986f82a175c2))


### Reverts

* Revert "fix: meeting note dto id field 타입 수정" ([#58](https://github.com/100-hours-a-week/11-ellu-be/issues/58)) ([7906505](https://github.com/100-hours-a-week/11-ellu-be/commit/790650523cc22e1a6ae161ed01e723741e10d9a1)), closes [#56](https://github.com/100-hours-a-week/11-ellu-be/issues/56)

# 1.0.0 (2025-05-11)


### Bug Fixes

* API 응답 형식 수정에 따른 수정([#19](https://github.com/100-hours-a-week/11-ellu-be/issues/19)) ([9cfb90d](https://github.com/100-hours-a-week/11-ellu-be/commit/9cfb90d12cdc25d80e2564a946cae3f1b986ad80))
* API 응답 형식 수정에 따른 수정([#19](https://github.com/100-hours-a-week/11-ellu-be/issues/19)) ([#20](https://github.com/100-hours-a-week/11-ellu-be/issues/20)) ([62b33bf](https://github.com/100-hours-a-week/11-ellu-be/commit/62b33bf747aa046cca69c76074546ea2ac1f54c7))
* API 응답 형식에 맞게 수정([#25](https://github.com/100-hours-a-week/11-ellu-be/issues/25)) ([a7af2b3](https://github.com/100-hours-a-week/11-ellu-be/commit/a7af2b3a1c84ebe6441b6a539c5f551f3ef4ff04))
* API 응답 형식에 맞게 수정([#25](https://github.com/100-hours-a-week/11-ellu-be/issues/25)) ([#26](https://github.com/100-hours-a-week/11-ellu-be/issues/26)) ([74e7d20](https://github.com/100-hours-a-week/11-ellu-be/commit/74e7d2047f2001ffff6a3f30674c9de3b5519093))
* cors 스웨거 해결 ([c73cd19](https://github.com/100-hours-a-week/11-ellu-be/commit/c73cd19f110404e7c94f3b875bf1e3a03fbef2ff))
* dto가 아닌 응답 형태 수정([#31](https://github.com/100-hours-a-week/11-ellu-be/issues/31)) ([d15ae66](https://github.com/100-hours-a-week/11-ellu-be/commit/d15ae66074b5d876424ca23a1295630873ac375b))
* fastapi wiki 연동 완료 ([7b164a3](https://github.com/100-hours-a-week/11-ellu-be/commit/7b164a33fd87350c791a9eeeed7d22790874be02))
* fastapi 회의록 연동 ([ae25188](https://github.com/100-hours-a-week/11-ellu-be/commit/ae251885af39395b344f3661b877ba2c37baa5e1))
* fastapi 회의록 연동 완료 ([c1c539d](https://github.com/100-hours-a-week/11-ellu-be/commit/c1c539d1639e8789c15c0e12c25773f588d32614))
* fastapi, frontend integration([#46](https://github.com/100-hours-a-week/11-ellu-be/issues/46)) ([#47](https://github.com/100-hours-a-week/11-ellu-be/issues/47)) ([cc1fe62](https://github.com/100-hours-a-week/11-ellu-be/commit/cc1fe62ff9b4817d27ec568345a0df09e1424e2b))
* frontend 요청 시 Cookie 포함되게 수정 ([ec7173f](https://github.com/100-hours-a-week/11-ellu-be/commit/ec7173f465d7f30aa377a6c06d7ee96eeee8c7c0))
* frontend에 반환될 회의록 응답 형식 수정 ([d891b43](https://github.com/100-hours-a-week/11-ellu-be/commit/d891b43fe6a6d79ee63e2fb60441c99a14ae0d0c))
* frontend에 반환될 회의록 응답 형식 수정 ([#49](https://github.com/100-hours-a-week/11-ellu-be/issues/49)) ([c8d8e2c](https://github.com/100-hours-a-week/11-ellu-be/commit/c8d8e2cbac7bae0400aaca7c5f0445eb4805f68d))
* is_new_user 변수명 수정 ([aecd00d](https://github.com/100-hours-a-week/11-ellu-be/commit/aecd00d5d98db7498f01bb722e388e7b38259f93))
* is_new_user 변수명 수정 ([#36](https://github.com/100-hours-a-week/11-ellu-be/issues/36)) ([719d742](https://github.com/100-hours-a-week/11-ellu-be/commit/719d74228664ba9f2f01029e81d6c20704389969))
* is_new_user 변수명 수정 ([#37](https://github.com/100-hours-a-week/11-ellu-be/issues/37)) ([0833110](https://github.com/100-hours-a-week/11-ellu-be/commit/083311073ac2a4e5954b8547c3e67fe24bf4b06c))
* is_new_user 변수명 수정 ([#38](https://github.com/100-hours-a-week/11-ellu-be/issues/38)) ([36d6a5a](https://github.com/100-hours-a-week/11-ellu-be/commit/36d6a5ab2795db4b4a59d6b91714e071da6ab08c))
* ScheduleService에 Transactional 추가([#46](https://github.com/100-hours-a-week/11-ellu-be/issues/46)) ([8c94e77](https://github.com/100-hours-a-week/11-ellu-be/commit/8c94e7742f3dcd3616b3cbd9750636988b2cbf23))
* swagger 문제 해결 ([34a488b](https://github.com/100-hours-a-week/11-ellu-be/commit/34a488bd96da5e36d6685d4eab5b2499b51b820f))
* swagger 문제 해결 ([1d779ff](https://github.com/100-hours-a-week/11-ellu-be/commit/1d779ff58408bd0c9a83a83f5b5a8b66be22ec14))
* swagger 문제 해결 ([20b6c90](https://github.com/100-hours-a-week/11-ellu-be/commit/20b6c905ee921e66ed33c4e47adb175c17967f59))
* swagger 문제 해결 ([4e8a3ac](https://github.com/100-hours-a-week/11-ellu-be/commit/4e8a3acf4ff21a94c99edffdfbd1def094890d93))
* swagger 문제 해결 ([2dd9f76](https://github.com/100-hours-a-week/11-ellu-be/commit/2dd9f76a9f666cf212906483ef4d6e2a5ec4a744))
* 로그인 시 응답 형식 수정([#12](https://github.com/100-hours-a-week/11-ellu-be/issues/12)) ([ea5eae6](https://github.com/100-hours-a-week/11-ellu-be/commit/ea5eae654908f00c9a713e4f22066fed3bbf48b4))
* 로그인 시 응답 형식 수정([#12](https://github.com/100-hours-a-week/11-ellu-be/issues/12)) ([#13](https://github.com/100-hours-a-week/11-ellu-be/issues/13)) ([0f0f079](https://github.com/100-hours-a-week/11-ellu-be/commit/0f0f079c4e507d84ddf63e31c924f5272b84bcab))
* 리프레시 토큰 재발급 수정([#44](https://github.com/100-hours-a-week/11-ellu-be/issues/44)) ([de09af2](https://github.com/100-hours-a-week/11-ellu-be/commit/de09af297fc00e55ad7a0ad047a336082263894d))
* 리프레시 토큰 재발급 수정([#44](https://github.com/100-hours-a-week/11-ellu-be/issues/44)) ([#45](https://github.com/100-hours-a-week/11-ellu-be/issues/45)) ([afe5463](https://github.com/100-hours-a-week/11-ellu-be/commit/afe54632385ab3d427bc187a5315f59c44a38f0d))
* 서버 상태 확인 관련된 api들에 대하여 api 시트 수정([#31](https://github.com/100-hours-a-week/11-ellu-be/issues/31)) ([008ef27](https://github.com/100-hours-a-week/11-ellu-be/commit/008ef274a5a70d316514264845dd4fa02c715a16))
* 유저가 포함된 스케줄 조회 시 응답 형식 수정 ([80f981c](https://github.com/100-hours-a-week/11-ellu-be/commit/80f981cb7c1008d815cad874f40d97a5e937be75))
* 프로젝트 스케줄 생성 endpoint가 잘못되어 수정함([#42](https://github.com/100-hours-a-week/11-ellu-be/issues/42)) ([2cff1e2](https://github.com/100-hours-a-week/11-ellu-be/commit/2cff1e22c1270e4528ceb04088ed5d8090c58cdf))
* 프로젝트 스케줄 생성 endpoint가 잘못되어 수정함([#42](https://github.com/100-hours-a-week/11-ellu-be/issues/42)) ([#43](https://github.com/100-hours-a-week/11-ellu-be/issues/43)) ([8b2feca](https://github.com/100-hours-a-week/11-ellu-be/commit/8b2feca3ce339ac32cf4997a9c9fa21e8637149c))
* 프로젝트 스케줄 조회 시 position포함되도록 수정([#46](https://github.com/100-hours-a-week/11-ellu-be/issues/46)) ([adb1f5f](https://github.com/100-hours-a-week/11-ellu-be/commit/adb1f5ff7366f72168d9e17218699a7b28bfb15f))
* 프로젝트 일정 수정/삭제 endpoint 수정 ([7f92505](https://github.com/100-hours-a-week/11-ellu-be/commit/7f92505721e310293348907ea9c2e822d8765850))
* 프로젝트 일정 수정/삭제 endpoint 수정 ([#50](https://github.com/100-hours-a-week/11-ellu-be/issues/50)) ([d713df1](https://github.com/100-hours-a-week/11-ellu-be/commit/d713df19fd487625b72cd435e66592243b05d4bd))
* 프로필이미지 파일에 presigned url 적용 ([8e265b6](https://github.com/100-hours-a-week/11-ellu-be/commit/8e265b6186ba766596e67f0ddf3052aba86df40a))
* 프로필이미지 파일에 presigned url 적용 ([#52](https://github.com/100-hours-a-week/11-ellu-be/issues/52)) ([5ac06f2](https://github.com/100-hours-a-week/11-ellu-be/commit/5ac06f215fe6b8849a4d5296fb3779698edb968e))


### Features

* .env.template 설정 및 application.yml 연동 ([aa63bc5](https://github.com/100-hours-a-week/11-ellu-be/commit/aa63bc5b0139effe3253c7d59c6f60ebc42ed2c6))
* FastAPI 연동을 위한 API 구현([#21](https://github.com/100-hours-a-week/11-ellu-be/issues/21)) ([0d3b249](https://github.com/100-hours-a-week/11-ellu-be/commit/0d3b249b1a3a58304fcd1269ea9930d692850e2e))
* FastAPI 연동을 위한 API 구현([#21](https://github.com/100-hours-a-week/11-ellu-be/issues/21)) ([#22](https://github.com/100-hours-a-week/11-ellu-be/issues/22)) ([581ca34](https://github.com/100-hours-a-week/11-ellu-be/commit/581ca346a13f0f3de5b6c169a14c5b265a39a477))
* FE 요청에 따라 프로젝트 월별 조회 수정 ([ea35eb0](https://github.com/100-hours-a-week/11-ellu-be/commit/ea35eb00190351e24bf1aa569c05d11aa2fc692a))
* Jwt 쿠키 설정 및 Project Domain API 구현([#10](https://github.com/100-hours-a-week/11-ellu-be/issues/10)) ([e32038b](https://github.com/100-hours-a-week/11-ellu-be/commit/e32038b6df05b7247a9f52b7d46a7e9d788a3a48))
* Jwt 쿠키 설정 및 Project Domain API 구현([#10](https://github.com/100-hours-a-week/11-ellu-be/issues/10)) ([#11](https://github.com/100-hours-a-week/11-ellu-be/issues/11)) ([c3eb8cb](https://github.com/100-hours-a-week/11-ellu-be/commit/c3eb8cb977deb87ce53a40ee3b5cafa9d0440bd7))
* looper.my 도메인 CORS 허용 설정 추가 ([37b56dd](https://github.com/100-hours-a-week/11-ellu-be/commit/37b56dd638a0ef04b4aa0af679fd818674c37b6e))
* looper.my 도메인 CORS 허용 설정 추가 ([#35](https://github.com/100-hours-a-week/11-ellu-be/issues/35)) ([6934f95](https://github.com/100-hours-a-week/11-ellu-be/commit/6934f952ba38e248f0b3f12674ade5c41e63d3d2))
* Project Schedule API 구현([#17](https://github.com/100-hours-a-week/11-ellu-be/issues/17)) ([ee1d27a](https://github.com/100-hours-a-week/11-ellu-be/commit/ee1d27abd8bbb744671266d3d0e5a9e2c865d91d))
* Project Schedule API 구현([#17](https://github.com/100-hours-a-week/11-ellu-be/issues/17)) ([#18](https://github.com/100-hours-a-week/11-ellu-be/issues/18)) ([dc34c0b](https://github.com/100-hours-a-week/11-ellu-be/commit/dc34c0be3fd22798aa2357e9bc70b9df2d227560))
* S3이미지 저장 처리 및 환경변수 설정([#23](https://github.com/100-hours-a-week/11-ellu-be/issues/23)) ([bd4c83d](https://github.com/100-hours-a-week/11-ellu-be/commit/bd4c83df7ccf74d145731ade06d96710c999d69d))
* S3이미지 저장 처리 및 환경변수 설정([#23](https://github.com/100-hours-a-week/11-ellu-be/issues/23)) ([#24](https://github.com/100-hours-a-week/11-ellu-be/issues/24)) ([04df652](https://github.com/100-hours-a-week/11-ellu-be/commit/04df652d0802e1d6bfcc6192d2cacff90b3faf8f))
* Schedule Domain API 구현([#15](https://github.com/100-hours-a-week/11-ellu-be/issues/15)) ([0169d5b](https://github.com/100-hours-a-week/11-ellu-be/commit/0169d5b9156a61ea1f0d19e5aef66cb9141a4807))
* Schedule Domain API 구현([#15](https://github.com/100-hours-a-week/11-ellu-be/issues/15)) ([#16](https://github.com/100-hours-a-week/11-ellu-be/issues/16)) ([8ae7ccf](https://github.com/100-hours-a-week/11-ellu-be/commit/8ae7ccf3f7184fd800ce611c5e376407d58b33ac))
* swagger 설정 ([d462426](https://github.com/100-hours-a-week/11-ellu-be/commit/d4624260f733cffdd0ae5a21d700725ef4fec825))
* Swagger 설정 ([#29](https://github.com/100-hours-a-week/11-ellu-be/issues/29)) ([3cf8b8d](https://github.com/100-hours-a-week/11-ellu-be/commit/3cf8b8da1476199f2ddbbd0ecfbf48f24fc3981a))
* User Domain API 구현([#3](https://github.com/100-hours-a-week/11-ellu-be/issues/3)) ([5ebf2da](https://github.com/100-hours-a-week/11-ellu-be/commit/5ebf2da3a4847346f2019cc8fc472d5f42778709))
* User Domain API 구현([#3](https://github.com/100-hours-a-week/11-ellu-be/issues/3)) ([#4](https://github.com/100-hours-a-week/11-ellu-be/issues/4)) ([c8aa093](https://github.com/100-hours-a-week/11-ellu-be/commit/c8aa09334c0264fead2b48b95a841f1709b32286))
* wiki api([#32](https://github.com/100-hours-a-week/11-ellu-be/issues/32)) ([60799f2](https://github.com/100-hours-a-week/11-ellu-be/commit/60799f26494184a29e256b01a639306cf0436c5f))

# 1.0.0 (2025-04-30)


### Features

* User Domain API 구현([#3](https://github.com/100-hours-a-week/11-ellu-be/issues/3)) ([5ebf2da](https://github.com/100-hours-a-week/11-ellu-be/commit/5ebf2da3a4847346f2019cc8fc472d5f42778709))
* User Domain API 구현([#3](https://github.com/100-hours-a-week/11-ellu-be/issues/3)) ([#4](https://github.com/100-hours-a-week/11-ellu-be/issues/4)) ([c8aa093](https://github.com/100-hours-a-week/11-ellu-be/commit/c8aa09334c0264fead2b48b95a841f1709b32286))
