export type TaskStatus = 'PENDING' | 'DONE' | 'TRANSFERRED' | 'DELEGATED' | 'REJECTED' | 'WITHDRAWN'

export type Priority = 1 | 2 | 3

export type ApprovalResult = 'APPROVE' | 'REJECT' | 'TRANSFER' | 'ADD_SIGN' | 'DELEGATE' | 'REJECT_TO_NODE' | 'WITHDRAW'

export type InstanceStatus = 1 | 2 | 3 | 4

export type ActivityType = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8

export interface AttachmentVO {
  id: number
  fileName: string
  fileSuffix: string
  fileSize: number
  fileType: string
  storageType?: number
  bucketName?: string
  objectName?: string
  accessUrl: string
  previewUrl: string
  downloadUrl: string
  uploadUserId?: number
  uploadUserName?: string
  bizType?: string
  bizId?: string
  nodeId?: string
  previewable: boolean
  createTime: string
}

export interface ApprovalTaskVO {
  id: number
  taskNo: string
  flowableTaskId: string
  instanceId: number
  instanceNo: string
  processKey: string
  processName: string
  title: string
  nodeId: string
  nodeName: string
  assigneeId: number
  assigneeName: string
  assignTime: string
  dueTime: string
  taskStatus: TaskStatus
  priority: Priority
  startUserId: number
  startUserName: string
  startUserAvatar?: string
  startDeptName: string
  startTime: string
  formData: Record<string, any>
  approvalResult?: ApprovalResult
  myComment?: string
  handleTime?: string
  readStatus?: 0 | 1
  canAddSign: boolean
  canTransfer: boolean
  canDelegate: boolean
  canReject: boolean
  canWithdraw?: boolean
  needSignature: boolean
  needComment: boolean
  rejectCount?: number
  maxRejectCount?: number
  rejectableNodeIds?: string[]
  version?: number
  aiRecommendation?: import('./ai').AiRecommendationVO
  aiRecommendationId?: number
}

export interface ApprovalHistoryVO {
  id: number
  nodeId: string
  nodeName: string
  activityType: ActivityType
  activityTypeDesc?: string
  operatorId: number
  operatorName: string
  operatorAvatar?: string
  operatorDeptName: string
  targetUserId?: number
  targetUserName?: string
  targetNodeId?: string
  targetNodeName?: string
  actionRemark?: string
  signatureUrl?: string
  attachmentIds?: number[]
  attachmentList?: AttachmentVO[]
  duration?: number
  operateTime: string
  status?: 'pending' | 'approved' | 'rejected' | 'transferred' | 'current'
}

export interface ProcessInstanceVO {
  id: number
  instanceNo: string
  processKey: string
  processName: string
  title: string
  formId: number
  formVersion: number
  formData: Record<string, any>
  formSchema?: any
  instanceStatus: InstanceStatus
  instanceStatusDesc?: string
  startUserId: number
  startUserName: string
  startUserAvatar?: string
  startDeptName: string
  startTime: string
  endTime?: string
  bpmnXml?: string
  currentNodeIds: string[]
  currentNodeNames?: string[]
  canWithdraw?: boolean
  rejectCount?: number
  maxRejectCount?: number
  multiInstanceSignList?: MultiInstanceSignVO[]
}

export interface ProcessCategoryVO {
  id: number
  parentId: number
  categoryName: string
  businessLineId?: number
  businessLineName?: string
  children?: ProcessCategoryVO[]
}

export interface StartableProcessVO {
  id: number
  processName: string
  processKey: string
  categoryId: number
  categoryName: string
  businessLineId?: number
  businessLineName?: string
  icon?: string
  description?: string
  formId: number
  formVersion: number
  formSchema?: any
}

export interface DraftVO {
  id: number
  draftNo: string
  processDefinitionId: number
  processKey: string
  processVersionId?: number
  processName: string
  title: string
  formId: number
  formVersion: number
  formData: Record<string, any>
  draftStatus?: number
  draftStatusName?: string
  lastAutoSaveTime?: string
  autoSaveCount?: number
  attachmentIds?: number[]
  ccUserIds?: number[]
  creatorId?: number
  creatorName?: string
  createTime: string
  updateTime: string
}

export interface DraftSaveDTO {
  id?: number
  draftNo?: string
  processDefinitionId: number
  processKey: string
  processVersionId?: number
  processName?: string
  title?: string
  formId: number
  formVersion: number
  formData: Record<string, any>
  draftStatus?: number
  attachmentIds?: number[]
  ccUserIds?: number[]
}

