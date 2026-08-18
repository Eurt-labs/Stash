declare module 'node-id3' {
  export interface Tags {
    title?: string
    artist?: string
    album?: string
    year?: string
    trackNumber?: string
    genre?: string
    image?: {
      mime: string
      type: { id: number; name: string }
      description: string
      imageBuffer: Buffer
    }
    [key: string]: any
  }

  export function write(tags: Tags, filebuffer: Buffer | string): boolean
  export function create(tags: Tags): Buffer
  export function read(filebuffer: Buffer | string): Tags
  export function update(tags: Tags, filebuffer: Buffer | string): boolean
  export function removeTags(filepath: string): boolean

  export default {
    write,
    create,
    read,
    update,
    removeTags
  }
}
