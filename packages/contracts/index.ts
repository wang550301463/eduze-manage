import type { components as Identity } from "./generated/identity";
import type { components as Teaching } from "./generated/teaching";
import type { components as Portfolio } from "./generated/portfolio";
import type { components as Academic } from "./generated/academic";
import type { components as Notification } from "./generated/notification";
import type { components as Media } from "./generated/media";

/** Generated request schemas are the source. UI requires populated defaults and narrows
 * domain states; map/object snapshot responses remain explicit until OpenAPI models them. */
/** Public v1 contracts. IDs remain strings across PC and WeChat. */
export type Id = string;
export type TemplateInput = Required<
  Omit<Teaching["schemas"]["TemplateInput"], "version">
> &
  Pick<Teaching["schemas"]["TemplateInput"], "version">;
export interface Template {
  id: Id;
  ownerId: Id;
  version: number;
  content: TemplateInput;
  createdAt: string;
}
export interface TemplateVersion {
  id: Id;
  templateId: Id;
  version: number;
  content: TemplateInput;
  createdAt: string;
}
export type ResourceInput = Omit<
  Teaching["schemas"]["ResourceInput"],
  "kind"
> & { kind: "IMAGE" | "PDF" | "VIDEO" | "PRESENTATION" | "AUDIO" };
export interface Resource {
  id: Id;
  ownerId: Id;
  version: number;
  published: boolean;
  content: ResourceInput;
  createdAt: string;
}
export type ThemeInput = Teaching["schemas"]["ThemeInput"];
export interface Theme {
  id: Id;
  version: number;
  status: "PLANNED" | "IN_PROGRESS" | "FINISHED";
  content: ThemeInput;
  template: TemplateInput;
  createdAt: string;
}
export type Artwork = Omit<Portfolio["schemas"]["Artwork"], "kind"> & {
  kind: "PROCESS" | "FINAL";
};
export type Progress =
  | "NOT_STARTED"
  | "IN_PROGRESS"
  | "MAKEUP_PENDING"
  | "COMPLETED";
export type DraftInput = Omit<
  Portfolio["schemas"]["DraftInput"],
  "progress" | "artworks"
> & { progress: Progress; artworks: Artwork[] };
export interface PortfolioRecord {
  needsPublishing?: boolean;
  id: Id;
  version: number;
  branchId: Id;
  status: "DRAFT" | "PUBLISHED" | "WITHDRAWN";
  content: DraftInput;
  createdAt: string;
}
export interface Publication {
  id: Id;
  recordId: Id;
  version: number;
  content: DraftInput;
  createdAt: string;
}
export interface RosterEntry {
  needsPublishing?: boolean;
  studentId: Id;
  name: string;
  recordId: Id | null;
  progress: Progress;
  status: string;
}
export interface Child {
  id: Id;
  name: string;
  branchId: Id;
}
export type FamilyBinding = Required<Academic["schemas"]["Binding"]> & {
  createdAt?: string;
};
export interface Lesson {
  id: Id;
  startTime: string;
  endTime: string;
  status: string;
  teacherId: Id;
  classGroupId: Id;
}
export type Exhibition = Omit<
  Portfolio["schemas"]["ExhibitionInput"],
  "publicationIds"
> & {
  id: Id;
  status: "DRAFT" | "PUBLISHED" | "WITHDRAWN" | "ARCHIVED";
  createdAt: string;
};
export type Plan = Teaching["schemas"]["PlanInput"] & { id: Id };
export type ClassroomEntry = Omit<
  Portfolio["schemas"]["EntryInput"],
  "idempotencyKey"
> & {
  id: Id;
  recordId: Id;
  status: "DRAFT" | "PUBLISHED" | "WITHDRAWN";
  createdAt: string;
};
export interface MediaLink {
  id: Id;
  url: string;
  expiresAt: string;
  contentType?: string;
  thumbnailUrl?: string;
}

export type NotificationMessage = Omit<
  Required<Notification["schemas"]["Message"]>,
  "readAt" | "confirmedAt"
> & { readAt: string | null; confirmedAt: string | null };
export type UploadTicket = Required<
  Pick<
    Media["schemas"]["UploadResult"],
    "id" | "method" | "headers" | "uploadUrl"
  >
> &
  Pick<Media["schemas"]["UploadResult"], "expiresAt">;
export type UploadRequest = Required<Media["schemas"]["UploadRequest"]>;

export type IdentityUser = Required<
  Pick<Identity["schemas"]["UserInfo"], "id" | "roles">
> &
  Pick<Identity["schemas"]["UserInfo"], "name"> & {
    branches: Required<
      Pick<Identity["schemas"]["BranchInfo"], "id" | "name">
    >[];
  };
export type IdentityLogin = Required<
  Pick<Identity["schemas"]["LoginResponse"], "accessToken">
> &
  Pick<Identity["schemas"]["LoginResponse"], "refreshToken"> & {
    user: IdentityUser;
  };
