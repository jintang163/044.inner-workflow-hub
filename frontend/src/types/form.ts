export interface FormilySchema {
  type: string
  properties: Record<string, any>
  [key: string]: any
}

export type FieldPermissionType = 'edit' | 'readonly' | 'hidden'

export type DisplayType = 'none' | 'visible' | 'embedded'

export interface WidgetConfig {
  name: string
  label: string
  icon: string
  group: 'basic' | 'advanced'
  component: string
  defaultSchema: Record<string, any>
  defaultProps: Record<string, any>
  hasOptions?: boolean
  hasDataSource?: boolean
}

export interface FormDefinitionVO {
  id: number
  formName: string
  formKey: string
  version: number
  description: string
  status: number
  processCount?: number
  schema?: FormilySchema
  createTime: string
  updateTime: string
}

export interface SelectOption {
  label: string
  value: string | number
  color?: string
  children?: SelectOption[]
}

export type SourceType = 'static' | 'api' | 'dict'

export interface DataSourceConfig {
  type: SourceType
  options?: SelectOption[]
  apiUrl?: string
  dictCode?: string
  sourceCode?: string
  cascadeParentField?: string
}

export interface ValidatorRule {
  type?: 'required' | 'pattern' | 'min' | 'max' | 'minLength' | 'maxLength' | 'custom'
  message?: string
  pattern?: string
  min?: number
  max?: number
  customCode?: string
}

export type ConditionOperator =
  | '=='
  | '!='
  | '>'
  | '<'
  | '>='
  | '<='
  | 'contains'
  | 'notContains'
  | 'startsWith'
  | 'endsWith'
  | 'empty'
  | 'notEmpty'
  | 'in'
  | 'notIn'

export type LogicalOperator = 'AND' | 'OR'

export type ReactionAction =
  | 'visible'
  | 'required'
  | 'readonly'
  | 'disabled'
  | 'setValue'
  | 'setOptions'
  | 'setComponentProps'

export interface SimpleCondition {
  type: 'simple'
  id: string
  dependencies: string[]
  operator: ConditionOperator
  value: any
}

export interface GroupCondition {
  type: 'group'
  id: string
  logicalOperator: LogicalOperator
  children: ConditionNode[]
}

export type ConditionNode = SimpleCondition | GroupCondition

export interface ReactionRuleV2 {
  id: string
  name?: string
  enabled?: boolean
  condition: ConditionNode
  action: ReactionAction
  actionValue?: any
}

export interface ReactionRule {
  id: string
  dependencies: string[]
  operator: ConditionOperator
  value: any
  targetField?: string
  action: ReactionAction
  actionValue?: any
}

export interface SubFormColumnConfig {
  fieldName: string
  title: string
  component: string
  componentProps?: Record<string, any>
  required?: boolean
  width?: number
}

export interface FieldConfig {
  name: string
  title: string
  componentType: string
  required: boolean
  placeholder?: string
  description?: string
  default?: any
  disabled?: boolean
  readOnly?: boolean
  hidden?: boolean
  display?: DisplayType
  width?: number
  sortOrder?: number
  validator?: ValidatorRule[]
  reactions?: ReactionRule[]
  componentProps?: Record<string, any>
  dataSource?: DataSourceConfig
  columns?: SubFormColumnConfig[]
}

export interface FormDesignerState {
  schema: FormilySchema
  selectedField: string | null
  hoverField: string | null
  setSchema: (schema: FormilySchema) => void
  setSelectedField: (field: string | null) => void
  setHoverField: (field: string | null) => void
  addField: (parentPath: string, fieldSchema: Record<string, any>, fieldName: string) => void
  updateField: (fieldPath: string, updates: Record<string, any>) => void
  removeField: (fieldPath: string) => void
  duplicateField: (fieldPath: string) => void
  moveField: (fromPath: string, toPath: string) => void
}
