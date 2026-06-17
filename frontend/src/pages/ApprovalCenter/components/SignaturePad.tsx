import { useRef, useEffect, useState, useImperativeHandle, forwardRef } from 'react'
import { Button, Space, ColorPicker } from 'antd'
import { UndoOutlined, ClearOutlined, CheckOutlined } from '@ant-design/icons'

interface SignaturePadProps {
  width?: number
  height?: number
  penColor?: string
  penWidth?: number
  onSave?: (dataUrl: string) => void
  readOnly?: boolean
  value?: string
}

export interface SignaturePadRef {
  clear: () => void
  undo: () => void
  save: () => string | null
  isEmpty: () => boolean
}

const SignaturePad = forwardRef<SignaturePadRef, SignaturePadProps>(
  ({ width = 600, height = 200, penColor = '#000000', penWidth = 2, onSave, readOnly = false, value }, ref) => {
    const canvasRef = useRef<HTMLCanvasElement>(null)
    const [isDrawing, setIsDrawing] = useState(false)
    const [currentColor, setCurrentColor] = useState(penColor)
    const [currentWidth, setCurrentWidth] = useState(penWidth)
    const historyRef = useRef<ImageData[]>([])
    const lastPosRef = useRef<{ x: number; y: number } | null>(null)

    useImperativeHandle(ref, () => ({
      clear: () => clearCanvas(),
      undo: () => undo(),
      save: () => save(),
      isEmpty: () => historyRef.current.length === 0
    }))

    useEffect(() => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return

      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'

      if (value) {
        const img = new Image()
        img.onload = () => {
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
        }
        img.src = value
      }

      historyRef.current = []
    }, [value])

    const saveState = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      historyRef.current.push(imageData)
    }

    const getPos = (e: React.MouseEvent | React.TouchEvent) => {
      const canvas = canvasRef.current
      if (!canvas) return { x: 0, y: 0 }
      const rect = canvas.getBoundingClientRect()
      const scaleX = canvas.width / rect.width
      const scaleY = canvas.height / rect.height

      if ('touches' in e) {
        const touch = e.touches[0]
        return {
          x: (touch.clientX - rect.left) * scaleX,
          y: (touch.clientY - rect.top) * scaleY
        }
      }
      return {
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY
      }
    }

    const startDraw = (e: React.MouseEvent | React.TouchEvent) => {
      if (readOnly) return
      e.preventDefault()
      saveState()
      setIsDrawing(true)
      lastPosRef.current = getPos(e)
    }

    const draw = (e: React.MouseEvent | React.TouchEvent) => {
      if (!isDrawing || readOnly) return
      e.preventDefault()
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return

      const pos = getPos(e)
      const lastPos = lastPosRef.current
      if (!lastPos) return

      ctx.strokeStyle = currentColor
      ctx.lineWidth = currentWidth
      ctx.beginPath()
      ctx.moveTo(lastPos.x, lastPos.y)
      ctx.lineTo(pos.x, pos.y)
      ctx.stroke()

      lastPosRef.current = pos
    }

    const endDraw = () => {
      setIsDrawing(false)
      lastPosRef.current = null
    }

    const clearCanvas = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      saveState()
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
    }

    const undo = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      if (historyRef.current.length === 0) return
      const prevState = historyRef.current.pop()
      if (prevState) {
        ctx.putImageData(prevState, 0, 0)
      }
    }

    const save = (): string | null => {
      const canvas = canvasRef.current
      if (!canvas) return null
      if (historyRef.current.length === 0) return null
      const dataUrl = canvas.toDataURL('image/png')
      onSave?.(dataUrl)
      return dataUrl
    }

    return (
      <div>
        <div
          style={{
            border: '1px solid #d9d9d9',
            borderRadius: 8,
            overflow: 'hidden',
            width: '100%',
            maxWidth: width,
            position: 'relative'
          }}
        >
          <canvas
            ref={canvasRef}
            width={width}
            height={height}
            style={{
              display: 'block',
              width: '100%',
              cursor: readOnly ? 'default' : 'crosshair',
              touchAction: 'none'
            }}
            onMouseDown={startDraw}
            onMouseMove={draw}
            onMouseUp={endDraw}
            onMouseLeave={endDraw}
            onTouchStart={startDraw}
            onTouchMove={draw}
            onTouchEnd={endDraw}
          />
        </div>
        {!readOnly && (
          <Space style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between' }}>
            <Space>
              <ColorPicker
                value={currentColor}
                onChange={(color) => setCurrentColor(color.toHexString())}
                showText={false}
              />
              <span style={{ fontSize: 12, color: '#666' }}>粗细:</span>
              <input
                type="range"
                min={1}
                max={10}
                value={currentWidth}
                onChange={(e) => setCurrentWidth(Number(e.target.value))}
                style={{ width: 100 }}
              />
              <span style={{ fontSize: 12, color: '#666' }}>{currentWidth}px</span>
            </Space>
            <Space>
              <Button size="small" icon={<UndoOutlined />} onClick={undo}>
                撤销
              </Button>
              <Button size="small" icon={<ClearOutlined />} onClick={clearCanvas}>
                清空
              </Button>
              <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => save()}>
                保存签名
              </Button>
            </Space>
          </Space>
        )}
      </div>
    )
  }
)

SignaturePad.displayName = 'SignaturePad'

export default SignaturePad
