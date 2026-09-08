# Verification

Executed 2026-09-08 in isolated art-platform worktree:

```sh
DOCKER_HOST=unix:///Users/wangyunfeng/.docker/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -pl services/teaching-service,services/portfolio-service -am verify -q -Dapi.version=1.44
```

Exit 0. Teaching: five unit persistence/access/version tests plus one MySQL 8.4 integration test. Portfolio: ten unit tests (draft immutability, stale writes, live family authorization, class membership, private/tenant boundaries, explicit public authorization, withdrawal, transaction/outbox rollback) plus one MySQL 8.4 concurrent retry integration test. Real MySQL exposed a repeatable-read idempotency race; fixed with current locking reads. Both simultaneous retry responses now return one publication ID, with one publication and one outbox row.

MySQL Flyway created only each service's owned tables plus runtime tables. Teaching seed preserves 5 stages and 22 dimensions; the service has no student table. Test containers are disposable and cleaned by Testcontainers.

The command verifies isolated backend behavior and real database migrations; it does not claim OSS credentials, WeChat real-device acceptance, deployment performance or the full PC end-to-end journey have been verified. The integration runtime still reports a Flyway support-version warning for MySQL 8.4; migrations execute successfully. JVM prints Mockito dynamic-agent notices under the host JDK.

## Review remediation verification

The six independent review findings were implemented and reverified on 2026-09-08:

- Authorized record paging uses descending `(created_at,id)` keyset scans and applies its result limit after live ACL; a fixture with 205 mixed-access records fills all 100 authorized slots. A 105-student roster retains every existing record.
- Historical publication/report media access remains possible after later versions replace the current draft; explicit withdrawal prevents new signing.
- `needsPublishing` counts edited published records independently from household publication status.
- Per-lesson entries preserve first-class lesson/time/note/media history, support supplement lessons, explicit classroom publication and withdrawal. Household classroom feeds work before final topic completion.
- Media references use local transaction Outbox events with owner revisions. Rollback tests prove no reference event or remote mutation survives failed creation/publication.
- Multiple collaborating children require separate current guardian consents. Dates/archive, curated private collections, and escaped SVG certificate/share-card generation are tested; unswitched staff-plus-parent identity cannot grant guardian consent.

Full `verify` above returned 0, including both MySQL 8.4 integration tests and new V2 migration. After the final paging-order refinement, targeted `test` also returned 0. Current owned test totals: **22 unit tests + 2 MySQL integration tests**, all passing (runtime prerequisite tests also passed).

Final spec re-review fixes: reproduced both missing 100-media aggregate validation and missing historical-roster dirty state in failing tests, then fixed them. `mvn -pl services/teaching-service,services/portfolio-service spotless:apply -q` exited 0. Isolated `mvn -f services/portfolio-service/pom.xml test -q -DskipITs` and the equivalent teaching command both exited 0: **24 unit tests** now pass. No schema changed in this final refinement; the preceding two real MySQL integration tests remain the migration/concurrency evidence.

## Media provenance security regression

A subsequent independent P1 investigation reproduced that a teacher could attach another uploader's same-campus private media by guessing its ID: the old implementation only inspected usable/tenant. Both rejection tests failed against that implementation. Domain checks now require current uploader ownership for fresh references, or media retained from an already authorized record/content, or an explicit live-authorized source (`sourceRecordId`, published-template/resource copy endpoints). A substitute teacher can retain and publish existing work, but cannot inject a different private file. Source denial is tested independently when destination access remains allowed.

Added seven teaching/portfolio provenance regression cases; isolated module commands both exited 0 after Spotless formatting. Current targeted totals: **32 unit tests passed** (including the other agent's additional exhibition case). No runtime or media protocol modification was needed; media's existing ownerId is treated as required trusted metadata, and missing uploader metadata cannot authorize a fresh reference. The older mixed-role fixture was aligned with the current runtime's PARENT-priority rule: a real unswitched STAFF token with an otherwise valid guardian account remains rejected for guardian consent.
