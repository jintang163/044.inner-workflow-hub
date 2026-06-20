import request from '@/utils/request'
import {
  LoginDTO,
  LoginVO,
  UserInfoVO,
  UserVO,
  RoleVO,
  DeptVO,
  MenuVO,
  PageResult,
  FormDefinitionVO,
  ProcessDefinitionVO,
  WfTaskVO,
  WfProcessInstanceVO,
  WfApprovalHistoryVO,
  TenantSimpleVO
} from '@/types'
import {
  ApprovalTaskVO,
  ApprovalHistoryVO,
  ProcessInstanceVO,
  ProcessCategoryVO,
  StartableProcessVO,
  DraftVO,
  CcTaskVO,
  DraftSaveDTO,
  ApproveDTO,
  RejectDTO,
  RejectToNodeDTO,
  TransferDTO,
  AddSignDTO,
  DelegateDTO,
  StartProcessDTO,
  DelegationVO,
  DelegationSaveDTO,
  DelegationQueryDTO,
  BatchTransferDTO,
  TransferRecordVO,
  AttachmentVO,
  CommentTemplateVO,
  CommentTemplateCategoryVO,
  CommentTemplateSaveDTO,
  CommentTemplateCategorySaveDTO,
  CommentTemplateQueryDTO,
  BatchRemindDTO,
  BatchRemindResultVO
} from '@/types/approval'
import {
  TenantVO,
  TenantStatsVO,
  TenantUserVO,
  TenantRegisterDTO,
  TenantUpdateDTO,
  TenantRoleVO,
  TenantRoleSaveDTO
} from '@/types/tenant'
import {
  AiRecommendationVO,
  AiStatsVO
} from '@/types/ai'
import {
  SysDictTypeVO,
  SysDictDataVO,
  WfDataSourceConfigVO,
  DictOptionItem
} from '@/types/dict'
import {
  WfRedocTemplateVO,
  WfRedocTemplateSaveDTO,
  WfRedocGenerateDTO,
  WfRedocGeneratedVO,
  WfRedocBatchDTO,
  WfSealConfigVO,
  WfSealConfigSaveDTO
} from '@/types/redoc'

export const authApi = {
  login: (data: LoginDTO) => request<LoginVO>({ url: '/auth/login', method: 'post', data }),
  logout: () => request<void>({ url: '/auth/logout', method: 'post' }),
  getUserInfo: () => request<UserInfoVO>({ url: '/auth/user-info', method: 'get' }),
  getMenus: () => request<MenuVO[]>({ url: '/auth/menus', method: 'get' }),
  switchTenant: (tenantId: number) =>
    request<UserInfoVO>({ url: '/auth/switch-tenant', method: 'post', params: { tenantId } })
}

export const userApi = {
  list: (params?: any) => request<PageResult<UserVO>>({ url: '/system/user/list', method: 'get', params }),
  get: (id: number) => request<UserVO>({ url: `/system/user/${id}`, method: 'get' }),
  save: (data: any) => request<UserVO>({ url: '/system/user', method: 'post', data }),
  update: (data: any) => request<UserVO>({ url: '/system/user', method: 'put', data }),
  remove: (id: number) => request<void>({ url: `/system/user/${id}`, method: 'delete' }),
  updateStatus: (id: number, status: number) =>
    request<void>({ url: `/system/user/${id}/status`, method: 'put', data: { status } }),
  resetPassword: (id: number, newPassword: string) =>
    request<void>({ url: `/system/user/${id}/reset-password`, method: 'put', data: { newPassword } })
}

export const roleApi = {
  list: (params?: any) => request<PageResult<RoleVO>>({ url: '/system/role/list', method: 'get', params }),
  all: () => request<RoleVO[]>({ url: '/system/role/all', method: 'get' }),
  get: (id: number) => request<RoleVO>({ url: `/system/role/${id}`, method: 'get' }),
  save: (data: any) => request<RoleVO>({ url: '/system/role', method: 'post', data }),
  update: (data: any) => request<RoleVO>({ url: '/system/role', method: 'put', data }),
  remove: (id: number) => request<void>({ url: `/system/role/${id}`, method: 'delete' })
}

