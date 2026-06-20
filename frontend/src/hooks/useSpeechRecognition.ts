import { useState, useCallback, useRef, useEffect } from 'react'

interface SpeechRecognitionResult {
  transcript: string
  isFinal: boolean
}

interface UseSpeechRecognitionOptions {
  lang?: string
  continuous?: boolean
  interimResults?: boolean
  maxAlternatives?: number
}

interface UseSpeechRecognitionReturn {
  isListening: boolean
  isSupported: boolean
  transcript: string
  interimTranscript: string
  error: string | null
  startListening: () => void
  stopListening: () => void
  resetTranscript: () => void
}

declare global {
  interface Window {
    SpeechRecognition: any
    webkitSpeechRecognition: any
  }
}

const useSpeechRecognition = (
  options: UseSpeechRecognitionOptions = {}
): UseSpeechRecognitionReturn => {
  const {
    lang = 'zh-CN',
    continuous = true,
    interimResults = true,
    maxAlternatives = 1
  } = options

  const recognitionRef = useRef<any>(null)
  const finalTranscriptRef = useRef<string>('')

  const [isListening, setIsListening] = useState(false)
  const [isSupported, setIsSupported] = useState(false)
  const [transcript, setTranscript] = useState('')
  const [interimTranscript, setInterimTranscript] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition

    if (SpeechRecognition) {
      setIsSupported(true)
      const recognition = new SpeechRecognition()
      recognition.lang = lang
      recognition.continuous = continuous
      recognition.interimResults = interimResults
      recognition.maxAlternatives = maxAlternatives

      recognition.onresult = (event: any) => {
        let interim = ''
        let finalText = ''

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i]
          const transcriptText = result[0].transcript

          if (result.isFinal) {
            finalText += transcriptText
          } else {
            interim += transcriptText
          }
        }

        if (finalText) {
          finalTranscriptRef.current += finalText
          setTranscript(finalTranscriptRef.current)
        }
        setInterimTranscript(interim)
      }

      recognition.onerror = (event: any) => {
        let errorMessage = '语音识别出错'
        switch (event.error) {
          case 'no-speech':
            errorMessage = '未检测到语音输入'
            break
          case 'audio-capture':
            errorMessage = '未检测到麦克风'
            break
          case 'not-allowed':
            errorMessage = '麦克风权限被拒绝'
            break
          case 'network':
            errorMessage = '网络连接错误'
            break
          case 'aborted':
            return
        }
        setError(errorMessage)
        setIsListening(false)
      }

      recognition.onend = () => {
        setIsListening(false)
      }

      recognition.onstart = () => {
        setIsListening(true)
        setError(null)
      }

      recognitionRef.current = recognition
    }

    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort()
        } catch (_) {
          // ignore
        }
      }
    }
  }, [lang, continuous, interimResults, maxAlternatives])

  const startListening = useCallback(() => {
    if (!recognitionRef.current) {
      setError('当前浏览器不支持语音识别')
      return
    }
    try {
      setError(null)
      finalTranscriptRef.current = ''
      setTranscript('')
      setInterimTranscript('')
      recognitionRef.current.start()
    } catch (err: any) {
      if (err.name !== 'InvalidStateError') {
        setError(err.message || '启动语音识别失败')
      }
    }
  }, [])

  const stopListening = useCallback(() => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop()
      } catch (_) {
        // ignore
      }
      setIsListening(false)
    }
  }, [])

  const resetTranscript = useCallback(() => {
    finalTranscriptRef.current = ''
    setTranscript('')
    setInterimTranscript('')
  }, [])

  return {
    isListening,
    isSupported,
    transcript,
    interimTranscript,
    error,
    startListening,
    stopListening,
    resetTranscript
  }
}

export default useSpeechRecognition