export interface CcTaskVO extends ApprovalTaskVO {
  readStatus: 0 | 1
  ccTime: string
}

export interface ApproveDTO {
  taskId: string
  instanceId: number
  version?: number
  actionRemark?: string
  signatureUrl?: string
  attachmentIds?: number[]
}

export interface RejectDTO extends ApproveDTO {
  targetNodeId?: string
  targetNodeName?: string
  resetFormData?: boolean
}

export interface RejectToNodeDTO {
  taskId: string
  instanceId?: number
  version?: number
  targetNodeId: string
  targetNodeName?: string
  actionRemark: string
  signatureUrl?: string
  attachmentIds?: number[]
  resetFormData?: boolean
}

export interface TransferDTO extends ApproveDTO {
  targetUserId: number
  targetUserName: string
}

export interface AddSignDTO extends ApproveDTO {
  targetUserIds: number[]
  targetUserNames: string[]
  signType: 'BEFORE' | 'AFTER'
}

export interface DelegateDTO extends ApproveDTO {
  targetUserId: number
  targetUserName: string
}

export interface StartProcessDTO {
  processKey: string
  title: string
  formData: Record<string, any>
  ccUserIds?: number[]
  attachmentIds?: number[]
  draftId?: number
}

export type DelegationStatus = 0 | 1 | 2 | 3

export interface DelegationVO {
  id: number
  delegatorId: number
  delegatorName: string
  delegateeId: number
  delegateeName: string
  startTime: string
  endTime: string
  delegationReason?: string
  delegationStatus: DelegationStatus
  delegationStatusName: string
  processKeyList: string[]
  remark?: string
  createTime: string
}

export interface DelegationSaveDTO {
  id?: number
  delegateeId: number
  delegateeName: string
  startTime: string
  endTime: string
  delegationReason?: string
  processKeys?: string[]
  remark?: string
}

export interface DelegationQueryDTO {
  pageNum?: number
  pageSize?: number
  delegationStatus?: DelegationStatus
  delegatorName?: string
  delegateeName?: string
  queryType?: number
}

export interface BatchTransferDTO {
  taskIds?: string[]
  transferAll?: boolean
  targetUserId: number
  targetUserName: string
  actionRemark?: string
}

export type TransferType = 1 | 2 | 3

export interface TransferRecordVO {
  id: number
  instanceId: number
  instanceNo: string
  taskId?: number
  flowableTaskId?: string
  nodeId?: string
  nodeName?: string
  transferType: TransferType
  transferTypeName: string
  sourceUserId: number
  sourceUserName: string
  targetUserId: number
  targetUserName: string
  transferReason?: string
  delegationId?: number
  title?: string
  createTime: string
}

export type SignerStatus = 0 | 1 | 2

export interface SignerStatusVO {
  userId: number
  userName: string
  userAvatar?: string
  deptName?: string
  signStatus: SignerStatus
  signStatusName: string
  comment?: string
  signatureUrl?: string
  attachmentIds?: number[]
  assignTime?: string
  handleTime?: string
  duration?: number
}

export interface MultiInstanceSignVO {
  nodeId: string
  nodeName: string
  approveType?: number
  approveTypeName?: string
  completionType?: number
  completionTypeName?: string
  passPercentage?: number
  vetoEnabled?: boolean
  totalSigners: number
  approvedCount: number
  rejectedCount: number
  pendingCount: number
  progressText: string
  signers: SignerStatusVO[]
}

export interface TrackingNodeOperatorVO {
  userId: number
  userName: string
  userAvatar?: string
  deptName?: string
  action?: string
  actionName?: string
  actionRemark?: string
  operateTime?: string
  duration?: number
}

export interface TrackingNodeVO {
  nodeId: string
  nodeName: string
  nodeType: string
  nodeCategory: number
  status: string
  statusName: string
  duration?: number
  historicalAvgDuration?: number
  durationDeviation?: number
  isBottleneck?: boolean
  operators: TrackingNodeOperatorVO[]
  startTime?: string
  endTime?: string
  actionRemark?: string
  signatureUrl?: string
  actionName?: string
}

export interface TrackingEdgeVO {
  sourceId: string
  targetId: string
  label?: string
}

export interface TrackingMapVO {
  instanceId: number
  instanceNo: string
  title: string
  averageDuration?: number
  historicalInstanceCount?: number
  nodes: TrackingNodeVO[]
  edges: TrackingEdgeVO[]
}

export interface ApprovalStatusUpdateVO {
  type: 'STATUS_UPDATE'
  instanceId: number
  instanceNo: string
  instanceStatus: InstanceStatus
  instanceStatusName?: string
  actionType: string
  actionTypeName: string
  operatorId: number
  operatorName: string
  version: number
  timestamp: string
}

