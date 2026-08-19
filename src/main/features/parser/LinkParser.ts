import { ParsedLink, Platform, ContentType } from '../../../shared/types'
import crypto from 'crypto'

export class LinkParser {
  private static readonly YOUTUBE_VIDEO_REGEX = /https?:\/\/(?:www\.)?youtube\.com\/watch\?(?:.*?[&])?v=([a-zA-Z0-9_-]{11})/i
  private static readonly YOUTUBE_SHORT_REGEX = /https?:\/\/(?:www\.)?youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})/i
  private static readonly YOUTU_BE_REGEX = /https?:\/\/youtu\.be\/([a-zA-Z0-9_-]{11})/i
  private static readonly YOUTUBE_PLAYLIST_REGEX = /https?:\/\/(?:www\.)?youtube\.com\/playlist\?(?:.*?[&])?list=([a-zA-Z0-9_-]+)/i
  private static readonly YOUTUBE_MUSIC_REGEX = /https?:\/\/music\.youtube\.com\/watch\?(?:.*?[&])?v=([a-zA-Z0-9_-]{11})/i
  private static readonly YOUTUBE_MUSIC_PLAYLIST_REGEX = /https?:\/\/music\.youtube\.com\/playlist\?(?:.*?[&])?list=([a-zA-Z0-9_-]+)/i
  private static readonly YOUTUBE_MUSIC_ALBUM_REGEX = /https?:\/\/music\.youtube\.com\/(?:album|browse)\/([a-zA-Z0-9_-]+)/i
  private static readonly YOUTUBE_CHANNEL_REGEX = /https?:\/\/(?:music\.|www\.)?youtube\.com\/(?:@|channel\/|c\/)([a-zA-Z0-9_.-]+)/i
  private static readonly PLAYLIST_PARAM_REGEX = /[?&]list=([a-zA-Z0-9_-]+)/i

  /**
   * Parses an input string (URL or search query) into a ParsedLink object.
   */
  public static parse(inputUrl: string): ParsedLink | null {
    const trimmed = (inputUrl || '').trim()
    if (!trimmed) return null

    // 1. YouTube / YouTube Music Playlist (Check query param first)
    const playlistMatch = trimmed.match(this.PLAYLIST_PARAM_REGEX)
    if (playlistMatch && playlistMatch[1]) {
      const isMusic = trimmed.includes('music.youtube.com')
      return {
        platform: isMusic ? 'youtube_music' : 'youtube',
        contentType: 'playlist',
        id: playlistMatch[1],
        originalUrl: trimmed
      }
    }

    // 2. YouTube Music Album / Playlist
    const ytMusicPlaylistMatch = trimmed.match(this.YOUTUBE_MUSIC_PLAYLIST_REGEX)
    if (ytMusicPlaylistMatch && ytMusicPlaylistMatch[1]) {
      return {
        platform: 'youtube_music',
        contentType: 'playlist',
        id: ytMusicPlaylistMatch[1],
        originalUrl: trimmed
      }
    }

    const ytMusicAlbumMatch = trimmed.match(this.YOUTUBE_MUSIC_ALBUM_REGEX)
    if (ytMusicAlbumMatch && ytMusicAlbumMatch[1]) {
      return {
        platform: 'youtube_music',
        contentType: 'album',
        id: ytMusicAlbumMatch[1],
        originalUrl: trimmed
      }
    }

    // 3. YouTube Music Track
    const ytMusicTrackMatch = trimmed.match(this.YOUTUBE_MUSIC_REGEX)
    if (ytMusicTrackMatch && ytMusicTrackMatch[1]) {
      return {
        platform: 'youtube_music',
        contentType: 'track',
        id: ytMusicTrackMatch[1],
        originalUrl: trimmed
      }
    }

    // 4. Standard YouTube Playlist
    const ytPlaylistMatch = trimmed.match(this.YOUTUBE_PLAYLIST_REGEX)
    if (ytPlaylistMatch && ytPlaylistMatch[1]) {
      return {
        platform: 'youtube',
        contentType: 'playlist',
        id: ytPlaylistMatch[1],
        originalUrl: trimmed
      }
    }

    // 5. YouTube Shorts / Watch / youtu.be
    const ytShortMatch = trimmed.match(this.YOUTUBE_SHORT_REGEX)
    if (ytShortMatch && ytShortMatch[1]) {
      return {
        platform: 'youtube',
        contentType: 'video',
        id: ytShortMatch[1],
        originalUrl: trimmed
      }
    }

    const youtuBeMatch = trimmed.match(this.YOUTU_BE_REGEX)
    if (youtuBeMatch && youtuBeMatch[1]) {
      return {
        platform: 'youtube',
        contentType: 'video',
        id: youtuBeMatch[1],
        originalUrl: trimmed
      }
    }

    const ytVideoMatch = trimmed.match(this.YOUTUBE_VIDEO_REGEX)
    if (ytVideoMatch && ytVideoMatch[1]) {
      return {
        platform: 'youtube',
        contentType: 'video',
        id: ytVideoMatch[1],
        originalUrl: trimmed
      }
    }

    // 6. YouTube Channel / Artist
    const ytChannelMatch = trimmed.match(this.YOUTUBE_CHANNEL_REGEX)
    if (ytChannelMatch && ytChannelMatch[1]) {
      const isMusic = trimmed.includes('music.youtube.com')
      return {
        platform: isMusic ? 'youtube_music' : 'youtube',
        contentType: 'playlist',
        id: ytChannelMatch[1],
        originalUrl: trimmed
      }
    }

    // 7. Generic URL fallback
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      const hashId = crypto.createHash('md5').update(trimmed).digest('hex').substring(0, 12)
      return {
        platform: 'other',
        contentType: 'video',
        id: hashId,
        originalUrl: trimmed
      }
    }

    // 8. Plain Text Artist / Query search fallback
    return {
      platform: 'youtube',
      contentType: 'playlist',
      id: trimmed,
      originalUrl: `ytsearch150:${trimmed} music.youtube.com`
    }
  }

  public static isSupported(url: string): boolean {
    return this.parse(url) !== null
  }
}
