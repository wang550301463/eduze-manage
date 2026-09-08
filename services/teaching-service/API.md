# Teaching API v1
All external responses `{code:0,message:"OK",data:T,traceId:string}`. IDs string. Error 400 validation,403 access,404,409 stale version. Actor comes from Bearer. All write teaching actions require staff `course:write`, reads `course:read`.

Base `/api/v1/teaching`. Lists arrays (limit max 100 via `limit` query).
- GET `/templates?q=` -> Template[]; POST `/templates` TemplateInput -> Template; GET `/templates/{id}` -> Template; PUT `/templates/{id}` TemplateInput -> Template; POST `/templates/{id}/publish` `{version:number}` -> TemplateVersion; GET `/templates/{id}/versions` -> TemplateVersion[].
- TemplateInput `{version?:number,title:string,ageMin:number,ageMax:number,goals:string,materials:string,expectedLessons:number,steps:[{title:string,content:string}],tags:string[],mediaIds:string[]}`
- Template `{id,ownerId,version:number,content:TemplateInput,createdAt:string}`; TemplateVersion `{id,templateId,version:number,content:TemplateInput,createdAt:string}`. Editing own draft only; published versions shared institution wide. GET `/template-versions?q=` lists accessible published versions.
- GET `/resources?q=` -> Resource[]; POST `/resources` ResourceInput -> Resource; PUT `/resources/{id}` ResourceInput -> Resource; POST `/resources/{id}/publish` `{version}` -> Resource. ResourceInput `{version?:number,title:string,kind:"IMAGE"|"PDF"|"VIDEO"|"PRESENTATION"|"AUDIO",mediaIds:string[],tags:string[],description:string}`; Resource `{id,ownerId,version:number,published:boolean,content:ResourceInput,createdAt}`. Publish freezes version; to revise create/copy another resource.
- GET `/plans?branchId=` -> Plan[]; POST `/plans` `{branchId,name,startsOn:"YYYY-MM-DD",endsOn:"YYYY-MM-DD",themeIds:string[]}` -> Plan `{id,...input,createdAt}`.
- GET `/themes?branchId=&groupId=` -> Theme[]; POST `/themes` ThemeInput -> Theme; GET `/themes/{id}` -> Theme; PUT `/themes/{id}` ThemeInput -> Theme; POST `/themes/{id}/transition` `{version:number,status:"PLANNED"|"IN_PROGRESS"|"FINISHED",reason:string}` -> Theme.
- ThemeInput `{version?:number,branchId:string,groupId:string,templateVersionId:string,title:string,lessonIds:string[],handoffNote:string}`. Theme `{id,version:number,status,content:ThemeInput,template:TemplateInput,createdAt}`. PUT can extend lessonIds even after finish; transition reason required for reopen. `templateVersionId` immutable after creation.
- GET `/themes/{id}/roster` -> [{id,name,branchId}].
- GET `/dashboard?branchId=` -> `{planned:number,inProgress:number,finished:number}`.
- GET `/stages`, `/dimensions` preserve legacy curriculum DTOs string ids.
Internal raw GET `/internal/teaching/themes/{id}` -> Theme, `/internal/teaching/stages/{id}`, POST `/internal/teaching/stages/batch` `{ids:string[]}` -> [{id,code,name}].

Additional integration details:
- Writes accept `teaching:write` as a scoped alternative to legacy `course:write`.
- POST `/{resources|templates|template-versions}/{id}/media-access` `{mediaIds:string[]}` -> `{items:[{id,url,expiresAt}]}`; IDs must be a subset of the authorized content.
- Class theme lists and dashboards use `/internal/academic/groups/batch-access` for current teacher authorization; null/unavailable authorization fails closed.
- Top-level `version` is the optimistic concurrency value; `content.version` is the original submitted value in the immutable content snapshot.

Media references now use transactionally committed `teaching.media-references` events targeting `media`, payload `{ownerType,ownerId,mediaIds,revision}`. Draft edits increase revision; immutable template-version owners use revision 1. Usability is checked synchronously before local writes; rollback never sends a remote reference replacement. Signing immediately after saving may require retry until the reference event is processed.

V1 limits each template/resource draft or published revision to **100 unique media IDs**, including audio. Save/publish rejects higher totals with HTTP 400, `单次内容最多关联 100 个不同媒体文件（含音频）`, before database/reference-event writes. Reference-event lists are deduplicated.

## Media provenance authorization

`media.usable` only proves ready status, tenant and uploader metadata. A **new** media reference must be uploaded by the authenticated actor. Existing media in an already-authorized draft can be retained on edit/publish, so substitute teachers and copied content continue working. Knowing another staff member's same-campus private media ID is insufficient and returns HTTP 403 before any write/event.

Legitimate cross-uploader reuse is explicit: POST `/template-versions/{id}/copy` copies an accessible immutable published template into a new private draft; POST `/resources/{id}/copy` copies an accessible resource (publicly published within the institution, or a private draft the actor can already read). Both return their usual Template/Resource shape. Do not implement public-template copying by raw create with its media IDs; use these source-authorized endpoints, then edit the resulting private draft.
