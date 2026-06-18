export interface TenantVO {
  id: number
  tenantName: string
  tenantCode: string
  contactName: string
  contactEmail: string
  contactPhone: string
  businessType: string
  status: number
  expireTime: string
  remark: string
  createTime: string
  userCount: number
}

export interface TenantSimpleVO {
  tenantId: number
  tenantName: string
  tenantCode: string
  businessType: string
}

export interface TenantStatsVO {
  tenantId: number
  tenantName: string
  processCount: number
  pendingCount: number
  avgDuration: number
}

export interface TenantUserVO {
  id: number
  tenantId: number
  userId: number
  username: string
  realName: string
  tenantRole: string
  status: number
  createTime: string
}

export interface TenantRegisterDTO {
  tenantName: string
  tenantCode: string
  contactName: string
  contactEmail?: string
  contactPhone?: string
  businessType: string
  remark?: string
}

export interface TenantUpdateDTO {
  id: number
  tenantName?: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  businessType?: string
  expireTime?: string
  remark?: string
  status?: number
}
