import { ref } from 'vue'

export const overflowCellRuntime = {
  activeKey: ref(''),
  listenersReady: false,
  mountedCount: 0
}