export interface WebSocketMessage<T = any> {
  type: string
  data?: T
}

export type CommentTemplateScopeType = 0 | 1 | 2

export interface CommentTemplateCategoryVO {
  id: number
  categoryName: string
  categoryCode: string
  scopeType: CommentTemplateScopeType
  scopeTypeName: string
  deptId?: number
  deptName?: string
  sortOrder: number
  status: number
  remark?: string
  createBy?: number
  createByName?: string
  createTime: string
  updateTime: string
}

export interface CommentTemplateVO {
  id: number
  categoryId: number
  categoryName: string
  templateName: string
  templateContent: string
  scopeType: CommentTemplateScopeType
  scopeTypeName: string
  deptId?: number
  deptName?: string
  sortOrder: number
  useCount: number
  status: number
  remark?: string
  createBy?: number
  createByName?: string
  createTime: string
  updateTime: string
}

export interface CommentTemplateCategorySaveDTO {
  id?: number
  categoryName: string
  categoryCode: string
  scopeType: CommentTemplateScopeType
  deptId?: number
  sortOrder?: number
  status?: number
  remark?: string
}

export interface CommentTemplateSaveDTO {
  id?: number
  categoryId: number
  templateName: string
  templateContent: string
  scopeType: CommentTemplateScopeType
  deptId?: number
  sortOrder?: number
  status?: number
  remark?: string
}

export interface CommentTemplateQueryDTO {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  templateName?: string
  keyword?: string
  scopeType?: CommentTemplateScopeType
  status?: number
  sortBy?: string
}

export interface BatchRemindDTO {
  taskIds: number[]
  remark?: string
}

export interface BatchRemindResultVO {
  totalCount: number
  successCount: number
  failCount: number
  remindMessageCount: number
  failItems: Array<{
    taskId: number
    taskNo: string
    reason: string
  }>
}

export interface ProcessVersion {
  id: number
  processDefinitionId: number
  processKey: string
  version: number
  flowableDeploymentId: string
  flowableProcessDefId: string
  formId: number
  formVersion: number
  versionRemark: string
  isCurrent: number
  suspendStatus: number
  createTime: string
}

export interface MigrateInstanceVO {
  id: number
  instanceNo: string
  processDefinitionId: number
  processKey: string
  processVersionId: number
  title: string
  startUserId: number
  startUserName: string
  startDeptId: number
  startDeptName: string
  startTime: string
  currentVersion: number
  latestVersion: number
  latestVersionId: number
  versionRemark: string
  versionGap: number
  canMigrate: boolean
  migrateTip: string
  currentNodeIds: string[]
  instanceStatus: number
  availableVersions: ProcessVersion[]
}

export interface CompatibilityCheckVO {
  compatible: boolean
  errors: string[]
  warnings: string[]
  infos: string[]
  nodeCheck: {
    sourceNodes: string[]
    targetNodes: string[]
    missingNodes: string[]
    matchedNodes: string[]
  }
  variableCheck: {
    sourceVariables: string[]
    targetVariables: string[]
    missingVariables: string[]
    matchedVariables: string[]
  }
}

export interface ProcessMigrationDTO {
  targetVersionId: number
  processDefinitionId: number
  instanceIds: number[]
  remark?: string
  forceMigrate?: boolean
}

export interface MigrationDetailItem {
  detailId: number
  instanceId: number
  instanceNo: string
  title: string
  startUserName: string
  migrationResult: number
  migrationResultName: string
  skipReason: string
  errorMessage: string
  sourceCurrentNodeIds: string[]
  targetCurrentNodeIds: string[]
  compatibilityCheck: CompatibilityCheckVO
}

export interface ProcessMigrationResultVO {
  migrationNo: string
  recordId: number
  processKey: string
  sourceVersionId: number
  sourceVersion: number
  targetVersionId: number
  targetVersion: number
  totalCount: number
  successCount: number
  failCount: number
  skipCount: number
  migrationStatus: number
  migrationStatusName: string
  createTime: string
  details: MigrationDetailItem[]
}

export interface ProcessMigrationRecordVO {
  id: number
  migrationNo: string
  processKey: string
  processDefinitionId: number
  sourceVersionId: number
  sourceVersion: number
  targetVersionId: number
  targetVersion: number
  totalCount: number
  successCount: number
  failCount: number
  skipCount: number
  migrationStatus: number
  migrationStatusName: string
  remark: string
  createByName: string
  createTime: string
}