export const menuApi = {
  tree: () => request<MenuVO[]>({ url: '/system/menu/tree', method: 'get' }),
  get: (id: number) => request<MenuVO>({ url: `/system/menu/${id}`, method: 'get' }),
  save: (data: any) => request<MenuVO>({ url: '/system/menu', method: 'post', data }),
  update: (data: any) => request<MenuVO>({ url: '/system/menu', method: 'put', data }),
  remove: (id: number) => request<void>({ url: `/system/menu/${id}`, method: 'delete' })
}

export const deptApi = {
  tree: () => request<DeptVO[]>({ url: '/system/dept/tree', method: 'get' }),
  get: (id: number) => request<DeptVO>({ url: `/system/dept/${id}`, method: 'get' }),
  save: (data: any) => request<DeptVO>({ url: '/system/dept', method: 'post', data }),
  update: (data: any) => request<DeptVO>({ url: '/system/dept', method: 'put', data }),
  remove: (id: number) => request<void>({ url: `/system/dept/${id}`, method: 'delete' })
}

export const processApi = {
  definitionList: (params?: any) =>
    request<PageResult<ProcessDefinitionVO>>({ url: '/process/definition/list', method: 'get', params }),
  definitionGet: (id: number) =>
    request<ProcessDefinitionVO>({ url: `/process/definition/${id}`, method: 'get' }),
  definitionSave: (data: any) =>
    request<ProcessDefinitionVO>({ url: '/process/definition', method: 'post', data }),
  definitionUpdate: (data: any) =>
    request<ProcessDefinitionVO>({ url: '/process/definition', method: 'put', data }),
  definitionRemove: (id: number) =>
    request<void>({ url: `/process/definition/${id}`, method: 'delete' }),
  definitionDeploy: (data: any) => request<void>({ url: '/process/definition/deploy', method: 'post', data }),
  designGet: (id: number) => request<any>({ url: `/process/design/${id}`, method: 'get' }),
  designSave: (data: any) => request<any>({ url: '/process/design/save', method: 'post', data }),
  designDeploy: (data: any) => request<any>({ url: '/process/design/deploy', method: 'post', data }),
  validate: (data: any) =>
    request<ValidateResultVO[]>({ url: '/process/design/validate', method: 'post', data }),
  simulate: (data: any) =>
    request<SimulateStepVO[]>({ url: '/process/design/simulate', method: 'post', data }),
  versionList: (processDefinitionId: number) =>
    request<ProcessVersionVO[]>({ url: `/process/version/${processDefinitionId}/list`, method: 'get' }),
  versionRollback: (id: number) =>
    request<void>({ url: `/process/version/${id}/rollback`, method: 'post' }),
  formFields: (formId: number) =>
    request<FormFieldVO[]>({ url: `/form/definition/${formId}/fields`, method: 'get' }),
  categoryTree: () => request<ProcessCategoryVO[]>({ url: '/process/category/tree', method: 'get' }),
  categorySave: (data: any) => request<any>({ url: '/process/category', method: 'post', data }),
  categoryUpdate: (data: any) => request<any>({ url: '/process/category', method: 'put', data }),
  categoryRemove: (id: number) => request<void>({ url: `/process/category/${id}`, method: 'delete' }),
  businessLineList: (params?: any) =>
    request<PageResult<any>>({ url: '/process/business-line/list', method: 'get', params }),
  businessLineSave: (data: any) => request<any>({ url: '/process/business-line', method: 'post', data }),
  businessLineUpdate: (data: any) => request<any>({ url: '/process/business-line', method: 'put', data }),
  businessLineRemove: (id: number) =>
    request<void>({ url: `/process/business-line/${id}`, method: 'delete' })
}

