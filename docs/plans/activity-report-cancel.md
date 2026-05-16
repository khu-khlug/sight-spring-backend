## 접수중인 활동보고 취소 API

### 비즈니스 규칙

- 그룹장 또는 운영진만 취소 가능
- 해당 활동보고에 연결된 세미나의 `schedule.endAt`이 지나지 않았어야 함 — 지났으면 400

### ExPoint 변동

| 행동        | 포인트 | 대상      |
| ----------- | ------ | --------- |
| 활동보고 취소 | -50  | 멤버 전체 |

ExPoint 변동 시 멤버별로 `members.expoint` 직접 UPDATE + `expoint_log` INSERT 처리.

### 관련 DB 테이블

| 테이블                  | 역할                                                |
| ----------------------- | --------------------------------------------------- |
| `group`                 | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인 |
| `group_activity_report` | 활동보고 DELETE                                     |
| `big_seminar`+`schedule`| 접수 기간 내 여부 확인 (`schedule.endAt`)           |
| `file_upload`        | 해당 파일 row DELETE                                |
| `group_member`          | 전체 멤버 목록 조회 (ExPoint 일괄 처리 시 사용)     |
| `members`               | `expoint` 직접 UPDATE                               |
| `expoint_log`           | ExPoint 변동 이력 INSERT                            |

### API

```
DELETE /groups/:groupId/activity-report/:reportId
```

#### 응답

```
204 No Content
```

#### 테스트 케이스

1. 존재하지 않는 `reportId` 요청 → 404
2. 접수 기간(`schedule.endAt`)이 지난 활동보고 취소 요청 → 400
3. 그룹원(비그룹장, 비운영진) 삭제 요청 → 403
4. 그룹장이 삭제 요청 → 204
5. 운영진이 삭제 요청 → 204
6. 삭제 성공 시
   - `group_activity_report.reportFileKey`로 R2 파일 삭제
   - `file_upload` row DELETE
   - `group_activity_report` row DELETE
   - 그룹원 전원 경험치 -50
   - 그룹 멤버 전체 + 운영진에게 알림 발송
