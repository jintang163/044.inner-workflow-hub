import type { WidgetConfig } from '@/types/form'

export const WIDGET_PRESETS: WidgetConfig[] = [
  {
    name: 'input',
    label: '输入框',
    icon: 'FormOutlined',
    group: 'basic',
    component: 'Input',
    defaultSchema: {
      type: 'string',
      title: '输入框',
      'x-decorator': 'FormItem',
      'x-component': 'Input',
      'x-component-props': {
        placeholder: '请输入'
      }
    },
    defaultProps: {}
  },
  {
    name: 'textarea',
    label: '文本域',
    icon: 'EditOutlined',
    group: 'basic',
    component: 'TextArea',
    defaultSchema: {
      type: 'string',
      title: '文本域',
      'x-decorator': 'FormItem',
      'x-component': 'TextArea',
      'x-component-props': {
        placeholder: '请输入',
        rows: 3
      }
    },
    defaultProps: {}
  },
  {
    name: 'numberpicker',
    label: '数字输入',
    icon: 'NumberOutlined',
    group: 'basic',
    component: 'NumberPicker',
    defaultSchema: {
      type: 'number',
      title: '数字输入',
      default: 0,
      'x-decorator': 'FormItem',
      'x-component': 'NumberPicker',
      'x-component-props': {
        min: 0,
        precision: 2,
        style: { width: '100%' }
      }
    },
    defaultProps: {}
  },
  {
    name: 'select',
    label: '选择器',
    icon: 'SelectOutlined',
    group: 'basic',
    component: 'Select',
    hasOptions: true,
    defaultSchema: {
      type: 'string',
      title: '选择器',
      'x-decorator': 'FormItem',
      'x-component': 'Select',
      'x-component-props': {
        placeholder: '请选择',
        options: [
          { label: '选项1', value: 'option1' },
          { label: '选项2', value: 'option2' },
          { label: '选项3', value: 'option3' }
        ]
      }
    },
    defaultProps: {}
  },
  {
    name: 'selectmultiple',
    label: '多选框',
    icon: 'CheckSquareOutlined',
    group: 'basic',
    component: 'Select',
    hasOptions: true,
    defaultSchema: {
      type: 'array',
      title: '多选框',
      'x-decorator': 'FormItem',
      'x-component': 'Select',
      'x-component-props': {
        placeholder: '请选择',
        mode: 'multiple',
        options: [
          { label: '选项1', value: 'option1' },
          { label: '选项2', value: 'option2' },
          { label: '选项3', value: 'option3' }
        ]
      }
    },
    defaultProps: {}
  },
  {
    name: 'datepicker',
    label: '日期选择',
    icon: 'CalendarOutlined',
    group: 'basic',
    component: 'DatePicker',
    defaultSchema: {
      type: 'string',
      title: '日期选择',
      'x-decorator': 'FormItem',
      'x-component': 'DatePicker',
      'x-component-props': {
        placeholder: '请选择日期',
        style: { width: '100%' }
      }
    },
    defaultProps: {}
  },
  {
    name: 'datetimepicker',
    label: '日期时间',
    icon: 'ClockCircleOutlined',
    group: 'basic',
    component: 'DateTimePicker',
    defaultSchema: {
      type: 'string',
      title: '日期时间',
      'x-decorator': 'FormItem',
      'x-component': 'DateTimePicker',
      'x-component-props': {
        placeholder: '请选择日期时间',
        style: { width: '100%' }
      }
    },
    defaultProps: {}
  },
  {
    name: 'timepicker',
    label: '时间选择',
    icon: 'FieldTimeOutlined',
    group: 'basic',
    component: 'TimePicker',
    defaultSchema: {
      type: 'string',
      title: '时间选择',
      'x-decorator': 'FormItem',
      'x-component': 'TimePicker',
      'x-component-props': {
        placeholder: '请选择时间',
        style: { width: '100%' }
      }
    },
    defaultProps: {}
  },
  {
    name: 'radiogroup',
    label: '单选组',
    icon: 'DotChartOutlined',
    group: 'basic',
    component: 'Radio.Group',
    hasOptions: true,
    defaultSchema: {
      type: 'string',
      title: '单选组',
      'x-decorator': 'FormItem',
      'x-component': 'Radio.Group',
      'x-component-props': {
        options: [
          { label: '选项1', value: 'option1' },
          { label: '选项2', value: 'option2' },
          { label: '选项3', value: 'option3' }
        ]
      }
    },
    defaultProps: {}
  },
  {
    name: 'checkboxgroup',
    label: '多选框组',
    icon: 'CheckCircleOutlined',
    group: 'basic',
    component: 'Checkbox.Group',
    hasOptions: true,
    defaultSchema: {
      type: 'array',
      title: '多选框组',
      'x-decorator': 'FormItem',
      'x-component': 'Checkbox.Group',
      'x-component-props': {
        options: [
          { label: '选项1', value: 'option1' },
          { label: '选项2', value: 'option2' },
          { label: '选项3', value: 'option3' }
        ]
      }
    },
    defaultProps: {}
  },
  {
    name: 'switch',
    label: '开关',
    icon: 'SwitcherOutlined',
    group: 'basic',
    component: 'Switch',
    defaultSchema: {
      type: 'boolean',
      title: '开关',
      default: false,
      'x-decorator': 'FormItem',
      'x-component': 'Switch'
    },
    defaultProps: {}
  },
  {
    name: 'rate',
    label: '评分',
    icon: 'StarOutlined',
    group: 'basic',
    component: 'Rate',
    defaultSchema: {
      type: 'number',
      title: '评分',
      default: 0,
      'x-decorator': 'FormItem',
      'x-component': 'Rate',
      'x-component-props': {
        count: 5
      }
    },
    defaultProps: {}
  },
  {
    name: 'slider',
    label: '滑块',
    icon: 'ColumnHeightOutlined',
    group: 'basic',
    component: 'Slider',
    defaultSchema: {
      type: 'number',
      title: '滑块',
      default: 0,
      'x-decorator': 'FormItem',
      'x-component': 'Slider',
      'x-component-props': {
        min: 0,
        max: 100,
        step: 1
      }
    },
    defaultProps: {}
  },
  {
    name: 'upload',
    label: '上传附件',
    icon: 'UploadOutlined',
    group: 'advanced',
    component: 'Upload',
    defaultSchema: {
      type: 'array',
      title: '上传附件',
      'x-decorator': 'FormItem',
      'x-component': 'Upload',
      'x-component-props': {
        textContent: '上传附件',
        listType: 'text',
        maxCount: 5
      }
    },
    defaultProps: {}
  },
  {
    name: 'imageupload',
    label: '图片上传',
    icon: 'PictureOutlined',
    group: 'advanced',
    component: 'Upload',
    defaultSchema: {
      type: 'array',
      title: '图片上传',
      'x-decorator': 'FormItem',
      'x-component': 'Upload',
      'x-component-props': {
        accept: 'image/*',
        listType: 'picture-card',
        maxCount: 3,
        textContent: ''
      }
    },
    defaultProps: {}
  },
  {
    name: 'userselect',
    label: '人员选择',
    icon: 'UserOutlined',
    group: 'advanced',
    component: 'UserSelect',
    defaultSchema: {
      type: 'array',
      title: '人员选择',
      'x-decorator': 'FormItem',
      'x-component': 'UserSelect',
      'x-component-props': {
        placeholder: '请选择人员',
        multiple: true
      }
    },
    defaultProps: {}
  },
  {
    name: 'deptselect',
    label: '部门选择',
    icon: 'TeamOutlined',
    group: 'advanced',
    component: 'DeptSelect',
    defaultSchema: {
      type: 'number',
      title: '部门选择',
      'x-decorator': 'FormItem',
      'x-component': 'DeptSelect',
      'x-component-props': {
        placeholder: '请选择部门',
        multiple: false
      }
    },
    defaultProps: {}
  },
  {
    name: 'cascader',
    label: '级联选择',
    icon: 'ApartmentOutlined',
    group: 'advanced',
    component: 'Cascader',
    hasOptions: true,
    defaultSchema: {
      type: 'array',
      title: '级联选择',
      'x-decorator': 'FormItem',
      'x-component': 'Cascader',
      'x-component-props': {
        placeholder: '请选择',
        style: { width: '100%' },
        options: [
          {
            label: '北京',
            value: 'beijing',
            children: [
              { label: '朝阳区', value: 'chaoyang' },
              { label: '海淀区', value: 'haidian' }
            ]
          },
          {
            label: '上海',
            value: 'shanghai',
            children: [
              { label: '浦东新区', value: 'pudong' },
              { label: '徐汇区', value: 'xuhui' }
            ]
          }
        ]
      }
    },
    defaultProps: {}
  },
  {
    name: 'subform',
    label: '子表单/表格',
    icon: 'TableOutlined',
    group: 'advanced',
    component: 'ArrayTable',
    defaultSchema: {
      type: 'array',
      title: '子表单',
      'x-decorator': 'FormItem',
      'x-component': 'ArrayTable',
      'x-component-props': {},
      items: {
        type: 'object',
        properties: {
          col1: {
            type: 'string',
            title: '列1',
            'x-decorator': 'FormItem',
            'x-component': 'Input',
            'x-component-props': {
              placeholder: '请输入'
            }
          },
          col2: {
            type: 'number',
            title: '列2',
            'x-decorator': 'FormItem',
            'x-component': 'NumberPicker',
            'x-component-props': {
              min: 0,
              style: { width: '100%' }
            }
          }
        }
      }
    },
    defaultProps: {}
  },
  {
    name: 'divider',
    label: '分割线',
    icon: 'MenuUnfoldOutlined',
    group: 'advanced',
    component: 'Divider',
    defaultSchema: {
      type: 'void',
      title: '',
      'x-component': 'Divider',
      'x-component-props': {
        type: 'horizontal',
        dashed: false
      }
    },
    defaultProps: {}
  },
  {
    name: 'description',
    label: '说明文字',
    icon: 'InfoCircleOutlined',
    group: 'advanced',
    component: 'Alert',
    defaultSchema: {
      type: 'void',
      title: '',
      'x-component': 'Alert',
      'x-component-props': {
        type: 'info',
        showIcon: true,
        message: '这里是说明文字内容'
      }
    },
    defaultProps: {}
  }
]

export const getWidgetPreset = (name: string): WidgetConfig | undefined => {
  return WIDGET_PRESETS.find(w => w.name === name)
}

export const BASIC_WIDGETS = WIDGET_PRESETS.filter(w => w.group === 'basic')
export const ADVANCED_WIDGETS = WIDGET_PRESETS.filter(w => w.group === 'advanced')
