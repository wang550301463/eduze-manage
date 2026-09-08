export interface paths {
    "/api/admin/jobs/absence:run": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["runAbsenceJob"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/attendance/check-in": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["checkIn"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/attendance/check-out": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["checkOut"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/attendance/student/{studentId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["studentHistory"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/attendance/today": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["today"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/attendance/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch: operations["update_7"];
        trace?: never;
    };
    "/api/class-groups": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_8"];
        put?: never;
        post: operations["create_10"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/class-groups/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_3"];
        put: operations["update_6"];
        post?: never;
        delete: operations["delete_6"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/class-groups/{id}/members": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listMembers"];
        put?: never;
        post: operations["addMembers"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/class-groups/{id}/members/{studentId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["removeMember"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/class-rooms": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_7"];
        put?: never;
        post: operations["create_9"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/class-rooms/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_2"];
        put: operations["update_5"];
        post?: never;
        delete: operations["delete_5"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/courses": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_6"];
        put?: never;
        post: operations["create_8"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/courses/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_1"];
        put: operations["update_4"];
        post?: never;
        delete: operations["delete_4"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/guardians": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["create_7"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/guardians/{id}/qr": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["generateQr"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/leaves": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_5"];
        put?: never;
        post: operations["create_6"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/leaves/{id}/approve": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["approve_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/leaves/{id}/reject": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["reject"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_4"];
        put?: never;
        post: operations["create_5"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/bulk-generate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["bulkGenerate"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/check-conflict": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["checkConflict"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_4"];
        put?: never;
        post?: never;
        delete: operations["delete_7"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/{id}/cancel": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["cancel"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/{id}/change-logs": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["changeLogs"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/{id}/reschedule": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["reschedule"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/{id}/students": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listStudents"];
        put?: never;
        post: operations["addStudent"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/lessons/{id}/students/{studentId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["removeStudent"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/packages/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["update_3"];
        post?: never;
        delete: operations["delete_3"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/pickup-records": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_3"];
        put?: never;
        post: operations["create_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/pickup-records/abnormal": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["abnormal"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/schedule/by-teacher": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["byTeacher"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/schedule/my-week": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["myWeek"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/schedule/week": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["week"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/stats/attendance/branch/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["branch"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/stats/attendance/class-group/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["classGroup"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/stats/attendance/dashboard": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["dashboard"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/stats/attendance/student/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["student"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/student-lesson-histories": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_9"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_2"];
        put?: never;
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/bulk/assign-class": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["assignClass"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/bulk/transfer-class": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["transfer"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/import": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["importExcel"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/import/template": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["downloadTemplate"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get"];
        put: operations["update_2"];
        post?: never;
        delete: operations["delete_2"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{id}/lesson-hour-ledger": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listLessonHourLedger"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{id}/lesson-hour-ledger/adjust": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["adjustLessonHours"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{id}/mentor": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["changeMentor"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{id}/mentor-history": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["mentorHistory"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch: operations["updateStatus"];
        trace?: never;
    };
    "/api/students/{studentId}/guardians": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["guardians"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{studentId}/guardians/upsert": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["upsertGuardian"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{studentId}/guardians/{guardianId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["linkGuardian"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/students/{studentId}/packages": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listByStudent"];
        put?: never;
        post: operations["create_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/subscriptions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_1"];
        put?: never;
        post: operations["create_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/subscriptions/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["update_1"];
        post?: never;
        delete: operations["delete_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/teacher-availabilities/conflicts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["checkConflict_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/teacher-availabilities/unbound": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listUnbound"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/teacher-availabilities/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["update"];
        post?: never;
        delete: operations["delete"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/teachers/{teacherId}/availabilities": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list"];
        put?: never;
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/bindings": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["bindings"];
        put?: never;
        post: operations["claim"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/bindings/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["revoke"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/bindings/{id}/approve": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["approve"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/children": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["children"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/children/{id}/balance": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["balance"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/children/{id}/leaves": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["leaves"];
        put?: never;
        post: operations["leave"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/children/{id}/schedule": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["schedule"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/academic/family/invites": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["invite"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        AddMembersRequest: {
            studentIds: string[];
        };
        ApiResponseAttendanceResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["AttendanceResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseBinding: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["Binding"];
            message?: string;
            traceId?: string;
        };
        ApiResponseBranchAttendanceStat: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["BranchAttendanceStat"];
            message?: string;
            traceId?: string;
        };
        ApiResponseBulkGenerateResult: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["BulkGenerateResult"];
            message?: string;
            traceId?: string;
        };
        ApiResponseClassGroupAttendanceStat: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["ClassGroupAttendanceStat"];
            message?: string;
            traceId?: string;
        };
        ApiResponseClassGroupResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["ClassGroupResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseClassRoomResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["ClassRoomResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseConflictReport: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["ConflictReport"];
            message?: string;
            traceId?: string;
        };
        ApiResponseCoursePackageResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["CoursePackageResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseCourseResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["CourseResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseGuardianQrResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["GuardianQrResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseGuardianResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["GuardianResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseInvite: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["Invite"];
            message?: string;
            traceId?: string;
        };
        ApiResponseLeave: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["Leave"];
            message?: string;
            traceId?: string;
        };
        ApiResponseLeaveResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LeaveResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseLessonResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LessonResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseLessonStudentResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LessonStudentResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseListAttendanceResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["AttendanceResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListBinding: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["Binding"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListClassMemberResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["ClassMemberResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListCoursePackageResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["CoursePackageResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListGuardianSummaryResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["GuardianSummaryResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListLeave: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["Leave"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListLeaveResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LeaveResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListLessonChangeLogResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LessonChangeLogResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListLessonResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LessonResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListLessonStudentResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["LessonStudentResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListMentorHistoryResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["MentorHistoryResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListPickupRecordResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PickupRecordResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListSchedule: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["Schedule"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListSearchHit: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["SearchHit"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListStudentLessonHourLedgerResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["StudentLessonHourLedgerResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListStudentView: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["StudentView"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListSubscriptionResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["SubscriptionResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseListTeacherAvailabilityResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["TeacherAvailabilityResponse"][];
            message?: string;
            traceId?: string;
        };
        ApiResponseMapStringBoolean: {
            /** Format: int32 */
            code?: number;
            data?: {
                [key: string]: boolean;
            };
            message?: string;
            traceId?: string;
        };
        ApiResponseMapStringObject: {
            /** Format: int32 */
            code?: number;
            data?: {
                [key: string]: unknown;
            };
            message?: string;
            traceId?: string;
        };
        ApiResponsePageResultClassGroupResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PageResultClassGroupResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponsePageResultClassRoomResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PageResultClassRoomResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponsePageResultCourseResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PageResultCourseResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponsePageResultStudentLessonHistoryResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PageResultStudentLessonHistoryResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponsePageResultStudentResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PageResultStudentResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponsePickupRecordResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["PickupRecordResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseScheduleResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["ScheduleResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseStudentAttendanceStat: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["StudentAttendanceStat"];
            message?: string;
            traceId?: string;
        };
        ApiResponseStudentImportResult: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["StudentImportResult"];
            message?: string;
            traceId?: string;
        };
        ApiResponseStudentLessonHourLedgerResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["StudentLessonHourLedgerResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseStudentResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["StudentResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseSubscriptionResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["SubscriptionResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseTeacherAvailabilityResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["TeacherAvailabilityResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseTeacherScheduleResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["TeacherScheduleResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseTodayRosterResponse: {
            /** Format: int32 */
            code?: number;
            data?: components["schemas"]["TodayRosterResponse"];
            message?: string;
            traceId?: string;
        };
        ApiResponseVoid: {
            /** Format: int32 */
            code?: number;
            data?: unknown;
            message?: string;
            traceId?: string;
        };
        AssignClassRequest: {
            classGroupId?: string;
            studentIds?: string[];
        };
        AssignMentorRequest: {
            keepSubscriptions?: boolean;
            reason?: string;
            toTeacherId: string;
        };
        AttendanceResponse: {
            /** Format: date-time */
            checkInAt?: string;
            checkInMethod?: string;
            /** Format: date-time */
            checkOutAt?: string;
            classGroupName?: string;
            id?: string;
            /** Format: date-time */
            lessonEndAt?: string;
            lessonId?: string;
            /** Format: date-time */
            lessonStartAt?: string;
            note?: string;
            /** Format: int32 */
            status?: number;
            statusLabel?: string;
            studentId?: string;
            studentName?: string;
        };
        Binding: {
            id?: string;
            status?: string;
            studentId?: string;
            userId?: string;
        };
        BranchAttendanceStat: {
            /** Format: int32 */
            absent?: number;
            branchId?: string;
            classGroups?: components["schemas"]["ClassGroupAttendanceStat"][];
            /** Format: int32 */
            leave?: number;
            /** Format: int32 */
            present?: number;
            rate?: number;
            /** Format: int32 */
            total?: number;
        };
        BulkGenerateRequest: {
            branchId?: string;
            /** Format: date */
            fromDate: string;
            holidays?: string[];
            teacherIds?: string[];
            /** Format: int32 */
            weeks: number;
        };
        BulkGenerateResult: {
            conflicts?: components["schemas"]["ConflictItem"][];
            /** Format: int32 */
            generated?: number;
            /** Format: int32 */
            rosterAdded?: number;
            /** Format: int32 */
            skipped?: number;
        };
        CancelLessonRequest: {
            reason: string;
        };
        CheckInRequest: {
            guardianId?: string;
            lessonId?: string;
            method: string;
            qrCode?: string;
            studentId?: string;
        };
        CheckOutRequest: {
            abnormalNote?: string;
            attendanceId: string;
            guardianId?: string;
            isAbnormal?: boolean;
        };
        ClaimRequest: {
            code?: string;
        };
        ClassGroupAttendanceStat: {
            /** Format: int32 */
            absent?: number;
            classGroupId?: string;
            classGroupName?: string;
            /** Format: int32 */
            leave?: number;
            /** Format: int32 */
            present?: number;
            rate?: number;
            /** Format: int32 */
            total?: number;
        };
        ClassGroupRef: {
            id?: string;
            name?: string;
        };
        ClassGroupRequest: {
            branchId: string;
            /** Format: int32 */
            capacity?: number;
            courseId?: string;
            headTeacherId?: string;
            name: string;
            nestedAvailability?: components["schemas"]["NestedTeacherAvailabilityRequest"];
            /** Format: int32 */
            status?: number;
            tagColor?: string;
            teacherAvailabilityId?: string;
        };
        ClassGroupResponse: {
            branchId?: string;
            /** Format: int32 */
            capacity?: number;
            courseId?: string;
            courseName?: string;
            /** Format: int32 */
            currentCount?: number;
            /** Format: int32 */
            dayOfWeek?: number;
            /** Format: int32 */
            endMinute?: number;
            headTeacherId?: string;
            headTeacherName?: string;
            id?: string;
            message?: string;
            name?: string;
            /** Format: int32 */
            startMinute?: number;
            /** Format: int32 */
            status?: number;
            tagColor?: string;
            teacherAvailabilityId?: string;
            teacherName?: string;
            tenantId?: string;
        };
        ClassMemberResponse: {
            enrollNo?: string;
            /** Format: date-time */
            joinedAt?: string;
            /** Format: date-time */
            leftAt?: string;
            studentId?: string;
            studentName?: string;
        };
        ClassRoomRequest: {
            branchId: string;
            /** Format: int32 */
            capacity?: number;
            name: string;
            note?: string;
        };
        ClassRoomResponse: {
            branchId?: string;
            /** Format: int32 */
            capacity?: number;
            id?: string;
            name?: string;
            note?: string;
            tenantId?: string;
        };
        ConflictCheckRequest: {
            branchId: string;
            classGroupId: string;
            classRoomId?: string;
            /** Format: date-time */
            endAt: string;
            lessonId?: string;
            /** Format: date-time */
            startAt: string;
            teacherId?: string;
        };
        ConflictItem: {
            /** Format: date */
            date?: string;
            reason?: string;
            teacherId?: string;
        };
        ConflictReport: {
            boundAvailability?: components["schemas"]["LessonResponse"];
            classGroup?: components["schemas"]["LessonResponse"];
            classRoom?: components["schemas"]["LessonResponse"];
            hasConflict?: boolean;
            teacher?: components["schemas"]["LessonResponse"];
        };
        CoursePackageRequest: {
            courseId?: string;
            /** Format: date */
            expireDate?: string;
            note?: string;
            /** Format: int32 */
            remainingLessons: number;
            /** Format: int32 */
            totalLessons: number;
        };
        CoursePackageResponse: {
            alertLow?: boolean;
            branchId?: string;
            courseId?: string;
            /** Format: date */
            expireDate?: string;
            /** Format: int32 */
            frozenLessons?: number;
            id?: string;
            note?: string;
            /** Format: int32 */
            remainingLessons?: number;
            studentId?: string;
            /** Format: int32 */
            totalLessons?: number;
        };
        CourseRequest: {
            /** Format: int32 */
            ageMax?: number;
            /** Format: int32 */
            ageMin?: number;
            coverUrl?: string;
            description?: string;
            /** Format: int32 */
            lessonMinutes?: number;
            name: string;
        };
        CourseResponse: {
            /** Format: int32 */
            ageMax?: number;
            /** Format: int32 */
            ageMin?: number;
            coverUrl?: string;
            description?: string;
            id?: string;
            /** Format: int32 */
            lessonMinutes?: number;
            name?: string;
            tenantId?: string;
        };
        DaySchedule: {
            /** Format: date */
            date?: string;
            lessons?: components["schemas"]["ScheduleLessonItem"][];
        };
        FieldError: {
            column?: string;
            message?: string;
        };
        GuardianQrResponse: {
            guardianId?: string;
            qrCode?: string;
        };
        GuardianRequest: {
            /** Format: int32 */
            canPickup?: number;
            /** Format: int32 */
            isMainContact?: number;
            name: string;
            phone: string;
            qrCode?: string;
        };
        GuardianResponse: {
            /** Format: int32 */
            canPickup?: number;
            id?: string;
            /** Format: int32 */
            isMainContact?: number;
            name?: string;
            phone?: string;
            qrCode?: string;
            relation?: string;
        };
        GuardianSummaryResponse: {
            /** Format: int32 */
            canPickup?: number;
            id?: string;
            /** Format: int32 */
            isMainContact?: number;
            name?: string;
            phone?: string;
            relation?: string;
        };
        GuardianUpsertRequest: {
            /** Format: int32 */
            canPickup?: number;
            /** Format: int32 */
            isMainContact?: number;
            name: string;
            phone?: string;
            relation?: string;
        };
        ImportFailure: {
            errors?: components["schemas"]["FieldError"][];
            /** Format: int32 */
            rowIndex?: number;
        };
        InitialSubscription: {
            teacherAvailabilityId: string;
            /** Format: date */
            validFrom: string;
            /** Format: date */
            validTo?: string;
        };
        Invite: {
            code?: string;
            /** Format: date-time */
            expiresAt?: string;
            id?: string;
        };
        InviteRequest: {
            studentId?: string;
        };
        Leave: {
            id?: string;
            lessonId?: string;
            reason?: string;
            status?: string;
        };
        LeaveCreateRequest: {
            /** Format: date */
            leaveEndDate: string;
            /** Format: date */
            leaveStartDate: string;
            lessonId?: string;
            reason?: string;
            studentId: string;
        };
        LeaveRequest: {
            lessonId?: string;
            reason?: string;
        };
        LeaveResponse: {
            /** Format: date-time */
            approvedAt?: string;
            approvedBy?: string;
            branchId?: string;
            /** Format: date-time */
            createdAt?: string;
            id?: string;
            /** Format: date */
            leaveEndDate?: string;
            /** Format: date */
            leaveStartDate?: string;
            lessonId?: string;
            reason?: string;
            /** Format: int32 */
            status?: number;
            statusLabel?: string;
            studentId?: string;
            studentName?: string;
        };
        LessonCell: {
            /** Format: int32 */
            capacity?: number;
            classRoomId?: string;
            classRoomName?: string;
            /** Format: int32 */
            dayOfWeek?: number;
            /** Format: date-time */
            endAt?: string;
            /** Format: int32 */
            endMinute?: number;
            lessonId?: string;
            /** Format: int32 */
            source?: number;
            /** Format: date-time */
            startAt?: string;
            /** Format: int32 */
            startMinute?: number;
            status?: string;
            /** Format: int32 */
            studentCount?: number;
            teacherAvailabilityId?: string;
        };
        LessonChangeLogResponse: {
            afterJson?: string;
            beforeJson?: string;
            changeType?: string;
            /** Format: date-time */
            createdAt?: string;
            id?: string;
            operatorId?: string;
            reason?: string;
        };
        LessonHourAdjustRequest: {
            eventType?: string;
            /** Format: int32 */
            minutesDelta: number;
            note?: string;
            packageId?: string;
            relatedLedgerId?: string;
        };
        LessonRequest: {
            branchId: string;
            classGroupId?: string;
            classRoomId?: string;
            /** Format: date-time */
            endAt: string;
            note?: string;
            /** Format: int32 */
            source?: number;
            /** Format: date-time */
            startAt: string;
            teacherId?: string;
        };
        LessonResponse: {
            branchId?: string;
            classGroupId?: string;
            classGroupName?: string;
            classRoomId?: string;
            classRoomName?: string;
            courseId?: string;
            courseName?: string;
            /** Format: date-time */
            endAt?: string;
            id?: string;
            note?: string;
            /** Format: date-time */
            startAt?: string;
            status?: string;
            teacherId?: string;
            teacherName?: string;
            tenantId?: string;
        };
        LessonStudentRequest: {
            note?: string;
            source?: string;
            studentId: string;
        };
        LessonStudentResponse: {
            id?: string;
            lessonId?: string;
            note?: string;
            source?: string;
            status?: string;
            studentId?: string;
            studentName?: string;
            subscriptionId?: string;
        };
        LinkGuardianRequest: {
            relation: string;
        };
        MentorHistoryResponse: {
            /** Format: date-time */
            changedAt?: string;
            fromTeacherId?: string;
            fromTeacherName?: string;
            id?: string;
            operatorId?: string;
            reason?: string;
            toTeacherId?: string;
            toTeacherName?: string;
        };
        NestedTeacherAvailabilityRequest: {
            branchId: string;
            /** Format: int32 */
            capacity: number;
            /** Format: int32 */
            dayOfWeek: number;
            defaultClassRoomId?: string;
            /** Format: int32 */
            endMinute: number;
            note?: string;
            /** Format: int32 */
            startMinute: number;
            /** Format: int32 */
            status?: number;
            teacherId: string;
            /** Format: date */
            validFrom: string;
            /** Format: date */
            validTo?: string;
        };
        PageResultClassGroupResponse: {
            page?: string;
            records?: components["schemas"]["ClassGroupResponse"][];
            size?: string;
            total?: string;
        };
        PageResultClassRoomResponse: {
            page?: string;
            records?: components["schemas"]["ClassRoomResponse"][];
            size?: string;
            total?: string;
        };
        PageResultCourseResponse: {
            page?: string;
            records?: components["schemas"]["CourseResponse"][];
            size?: string;
            total?: string;
        };
        PageResultStudentLessonHistoryResponse: {
            page?: string;
            records?: components["schemas"]["StudentLessonHistoryResponse"][];
            size?: string;
            total?: string;
        };
        PageResultStudentResponse: {
            page?: string;
            records?: components["schemas"]["StudentResponse"][];
            size?: string;
            total?: string;
        };
        PickupRecordRequest: {
            abnormalNote?: string;
            attendanceId: string;
            /** Format: date-time */
            eventTime?: string;
            eventType: string;
            guardianId?: string;
            isAbnormal?: boolean;
        };
        PickupRecordResponse: {
            abnormalNote?: string;
            attendanceId?: string;
            /** Format: date-time */
            eventTime?: string;
            eventType?: string;
            guardianId?: string;
            guardianName?: string;
            id?: string;
            /** Format: int32 */
            isAbnormal?: number;
            lessonId?: string;
            studentId?: string;
            studentName?: string;
        };
        RescheduleRequest: {
            classRoomId?: string;
            /** Format: date-time */
            endAt: string;
            reason: string;
            /** Format: date-time */
            startAt: string;
            teacherId?: string;
        };
        RosterItem: {
            attendanceId?: string;
            /** Format: date-time */
            checkInAt?: string;
            /** Format: date-time */
            checkOutAt?: string;
            classGroupName?: string;
            lessonId?: string;
            /** Format: date-time */
            lessonStartAt?: string;
            /** Format: int32 */
            status?: number;
            statusLabel?: string;
            studentId?: string;
            studentName?: string;
        };
        Schedule: {
            classGroupId?: string;
            endTime?: string;
            id?: string;
            startTime?: string;
            status?: string;
            teacherId?: string;
        };
        ScheduleLessonItem: {
            classGroupId?: string;
            classGroupName?: string;
            classRoomId?: string;
            classRoomShortName?: string;
            color?: string;
            courseId?: string;
            courseName?: string;
            endAt?: string;
            id?: string;
            startAt?: string;
            status?: string;
            teacherId?: string;
            teacherShortName?: string;
        };
        ScheduleResponse: {
            days?: components["schemas"]["DaySchedule"][];
            /** Format: date */
            weekEnd?: string;
            /** Format: date */
            weekStart?: string;
        };
        SearchHit: {
            branch?: string;
            branchId?: string;
            id?: string;
            subtitle?: string;
            title?: string;
            type?: string;
            url?: string;
        };
        StudentAttendanceStat: {
            /** Format: int32 */
            absent?: number;
            /** Format: int32 */
            leave?: number;
            /** Format: int32 */
            present?: number;
            rate?: number;
            /** Format: int32 */
            total?: number;
        };
        StudentImportResult: {
            failures?: components["schemas"]["ImportFailure"][];
            /** Format: int32 */
            successCount?: number;
        };
        StudentLessonHistoryResponse: {
            attendanceId?: string;
            /** Format: int32 */
            attendanceStatus?: number;
            branchId?: string;
            classGroupId?: string;
            classGroupName?: string;
            classRoomId?: string;
            classRoomName?: string;
            courseId?: string;
            courseName?: string;
            /** Format: date-time */
            createdAt?: string;
            /** Format: date-time */
            endAt?: string;
            id?: string;
            lessonId?: string;
            /** Format: int32 */
            minutes?: number;
            /** Format: date-time */
            occurredAt?: string;
            snapshotJson?: string;
            /** Format: int32 */
            source?: number;
            sourceLabel?: string;
            /** Format: date-time */
            startAt?: string;
            studentId?: string;
            studentName?: string;
            teacherId?: string;
            teacherName?: string;
        };
        StudentLessonHourLedgerResponse: {
            /** Format: int32 */
            balanceAfterMinutes?: number;
            branchId?: string;
            /** Format: date-time */
            createdAt?: string;
            eventType?: string;
            id?: string;
            lessonId?: string;
            lessonStudentId?: string;
            /** Format: int32 */
            lessonUnitsDelta?: number;
            /** Format: int32 */
            minutesDelta?: number;
            note?: string;
            /** Format: date-time */
            occurredAt?: string;
            operatorId?: string;
            packageId?: string;
            relatedLedgerId?: string;
            /** Format: int32 */
            remainingLessonsAfter?: number;
            studentId?: string;
        };
        StudentRequest: {
            allergy?: string;
            avatarUrl?: string;
            /** Format: date */
            birthday?: string;
            branchId: string;
            currentStageId?: string;
            emergencyContact?: string;
            emergencyPhone?: string;
            /** Format: date */
            enrollDate?: string;
            enrollNo: string;
            /** Format: int32 */
            gender?: number;
            healthNote?: string;
            initialSubscriptions?: components["schemas"]["InitialSubscription"][];
            mentorTeacherId: string;
            name: string;
            /** Format: int32 */
            status?: number;
        };
        StudentResponse: {
            alertLow?: boolean;
            allergy?: string;
            avatarUrl?: string;
            /** Format: date */
            birthday?: string;
            branchId?: string;
            branchName?: string;
            classGroups?: components["schemas"]["ClassGroupRef"][];
            currentStageCode?: string;
            currentStageId?: string;
            currentStageName?: string;
            emergencyContact?: string;
            emergencyPhone?: string;
            /** Format: date */
            enrollDate?: string;
            enrollNo?: string;
            /** Format: int32 */
            gender?: number;
            healthNote?: string;
            id?: string;
            mentorTeacherId?: string;
            mentorTeacherName?: string;
            name?: string;
            /** Format: int32 */
            status?: number;
            tenantId?: string;
            /** Format: int32 */
            totalRemaining?: number;
        };
        StudentStatusRequest: {
            /** Format: int32 */
            status: number;
        };
        StudentUpdateRequest: {
            allergy?: string;
            avatarUrl?: string;
            /** Format: date */
            birthday?: string;
            branchId: string;
            currentStageId?: string;
            emergencyContact?: string;
            emergencyPhone?: string;
            /** Format: date */
            enrollDate?: string;
            enrollNo: string;
            /** Format: int32 */
            gender?: number;
            healthNote?: string;
            id: string;
            mentorTeacherId?: string;
            name: string;
            /** Format: int32 */
            status?: number;
        };
        StudentView: {
            branchId?: string;
            id?: string;
            name?: string;
        };
        SubscriptionRequest: {
            note?: string;
            source?: string;
            /** Format: int32 */
            status?: number;
            studentId: string;
            teacherAvailabilityId: string;
            /** Format: date */
            validFrom: string;
            /** Format: date */
            validTo?: string;
        };
        SubscriptionResponse: {
            branchId?: string;
            /** Format: int32 */
            dayOfWeek?: number;
            /** Format: int32 */
            endMinute?: number;
            id?: string;
            note?: string;
            source?: string;
            /** Format: int32 */
            startMinute?: number;
            /** Format: int32 */
            status?: number;
            studentId?: string;
            studentName?: string;
            teacherAvailabilityId?: string;
            teacherId?: string;
            teacherName?: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validTo?: string;
        };
        TeacherAvailabilityRequest: {
            branchId: string;
            /** Format: int32 */
            capacity: number;
            /** Format: int32 */
            dayOfWeek: number;
            defaultClassRoomId?: string;
            /** Format: int32 */
            endMinute: number;
            note?: string;
            /** Format: int32 */
            startMinute: number;
            /** Format: int32 */
            status: number;
            /** Format: date */
            validFrom: string;
            /** Format: date */
            validTo?: string;
        };
        TeacherAvailabilityResponse: {
            boundClassGroupId?: string;
            branchId?: string;
            /** Format: int32 */
            capacity?: number;
            /** Format: int32 */
            dayOfWeek?: number;
            defaultClassRoomId?: string;
            /** Format: int32 */
            endMinute?: number;
            id?: string;
            note?: string;
            /** Format: int32 */
            startMinute?: number;
            /** Format: int32 */
            status?: number;
            teacherId?: string;
            teacherName?: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validTo?: string;
        };
        TeacherColumn: {
            branchId?: string;
            lessons?: components["schemas"]["LessonCell"][];
            teacherId?: string;
            teacherName?: string;
        };
        TeacherScheduleResponse: {
            columns?: components["schemas"]["TeacherColumn"][];
            /** Format: date */
            weekStart?: string;
        };
        TodayRosterResponse: {
            /** Format: int32 */
            checkedInCount?: number;
            items?: components["schemas"]["RosterItem"][];
            /** Format: int32 */
            totalExpected?: number;
        };
        TransferClassRequest: {
            fromClassGroupId: string;
            studentIds: string[];
            toClassGroupId: string;
        };
        UpdateAttendanceRequest: {
            note?: string;
            /** Format: int32 */
            status: number;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    runAbsenceJob: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    checkIn: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CheckInRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseAttendanceResponse"];
                };
            };
        };
    };
    checkOut: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CheckOutRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseAttendanceResponse"];
                };
            };
        };
    };
    studentHistory: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path: {
                studentId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListAttendanceResponse"];
                };
            };
        };
    };
    today: {
        parameters: {
            query: {
                branchId: string;
                period?: string;
                date?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseTodayRosterResponse"];
                };
            };
        };
    };
    update_7: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateAttendanceRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseAttendanceResponse"];
                };
            };
        };
    };
    list_8: {
        parameters: {
            query?: {
                page?: number;
                size?: number;
                branchId?: string;
                courseId?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponsePageResultClassGroupResponse"];
                };
            };
        };
    };
    create_10: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ClassGroupRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassGroupResponse"];
                };
            };
        };
    };
    get_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassGroupResponse"];
                };
            };
        };
    };
    update_6: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ClassGroupRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassGroupResponse"];
                };
            };
        };
    };
    delete_6: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    listMembers: {
        parameters: {
            query?: {
                activeOnly?: boolean;
            };
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListClassMemberResponse"];
                };
            };
        };
    };
    addMembers: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AddMembersRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    removeMember: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
                studentId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    list_7: {
        parameters: {
            query?: {
                page?: number;
                size?: number;
                branchId?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponsePageResultClassRoomResponse"];
                };
            };
        };
    };
    create_9: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ClassRoomRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassRoomResponse"];
                };
            };
        };
    };
    get_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassRoomResponse"];
                };
            };
        };
    };
    update_5: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ClassRoomRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassRoomResponse"];
                };
            };
        };
    };
    delete_5: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    list_6: {
        parameters: {
            query?: {
                page?: number;
                size?: number;
                keyword?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponsePageResultCourseResponse"];
                };
            };
        };
    };
    create_8: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CourseRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseCourseResponse"];
                };
            };
        };
    };
    get_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseCourseResponse"];
                };
            };
        };
    };
    update_4: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CourseRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseCourseResponse"];
                };
            };
        };
    };
    delete_4: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    create_7: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GuardianRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseGuardianResponse"];
                };
            };
        };
    };
    generateQr: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseGuardianQrResponse"];
                };
            };
        };
    };
    list_5: {
        parameters: {
            query?: {
                status?: number;
                studentId?: string;
                from?: string;
                to?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListLeaveResponse"];
                };
            };
        };
    };
    create_6: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LeaveCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLeaveResponse"];
                };
            };
        };
    };
    approve_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLeaveResponse"];
                };
            };
        };
    };
    reject: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLeaveResponse"];
                };
            };
        };
    };
    list_4: {
        parameters: {
            query?: {
                branchId?: string;
                classGroupId?: string;
                teacherId?: string;
                from?: string;
                to?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListLessonResponse"];
                };
            };
        };
    };
    create_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LessonRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLessonResponse"];
                };
            };
        };
    };
    bulkGenerate: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["BulkGenerateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseBulkGenerateResult"];
                };
            };
        };
    };
    checkConflict: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ConflictCheckRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseConflictReport"];
                };
            };
        };
    };
    get_4: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLessonResponse"];
                };
            };
        };
    };
    delete_7: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    cancel: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CancelLessonRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLessonResponse"];
                };
            };
        };
    };
    changeLogs: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListLessonChangeLogResponse"];
                };
            };
        };
    };
    reschedule: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RescheduleRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLessonResponse"];
                };
            };
        };
    };
    listStudents: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListLessonStudentResponse"];
                };
            };
        };
    };
    addStudent: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LessonStudentRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLessonStudentResponse"];
                };
            };
        };
    };
    removeStudent: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
                studentId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    update_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CoursePackageRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseCoursePackageResponse"];
                };
            };
        };
    };
    delete_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    list_3: {
        parameters: {
            query?: {
                lessonId?: string;
                studentId?: string;
                isAbnormal?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListPickupRecordResponse"];
                };
            };
        };
    };
    create_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PickupRecordRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponsePickupRecordResponse"];
                };
            };
        };
    };
    abnormal: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListPickupRecordResponse"];
                };
            };
        };
    };
    byTeacher: {
        parameters: {
            query?: {
                branchId?: string;
                weekStart?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseTeacherScheduleResponse"];
                };
            };
        };
    };
    myWeek: {
        parameters: {
            query?: {
                weekStart?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseTeacherScheduleResponse"];
                };
            };
        };
    };
    week: {
        parameters: {
            query?: {
                branchId?: string;
                weekStart?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseScheduleResponse"];
                };
            };
        };
    };
    search: {
        parameters: {
            query: {
                q: string;
                types?: string;
                limit?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListSearchHit"];
                };
            };
        };
    };
    branch: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseBranchAttendanceStat"];
                };
            };
        };
    };
    classGroup: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseClassGroupAttendanceStat"];
                };
            };
        };
    };
    dashboard: {
        parameters: {
            query: {
                branchId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseMapStringObject"];
                };
            };
        };
    };
    student: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentAttendanceStat"];
                };
            };
        };
    };
    list_9: {
        parameters: {
            query?: {
                studentId?: string;
                branchId?: string;
                from?: string;
                to?: string;
                page?: number;
                size?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponsePageResultStudentLessonHistoryResponse"];
                };
            };
        };
    };
    list_2: {
        parameters: {
            query?: {
                page?: number;
                size?: number;
                keyword?: string;
                branchId?: string;
                classGroupId?: string;
                status?: number;
                pkgRemainingMax?: number;
                mask?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponsePageResultStudentResponse"];
                };
            };
        };
    };
    create_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["StudentRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentResponse"];
                };
            };
        };
    };
    assignClass: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AssignClassRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    transfer: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TransferClassRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    importExcel: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": {
                    /** Format: binary */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentImportResult"];
                };
            };
        };
    };
    downloadTemplate: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    get: {
        parameters: {
            query?: {
                mask?: string;
            };
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentResponse"];
                };
            };
        };
    };
    update_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["StudentUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentResponse"];
                };
            };
        };
    };
    delete_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    listLessonHourLedger: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListStudentLessonHourLedgerResponse"];
                };
            };
        };
    };
    adjustLessonHours: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LessonHourAdjustRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentLessonHourLedgerResponse"];
                };
            };
        };
    };
    changeMentor: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AssignMentorRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    mentorHistory: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListMentorHistoryResponse"];
                };
            };
        };
    };
    updateStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["StudentStatusRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseStudentResponse"];
                };
            };
        };
    };
    guardians: {
        parameters: {
            query?: {
                pickupOnly?: boolean;
            };
            header?: never;
            path: {
                studentId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListGuardianSummaryResponse"];
                };
            };
        };
    };
    upsertGuardian: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                studentId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GuardianUpsertRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseGuardianResponse"];
                };
            };
        };
    };
    linkGuardian: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                studentId: string;
                guardianId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LinkGuardianRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseGuardianResponse"];
                };
            };
        };
    };
    listByStudent: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                studentId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListCoursePackageResponse"];
                };
            };
        };
    };
    create_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                studentId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CoursePackageRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseCoursePackageResponse"];
                };
            };
        };
    };
    list_1: {
        parameters: {
            query?: {
                studentId?: string;
                teacherId?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListSubscriptionResponse"];
                };
            };
        };
    };
    create_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubscriptionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseSubscriptionResponse"];
                };
            };
        };
    };
    update_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SubscriptionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseSubscriptionResponse"];
                };
            };
        };
    };
    delete_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    checkConflict_1: {
        parameters: {
            query: {
                teacherId: string;
                dayOfWeek: number;
                start: number;
                end: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseMapStringBoolean"];
                };
            };
        };
    };
    listUnbound: {
        parameters: {
            query: {
                branchId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListTeacherAvailabilityResponse"];
                };
            };
        };
    };
    update: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TeacherAvailabilityRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseTeacherAvailabilityResponse"];
                };
            };
        };
    };
    delete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseVoid"];
                };
            };
        };
    };
    list: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                teacherId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListTeacherAvailabilityResponse"];
                };
            };
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                teacherId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TeacherAvailabilityRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseTeacherAvailabilityResponse"];
                };
            };
        };
    };
    bindings: {
        parameters: {
            query: {
                studentId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListBinding"];
                };
            };
        };
    };
    claim: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ClaimRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseBinding"];
                };
            };
        };
    };
    revoke: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseBinding"];
                };
            };
        };
    };
    approve: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseBinding"];
                };
            };
        };
    };
    children: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListStudentView"];
                };
            };
        };
    };
    balance: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseMapStringObject"];
                };
            };
        };
    };
    leaves: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListLeave"];
                };
            };
        };
    };
    leave: {
        parameters: {
            query?: never;
            header: {
                "Idempotency-Key": string;
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LeaveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseLeave"];
                };
            };
        };
    };
    schedule: {
        parameters: {
            query: {
                from: string;
                to: string;
            };
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseListSchedule"];
                };
            };
        };
    };
    invite: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["InviteRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResponseInvite"];
                };
            };
        };
    };
}
