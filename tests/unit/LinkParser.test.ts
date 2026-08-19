import { LinkParser } from '../../src/main/features/parser/LinkParser'

describe('LinkParser', () => {
  test('correctly identifies YouTube video URL', () => {
    const parsed = LinkParser.parse('https://www.youtube.com/watch?v=dQw4w9WgXcQ')
    expect(parsed.platform).toBe('youtube')
    expect(parsed.contentType).toBe('video')
    expect(parsed.id).toBe('dQw4w9WgXcQ')
  })

  test('correctly identifies YouTube Music playlist URL', () => {
    const parsed = LinkParser.parse('https://music.youtube.com/playlist?list=PL123456789')
    expect(parsed.platform).toBe('youtube_music')
    expect(parsed.contentType).toBe('playlist')
    expect(parsed.id).toBe('PL123456789')
  })

  test('falls back to search query for plain text input', () => {
    const parsed = LinkParser.parse('The Weeknd Starboy')
    expect(parsed.platform).toBe('youtube')
    expect(parsed.contentType).toBe('track')
    expect(parsed.originalUrl).toBe('ytsearch1:The Weeknd Starboy')
  })
})
