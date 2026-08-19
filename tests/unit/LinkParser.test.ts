import { describe, test, expect } from 'vitest'
import { LinkParser } from '../../src/main/features/parser/LinkParser'

describe('LinkParser', () => {
  test('correctly identifies YouTube video URL', () => {
    const parsed = LinkParser.parse('https://www.youtube.com/watch?v=dQw4w9WgXcQ')
    expect(parsed).not.toBeNull()
    expect(parsed!.platform).toBe('youtube')
    expect(parsed!.contentType).toBe('video')
    expect(parsed!.id).toBe('dQw4w9WgXcQ')
  })

  test('correctly identifies youtu.be shortlinks', () => {
    const parsed = LinkParser.parse('https://youtu.be/dQw4w9WgXcQ')
    expect(parsed).not.toBeNull()
    expect(parsed!.platform).toBe('youtube')
    expect(parsed!.contentType).toBe('video')
    expect(parsed!.id).toBe('dQw4w9WgXcQ')
  })

  test('correctly identifies YouTube Music playlist URL', () => {
    const parsed = LinkParser.parse('https://music.youtube.com/playlist?list=PL123456789')
    expect(parsed).not.toBeNull()
    expect(parsed!.platform).toBe('youtube_music')
    expect(parsed!.contentType).toBe('playlist')
    expect(parsed!.id).toBe('PL123456789')
  })

  test('correctly identifies YouTube Music album URL', () => {
    const parsed = LinkParser.parse('https://music.youtube.com/album/OLAK5uy_sampleAlbum')
    expect(parsed).not.toBeNull()
    expect(parsed!.platform).toBe('youtube_music')
    expect(parsed!.contentType).toBe('album')
    expect(parsed!.id).toBe('OLAK5uy_sampleAlbum')
  })

  test('falls back to search query for plain text input', () => {
    const parsed = LinkParser.parse('The Weeknd Starboy')
    expect(parsed).not.toBeNull()
    expect(parsed!.platform).toBe('youtube')
    expect(parsed!.contentType).toBe('playlist')
    expect(parsed!.id).toBe('The Weeknd Starboy')
    expect(parsed!.originalUrl).toBe('ytsearch150:The Weeknd Starboy music.youtube.com')
  })
})