export const formApi = {
  definitionList: (params?: any) =>
    request<PageResult<FormDefinitionVO>>({ url: '/form/definition/list', method: 'get', params }),
  definitionGet: (id: number) =>
    request<FormDefinitionVO>({ url: `/form/definition/${id}`, method: 'get' }),
  definitionSave: (data: any) =>
    request<FormDefinitionVO>({ url: '/form/definition', method: 'post', data }),
  definitionUpdate: (data: any) =>
    request<FormDefinitionVO>({ url: '/form/definition', method: 'put', data }),
  definitionRemove: (id: number) =>
    request<void>({ url: `/form/definition/${id}`, method: 'delete' }),
  definitionPublish: (id: number) =>
    request<void>({ url: `/form/definition/${id}/publish`, method: 'post' }),
  designGet: (id: number) => request<any>({ url: `/form/design/${id}`, method: 'get' }),
  designSave: (data: any) => request<any>({ url: '/form/design', method: 'post', data }),
  schemaGet: (definitionId: number, version?: number) =>
    request<FormilySchema>({
      url: version !== undefined
        ? `/form/definition/${definitionId}/schema/${version}`
        : `/form/definition/${definitionId}/schema`,
      method: 'get'
    }),
  draftList: (params?: any) => request<DraftVO[]>({ url: '/form/draft/list', method: 'get', params }),
  draftPage: (params?: any) => request<PageResult<DraftVO>>({ url: '/form/draft/page', method: 'get', params }),
  draftSave: (data: DraftSaveDTO) => request<DraftVO>({ url: '/form/draft', method: 'post', data }),
  draftAutoSave: (data: DraftSaveDTO) => request<DraftVO>({ url: '/form/draft/auto-save', method: 'post', data }),
  draftGet: (id: number) => request<DraftVO>({ url: `/form/draft/${id}`, method: 'get' }),
  draftGetByNo: (draftNo: string) => request<DraftVO>({ url: `/form/draft/no/${draftNo}`, method: 'get' }),
  draftGetLatest: (processKey: string) => request<DraftVO>({ url: `/form/draft/latest/${processKey}`, method: 'get' }),
  draftRemove: (id: number) => request<void>({ url: `/form/draft/${id}`, method: 'delete' })
}

