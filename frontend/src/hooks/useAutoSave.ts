import { useState, useEffect, useRef, useCallback } from 'react'
import { message } from 'antd'
import { formApi } from '@/api'
import type { DraftSaveDTO, DraftVO } from '@/types/approval'

interface UseAutoSaveOptions {
  processKey: string
  processDefinitionId: number
  formId: number
  formVersion: number
  processVersionId?: number
  processName?: string
  title?: string
  interval?: number
  enabled?: boolean
  draftId?: number
  onSaveSuccess?: (draft: DraftVO) => void
}

const LOCAL_STORAGE_KEY_PREFIX = 'wf_draft_'

export const useAutoSave = (options: UseAutoSaveOptions) => {
  const {
    processKey,
    processDefinitionId,
    formId,
    formVersion,
    processVersionId,
    processName,
    title,
    interval = 30000,
    enabled = true,
    draftId: initialDraftId,
    onSaveSuccess
  } = options

  const [draftId, setDraftId] = useState<number | undefined>(initialDraftId)
  const [draftNo, setDraftNo] = useState<string | undefined>()
  const [lastSaveTime, setLastSaveTime] = useState<string>('')
  const [saving, setSaving] = useState(false)
  const [autoSaveEnabled, setAutoSaveEnabled] = useState(false)

  const formDataRef = useRef<Record<string, any> | null>(null)
  const attachmentIdsRef = useRef<number[]>([])
  const ccUserIdsRef = useRef<number[]>([])
  const timerRef = useRef<NodeJS.Timeout | null>(null)
  const localStorageKey = `${LOCAL_STORAGE_KEY_PREFIX}${processKey}`

  const saveToLocalStorage = useCallback((data: Record<string, any>) => {
    try {
      const storageData = {
        draftId,
        draftNo,
        processKey,
        processDefinitionId,
        formId,
        formVersion,
        formData: data,
        attachmentIds: attachmentIdsRef.current,
        ccUserIds: ccUserIdsRef.current,
        title,
        processName,
        processVersionId,
        timestamp: Date.now()
      }
      localStorage.setItem(localStorageKey, JSON.stringify(storageData))
    } catch (e) {
      console.warn('保存到 localStorage 失败:', e)
    }
  }, [localStorageKey, draftId, draftNo, processKey, processDefinitionId, formId, formVersion, title, processName, processVersionId])

  const loadFromLocalStorage = useCallback(() => {
    try {
      const data = localStorage.getItem(localStorageKey)
      if (data) {
        return JSON.parse(data)
      }
    } catch (e) {
      console.warn('从 localStorage 读取失败:', e)
    }
    return null
  }, [localStorageKey])

  const clearLocalStorage = useCallback(() => {
    try {
      localStorage.removeItem(localStorageKey)
    } catch (e) {
      console.warn('清除 localStorage 失败:', e)
    }
  }, [localStorageKey])

  const saveDraft = useCallback(async (formData: Record<string, any>, isAuto = false) => {
    if (!enabled) return null

    try {
      setSaving(true)
      const saveData: DraftSaveDTO = {
        id: draftId,
        draftNo,
        processDefinitionId,
        processKey,
        processVersionId,
        processName,
        title,
        formId,
        formVersion,
        formData,
        draftStatus: isAuto ? 2 : 1,
        attachmentIds: attachmentIdsRef.current,
        ccUserIds: ccUserIdsRef.current
      }

      const result = isAuto
        ? await formApi.draftAutoSave(saveData)
        : await formApi.draftSave(saveData)

      if (result) {
        setDraftId(result.id)
        setDraftNo(result.draftNo)
        setLastSaveTime(result.updateTime)
        saveToLocalStorage(formData)
        onSaveSuccess?.(result)
      }

      return result
    } catch (err: any) {
      saveToLocalStorage(formData)
      if (!isAuto) {
        message.error(err?.message || '保存失败，已在本地缓存')
      }
      return null
    } finally {
      setSaving(false)
    }
  }, [enabled, draftId, draftNo, processDefinitionId, processKey, processVersionId, processName, title, formId, formVersion, saveToLocalStorage, onSaveSuccess])

  const manualSave = useCallback(async (formData: Record<string, any>) => {
    const result = await saveDraft(formData, false)
    if (result) {
      message.success('保存成功')
    }
    return result
  }, [saveDraft])

  const updateFormDataRef = useCallback((formData: Record<string, any>) => {
    formDataRef.current = formData
  }, [])

  const updateAttachmentIds = useCallback((ids: number[]) => {
    attachmentIdsRef.current = ids
  }, [])

  const updateCcUserIds = useCallback((ids: number[]) => {
    ccUserIdsRef.current = ids
  }, [])

  useEffect(() => {
    if (!enabled || !autoSaveEnabled) {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
      return
    }

    timerRef.current = setInterval(() => {
      if (formDataRef.current && Object.keys(formDataRef.current).length > 0) {
        saveDraft(formDataRef.current, true)
      }
    }, interval)

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [enabled, autoSaveEnabled, interval, saveDraft])

  const enableAutoSave = useCallback(() => {
    setAutoSaveEnabled(true)
  }, [])

  const disableAutoSave = useCallback(() => {
    setAutoSaveEnabled(false)
  }, [])

  return {
    draftId,
    draftNo,
    lastSaveTime,
    saving,
    autoSaveEnabled,
    setDraftId,
    setDraftNo,
    manualSave,
    saveDraft,
    enableAutoSave,
    disableAutoSave,
    updateFormDataRef,
    updateAttachmentIds,
    updateCcUserIds,
    loadFromLocalStorage,
    clearLocalStorage,
    saveToLocalStorage
  }
}

export default useAutoSave
