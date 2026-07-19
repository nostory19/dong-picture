/**
 * OfflineStore — 基于 IndexedDB 的离线操作队列。
 *
 * 当 WebSocket 断开时，CRDT 操作先缓存到 IndexedDB，
 * 重连后通过 SYNC_STEP2 批量提交到服务端。
 *
 * 使用方式：
 *   const store = new OfflineStore()
 *   await store.enqueue(pictureId, { type: 'OPERATION', field: 'rotate', newValue: '90' })
 *   const pending = await store.getPending(pictureId)
 *   await store.ack(pictureId, seq)
 */

const DB_NAME = 'collab-offline'
const DB_VERSION = 1
const STORE_NAME = 'pendingOps'

interface PendingOp {
  id?: number
  pictureId: number
  operation: unknown
  timestamp: number
}

export class OfflineStore {
  private db: IDBDatabase | null = null
  private dbReady: Promise<void>

  constructor() {
    this.dbReady = this.openDB()
  }

  /** 缓存操作到离线队列 */
  async enqueue(pictureId: number, operation: unknown): Promise<void> {
    await this.dbReady
    const db = this.db
    if (!db) return

    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const record: PendingOp = {
        pictureId,
        operation,
        timestamp: Date.now(),
      }
      const request = store.add(record)
      request.onsuccess = () => resolve()
      request.onerror = () => reject(request.error)
    })
  }

  /** 获取指定图片的所有待同步操作 */
  async getPending(pictureId: number): Promise<unknown[]> {
    await this.dbReady
    const db = this.db
    if (!db) return []

    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const index = store.index('pictureId')
      const request = index.getAll(pictureId)
      request.onsuccess = () => {
        const records = request.result as PendingOp[]
        resolve(records.sort((a, b) => a.timestamp - b.timestamp).map(r => r.operation))
      }
      request.onerror = () => reject(request.error)
    })
  }

  /** 确认操作已同步，从队列中删除 */
  async ack(pictureId: number, seq: number): Promise<void> {
    await this.dbReady
    const db = this.db
    if (!db) return

    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const index = store.index('pictureId')
      const request = index.openCursor(pictureId)
      request.onsuccess = () => {
        const cursor = request.result
        if (cursor) {
          const record = cursor.value as PendingOp
          if (record.timestamp <= seq) {
            cursor.delete()
          }
          cursor.continue()
        } else {
          resolve()
        }
      }
      request.onerror = () => reject(request.error)
    })
  }

  /** 清空指定图片的离线队列 */
  async clear(pictureId: number): Promise<void> {
    await this.dbReady
    const db = this.db
    if (!db) return

    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const index = store.index('pictureId')
      const request = index.openCursor(pictureId)
      request.onsuccess = () => {
        const cursor = request.result
        if (cursor) {
          cursor.delete()
          cursor.continue()
        } else {
          resolve()
        }
      }
      request.onerror = () => reject(request.error)
    })
  }

  /** 获取所有有待同步操作的图片 ID */
  async getPendingPictureIds(): Promise<number[]> {
    await this.dbReady
    const db = this.db
    if (!db) return []

    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const request = store.getAll()
      request.onsuccess = () => {
        const records = request.result as PendingOp[]
        const ids = [...new Set(records.map(r => r.pictureId))]
        resolve(ids)
      }
      request.onerror = () => reject(request.error)
    })
  }

  /** 打开 IndexedDB */
  private openDB(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION)

      request.onupgradeneeded = () => {
        const db = request.result
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          const store = db.createObjectStore(STORE_NAME, {
            keyPath: 'id',
            autoIncrement: true,
          })
          store.createIndex('pictureId', 'pictureId', { unique: false })
          store.createIndex('timestamp', 'timestamp', { unique: false })
        }
      }

      request.onsuccess = () => {
        this.db = request.result
        resolve()
      }

      request.onerror = () => reject(request.error)
    })
  }
}