export const approvalApi = {
  todoList: (params?: any) =>
    request<PageResult<ApprovalTaskVO>>({ url: '/api/approval/task/todo', method: 'get', params }),
  doneList: (params?: any) =>
    request<PageResult<ApprovalTaskVO>>({ url: '/api/approval/task/done', method: 'get', params }),
  ccList: (params?: any) =>
    request<PageResult<CcTaskVO>>({ url: '/api/approval/task/cc', method: 'get', params }),
  myApplyList: (params?: any) =>
    request<PageResult<ProcessInstanceVO>>({ url: '/api/approval/process/my-apply', method: 'get', params }),
  startableList: () =>
    request<StartableProcessVO[]>({ url: '/api/bpmn/process/startable', method: 'get' }),
  instanceDetail: (instanceNo: string) =>
    request<ProcessInstanceVO>({ url: `/api/approval/instance/${instanceNo}`, method: 'get' }),
  approvalHistory: (instanceNo: string) =>
    request<ApprovalHistoryVO[]>({ url: `/api/approval/instance/${instanceNo}/history`, method: 'get' }),
  approve: (data: ApproveDTO) => request<void>({ url: '/api/approval/approve', method: 'post', data }),
  reject: (data: RejectDTO) => request<void>({ url: '/api/approval/reject', method: 'post', data }),
  rejectToNode: (data: RejectToNodeDTO) => request<void>({ url: '/api/approval/reject-to-node', method: 'post', data }),
  transfer: (data: TransferDTO) => request<void>({ url: '/api/approval/transfer', method: 'post', data }),
  addSign: (data: AddSignDTO) => request<void>({ url: '/api/approval/addSign', method: 'post', data }),
  delegate: (data: DelegateDTO) => request<void>({ url: '/api/approval/delegate', method: 'post', data }),
  withdraw: (data: { instanceId: number; comment?: string }) =>
    request<void>({ url: '/api/approval/withdraw', method: 'post', data }),
  start: (data: StartProcessDTO) =>
    request<string>({ url: '/api/approval/start', method: 'post', data }),
  batchApprove: (data: { taskIds: string[]; comment?: string; signatureUrl?: string }) =>
    request<void>({ url: '/api/approval/batch-approve', method: 'post', data }),
  batchReject: (data: { taskIds: string[]; comment?: string }) =>
    request<void>({ url: '/api/approval/batch-reject', method: 'post', data }),
  batchTransfer: (data: BatchTransferDTO) =>
    request<void>({ url: '/api/approval/batch-transfer', method: 'post', data }),
  markCcRead: (taskId: number) =>
    request<void>({ url: '/api/notify/log/read', method: 'post', data: { taskId } }),
  delegationPage: (params?: DelegationQueryDTO) =>
    request<PageResult<DelegationVO>>({ url: '/api/approval/delegation/page', method: 'get', params }),
  delegationGet: (id: number) =>
    request<DelegationVO>({ url: `/api/approval/delegation/${id}`, method: 'get' }),
  delegationSave: (data: DelegationSaveDTO) =>
    request<void>({ url: '/api/approval/delegation', method: 'post', data }),
  delegationUpdate: (data: DelegationSaveDTO) =>
    request<void>({ url: '/api/approval/delegation', method: 'put', data }),
  delegationRevoke: (id: number) =>
    request<void>({ url: `/api/approval/delegation/${id}/revoke`, method: 'put' }),
  hasActiveDelegation: () =>
    request<boolean>({ url: '/api/approval/delegation/active/current', method: 'get' }),
  transferRecordPage: (params?: { pageNum?: number; pageSize?: number; transferType?: number }) =>
    request<PageResult<TransferRecordVO>>({ url: '/api/approval/transfer-record/page', method: 'get', params }),
  transferRecordByInstance: (instanceId: number) =>
    request<TransferRecordVO[]>({ url: `/api/approval/transfer-record/instance/${instanceId}`, method: 'get' }),

  attachmentUpload: (file: File, bizType?: string, bizId?: string, nodeId?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (bizType) formData.append('bizType', bizType)
    if (bizId) formData.append('bizId', bizId)
    if (nodeId) formData.append('nodeId', nodeId)
    return request<AttachmentVO>({
      url: '/api/approval/attachment/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  attachmentList: (params: { bizType: string; bizId: string; nodeId?: string; processVersionId?: number }) =>
    request<AttachmentVO[]>({ url: '/api/approval/attachment/list', method: 'get', params }),
  attachmentListByIds: (ids: number[]) =>
    request<AttachmentVO[]>({ url: '/api/approval/attachment/listByIds', method: 'get', params: { ids } }),
  attachmentRemove: (id: number) =>
    request<void>({ url: `/api/approval/attachment/${id}`, method: 'delete' }),
  attachmentPreviewUrl: (id: number) =>
    request<string>({ url: `/api/approval/attachment/${id}/preview`, method: 'get' }),
  attachmentDownloadUrl: (id: number) =>
    request<string>({ url: `/api/approval/attachment/${id}/download-url`, method: 'get' }),
  attachmentBatchDownload: (ids: number[]) =>
    request<Blob>({ url: '/api/approval/attachment/batch-download', method: 'post', data: ids, responseType: 'blob' }),

  vacationPage: (params?: any) =>
    request<PageResult<any>>({ url: '/api/approval/vacation/page', method: 'get', params }),
  vacationGet: (id: number) =>
    request<any>({ url: `/api/approval/vacation/${id}`, method: 'get' }),
  vacationSave: (data: any) =>
    request<void>({ url: '/api/approval/vacation', method: 'post', data }),
  vacationUpdate: (data: any) =>
    request<void>({ url: '/api/approval/vacation', method: 'put', data }),
  vacationDelete: (id: number) =>
    request<void>({ url: `/api/approval/vacation/${id}`, method: 'delete' }),
  vacationCancel: (id: number) =>
    request<void>({ url: `/api/approval/vacation/cancel/${id}`, method: 'post' }),
  vacationMyList: (params?: any) =>
    request<any[]>({ url: '/api/approval/vacation/my/list', method: 'get', params }),
  vacationMyCurrent: () =>
    request<any>({ url: '/api/approval/vacation/my/current', method: 'get' }),
  vacationCheck: (userId: number, time?: string) =>
    request<boolean>({ url: `/api/approval/vacation/check/${userId}`, method: 'get', params: time ? { time } : undefined }),
  vacationSync: (sourceType: string) =>
    request<void>({ url: `/api/approval/vacation/sync/${sourceType}`, method: 'post' }),
  vacationBatchTransfer: () =>
    request<number>({ url: '/api/approval/vacation/batch-transfer', method: 'post' }),
  vacationTransfer: (vacationId: number) =>
    request<void>({ url: `/api/approval/vacation/transfer/${vacationId}`, method: 'post' }),

  agentConfigPage: (params?: any) =>
    request<PageResult<any>>({ url: '/api/approval/agent-config/page', method: 'get', params }),
  agentConfigGet: (id: number) =>
    request<any>({ url: `/api/approval/agent-config/${id}`, method: 'get' }),
  agentConfigSave: (data: any) =>
    request<void>({ url: '/api/approval/agent-config', method: 'post', data }),
  agentConfigUpdate: (data: any) =>
    request<void>({ url: '/api/approval/agent-config', method: 'put', data }),
  agentConfigDelete: (id: number) =>
    request<void>({ url: `/api/approval/agent-config/${id}`, method: 'delete' }),
  agentConfigMyList: () =>
    request<any[]>({ url: '/api/approval/agent-config/my/list', method: 'get' }),

  commentTemplateCategoryPage: (params?: any) =>
    request<PageResult<CommentTemplateCategoryVO>>({ url: '/api/approval/comment-template-category/page', method: 'get', params }),
  commentTemplateCategoryAvailable: () =>
    request<CommentTemplateCategoryVO[]>({ url: '/api/approval/comment-template-category/available', method: 'get' }),
  commentTemplateCategoryGet: (id: number) =>
    request<CommentTemplateCategoryVO>({ url: `/api/approval/comment-template-category/${id}`, method: 'get' }),
  commentTemplateCategorySave: (data: CommentTemplateCategorySaveDTO) =>
    request<void>({ url: '/api/approval/comment-template-category', method: 'post', data }),
  commentTemplateCategoryUpdate: (data: CommentTemplateCategorySaveDTO) =>
    request<void>({ url: '/api/approval/comment-template-category', method: 'put', data }),
  commentTemplateCategoryDelete: (id: number) =>
    request<void>({ url: `/api/approval/comment-template-category/${id}`, method: 'delete' }),
  commentTemplateCategoryUpdateStatus: (id: number, status: number) =>
    request<void>({ url: `/api/approval/comment-template-category/status/${id}`, method: 'put', params: { status } }),

  commentTemplatePage: (params?: CommentTemplateQueryDTO) =>
    request<PageResult<CommentTemplateVO>>({ url: '/api/approval/comment-template/page', method: 'get', params }),
  commentTemplateMyAvailable: () =>
    request<CommentTemplateVO[]>({ url: '/api/approval/comment-template/my-available', method: 'get' }),
  commentTemplateByCategory: (categoryId: number) =>
    request<CommentTemplateVO[]>({ url: `/api/approval/comment-template/category/${categoryId}`, method: 'get' }),
  commentTemplateGet: (id: number) =>
    request<CommentTemplateVO>({ url: `/api/approval/comment-template/${id}`, method: 'get' }),
  commentTemplateSave: (data: CommentTemplateSaveDTO) =>
    request<void>({ url: '/api/approval/comment-template', method: 'post', data }),
  commentTemplateUpdate: (data: CommentTemplateSaveDTO) =>
    request<void>({ url: '/api/approval/comment-template', method: 'put', data }),
  commentTemplateDelete: (id: number) =>
    request<void>({ url: `/api/approval/comment-template/${id}`, method: 'delete' }),
  commentTemplateUpdateStatus: (id: number, status: number) =>
    request<void>({ url: `/api/approval/comment-template/status/${id}`, method: 'put', params: { status } }),
  commentTemplateUse: (id: number) =>
    request<void>({ url: `/api/approval/comment-template/use/${id}`, method: 'post' }),

  manualRemind: (data: { taskId: number; remark?: string }) =>
    request<void>({ url: '/api/approval/remind/manual', method: 'post', data }),
  batchRemind: (data: BatchRemindDTO) =>
    request<BatchRemindResultVO>({ url: '/api/approval/remind/batch', method: 'post', data }),
  getLastRemindTime: (taskId: number) =>
    request<string>({ url: `/api/approval/remind/${taskId}/last-time`, method: 'get' }),
  canRemind: (taskId: number, intervalMinutes?: number) =>
    request<boolean>({
      url: `/api/approval/remind/${taskId}/can-remind`,
      method: 'get',
      params: intervalMinutes !== undefined ? { intervalMinutes } : undefined
    }),
}

export const notifyApi = {
  templateList: (params?: any) =>
    request<PageResult<any>>({ url: '/notify/template/list', method: 'get', params }),
  templateGet: (id: number) => request<any>({ url: `/notify/template/${id}`, method: 'get' }),
  templateSave: (data: any) => request<any>({ url: '/notify/template', method: 'post', data }),
  templateUpdate: (data: any) => request<any>({ url: '/notify/template', method: 'put', data }),
  templateRemove: (id: number) => request<void>({ url: `/notify/template/${id}`, method: 'delete' }),
  logList: (params?: any) => request<PageResult<any>>({ url: '/notify/log/list', method: 'get', params }),
  logGet: (id: number) => request<any>({ url: `/notify/log/${id}`, method: 'get' })
}

export const tenantApi = {
  register: (data: TenantRegisterDTO) =>
    request<void>({ url: '/tenant/register', method: 'post', data }),
  page: (params?: any) =>
    request<PageResult<TenantVO>>({ url: '/tenant/page', method: 'get', params }),
  get: (id: number) => request<TenantVO>({ url: `/tenant/${id}`, method: 'get' }),
  approve: (id: number) =>
    request<void>({ url: `/tenant/approve/${id}`, method: 'put' }),
  reject: (id: number) =>
    request<void>({ url: `/tenant/reject/${id}`, method: 'put' }),
  update: (data: TenantUpdateDTO) =>
    request<void>({ url: '/tenant', method: 'put', data }),
  remove: (id: number) =>
    request<void>({ url: `/tenant/${id}`, method: 'delete' }),
  listByUser: () => request<TenantVO[]>({ url: '/tenant/list-by-user', method: 'get' }),
  getStats: (id: number) =>
    request<TenantStatsVO>({ url: `/tenant/${id}/stats`, method: 'get' }),
  pageUsers: (id: number, params?: any) =>
    request<PageResult<TenantUserVO>>({ url: `/tenant/${id}/users`, method: 'get', params }),
  addTenantUser: (id: number, userId: number, tenantRole: string) =>
    request<void>({ url: `/tenant/${id}/users`, method: 'post', params: { userId, tenantRole } }),
  removeTenantUser: (id: number, userId: number) =>
    request<void>({ url: `/tenant/${id}/users/${userId}`, method: 'delete' }),

  listRoles: (tenantId: number) =>
    request<TenantRoleVO[]>({ url: '/tenant/role/list', method: 'get', params: { tenantId } }),
  getRole: (id: number) =>
    request<TenantRoleVO>({ url: `/tenant/role/${id}`, method: 'get' }),
  saveRole: (data: TenantRoleSaveDTO) =>
    request<void>({ url: '/tenant/role', method: 'post', data }),
  updateRole: (data: TenantRoleSaveDTO) =>
    request<void>({ url: '/tenant/role', method: 'put', data }),
  removeRole: (id: number) =>
    request<void>({ url: `/tenant/role/${id}`, method: 'delete' }),
  assignUserRole: (tenantId: number, userId: number, tenantRoleId: number) =>
    request<void>({ url: '/tenant/role/assign-user', method: 'post', params: { tenantId, userId, tenantRoleId } }),
  removeUserRole: (tenantId: number, userId: number, tenantRoleId: number) =>
    request<void>({ url: '/tenant/role/remove-user', method: 'delete', params: { tenantId, userId, tenantRoleId } })
}

export const aiApi = {
  getRecommendation: (taskId: number) =>
    request<AiRecommendationVO>({ url: `/ai/recommendation/${taskId}`, method: 'get' }),
  recordAdoption: (recommendationId: number, adopted: number) =>
    request<void>({ url: `/ai/adoption/${recommendationId}`, method: 'post', params: { adopted } }),
  getStats: () =>
    request<AiStatsVO>({ url: '/ai/stats', method: 'get' }),
  triggerTraining: () =>
    request<void>({ url: '/ai/trigger-training', method: 'post' })
}

export const dictApi = {
  typeList: () =>
    request<SysDictTypeVO[]>({ url: '/system/dict/type/list', method: 'get' }),
  typeDetail: (id: number) =>
    request<SysDictTypeVO>({ url: `/system/dict/type/${id}`, method: 'get' }),
  typeSave: (data: any) =>
    request<boolean>({ url: '/system/dict/type', method: 'post', data }),
  typeUpdate: (data: any) =>
    request<boolean>({ url: '/system/dict/type', method: 'put', data }),
  typeRemove: (id: number) =>
    request<boolean>({ url: `/system/dict/type/${id}`, method: 'delete' }),
  dataList: (dictCode: string) =>
    request<SysDictDataVO[]>({ url: `/system/dict/data/${dictCode}`, method: 'get' }),
  cascadeData: (dictCode: string, parentValue?: string) =>
    request<SysDictDataVO[]>({
      url: `/system/dict/data/${dictCode}/cascade`,
      method: 'get',
      params: parentValue ? { parentValue } : undefined
    }),
  dataSave: (data: any) =>
    request<boolean>({ url: '/system/dict/data', method: 'post', data }),
  dataUpdate: (data: any) =>
    request<boolean>({ url: '/system/dict/data', method: 'put', data }),
  dataRemove: (id: number) =>
    request<boolean>({ url: `/system/dict/data/${id}`, method: 'delete' }),
  refreshCache: (dictCode: string) =>
    request<void>({ url: `/system/dict/cache/refresh/${dictCode}`, method: 'post' }),
  clearAllCache: () =>
    request<void>({ url: '/system/dict/cache/clear', method: 'post' }),
  getOptions: (dictCode: string) =>
    request<DictOptionItem[]>({ url: `/system/dict/data/${dictCode}`, method: 'get' })
}

export const dataSourceApi = {
  list: () =>
    request<WfDataSourceConfigVO[]>({ url: '/system/datasource/list', method: 'get' }),
  detail: (id: number) =>
    request<WfDataSourceConfigVO>({ url: `/system/datasource/${id}`, method: 'get' }),
  save: (data: any) =>
    request<boolean>({ url: '/system/datasource', method: 'post', data }),
  update: (data: any) =>
    request<boolean>({ url: '/system/datasource', method: 'put', data }),
  remove: (id: number) =>
    request<boolean>({ url: `/system/datasource/${id}`, method: 'delete' }),
  fetchData: (sourceCode: string, params?: Record<string, any>) =>
    request<DictOptionItem[]>({ url: `/system/datasource/fetch/${sourceCode}`, method: 'post', data: params || {} }),
  refreshCache: (sourceCode: string) =>
    request<void>({ url: `/system/datasource/cache/refresh/${sourceCode}`, method: 'post' })
}

export const redocApi = {
  templateList: (category?: string, processKey?: string) =>
    request<WfRedocTemplateVO[]>({
      url: '/approval/redoc/template/list',
      method: 'get',
      params: { category, processKey }
    }),
  templatePage: (current: number, size: number, keyword?: string, category?: string) =>
    request<PageResult<WfRedocTemplateVO>>({
      url: '/approval/redoc/template/page',
      method: 'get',
      params: { current, size, keyword, category }
    }),
  templateDetail: (id: number) =>
    request<WfRedocTemplateVO>({ url: `/approval/redoc/template/${id}`, method: 'get' }),
  templateSave: (data: WfRedocTemplateSaveDTO) =>
    request<WfRedocTemplateVO>({ url: '/approval/redoc/template', method: 'post', data }),
  templateRemove: (id: number) =>
    request<void>({ url: `/approval/redoc/template/${id}`, method: 'delete' }),
  templatePlaceholders: (id: number) =>
    request<string[]>({ url: `/approval/redoc/template/${id}/placeholders`, method: 'get' }),
  templateUpload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<AttachmentVO>({ url: '/approval/redoc/template/upload', method: 'post', data: form })
  },

  generate: (data: WfRedocGenerateDTO) =>
    request<WfRedocGeneratedVO>({ url: '/approval/redoc/generate', method: 'post', data }),

  listByInstance: (instanceNo: string) =>
    request<WfRedocGeneratedVO[]>({
      url: '/approval/redoc/generated/list',
      method: 'get',
      params: { instanceNo }
    }),
  pageGenerated: (current: number, size: number, instanceNo?: string, templateId?: string) =>
    request<PageResult<WfRedocGeneratedVO>>({
      url: '/approval/redoc/generated/page',
      method: 'get',
      params: { current, size, instanceNo, templateId }
    }),
  generatedDetail: (id: number) =>
    request<WfRedocGeneratedVO>({ url: `/approval/redoc/generated/${id}`, method: 'get' }),
  markPrinted: (id: number) =>
    request<boolean>({ url: `/approval/redoc/generated/${id}/printed`, method: 'post' }),
  markDownloaded: (id: number) =>
    request<boolean>({ url: `/approval/redoc/generated/${id}/downloaded`, method: 'post' }),

  batchPrint: (data: WfRedocBatchDTO) =>
    request<Blob>({ url: '/approval/redoc/batch/print', method: 'post', data, responseType: 'blob' }),
  batchDownload: (data: WfRedocBatchDTO) =>
    request<Blob>({ url: '/approval/redoc/batch/download', method: 'post', data, responseType: 'blob' })
}

export const sealApi = {
  list: (sealType?: number) =>
    request<WfSealConfigVO[]>({ url: '/approval/seal/list', method: 'get', params: { sealType } }),
  page: (current: number, size: number, keyword?: string, sealType?: number) =>
    request<PageResult<WfSealConfigVO>>({
      url: '/approval/seal/page',
      method: 'get',
      params: { current, size, keyword, sealType }
    }),
  detail: (id: number) =>
    request<WfSealConfigVO>({ url: `/approval/seal/${id}`, method: 'get' }),
  save: (data: WfSealConfigSaveDTO) =>
    request<WfSealConfigVO>({ url: '/approval/seal', method: 'post', data }),
  remove: (id: number) =>
    request<void>({ url: `/approval/seal/${id}`, method: 'delete' }),
  imageUpload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<AttachmentVO>({ url: '/approval/seal/image/upload', method: 'post', data: form })
  },
  certUpload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<AttachmentVO>({ url: '/approval/seal/cert/upload', method: 'post', data: form })
  }
}

const api = {
  auth: authApi,
  user: userApi,
  role: roleApi,
  menu: menuApi,
  dept: deptApi,
  process: processApi,
  form: formApi,
  approval: approvalApi,
  notify: notifyApi,
  tenant: tenantApi,
  ai: aiApi,
  dict: dictApi,
  dataSource: dataSourceApi,
  redoc: redocApi,
  seal: sealApi
}

export default api
