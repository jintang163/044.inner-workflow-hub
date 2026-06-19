declare module '@antv/g6' {
  interface GraphConfig {
    container: HTMLElement | string
    width?: number
    height?: number
    modes?: Record<string, string[]>
    layout?: Record<string, any>
    defaultNode?: Record<string, any>
    defaultEdge?: Record<string, any>
    nodeStateStyles?: Record<string, any>
    fitView?: boolean
    fitViewPadding?: number | number[]
    animate?: boolean
    animateCfg?: Record<string, any>
  }

  class Graph {
    constructor(config: GraphConfig)
    data(data: { nodes: any[]; edges: any[] }): void
    render(): void
    destroy(): void
    on(eventName: string, callback: (evt: any) => void): void
    getZoom(): number
    zoomTo(ratio: number, center?: object): void
    fitView(padding?: number | number[]): void
    setItemState(item: any, state: string, value: boolean): void
    changeSize(width: number, height: number): void
    get(key: string): any
  }

  export default { Graph }
}
