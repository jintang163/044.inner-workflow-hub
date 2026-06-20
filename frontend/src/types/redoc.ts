export interface WfRedocTemplateVO {
  id: number
  templateCode: string
  templateName: string
  category?: string
  processKey?: string
  templateFileId?: number
  templateFileName?: string
  headerColor?: string
  headerFontSize?: number
  paperSize?: string
  orientation?: number
  sealEnabled?: number
  sealId?: number
  sealPositionType?: number
  sealScale?: number
  signatureEnabled?: number
  outputFormat?: number
  watermarkEnabled?: number
  watermarkText?: string
  placeholderSample?: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface WfRedocTemplateSaveDTO {
  id?: number
  templateCode: string
  templateName: string
  category?: string
  processKey?: string
  templateFileId: number
  headerColor?: string
  headerFontSize?: number
  paperSize?: string
  orientation?: number
  sealEnabled?: number
  sealId?: number
  sealPositionType?: number
  sealScale?: number
  signatureEnabled?: number
  outputFormat?: number
  watermarkEnabled?: number
  watermarkText?: string
  placeholderSample?: string
  status?: number
  remark?: string
}

export interface WfRedocGenerateDTO {
  instanceNo: string
  taskId?: number
  templateId: number
  fileTitle: string
  approvalNo?: string
  fileNo?: string
  outputFormat?: number
  sealId?: number
  sealEnabled?: number
  signatureEnabled?: number
  placeholderValues?: Record<string, any>
}

export interface WfRedocGeneratedVO {
  id: number
  instanceNo: string
  processKey?: string
  templateId: number
  templateCode?: string
  templateName?: string
  fileTitle: string
  approvalNo?: string
  fileNo?: string
  outputFormat: number
  wordFileId?: number
  wordFileName?: string
  wordPreviewUrl?: string
  wordDownloadUrl?: string
  pdfFileId?: number
  pdfFileName?: string
  pdfPreviewUrl?: string
  pdfDownloadUrl?: string
  sealApplied?: number
  sealId?: number
  signatureApplied?: number
  generateTime?: string
  generateBy?: number
  generateByName?: string
  printCount?: number
  downloadCount?: number
  status: number
  remark?: string
  createTime?: string
}

export interface WfRedocBatchDTO {
  ids?: number[]
  instanceNos?: string[]
}

export interface WfSealConfigVO {
  id: number
  sealCode: string
  sealName: string
  sealType?: number
  sealOwnerName?: string
  sealImageId?: number
  sealImageUrl?: string
  sealText?: string
  sealShape?: number
  sealDiameter?: number
  sealColor?: string
  signatureAlgorithm?: string
  status: number
  remark?: string
  createTime?: string
}

export interface WfSealConfigSaveDTO {
  id?: number
  sealCode: string
  sealName: string
  sealType?: number
  sealOwnerId?: number
  sealOwnerName?: string
  sealImageId?: number
  sealText?: string
  sealShape?: number
  sealDiameter?: number
  sealColor?: string
  digitalCertId?: number
  signatureAlgorithm?: string
  allowedUserIds?: string
  allowedDeptIds?: string
  status?: number
  remark?: string
}
