export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface LoginDTO {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  tokenHead: string
  expiresIn: number
}

export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  deptId: number
  deptName: string
  roles: string[]
  permissions: string[]
  tenantId: number
  tenantIds: number[]
  tenants: TenantSimpleVO[]
  createTime: string
}

export interface TenantSimpleVO {
  tenantId: number
  tenantName: string
  tenantCode: string
  businessType: string
}

export interface MenuVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  icon: string
  sort: number
  type: number
  permission: string
  children?: MenuVO[]
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  deptId: number
  deptName: string
  status: number
  createTime: string
}

export interface RoleVO {
  id: number
  roleCode: string
  roleName: string
  description: string
  createTime: string
}

export interface DeptVO {
  id: number
  parentId: number
  deptName: string
  leader: string
  phone: string
  email: string
  sort: number
  status: number
  children?: DeptVO[]
}

export interface FormDefinitionVO {
  id: number
  formName: string
  formKey: string
  version: number
  description: string
  status: number
  createTime: string
}

export interface ProcessDefinitionVO {
  id: number
  processName: string
  processKey: string
  version: number
  categoryId: number
  categoryName: string
  businessLineId: number
  businessLineName: string
  formId: number
  formName: string
  status: number
  createTime: string
}

export interface WfTaskVO {
  taskId: string
  taskName: string
  processInstanceId: string
  processDefinitionId: string
  processDefinitionName: string
  processDefinitionKey: string
  businessKey: string
  createTime: string
  assignee: string
  assigneeName: string
  startUserId: string
  startUserName: string
  formKey: string
}

export interface WfProcessInstanceVO {
  processInstanceId: string
  processDefinitionId: string
  processDefinitionName: string
  processDefinitionKey: string
  businessKey: string
  startUserId: string
  startUserName: string
  startTime: string
  endTime: string
  status: string
}

export interface WfApprovalHistoryVO {
  taskId: string
  taskName: string
  assignee: string
  assigneeName: string
  action: string
  comment: string
  createTime: string
  endTime: string
}

export * from './form'
export * from './ai'

export interface NodeConfig {
  nodeId: string
  nodeName: string
  nodeType: string
  approveType: number
  assigneeType: number
  assigneeValue: number[]
  assigneeDeptLevel: number
  assigneeScript: string
  formPermission: Record<string, 'edit' | 'readonly' | 'hidden'>
  timeoutStrategy: number
  timeoutHours: number
  timeoutEscalateLevels: number
  passRate: number
  notifyConfig: {
    channels: string[]
    ccUserIds: number[]
  }
  emptyAssigneeStrategy: number
  refuseStrategy: number
  refuseTargetNodeId: string
  canAddSign: 0 | 1
  canTransfer: 0 | 1
  canDelegate: 0 | 1
  needSignature: 0 | 1
  needComment: 0 | 1
}

export interface SequenceFlowConfig {
  flowId: string
  sourceNodeId: string
  targetNodeId: string
  conditionExpression: string
  isDefault: 0 | 1
}

export interface DeployData {
  processKey: string
  processName: string
  businessLineId: number
  categoryId: number
  description: string
  formId: number
  bpmnXml: string
  nodeConfigs: NodeConfig[]
  sequenceFlowConfigs: SequenceFlowConfig[]
  versionRemark: string
}

export interface ProcessVersionVO {
  id: number
  processDefinitionId: number
  version: number
  bpmnXml: string
  status: number
  deployTime: string
  remark: string
}

export interface ValidateResultVO {
  level: 'error' | 'warning' | 'info'
  nodeId?: string
  nodeName?: string
  message: string
}

export interface FormFieldVO {
  fieldKey: string
  fieldName: string
  fieldType: string
}

export interface SimulateStepVO {
  step: number
  nodeId: string
  nodeName: string
  assigneeNames: string[]
  action?: string
}
