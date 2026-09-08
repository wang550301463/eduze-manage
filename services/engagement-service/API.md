# Engagement API
All paths begin `/api/v1/engagement`, return `{code:0,data,...}`. IDs strings, timestamps RFC3339. Staff needs `engagement:write` + branch access. Maximum lists 100/200. Public studio IDs are branch IDs.

- GET `/public/studios` -> Studio[] `{branchId,name,description,address,phone}`; GET `/public/studios/{branchId}` -> Studio.
- POST `/studios` staff body `{branchId,name,description,address,phone,published}` -> Studio (create/update).
- POST `/public/studios/{branchId}/enquiries` body `{studentName,phone,age,source,sourceId,referrerId,website:""}` -> `{id,status:"NEW"}`. `website` honeypot must be empty. Same contact to same branch within 30m returns429. Source and referrer are attribution only, never trusted enrollment/bonus evidence.
- GET `/enquiries?branchId=` -> Enquiry[] `{id,branchId,studentName,phone,age,source,sourceId,status,assigneeId,reservationId}`.
- GET `/enquiries/{id}/followups` -> history array `{authorId,note,status,nextAt,createdAt}`.
- POST `/enquiries/{id}/followups` body `{status,assigneeId,note,nextAt?}` -> null. Editable status NEW/CONTACTED/TRIAL_PENDING/TRIAL_BOOKED/ATTENDED/LOST. ENROLLED is set by authoritative academic event only. After enrollment, followups retain ENROLLED regardless of an older form stage and continue recording notes; input ENROLLED is accepted only for an already-enrolled lead.
- POST `/enquiries/{id}/trial` body `{sessionId}` -> `{reservationId,status:"CONFIRMED"}`. sessionId is actual lesson ID; academic decides capacity. Repeat same lead uses same reservation id.
- POST `/activities` staff body `{branchId,title,description,startsAt,capacity,published}` -> Activity `{id,branchId,title,description,startsAt,capacity,reserved,published}`.
- GET `/public/activities?branchId=` -> Activity[].
- POST `/activities/{id}/signups` logged-in body none -> `{id,activityId,userId,status:"CONFIRMED"}`. Same actor idempotent; capacity atomically checked.
- POST `/signups/{id}/cancel` own body none -> null; releases one seat once.
- GET `/renewals?branchId=` -> `{id,studentId,assigneeId,note,nextAt,status}`[].
- POST `/renewals` body `{branchId,studentId,assigneeId,note,nextAt?,status:"OPEN"|"CONTACTED"|"CLOSED"}` -> `{id}`.

Paid activity goods belong to commerce; engagement activities are free studio events. Publish one paid event in commerce to avoid separately counting free-event seats.

Operational additions:
- GET `/signups` -> current actor `{id,activityId,activityTitle,startsAt,status}`[], persisted across devices.
- GET `/activities/{id}/signups` staff -> `{id,userId,status,createdAt}`[]. POST `/signups/{id}/checkin` -> null; CONFIRMED->CHECKED_IN idempotently.
- PUT `/renewals/{id}` body same RenewalInput, branch/student immutable -> null.
- GET `/dashboard?branchId=` -> `{stages:[{status,count}],sources:[{source,enquiries,enrollments}],definition}`.
- GET `/referrals?branchId=` -> `{enquiryId,referrerId,enrollmentId,status,fulfillmentNote}`[]. POST `/referrals/{enquiryId}/fulfill` body `{note}` records externally delivered reward once, does not initiate money transfer.
- POST `/enquiries/{id}/enroll` staff body `{classGroupId}` -> `{status:"ENROLLED",studentId,enrollmentId}`. Requires existing confirmed trial and staff `classgroup:assign`; academic performs real class membership assignment and emits enrollment-confirmed. Lead ENROLLED and referral eligibility update from that event. Same lead enrollment is idempotent; never submit without explicit staff class selection.

An ENROLLED enquiry cannot reserve another trial (409); its authoritative conversion status is never downgraded by followup or trial operations.
