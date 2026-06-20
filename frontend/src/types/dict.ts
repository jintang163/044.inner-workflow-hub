export interface SysDictTypeVO {
  id: number
  dictName: string
  dictCode: string
  sourceType: number
  apiUrl?: string
  apiMethod?: string
  apiResponsePath?: string
  cascadeField?: string
  cascadeParent?: string
  cacheEnabled: number
  cacheTtl: number
  status: number
  remark?: string
  createTime: string
  updateTime: string
  items?: SysDictDataVO[]
}

export interface SysDictDataVO {
  id: number
  dictCode: string
  dictLabel: string
  dictValue: string
  dictSort: number
  colorTag?: string
  parentValue?: string
  isDefault?: number
  status: number
  remark?: string
  createTime: string
  children?: SysDictDataVO[]
}

export interface WfDataSourceConfigVO {
  id: number
  sourceCode: string
  sourceName: string
  sourceType: number
  apiUrl: string
  apiMethod?: string
  apiParamsTemplate?: string
  responsePath?: string
  labelField?: string
  valueField?: string
  childrenField?: string
  cacheEnabled: number
  cacheTtl: number
  timeout: number
  authType: number
  status: number
  remark?: string
  createTime: string
}

export interface DictOptionItem {
  label: string
  value: string | number
  color?: string
  children?: DictOptionItem[]
}

export interface DictChangeMessage {
  type: 'dictChange'
  dictCode: string
  changeType: string
  timestamp: number
}
